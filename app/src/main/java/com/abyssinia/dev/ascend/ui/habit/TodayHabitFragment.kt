package com.abyssinia.dev.ascend.ui.habit

import android.animation.Animator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.abyssinia.dev.ascend.R
import androidx.recyclerview.widget.LinearLayoutManager
import com.abyssinia.dev.ascend.databinding.FragmentTodayHabitBinding
import com.abyssinia.dev.ascend.util.TipsManager
import com.abyssinia.dev.ascend.viewmodel.HabitViewModel
import com.abyssinia.dev.ascend.viewmodel.HabitViewModelFactory
import com.abyssinia.dev.ascend.data.database.AppDatabase
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import java.util.*
import kotlinx.coroutines.launch

class TodayHabitFragment : Fragment() {

    private var _binding: FragmentTodayHabitBinding? = null
    private val binding get() = _binding!!

    private lateinit var todayHabitAdapter: HabitAdapter
    private lateinit var tipsManager: TipsManager
    private lateinit var habitAdView: AdView // ✅ AdView for banner

    private val habitViewModel: HabitViewModel by viewModels {
        HabitViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayHabitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tipsManager = TipsManager(requireActivity())

        val appDatabase = AppDatabase.getDatabase(requireContext())
        val habitCheckInDao = appDatabase.habitCheckInDao()

        todayHabitAdapter = HabitAdapter(
            habitCheckInDao = habitCheckInDao,
            lifecycleScope = viewLifecycleOwner.lifecycleScope,
            onHabitClick = { habit ->
                val holderView = binding.recyclerTodayHabits.findViewHolderForAdapterPosition(
                    todayHabitAdapter.currentList.indexOf(habit)
                )?.itemView
                holderView?.animate()?.translationZ(20f)?.setDuration(150)?.withEndAction {
                    holderView.animate().translationZ(0f).duration = 150
                }

                Toast.makeText(requireContext(), "Habit clicked: ${habit.name}", Toast.LENGTH_SHORT).show()
            },
            onCheckInClick = { habit ->
                habitViewModel.checkInHabit(habit) { success, newStreak ->
                    requireActivity().runOnUiThread {
                        if (success) {
                            playConfettiAnimation()
                            Toast.makeText(requireContext(), "Checked in! Streak: $newStreak", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Already checked in today", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onDeleteClick = { habit ->
                habitViewModel.delete(habit)
                Toast.makeText(requireContext(), "Habit deleted", Toast.LENGTH_SHORT).show()
            },
            onUndoCheckInClick = { habit ->
                habitViewModel.undoTodayCheckIn(habit) { success ->
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            if (success) "Today's check-in undone!" else "Nothing to undo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )

        binding.recyclerTodayHabits.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = todayHabitAdapter
            layoutAnimation = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_slide_fade_in)
        }

        // ✅ Initialize AdView
        habitAdView = binding.adView
        val adRequest = AdRequest.Builder().build()
        habitAdView.loadAd(adRequest)

        habitViewModel.allHabits.observe(viewLifecycleOwner) { habits ->
            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayDayOfWeek = todayCal.get(Calendar.DAY_OF_WEEK)
            val todayDayOfMonth = todayCal.get(Calendar.DAY_OF_MONTH)

            val todayHabits = habits.filter { habit ->
                val startCal = habit.startDate?.let {
                    Calendar.getInstance().apply { timeInMillis = it }
                } ?: todayCal

                when (habit.frequency.lowercase()) {
                    "daily" -> true
                    "weekly" -> startCal.get(Calendar.DAY_OF_WEEK) == todayDayOfWeek
                    "monthly" -> startCal.get(Calendar.DAY_OF_MONTH) == todayDayOfMonth
                    else -> false
                }
            }

            todayHabitAdapter.submitList(todayHabits) {
                binding.recyclerTodayHabits.scheduleLayoutAnimation()
            }

            if (todayHabits.isEmpty()) {
                binding.recyclerTodayHabits.visibility = View.GONE
                binding.lottieEmpty.apply {
                    visibility = View.VISIBLE
                    setAnimation("empty.json")
                    playAnimation()
                }
            } else {
                binding.recyclerTodayHabits.visibility = View.VISIBLE
                binding.lottieEmpty.visibility = View.GONE
            }
        }
    }

    private fun playConfettiAnimation() {
        val confettiView = LottieAnimationView(requireContext()).apply {
            setAnimation("confetti.json")
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            repeatCount = 0
        }
        (binding.root as ViewGroup).addView(confettiView)
        confettiView.playAnimation()
        confettiView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                (binding.root as ViewGroup).removeView(confettiView)
            }
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    // ✅ Proper Ad lifecycle
    override fun onPause() {
        habitAdView.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        habitAdView.resume()
    }

    override fun onDestroyView() {
        habitAdView.destroy()
        super.onDestroyView()
        _binding = null
    }
}
