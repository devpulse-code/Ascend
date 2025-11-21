package com.abyssinia.dev.ascend.ui.settings

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.abyssinia.dev.ascend.R
import com.abyssinia.dev.ascend.data.database.AppDatabase
import com.abyssinia.dev.ascend.databinding.FragmentSettingsBinding
import com.abyssinia.dev.ascend.preferences.ReminderPreferences
import com.abyssinia.dev.ascend.preferences.ThemePreferences
import com.abyssinia.dev.ascend.util.ReminderHelper
import com.abyssinia.dev.ascend.viewmodel.ThemeViewModel
import com.abyssinia.dev.ascend.viewmodel.ThemeViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var themeViewModel: ThemeViewModel
    private lateinit var themePrefs: ThemePreferences
    private lateinit var reminderPrefs: ReminderPreferences

    private var updatingUi = true // Block programmatic changes

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        themePrefs = ThemePreferences(requireContext().applicationContext)
        reminderPrefs = ReminderPreferences(requireContext().applicationContext)

        val factory = ThemeViewModelFactory(themePrefs)
        themeViewModel = ViewModelProvider(requireActivity(), factory)[ThemeViewModel::class.java]

        animateCardEntry()
        observeThemeSwitch()
        observeReminderSwitches()
        setupListeners()
        displayAppVersion()

        // Only after everything initialized, allow user actions to trigger
        view.post { updatingUi = false }
    }

    /** Animate CardViews sequentially */
    private fun animateCardEntry() {
        val cards = listOf(
            binding.cardAppearance,
            binding.cardNotifications,
            binding.buttonPrivacyPolicy,
            binding.cardAccountData,
            binding.cardAbout
        )

        cards.forEachIndexed { index, card ->
            card.translationY = 100f
            card.alpha = 0f
            card.post {
                val slideAnim = ObjectAnimator.ofFloat(card, "translationY", 100f, 0f)
                val fadeAnim = ObjectAnimator.ofFloat(card, "alpha", 0f, 1f)
                val set = AnimatorSet()
                set.playTogether(slideAnim, fadeAnim)
                set.startDelay = (index * 100).toLong()
                set.duration = 300
                set.start()
            }
        }
    }

    private fun observeThemeSwitch() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                themeViewModel.isDarkThemeFlow.collect { isDark ->
                    updatingUi = true
                    binding.switchTheme.isChecked = isDark
                    val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                    if (AppCompatDelegate.getDefaultNightMode() != mode) {
                        AppCompatDelegate.setDefaultNightMode(mode)
                    }
                    updatingUi = false
                }
            }
        }
    }

    private fun observeReminderSwitches() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                reminderPrefs.isHabitReminderEnabled.collect { enabled ->
                    updatingUi = true
                    binding.switchHabitReminder.isChecked = enabled
                    updatingUi = false
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                reminderPrefs.isTaskReminderEnabled.collect { enabled ->
                    updatingUi = true
                    binding.switchTaskReminder.isChecked = enabled
                    updatingUi = false
                }
            }
        }
    }

    private fun setupListeners() {
        // Theme toggle
        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            if (updatingUi) return@setOnCheckedChangeListener
            animateSwitch(binding.switchTheme)
            lifecycleScope.launch { themeViewModel.setDarkTheme(isChecked) }
        }

        // Privacy Policy button
        binding.buttonPrivacyPolicy.setOnClickListener {
            animateButton(binding.buttonPrivacyPolicy)
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://nigus-lab.github.io/Site/Ascend/ascend_privacy.html")
            )
            startActivity(intent)
        }

        // Habit reminder toggle
        binding.switchHabitReminder.setOnCheckedChangeListener { _, isChecked ->
            if (updatingUi) return@setOnCheckedChangeListener
            animateSwitch(binding.switchHabitReminder)
            handleReminderSwitch(
                isChecked,
                requestCode = 1,
                message = "Don't forget your habits today!",
                getHour = { reminderPrefs.habitReminderHour },
                getMinute = { reminderPrefs.habitReminderMinute },
                saveHourMinute = { h, m -> reminderPrefs.setHabitReminderTime(h, m) },
                setEnabledPref = { enabled -> reminderPrefs.setHabitReminderEnabled(enabled) },
                scheduleReminder = { h, m -> ReminderHelper.scheduleHabitReminder(requireContext(), h, m) },
                cancelReminder = { ReminderHelper.cancelHabitReminder(requireContext()) }
            )
        }

        // Task reminder toggle
        binding.switchTaskReminder.setOnCheckedChangeListener { _, isChecked ->
            if (updatingUi) return@setOnCheckedChangeListener
            animateSwitch(binding.switchTaskReminder)
            handleReminderSwitch(
                isChecked,
                requestCode = 2,
                message = "Don't forget your tasks today!",
                getHour = { reminderPrefs.taskReminderHour },
                getMinute = { reminderPrefs.taskReminderMinute },
                saveHourMinute = { h, m -> reminderPrefs.setTaskReminderTime(h, m) },
                setEnabledPref = { enabled -> reminderPrefs.setTaskReminderEnabled(enabled) },
                scheduleReminder = { h, m -> ReminderHelper.scheduleTaskReminder(requireContext(), h, m) },
                cancelReminder = { ReminderHelper.cancelTaskReminder(requireContext()) }
            )
        }
        // Clear data button
        binding.buttonClearData.setOnClickListener {
            animateButton(binding.buttonClearData)
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(requireContext()).clearAllTables()
                }
                themePrefs.clearAll()
                reminderPrefs.clearAll()
                ReminderHelper.cancelHabitReminder(requireContext())
                ReminderHelper.cancelTaskReminder(requireContext())
                Toast.makeText(requireContext(), "All data cleared", Toast.LENGTH_SHORT).show()
            }
        }

        // Export/Backup button
        binding.buttonExportData.setOnClickListener {
            animateButton(binding.buttonExportData)
            Toast.makeText(requireContext(), "More features are upcoming, stay tuned.", Toast.LENGTH_LONG).show()
        }
    }

    private fun animateSwitch(switch: View) {
        val scaleUpX = ObjectAnimator.ofFloat(switch, "scaleX", 1f, 1.1f)
        val scaleUpY = ObjectAnimator.ofFloat(switch, "scaleY", 1f, 1.1f)
        val scaleDownX = ObjectAnimator.ofFloat(switch, "scaleX", 1.1f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(switch, "scaleY", 1.1f, 1f)
        val set = AnimatorSet()
        set.play(scaleUpX).with(scaleUpY)
        set.play(scaleDownX).with(scaleDownY).after(scaleUpX)
        set.duration = 100
        set.start()
    }

    private fun animateButton(button: View) {
        val scaleUpX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 1.05f)
        val scaleUpY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 1.05f)
        val scaleDownX = ObjectAnimator.ofFloat(button, "scaleX", 1.05f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(button, "scaleY", 1.05f, 1f)
        val set = AnimatorSet()
        set.play(scaleUpX).with(scaleUpY)
        set.play(scaleDownX).with(scaleDownY).after(scaleUpX)
        set.duration = 100
        set.start()
    }

    private fun displayAppVersion() {
        binding.textAppVersion.text =
            "App version: ${requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName}"
    }

    private fun handleReminderSwitch(
        isChecked: Boolean,
        requestCode: Int,
        message: String,
        getHour: () -> kotlinx.coroutines.flow.Flow<Int>,
        getMinute: () -> kotlinx.coroutines.flow.Flow<Int>,
        saveHourMinute: suspend (Int, Int) -> Unit,
        setEnabledPref: suspend (Boolean) -> Unit,
        scheduleReminder: suspend (Int, Int) -> Unit,  // suspend
        cancelReminder: suspend () -> Unit            // suspend
    ) {
        if (isChecked) {
            if (!ensureExactAlarmAllowed()) {
                updatingUi = true
                if (requestCode == 1) binding.switchHabitReminder.isChecked = false
                else binding.switchTaskReminder.isChecked = false
                lifecycleScope.launch { setEnabledPref(false) }
                view?.post { updatingUi = false }
                return
            }

            lifecycleScope.launch {
                val hour = getHour().first()
                val minute = getMinute().first()

                showTimePicker(hour, minute) { h, m ->
                    lifecycleScope.launch {
                        saveHourMinute(h, m)
                        setEnabledPref(true)
                        scheduleReminder(h, m)   // call inside coroutine
                        Toast.makeText(requireContext(), "Reminder set", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            lifecycleScope.launch {
                setEnabledPref(false)
                cancelReminder()   // call inside coroutine
                Toast.makeText(requireContext(), "Reminder cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun showTimePicker(hour: Int, minute: Int, onTimeSet: (Int, Int) -> Unit) {
        TimePickerDialog(requireContext(), { _, h, m -> onTimeSet(h, m) }, hour, minute, true).show()
    }

    private fun ensureExactAlarmAllowed(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val allowed = am.canScheduleExactAlarms()
        if (!allowed) {
            Toast.makeText(
                requireContext(),
                "Enable 'Exact alarms' for Ascend to use reminders.",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            }
            runCatching { startActivity(intent) }
        }
        return allowed
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
