package com.abyssinia.dev.ascend.ui.task

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.abyssinia.dev.ascend.data.model.Tag
import com.abyssinia.dev.ascend.data.model.Task
import com.abyssinia.dev.ascend.databinding.FragmentAddEditTaskBinding
import com.abyssinia.dev.ascend.viewmodel.TaskViewModel
import com.google.android.material.chip.Chip
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AddEditTaskFragment : Fragment() {

    private var _binding: FragmentAddEditTaskBinding? = null
    private val binding get() = _binding!!

    private val taskViewModel: TaskViewModel by viewModels()
    private val args: AddEditTaskFragmentArgs by navArgs()

    private var taskId: Long? = null
    private var dueDateMillis: Long = 0L
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private lateinit var adView: AdView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditTaskBinding.inflate(inflater, container, false)

        binding.root.translationY = 1000f
        binding.root.alpha = 0f
        binding.root.animate()
            .translationY(0f)
            .alpha(1f)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(400)
            .start()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdBanner()
        binding.backButton.setOnClickListener {
            binding.root.animate()
                .translationY(1000f)
                .alpha(0f)
                .setDuration(300)
                .withEndAction { findNavController().popBackStack() }
                .start()
        }

        setupDatePicker()

        taskId = args.taskId.takeIf { it != -1L }
        taskId?.let { id ->
            lifecycleScope.launch {
                val task = withContext(Dispatchers.IO) { taskViewModel.getTaskById(id) }
                task?.let { populateTask(it) }
            }
        }

        taskViewModel.allTags.observe(viewLifecycleOwner) { tags ->
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                tags.map { it.name }
            )
            binding.dropdownTag.setAdapter(adapter)
        }

        binding.dropdownTag.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) binding.dropdownTag.showDropDown() }
        binding.dropdownTag.setOnClickListener { binding.dropdownTag.showDropDown() }
        binding.dropdownTag.setOnItemClickListener { _, _, _, _ ->
            val tagName = binding.dropdownTag.text.toString().trim()
            taskViewModel.allTags.value?.find { it.name.equals(tagName, true) }?.let { addTag(it) }
            binding.dropdownTag.setText("")
        }

        binding.tagInputLayout.setEndIconOnClickListener {
            val tagName = binding.dropdownTag.text.toString().trim()
            if (tagName.isNotEmpty()) {
                lifecycleScope.launch {
                    val existingTag = withContext(Dispatchers.IO) { taskViewModel.getTagByName(tagName) }
                    val tag = existingTag ?: Tag(name = tagName).also { newTag ->
                        newTag.id = withContext(Dispatchers.IO) { taskViewModel.insertTagSuspend(newTag) }
                    }
                    addTag(tag)
                    binding.dropdownTag.setText("")
                }
            }
        }

        listOf(binding.editTextTitle, binding.editTextDescription, binding.editTextDueDate).forEach { field ->
            field.setOnFocusChangeListener { v, hasFocus ->
                v.animate().scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(200)
                    .start()
            }
        }

        enableSaveButtonWithPulse()
        binding.buttonSave.setOnClickListener { saveTask() }
    }

    private fun setupAdBanner() {
        adView = binding.adView
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    private fun setupDatePicker() {
        binding.editTextDueDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            if (dueDateMillis != 0L) calendar.timeInMillis = dueDateMillis
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    dueDateMillis = calendar.timeInMillis
                    binding.editTextDueDate.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun populateTask(task: Task) {
        binding.editTextTitle.setText(task.title)
        binding.editTextDescription.setText(task.description)
        binding.checkboxCompleted.isChecked = task.isCompleted
        task.dueDate?.let { due ->
            dueDateMillis = due
            binding.editTextDueDate.setText(dateFormat.format(Date(due)))
        }
        lifecycleScope.launch {
            val tags = withContext(Dispatchers.IO) { taskViewModel.getTagsByTask(task.id) }
            tags.forEach { addTag(it) }
        }
    }

    private fun addTag(tag: Tag) {
        if (taskViewModel.selectedTags.value?.any { it.id == tag.id } == true) return
        taskViewModel.selectTag(tag)

        val chip = Chip(requireContext()).apply {
            text = tag.name
            isCloseIconVisible = true
            alpha = 0f
            scaleX = 0.5f
            scaleY = 0.5f
            animate().alpha(1f).scaleX(1.2f).scaleY(1.2f)
                .setDuration(150)
                .withEndAction { animate().scaleX(1f).scaleY(1f).setDuration(100).start() }
                .start()

            setOnCloseIconClickListener {
                taskViewModel.deselectTag(tag)
                animate().scaleX(0f).scaleY(0f).alpha(0f)
                    .setDuration(200)
                    .withEndAction { binding.chipGroupTags.removeView(this) }
                    .start()
                taskId?.let { id ->
                    lifecycleScope.launch(Dispatchers.IO) { taskViewModel.removeTagFromTask(id, tag.id) }
                }
            }

            setOnClickListener {
                binding.dropdownTag.setText(tag.name)
                binding.chipGroupTags.removeView(this)
                taskViewModel.deselectTag(tag)
            }
        }
        binding.chipGroupTags.addView(chip)
    }

    private fun enableSaveButtonWithPulse() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val valid = binding.editTextTitle.text.toString().trim().isNotEmpty() &&
                        binding.editTextDueDate.text.toString().trim().isNotEmpty()
                if (valid && !binding.buttonSave.isEnabled) {
                    binding.buttonSave.animate()
                        .scaleX(1.1f).scaleY(1.1f)
                        .setDuration(150)
                        .withEndAction { binding.buttonSave.animate().scaleX(1f).scaleY(1f).setDuration(150).start() }
                        .start()
                }
                binding.buttonSave.isEnabled = valid
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.editTextTitle.addTextChangedListener(watcher)
        binding.editTextDueDate.addTextChangedListener(watcher)
    }

    private fun saveTask() {
        val title = binding.editTextTitle.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val isCompleted = binding.checkboxCompleted.isChecked
        if (title.isEmpty()) { binding.editTextTitle.error = "Title required"; return }
        if (dueDateMillis == 0L) { binding.editTextDueDate.error = "Due date required"; return }

        val task = Task(
            id = taskId ?: 0L,
            title = title,
            description = description,
            dueDate = dueDateMillis,
            isCompleted = isCompleted
        )

        lifecycleScope.launch(Dispatchers.IO) {
            val id = if (taskId == null) taskViewModel.insertTaskSuspend(task)
            else { taskViewModel.updateTaskSuspend(task); task.id }
            taskViewModel.selectedTags.value?.forEach { tag -> taskViewModel.assignTagToTask(id, tag.id) }
            withContext(Dispatchers.Main) {
                binding.root.animate()
                    .translationY(1000f)
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction { findNavController().navigateUp() }
                    .start()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        adView.pause()
    }

    override fun onResume() {
        super.onResume()
        adView.resume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adView.destroy()
        _binding = null
    }
}
