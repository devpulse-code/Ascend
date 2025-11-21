package com.abyssinia.dev.ascend.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.abyssinia.dev.ascend.data.database.AppDatabase
import com.abyssinia.dev.ascend.data.model.Tag
import com.abyssinia.dev.ascend.data.model.Task
import com.abyssinia.dev.ascend.data.model.Habit
import com.abyssinia.dev.ascend.databinding.FragmentSearchResultsBinding
import com.abyssinia.dev.ascend.ui.habit.HabitAdapter
import com.abyssinia.dev.ascend.ui.task.TaskAdapter
import com.abyssinia.dev.ascend.viewmodel.HabitViewModel
import com.abyssinia.dev.ascend.viewmodel.TaskViewModel
import com.abyssinia.dev.ascend.viewmodel.HabitViewModelFactory
import com.abyssinia.dev.ascend.viewmodel.TaskViewModelFactory
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.*

class SearchResultsFragment : Fragment() {

    private var _binding: FragmentSearchResultsBinding? = null
    private val binding get() = _binding!!

    private lateinit var habitAdapter: HabitAdapter
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var adView: AdView   // ✅ Banner AdView
    private var searchJob: Job? = null

    private val habitViewModel: HabitViewModel by activityViewModels {
        HabitViewModelFactory(requireActivity().application)
    }
    private val taskViewModel: TaskViewModel by activityViewModels {
        TaskViewModelFactory(requireActivity().application)
    }

    private var habitsCache: List<Habit> = emptyList()
    private var tasksCache: List<Task> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchResultsBinding.inflate(inflater, container, false)

        val appDatabase = AppDatabase.getDatabase(requireContext())
        val habitCheckInDao = appDatabase.habitCheckInDao()

        habitAdapter = HabitAdapter(
            habitCheckInDao = habitCheckInDao,
            lifecycleScope = viewLifecycleOwner.lifecycleScope,
            onHabitClick = { /* handle click */ },
            onCheckInClick = { /* handle check-in */ },
            onDeleteClick = { /* handle delete */ },
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

        taskAdapter = TaskAdapter(
            lifecycleOwner = viewLifecycleOwner,
            taskViewModel = taskViewModel,
            onTaskClick = { task -> /* handle click */ },
            onTaskCompletionChange = { task, isCompleted -> /* handle completion */ },
            onTaskDelete = { task -> /* handle delete */ }
        )

        binding.recyclerHabits.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = habitAdapter
            isNestedScrollingEnabled = false
        }
        binding.recyclerTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
            isNestedScrollingEnabled = false
        }

        setupTagChips()

        // Observe habits/tasks and cache them
        habitViewModel.allHabits.observe(viewLifecycleOwner) { habits ->
            habitsCache = habits
            performSearch(binding.searchEditText.text.toString())
        }

        taskViewModel.allTasks.observe(viewLifecycleOwner) { tasks ->
            tasksCache = tasks
            performSearch(binding.searchEditText.text.toString())
        }

        binding.searchEditText.addTextChangedListener { editable ->
            val query = editable.toString()
            debounceSearch(query)
        }

        // ✅ Setup AdView
        adView = binding.adView
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        return binding.root
    }

    private fun debounceSearch(query: String) {
        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.Main).launch {
            delay(250)
            performSearch(query)
        }
    }

    private fun performSearch(query: String) {
        CoroutineScope(Dispatchers.Main).launch {
            if (query.isEmpty()) {
                habitAdapter.submitList(emptyList())
                taskAdapter.submitList(emptyList())
                binding.noResultsText.visibility = View.GONE
                return@launch
            }

            val selectedTags = binding.tagChipGroup.checkedChipIds.mapNotNull { id ->
                val chip = binding.tagChipGroup.findViewById<com.google.android.material.chip.Chip>(id)
                val tagName = chip.text.toString()
                habitViewModel.allTags.value?.firstOrNull { it.name.equals(tagName, true) }
                    ?: taskViewModel.allTags.value?.firstOrNull { it.name.equals(tagName, true) }
            }

            val habitTagsMap: Map<Habit, List<Tag>> = withContext(Dispatchers.IO) {
                habitsCache.associateWith { habit -> habitViewModel.getTagsByHabit(habit.id) }
            }

            val taskTagsMap: Map<Task, List<Tag>> = withContext(Dispatchers.IO) {
                tasksCache.associateWith { task -> taskViewModel.getTagsByTask(task.id) }
            }

            val filteredHabits = habitsCache.filter { habit ->
                val matchesQuery = fuzzyMatch(habit.name, query)
                val matchesTags = if (selectedTags.isEmpty()) true
                else selectedTags.all { it in (habitTagsMap[habit] ?: emptyList()) }
                matchesQuery && matchesTags
            }

            val filteredTasks = tasksCache.filter { task ->
                val matchesQuery = fuzzyMatch(task.title, query)
                val matchesTags = if (selectedTags.isEmpty()) true
                else selectedTags.all { it in (taskTagsMap[task] ?: emptyList()) }
                matchesQuery && matchesTags
            }

            habitAdapter.submitList(filteredHabits)
            taskAdapter.submitList(filteredTasks)

            binding.noResultsText.visibility =
                if (filteredHabits.isEmpty() && filteredTasks.isEmpty()) {
                    binding.noResultsText.text = "No match found"
                    View.VISIBLE
                } else View.GONE
        }
    }

    private fun fuzzyMatch(text: String, query: String): Boolean {
        if (query.isEmpty()) return true
        var tIndex = 0
        var qIndex = 0
        val textLower = text.lowercase()
        val queryLower = query.lowercase()
        while (tIndex < textLower.length && qIndex < queryLower.length) {
            if (textLower[tIndex] == queryLower[qIndex]) qIndex++
            tIndex++
        }
        return qIndex == queryLower.length
    }

    private fun setupTagChips() {
        val allTags = mutableSetOf<Tag>()
        habitViewModel.allTags.value?.let { allTags.addAll(it) }
        taskViewModel.allTags.value?.let { allTags.addAll(it) }

        binding.tagChipGroup.removeAllViews()
        allTags.forEach { tag ->
            val chip = com.google.android.material.chip.Chip(requireContext())
            chip.text = tag.name
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { _, _ ->
                debounceSearch(binding.searchEditText.text.toString())
            }
            binding.tagChipGroup.addView(chip)
        }
    }

    // ✅ Proper AdView lifecycle
    override fun onPause() {
        adView.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        adView.resume()
    }

    override fun onDestroyView() {
        adView.destroy()
        super.onDestroyView()
        _binding = null
    }
}
