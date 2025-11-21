package com.abyssinia.dev.ascend.ui.task

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieDrawable
import com.abyssinia.dev.ascend.databinding.FragmentTodayTaskBinding
import com.abyssinia.dev.ascend.viewmodel.TaskViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import java.util.*

class TodayTaskFragment : Fragment() {

    private var _binding: FragmentTodayTaskBinding? = null
    private val binding get() = _binding!!

    private val taskViewModel: TaskViewModel by viewModels()
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskAdView: AdView // ✅ Banner Ad

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayTaskBinding.inflate(inflater, container, false)
        setupRecycler()
        setupAd()
        observeTasks()
        return binding.root
    }

    private fun setupRecycler() {
        taskAdapter = TaskAdapter(
            lifecycleOwner = viewLifecycleOwner,
            taskViewModel = taskViewModel,
            onTaskClick = { /* handle edit */ },
            onTaskCompletionChange = { task, completed ->
                taskViewModel.update(task.copy(isCompleted = completed))
            },
            onTaskDelete = { /* disabled */ }
        )

        binding.recyclerTodayTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }
    }

    private fun setupAd() {
        taskAdView = binding.adView
        val adRequest = AdRequest.Builder().build()
        taskAdView.loadAd(adRequest)
    }

    private fun observeTasks() {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        taskViewModel.allTasks.observe(viewLifecycleOwner) { tasks ->
            val todayTasks = tasks.filter { it.dueDate != null && it.dueDate in todayStart..todayEnd && !it.isCompleted }
            taskAdapter.submitList(todayTasks)

            if (todayTasks.isEmpty()) {
                binding.lottieEmpty.visibility = View.VISIBLE
                binding.lottieEmpty.setAnimation("empty.json")
                binding.lottieEmpty.repeatCount = LottieDrawable.INFINITE
                binding.lottieEmpty.playAnimation()
                binding.recyclerTodayTasks.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
            } else {
                binding.lottieEmpty.visibility = View.GONE
                binding.lottieEmpty.pauseAnimation()
                binding.recyclerTodayTasks.visibility = View.VISIBLE
                binding.tvEmpty.visibility = View.GONE
            }

            binding.recyclerTodayTasks.scheduleLayoutAnimation()
        }
    }

    // ✅ Proper Ad lifecycle
    override fun onPause() {
        taskAdView.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        taskAdView.resume()
    }

    override fun onDestroyView() {
        taskAdView.destroy()
        super.onDestroyView()
        _binding = null
    }
}