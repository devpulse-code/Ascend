package com.abyssinia.dev.ascend.ui.home

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.abyssinia.dev.ascend.R
import com.abyssinia.dev.ascend.data.model.Task
import com.abyssinia.dev.ascend.databinding.FragmentHomeBinding
import com.abyssinia.dev.ascend.viewmodel.HabitViewModel
import com.abyssinia.dev.ascend.viewmodel.TaskViewModel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.*
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val habitViewModel: HabitViewModel by viewModels {
        HabitViewModelFactory(requireActivity().application)
    }

    private val taskViewModel: TaskViewModel by activityViewModels {
        TaskViewModelFactory(requireActivity().application)
    }

    private var searchJob: Job? = null
    private var adViewBottom: AdView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe habits
        habitViewModel.allHabits.observe(viewLifecycleOwner) { habits ->
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val habitsLeft = habits.count { habit ->
                habit.lastCheckInDate?.let { it < todayStart } ?: true
            }

            updateBadge(binding.habitBadge, habitsLeft)
            animateCardSafe(binding.habitCheckIn)
        }

        // Observe tasks
        taskViewModel.allTasks.observe(viewLifecycleOwner) { tasks: List<Task> ->
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val todayEnd = todayStart + 24 * 60 * 60 * 1000 - 1

            val tasksLeft = tasks.count { task ->
                !task.isCompleted &&
                        task.dueDate?.let { it in todayStart..todayEnd } == true
            }

            updateBadge(binding.taskBadge, tasksLeft)
            animateCardSafe(binding.cardOneTime)
        }

        // Click navigation
        binding.habitCheckIn.setOnClickListener {
            animateCardClick(binding.habitCheckIn)
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToTodayHabitFragment())
        }

        binding.cardOneTime.setOnClickListener {
            animateCardClick(binding.cardOneTime)
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToTodayTaskFragment())
        }

        // Search functionality
        binding.searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) navigateToSearch("")
        }
        binding.searchEditText.setOnClickListener { navigateToSearch("") }
        binding.searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                searchJob?.cancel()
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    searchJob = CoroutineScope(Dispatchers.Main).launch {
                        delay(300)
                        navigateToSearch(query)
                    }
                }
            }
        })

        setupAdBanner()
    }

    private fun navigateToSearch(query: String) {
        try {
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToSearchResultsFragment(query)
            )
        } catch (_: IllegalArgumentException) {}
    }

    private fun updateBadge(badgeView: View, count: Int) {
        if (badgeView is TextView) {
            badgeView.isVisible = count > 0
            if (count > 0) {
                badgeView.text = count.toString()
                animateBadgeCount(badgeView)
            }
        }
    }

    private fun animateCardSafe(card: View) {
        card.translationY = 0f
        card.alpha = 1f
        ObjectAnimator.ofFloat(card, "alpha", 0f, 1f).apply {
            duration = 300
            start()
        }
    }

    private fun animateCardClick(card: View) {
        val up = ObjectAnimator.ofFloat(card, "translationZ", 12f).setDuration(100)
        val down = ObjectAnimator.ofFloat(card, "translationZ", 4f).setDuration(100)
        AnimatorSet().apply { playSequentially(up, down); start() }
    }

    private fun animateBadgeCount(badge: TextView) {
        val scaleX = ObjectAnimator.ofFloat(badge, "scaleX", 1f, 1.3f, 1f).setDuration(300)
        val scaleY = ObjectAnimator.ofFloat(badge, "scaleY", 1f, 1.3f, 1f).setDuration(300)
        AnimatorSet().apply { playTogether(scaleX, scaleY); start() }
    }

    // ---------------- AdMob Banner ----------------
    private fun setupAdBanner() {
        val adRequest = AdRequest.Builder().build()

        // Bottom banner
        adViewBottom = binding.homeAdViewTwo
        val adContainerBottom = binding.adContainerBottom
        adViewBottom?.apply {
            loadAd(adRequest)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    adContainerBottom.visibility = View.VISIBLE
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    adContainerBottom.visibility = View.GONE
                }
            }
        }

        // Hide bottom ad when keyboard shows
        val rootView = binding.root
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val keypadHeight = rootView.height - rect.bottom
            adContainerBottom.visibility =
                if (keypadHeight > rootView.height * 0.15) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        adViewBottom = null
        _binding = null
        super.onDestroyView()
    }
}

// ---------------- ViewModel Factories ----------------
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

class TaskViewModelFactory(private val application: android.app.Application) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
