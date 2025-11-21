package com.abyssinia.dev.ascend.ui.habit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.abyssinia.dev.ascend.data.model.HabitCheckIn
import com.abyssinia.dev.ascend.databinding.FragmentHabitCalendarBinding
import com.abyssinia.dev.ascend.viewmodel.HabitViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class HabitCalendarFragment : Fragment() {

    private var _binding: FragmentHabitCalendarBinding? = null
    private val binding get() = _binding!!

    private val habitViewModel: HabitViewModel by viewModels()
    private val args: HabitCalendarFragmentArgs by navArgs() // Safe Args

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private lateinit var calendarAdapter: CalendarAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val habitId: Long = args.habitId

        // Setup RecyclerView as 7-column grid
        calendarAdapter = CalendarAdapter()
        binding.recyclerViewCalendar.apply {
            layoutManager = GridLayoutManager(requireContext(), 7)
            adapter = calendarAdapter
        }

        // Load habit and its check-ins
        lifecycleScope.launch {
            val habit = withContext(Dispatchers.IO) { habitViewModel.getHabitById(habitId) }

            habit?.let { h ->
                binding.textHabitTitle.text = h.name

                // Fetch check-ins for this habit
                val checkIns: List<HabitCheckIn> = withContext(Dispatchers.IO) {
                    habitViewModel.getHabitCheckIns(habitId)
                }

                // Convert check-ins to a set of formatted date strings
                val checkedDates = checkIns.map { dateFormat.format(Date(it.checkInDate)) }.toSet()

                // Generate last 30 days for the calendar
                val calendarDays = (29 downTo 0).map { i ->
                    Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                }.map { cal ->
                    val dateStr = dateFormat.format(cal.time)
                    CalendarDay(dateStr, checkedDates.contains(dateStr))
                }

                // Submit to adapter
                calendarAdapter.submitList(calendarDays)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/** Represents a single day in the calendar with check-in status */
data class CalendarDay(
    val date: String,
    val isCheckedIn: Boolean
)
