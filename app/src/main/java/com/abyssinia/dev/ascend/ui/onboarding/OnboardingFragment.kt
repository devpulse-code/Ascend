package com.abyssinia.dev.ascend.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.abyssinia.dev.ascend.R
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.abyssinia.dev.ascend.preferences.OnboardingPreferences

class OnboardingFragment : Fragment() {

    private lateinit var onboardingAdapter: OnboardingAdapter
    private lateinit var viewPager: ViewPager2
    private lateinit var indicatorsLayout: LinearLayout
    private lateinit var actionButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_onboarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById(R.id.viewPagerOnboarding)
        indicatorsLayout = view.findViewById(R.id.layoutOnboardingIndicators)
        actionButton = view.findViewById(R.id.buttonOnboardingAction)

        // List of onboarding pages
        val onboardingItems = listOf(
            OnboardingItem(
                R.drawable.img_habit,
                "Build Habits",
                "Track your daily habits and grow consistently."
            ),
            OnboardingItem(
                R.drawable.img_task,
                "Manage Tasks",
                "Organize tasks efficiently and boost productivity."
            ),
            OnboardingItem(
                R.drawable.ic_habit,
                "Stay Motivated",
                "Achieve your goals with streaks and reminders."
            )
        )

        onboardingAdapter = OnboardingAdapter(onboardingItems)
        viewPager.adapter = onboardingAdapter

        setupIndicators(onboardingItems.size)
        setCurrentIndicator(0)

        // Page change listener
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)

                if (position == onboardingItems.lastIndex) {
                    actionButton.text = "Get Started"
                } else {
                    actionButton.text = "Next"
                }
            }
        })
        // Button click
        actionButton.setOnClickListener {
            if (viewPager.currentItem + 1 < onboardingAdapter.itemCount) {
                viewPager.currentItem += 1
            } else {
                // Save preference flag
                lifecycleScope.launch {
                    val prefs = OnboardingPreferences(requireContext())
                    prefs.setHasSeenOnboarding(true)

                    // Navigate to HomeFragment
                    findNavController().navigate(R.id.action_onboardingFragment_to_homeFragment)
                }
            }
        }

    }

    // ------------------------------
    // Indicator dots
    // ------------------------------
    private fun setupIndicators(count: Int) {
        val indicators = arrayOfNulls<ImageView>(count)
        val layoutParams: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        layoutParams.setMargins(8, 0, 8, 0)

        for (i in indicators.indices) {
            indicators[i] = ImageView(context)
            indicators[i]?.setImageResource(R.drawable.ic_circle_inactive)
            indicators[i]?.layoutParams = layoutParams
            indicatorsLayout.addView(indicators[i])
        }
    }

    private fun setCurrentIndicator(position: Int) {
        val childCount = indicatorsLayout.childCount
        for (i in 0 until childCount) {
            val imageView = indicatorsLayout.getChildAt(i) as ImageView
            if (i == position) {
                imageView.setImageResource(R.drawable.ic_circle_active)
            } else {
                imageView.setImageResource(R.drawable.ic_circle_inactive)
            }
        }
    }
}
