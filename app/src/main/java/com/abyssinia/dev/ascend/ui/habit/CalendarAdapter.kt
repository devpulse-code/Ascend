package com.abyssinia.dev.ascend.ui.habit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abyssinia.dev.ascend.databinding.ItemCalendarDayBinding

class CalendarAdapter :
    ListAdapter<CalendarDay, CalendarAdapter.DayViewHolder>(CalendarDayDiffCallback()) {

    inner class DayViewHolder(private val binding: ItemCalendarDayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(day: CalendarDay) {
            binding.textDay.text = day.date.takeLast(2) // show only day of month
            binding.root.setBackgroundColor(
                if (day.isCheckedIn) 0xFF4CAF50.toInt() else 0x00000000
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class CalendarDayDiffCallback : DiffUtil.ItemCallback<CalendarDay>() {
    override fun areItemsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean =
        oldItem.date == newItem.date

    override fun areContentsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean =
        oldItem == newItem
}
