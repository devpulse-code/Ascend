package com.abyssinia.dev.ascend.ui.task

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abyssinia.dev.ascend.data.model.Task
import com.abyssinia.dev.ascend.databinding.FragmentTaskListBinding
import com.abyssinia.dev.ascend.viewmodel.TaskViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.launch

class TaskListFragment : Fragment() {

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskAdView: AdView

    private val taskViewModel: TaskViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeTasks()
        setupFab()
        setupAdBanner()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            lifecycleOwner = viewLifecycleOwner,
            taskViewModel = taskViewModel,
            onTaskClick = { task ->
                findNavController().navigate(
                    TaskListFragmentDirections.actionTaskListFragmentToAddEditTaskFragment(task.id)
                )
            },
            onTaskCompletionChange = { task, isChecked ->
                val updatedTask = task.copy(isCompleted = isChecked)
                taskViewModel.update(updatedTask)
                val currentList = taskAdapter.currentList.toMutableList()
                val index = currentList.indexOfFirst { it.id == task.id }
                if (index != -1) currentList[index] = updatedTask
                taskAdapter.submitList(currentList)
            },
            onTaskDelete = { task ->
                taskViewModel.delete(task)
                Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show()
                val currentList = taskAdapter.currentList.toMutableList()
                currentList.removeAll { it.id == task.id }
                taskAdapter.submitList(currentList)
            }
        )

        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }
    }

    private fun observeTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.allTasks.observe(viewLifecycleOwner) { tasks: List<Task> ->
                    taskAdapter.submitList(tasks)
                    binding.animationEmptyTasks.visibility =
                        if (tasks.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerViewTasks.visibility =
                        if (tasks.isEmpty()) View.GONE else View.VISIBLE
                    binding.recyclerViewTasks.post { animateRecyclerItems(binding.recyclerViewTasks) }
                }
            }
        }
    }

    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            findNavController().navigate(
                TaskListFragmentDirections.actionTaskListFragmentToAddEditTaskFragment(-1)
            )
        }
        startFabPulseAnimation()
    }

    private fun startFabPulseAnimation() {
        val scaleUpX = ObjectAnimator.ofFloat(binding.fabAddTask, "scaleX", 1f, 1.2f)
        val scaleUpY = ObjectAnimator.ofFloat(binding.fabAddTask, "scaleY", 1f, 1.2f)
        val scaleDownX = ObjectAnimator.ofFloat(binding.fabAddTask, "scaleX", 1.2f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(binding.fabAddTask, "scaleY", 1.2f, 1f)

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

        val fullPulse = AnimatorSet().apply {
            playSequentially(pulse, pulseReverse)
        }

        fullPulse.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animator: android.animation.Animator) {
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

    private fun setupAdBanner() {
        taskAdView = binding.taskAdView
        val adRequest = AdRequest.Builder().build()
        taskAdView.loadAd(adRequest)
    }

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
        _binding = null
        super.onDestroyView()
    }
}
