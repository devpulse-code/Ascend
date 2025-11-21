package com.abyssinia.dev.ascend.ui.habit

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abyssinia.dev.ascend.R
import com.abyssinia.dev.ascend.data.database.AppDatabase
import com.abyssinia.dev.ascend.databinding.FragmentHabitListBinding
import com.abyssinia.dev.ascend.util.TipsManager
import com.abyssinia.dev.ascend.viewmodel.HabitViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
class HabitListFragment : Fragment() {

    private var _binding: FragmentHabitListBinding? = null
    private val binding get() = _binding!!

    private val habitViewModel: HabitViewModel by viewModels {
        HabitViewModelFactory(requireActivity().application)
    }

    private lateinit var habitAdapter: HabitAdapter
    private lateinit var tipsManager: TipsManager

    private lateinit var habitAdView: AdView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tipsManager = TipsManager(requireActivity())

        setupRecyclerView()
        observeHabits()
        setupFab()

        habitAdView = view.findViewById(R.id.habitAdView)
        val adRequest = AdRequest.Builder().build()
        habitAdView.loadAd(adRequest)
    }

    private fun setupRecyclerView() {
        val appDatabase = AppDatabase.getDatabase(requireContext())
        habitAdapter = HabitAdapter(
            habitCheckInDao = appDatabase.habitCheckInDao(),
            lifecycleScope = viewLifecycleOwner.lifecycleScope,
            onHabitClick = { habit ->
                Toast.makeText(requireContext(), "Habit clicked: ${habit.name}", Toast.LENGTH_SHORT).show()
            },
            onCheckInClick = { habit ->
                habitViewModel.checkInHabit(habit) { success, streak ->
                    val msg = if (success) "Checked in! Streak: $streak" else "Already checked in today"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            },
            onDeleteClick = { habit ->
                habitViewModel.delete(habit)
                Toast.makeText(requireContext(), "Habit deleted", Toast.LENGTH_SHORT).show()
            },
            onUndoCheckInClick = { habit ->
                habitViewModel.undoTodayCheckIn(habit) { success ->
                    val msg = if (success) "Today's check-in undone!" else "Nothing to undo"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.recyclerViewHabits.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = habitAdapter
        }
    }

    private fun observeHabits() {
        habitViewModel.allHabits.observe(viewLifecycleOwner) { habits ->
            habitAdapter.submitList(habits)
            binding.recyclerViewHabits.post { animateRecyclerItems(binding.recyclerViewHabits) }

            if (habits.isEmpty()) {
                binding.animationEmptyHabits.visibility = View.VISIBLE
                binding.recyclerViewHabits.visibility = View.GONE
            } else {
                binding.animationEmptyHabits.visibility = View.GONE
                binding.recyclerViewHabits.visibility = View.VISIBLE
            }
        }
    }

    private fun setupFab() {
        binding.fabAddHabit.setOnClickListener {
            findNavController().navigate(
                HabitListFragmentDirections.actionHabitListFragmentToAddEditHabitFragment(-1)
            )
        }

        startFabPulseAnimation()
    }

    private fun startFabPulseAnimation() {
        val scaleUpX = ObjectAnimator.ofFloat(binding.fabAddHabit, "scaleX", 1f, 1.2f)
        val scaleUpY = ObjectAnimator.ofFloat(binding.fabAddHabit, "scaleY", 1f, 1.2f)
        val scaleDownX = ObjectAnimator.ofFloat(binding.fabAddHabit, "scaleX", 1.2f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(binding.fabAddHabit, "scaleY", 1.2f, 1f)

        val pulse = AnimatorSet().apply {
            playTogether(scaleUpX, scaleUpY)
            duration = 500
            interpolator = DecelerateInterpolator()
        }

        val pulseReverse = AnimatorSet().apply {
            playTogether(scaleDownX, scaleDownY)
            duration = 900
            interpolator = DecelerateInterpolator()
        }

        // Declare first
        val fullPulse = AnimatorSet()

        fullPulse.playSequentially(pulse, pulseReverse)
        fullPulse.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                // Loop the pulse safely
                fullPulse.start()
            }
        })

        fullPulse.start()
    }


    private fun animateRecyclerItems(recyclerView: RecyclerView) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val first = layoutManager.findFirstVisibleItemPosition()
        val last = layoutManager.findLastVisibleItemPosition()

        for (i in first..last) {
            val holder = recyclerView.findViewHolderForAdapterPosition(i) ?: continue
            val animatorSet = AnimatorSet()
            val translationY = ObjectAnimator.ofFloat(holder.itemView, "translationY", 100f, 0f)
            val alpha = ObjectAnimator.ofFloat(holder.itemView, "alpha", 0f, 1f)
            animatorSet.playTogether(translationY, alpha)
            animatorSet.duration = 400
            animatorSet.startDelay = ((i - first) * 50).toLong()
            animatorSet.interpolator = DecelerateInterpolator()
            animatorSet.start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class HabitViewModelFactory(private val application: android.app.Application) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HabitViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}
