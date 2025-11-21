package com.abyssinia.dev.ascend.ui.habit

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.abyssinia.dev.ascend.data.model.Habit
import com.abyssinia.dev.ascend.data.model.Tag
import com.abyssinia.dev.ascend.databinding.FragmentAddEditHabitBinding
import com.abyssinia.dev.ascend.viewmodel.HabitViewModel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddEditHabitFragment : Fragment() {

    private var _binding: FragmentAddEditHabitBinding? = null
    private val binding get() = _binding!!

    private val habitViewModel: HabitViewModel by viewModels {
        HabitViewModelFactory(requireActivity().application)
    }

    private val args: AddEditHabitFragmentArgs by navArgs()
    private var existingHabit: Habit? = null
    private val selectedTags = mutableSetOf<Tag>()

    private var adViewBottom: AdView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditHabitBinding.inflate(inflater, container, false)

        // Slide-in animation
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

        // Back button with slide-out
        binding.backButton.setOnClickListener {
            binding.root.animate()
                .translationY(1000f)
                .alpha(0f)
                .setDuration(300)
                .withEndAction { findNavController().popBackStack() }
                .start()
        }

        setupFrequencyDropdown()
        setupTagsDropdown()
        loadExistingHabit()
        setupInputAnimations()
        setupSaveButton()
        setupAdBanner() // <--- Load bottom ad
    }

    private fun setupFrequencyDropdown() {
        val frequencies = listOf("Daily", "Weekly", "Monthly")
        binding.dropdownFrequency.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, frequencies)
        )
    }

    private fun setupTagsDropdown() {
        habitViewModel.allTags.observe(viewLifecycleOwner) { tags ->
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                tags.map { it.name }
            )
            binding.dropdownTag.setAdapter(adapter)
        }

        binding.dropdownTag.setOnItemClickListener { _, _, _, _ ->
            val tagName = binding.dropdownTag.text.toString()
            habitViewModel.allTags.value
                ?.find { it.name.equals(tagName, true) }
                ?.let { addTagChip(it) }
            binding.dropdownTag.setText("")
        }

        binding.tagInputLayout.setEndIconOnClickListener {
            val tagName = binding.dropdownTag.text.toString().trim()
            if (tagName.isNotEmpty()) {
                lifecycleScope.launch {
                    val existingTag = withContext(Dispatchers.IO) {
                        habitViewModel.getTagByName(tagName)
                    }
                    val tag = existingTag ?: Tag(name = tagName).also { newTag ->
                        newTag.id = withContext(Dispatchers.IO) { habitViewModel.insertTagSuspend(newTag) }
                    }
                    addTagChip(tag)
                    binding.dropdownTag.setText("")
                }
            }
        }
    }

    private fun loadExistingHabit() {
        if (args.habitId != -1L) {
            lifecycleScope.launch {
                val habit = withContext(Dispatchers.IO) {
                    habitViewModel.getHabitById(args.habitId)
                }
                habit?.let { h ->
                    existingHabit = h
                    binding.editTextTitle.setText(h.name)
                    binding.editTextDescription.setText(h.description)
                    binding.dropdownFrequency.setText(h.frequency.replaceFirstChar { it.uppercase() }, false)
                    val tags = withContext(Dispatchers.IO) { habitViewModel.getTagsByHabit(h.id) }
                    tags.forEach { addTagChip(it) }
                }
            }
        }
    }

    private fun setupInputAnimations() {
        listOf(binding.editTextTitle, binding.editTextDescription, binding.dropdownFrequency).forEach { field ->
            field.setOnFocusChangeListener { v, hasFocus ->
                v.animate().scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(200)
                    .start()
            }
        }
    }

    private fun setupSaveButton() {
        binding.buttonSave.isEnabled = false
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val titleFilled = binding.editTextTitle.text.toString().trim().isNotEmpty()
                val freqFilled = binding.dropdownFrequency.text.toString().trim().isNotEmpty()
                val enabled = titleFilled && freqFilled
                if (enabled && !binding.buttonSave.isEnabled) {
                    binding.buttonSave.animate()
                        .scaleX(1.1f).scaleY(1.1f)
                        .setDuration(150)
                        .withEndAction {
                            binding.buttonSave.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                        }.start()
                }
                binding.buttonSave.isEnabled = enabled
                if (freqFilled) binding.frequencyInputLayout.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.editTextTitle.addTextChangedListener(watcher)
        binding.dropdownFrequency.addTextChangedListener(watcher)

        binding.buttonSave.setOnClickListener { saveHabit() }
    }

    private fun addTagChip(tag: Tag) {
        if (selectedTags.any { it.id == tag.id }) return
        selectedTags.add(tag)
        val chip = Chip(requireContext()).apply {
            text = tag.name
            isCloseIconVisible = true
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()

            setOnCloseIconClickListener {
                selectedTags.remove(tag)
                animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(200)
                    .withEndAction { binding.chipGroupTags.removeView(this) }.start()
                existingHabit?.let { habit ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        habitViewModel.removeTagFromHabit(habit.id, tag.id)
                    }
                }
            }

            setOnClickListener {
                binding.dropdownTag.setText(tag.name)
                binding.chipGroupTags.removeView(this)
                selectedTags.remove(tag)
            }
        }
        binding.chipGroupTags.addView(chip)
    }

    private fun saveHabit() {
        val title = binding.editTextTitle.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val frequency = binding.dropdownFrequency.text.toString().trim()
        if (frequency.isEmpty()) { binding.frequencyInputLayout.error = "Please select a frequency"; return }

        val habit = existingHabit?.copy(
            name = title,
            description = description,
            frequency = frequency.lowercase()
        ) ?: Habit(name = title, description = description, frequency = frequency.lowercase())

        lifecycleScope.launch {
            if (existingHabit != null) {
                habitViewModel.update(habit)
                selectedTags.forEach { tag -> habitViewModel.assignTagToHabit(habit.id, tag.id) }
                Toast.makeText(requireContext(), "Habit updated", Toast.LENGTH_SHORT).show()
            } else {
                val newId = habitViewModel.insertSuspend(habit)
                habit.id = newId
                selectedTags.forEach { tag -> habitViewModel.assignTagToHabit(newId, tag.id) }
                Toast.makeText(requireContext(), "Habit added", Toast.LENGTH_SHORT).show()
            }
            // Slide out animation
            binding.root.animate()
                .translationY(1000f)
                .alpha(0f)
                .setDuration(300)
                .withEndAction { findNavController().popBackStack() }
                .start()
        }
    }

    private fun setupAdBanner() {
        adViewBottom = binding.habitAdViewBottom
        val adRequest = AdRequest.Builder().build()
        adViewBottom?.loadAd(adRequest)
        adViewBottom?.adListener = object : AdListener() {
            override fun onAdLoaded() { binding.adContainerBottom.visibility = View.VISIBLE }
            override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) { binding.adContainerBottom.visibility = View.GONE }
        }
    }

    override fun onDestroyView() {
        adViewBottom = null
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
