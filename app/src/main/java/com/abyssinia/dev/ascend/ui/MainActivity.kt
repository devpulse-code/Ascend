// MainActivity.kt
package com.abyssinia.dev.ascend.ui

import android.widget.Button
import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.abyssinia.dev.ascend.R
import com.abyssinia.dev.ascend.databinding.ActivityMainBinding
import com.abyssinia.dev.ascend.preferences.OnboardingPreferences
import com.abyssinia.dev.ascend.preferences.ReminderPreferences
import com.abyssinia.dev.ascend.preferences.ThemePreferences
import com.abyssinia.dev.ascend.util.ReminderHelper
import com.abyssinia.dev.ascend.viewmodel.ThemeViewModel
import com.abyssinia.dev.ascend.viewmodel.ThemeViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var themeViewModel: ThemeViewModel
    private lateinit var reminderPrefs: ReminderPreferences

    private var backPressedTime: Long = 0
    private val backPressInterval = 2000L // 2 seconds

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Notifications are disabled. Reminders won't work.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reminderPrefs = ReminderPreferences(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        // Core features
        checkAndRequestNotificationPermission()
        checkBatteryOptimizationWhitelist()
        initThemeViewModel()
        initNavController()
        lifecycleScope.launch { showOnboardingIfNeeded() }
        rescheduleReminders()
        removeBottomNavIndicator()
    }
    private fun removeBottomNavIndicator() {
        binding.bottomNavigationView.itemActiveIndicatorColor = null
        binding.bottomNavigationView.itemBackground = null
    }

    // ----------------------------
    // Battery optimization check
    // ----------------------------
    private fun checkBatteryOptimizationWhitelist() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = packageName
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                AlertDialog.Builder(this)
                    .setTitle("Whitelist Ascend")
                    .setMessage("To ensure reminders are delivered on time, please allow Ascend to ignore battery optimizations.")
                    .setPositiveButton("Allow") { _, _ ->
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            intent.data = Uri.parse("package:$packageName")
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(this, "Unable to open settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    // ----------------------------
    // Onboarding check
    // ----------------------------
    private suspend fun showOnboardingIfNeeded() {
        val prefs = OnboardingPreferences(this)
        val hasSeen = prefs.hasSeenOnboarding.first()
        if (!hasSeen) {
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navHostFragment.navController.navigate(R.id.onboardingFragment)
        }
    }

    override fun onBackPressed() {
        handleBackPressed()
    }

    private fun handleBackPressed() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()

        if (currentFragment is com.abyssinia.dev.ascend.ui.home.HomeFragment) {
            if (backPressedTime + backPressInterval > System.currentTimeMillis()) {
                showExitDialog()
            } else {
                Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
            backPressedTime = System.currentTimeMillis()
        } else {
            if (!navHostFragment?.childFragmentManager?.popBackStackImmediate()!!) {
                finish()
            }
        }
    }
    private var nativeAd: NativeAd? = null

    private fun showExitDialog() {
        // Inflate custom dialog layout
        val dialogView = layoutInflater.inflate(R.layout.exit, null)
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Buttons
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener { alertDialog.dismiss() }
        dialogView.findViewById<Button>(R.id.btnExit).setOnClickListener {
            alertDialog.dismiss()
            finish()
        }

        // Load Native Ad using the modern non-deprecated way
        val adLoader = AdLoader.Builder(this, "ca-app-pub-3940256099942544/2247696110") // Test ID
            .forNativeAd { ad: NativeAd ->
                // Destroy previous ad if any
                nativeAd?.destroy()
                nativeAd = ad

                // Populate the ad into your NativeAdView in the dialog layout
                val nativeAdView = dialogView.findViewById<NativeAdView>(R.id.nativeAdView)
                populateNativeAdView(ad, nativeAdView)
            }
            .withNativeAdOptions(NativeAdOptions.Builder().build()) // default options
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        alertDialog.show()
    }

    // Helper function to bind ad assets to NativeAdView
    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        adView.headlineView = adView.findViewById(R.id.adHeadline)
        adView.bodyView = adView.findViewById(R.id.adBody)

        (adView.headlineView as TextView).text = nativeAd.headline
        (adView.bodyView as TextView).text = nativeAd.body ?: ""

        adView.setNativeAd(nativeAd)
    }

    // Make sure to destroy the ad on activity destroy
    override fun onDestroy() {
        nativeAd?.destroy()
        super.onDestroy()
    }

    private fun initThemeViewModel() {
        val themePreferences = ThemePreferences(applicationContext)
        val factory = ThemeViewModelFactory(themePreferences)
        themeViewModel = ViewModelProvider(this, factory)[ThemeViewModel::class.java]

        lifecycleScope.launch {
            themeViewModel.isDarkThemeFlow.collect { isDark ->
                val mode =
                    if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                if (AppCompatDelegate.getDefaultNightMode() != mode) {
                    AppCompatDelegate.setDefaultNightMode(mode)
                }
            }
        }
    }

    private fun initNavController() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigationView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigationView.visibility =
                if (destination.id == R.id.onboardingFragment) View.GONE else View.VISIBLE
        }

        val menuView = binding.bottomNavigationView.getChildAt(0) as BottomNavigationMenuView
        disableClipping(menuView)

        binding.bottomNavigationView.post {
            animateSelectedState(menuView, binding.bottomNavigationView.selectedItemId)
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            animateSelectedState(menuView, item.itemId)
            when (item.itemId) {
                R.id.homeFragment -> navController.navigate(R.id.homeFragment)
                R.id.habitListFragment -> navController.navigate(R.id.habitListFragment)
                R.id.taskListFragment -> navController.navigate(R.id.taskListFragment)
                R.id.settingsFragment -> navController.navigate(R.id.settingsFragment)
            }
            return@setOnItemSelectedListener true
        }
    }

    private fun disableClipping(menuView: BottomNavigationMenuView) {
        binding.bottomNavigationView.clipToPadding = false
        binding.bottomNavigationView.clipChildren = false
        menuView.clipToPadding = false
        menuView.clipChildren = false
        for (i in 0 until menuView.childCount) {
            val child = menuView.getChildAt(i)
            if (child is ViewGroup) {
                child.clipToPadding = false
                child.clipChildren = false
            }
        }
    }

    private fun animateSelectedState(menuView: BottomNavigationMenuView, selectedItemId: Int) {
        for (i in 0 until menuView.childCount) {
            val itemView = menuView.getChildAt(i) as ViewGroup
            val menuItem = binding.bottomNavigationView.menu.getItem(i)
            val isSelected = menuItem.itemId == selectedItemId

            val iconView = itemView.findViewById<ImageView>(com.google.android.material.R.id.icon)
            val largeLabelM3 = itemView.findViewById<TextView>(
                com.google.android.material.R.id.navigation_bar_item_large_label_view
            )
            val smallLabelM3 = itemView.findViewById<TextView>(
                com.google.android.material.R.id.navigation_bar_item_small_label_view
            )

            val targetScale = if (isSelected) 1.4f else 1f
            val scaleX = ObjectAnimator.ofFloat(itemView, "scaleX", targetScale)
            val scaleY = ObjectAnimator.ofFloat(itemView, "scaleY", targetScale)
            val rotate = ObjectAnimator.ofFloat(iconView, "rotation", 0f, if (isSelected) 10f else 0f, 0f)
            val alpha = ObjectAnimator.ofFloat(iconView, "alpha", if (isSelected) 1f else 0.7f)

            AnimatorSet().apply {
                playTogether(scaleX, scaleY, rotate, alpha)
                duration = if (isSelected) 300 else 180
                interpolator = if (isSelected) OvershootInterpolator(1.2f) else null
                start()
            }

            val targetAlpha = if (isSelected) 0f else 1f
            listOf(largeLabelM3, smallLabelM3).forEach { label ->
                label?.animate()?.alpha(targetAlpha)?.setDuration(150)?.start()
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun rescheduleReminders() {
        lifecycleScope.launch {
            reminderPrefs.isHabitReminderEnabled.collect { enabled ->
                val (hour, minute) = reminderPrefs.getHabitReminderTime()
                if (enabled) {
                    ReminderHelper.scheduleHabitReminder(this@MainActivity, hour, minute)
                } else {
                    ReminderHelper.cancelHabitReminder(this@MainActivity)
                }
            }
        }

        lifecycleScope.launch {
            reminderPrefs.isTaskReminderEnabled.collect { enabled ->
                val (hour, minute) = reminderPrefs.getTaskReminderTime()
                if (enabled) {
                    ReminderHelper.scheduleTaskReminder(this@MainActivity, hour, minute)
                } else {
                    ReminderHelper.cancelTaskReminder(this@MainActivity)
                }
            }
        }
    }
}
