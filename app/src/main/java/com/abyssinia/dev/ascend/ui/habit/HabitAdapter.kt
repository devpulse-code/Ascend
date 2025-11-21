package com.abyssinia.dev.ascend.ui.habit

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abyssinia.dev.ascend.R
import com.abyssinia.dev.ascend.data.dao.HabitCheckInDao
import com.abyssinia.dev.ascend.data.model.Habit
import com.abyssinia.dev.ascend.data.model.HabitCheckIn
import com.abyssinia.dev.ascend.databinding.ItemHabitBinding
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class HabitAdapter(
    private val habitCheckInDao: HabitCheckInDao,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val onHabitClick: (Habit) -> Unit,
    private val onCheckInClick: (Habit) -> Unit,
    private val onDeleteClick: (Habit) -> Unit,
    private val onUndoCheckInClick: (Habit) -> Unit
) : ListAdapter<Habit, HabitAdapter.HabitViewHolder>(HabitDiffCallback()) {

    inner class HabitViewHolder(private val binding: ItemHabitBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var checkInJob: Job? = null

        fun bind(habit: Habit) {
            binding.textHabitTitle.text = habit.name
            val freq = habit.frequency.replaceFirstChar { it.uppercase() }
            binding.textHabitStreak.text = "Streak: ${habit.streakCount} ($freq)"

            val max = when (habit.frequency.lowercase(Locale.getDefault())) {
                "daily" -> 30
                "weekly" -> 4
                "monthly" -> 12
                else -> 30
            }

            binding.progressHabitStreak.max = max
            binding.progressHabitStreak.progress = habit.streakCount.coerceAtMost(max).toInt()

            checkInJob?.cancel()
            checkInJob = lifecycleScope.launch {
                habitCheckInDao.getCheckInsForHabit(habit.id).collectLatest { checkIns ->
                    updateWeeklyCalendar(checkIns)
                    updateCheckInButtonVisibility(habit, checkIns)
                }
            }

            // ---- Check-in Tap Animation ----
            binding.buttonCheckIn.setOnClickListener {
                if (binding.buttonCheckIn.isEnabled) {
                    val newStreak = habit.streakCount + 1
                    binding.progressHabitStreak.setProgress(newStreak.coerceAtMost(max).toInt(), true)
                    binding.textHabitStreak.text = "Streak: $newStreak ($freq)"

                    binding.buttonCheckIn.animate()
                        .scaleX(1.5f).scaleY(1.5f)
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction {
                            binding.buttonCheckIn.visibility = View.GONE
                            binding.buttonCheckIn.scaleX = 1f
                            binding.buttonCheckIn.scaleY = 1f
                        }
                        .start()

                    onCheckInClick(habit)
                }
            }

            // ---- Card Click Animation ----
            binding.root.setOnClickListener {
                val elevationAnim = AnimatorInflater.loadAnimator(binding.root.context, R.animator.card_elevation) as AnimatorSet
                elevationAnim.setTarget(binding.root)
                elevationAnim.start()
                onHabitClick(habit)
            }

            // ---- Long Click for Undo/Delete ----
            binding.root.setOnLongClickListener {
                val todayCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayTimestamp = todayCal.timeInMillis
                val canUndo = habit.lastCheckInDate?.let { it == todayTimestamp } ?: false

                val options = mutableListOf<String>()
                if (canUndo) options.add("Undo Today’s Check-in")
                options.add("Delete Habit")

                AlertDialog.Builder(binding.root.context)
                    .setTitle("Options")
                    .setItems(options.toTypedArray()) { _, which ->
                        when (options[which]) {
                            "Undo Today’s Check-in" -> {
                                onUndoCheckInClick(habit)
                                binding.buttonCheckIn.visibility = View.VISIBLE
                                binding.buttonCheckIn.alpha = 0f
                                binding.buttonCheckIn.animate().alpha(1f).setDuration(300).start()
                            }
                            "Delete Habit" -> onDeleteClick(habit)
                        }
                    }
                    .show()
                true
            }
        }

        private fun updateCheckInButtonVisibility(habit: Habit, checkIns: List<HabitCheckIn>) {
            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayTimestamp = todayCal.timeInMillis

            val hasCheckedInToday = checkIns.any { ci ->
                val ciCal = Calendar.getInstance().apply {
                    timeInMillis = ci.checkInDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                ciCal.timeInMillis == todayTimestamp
            }

            if (hasCheckedInToday) {
                binding.buttonCheckIn.visibility = View.GONE
            } else {
                binding.buttonCheckIn.visibility = View.VISIBLE
                binding.buttonCheckIn.alpha = 1f
            }
        }

        private fun updateWeeklyCalendar(checkIns: List<HabitCheckIn>) {
            val datesLayout = binding.datesLayout
            val today = Calendar.getInstance()
            val startOfWeek = today.clone() as Calendar
            startOfWeek.set(Calendar.DAY_OF_WEEK, startOfWeek.firstDayOfWeek)

            for (i in 0 until datesLayout.childCount) {
                val dateView = datesLayout.getChildAt(i) as TextView
                val dateCal = startOfWeek.clone() as Calendar
                dateCal.add(Calendar.DAY_OF_WEEK, i)
                dateView.text = dateCal.get(Calendar.DAY_OF_MONTH).toString()

                val defaultTextColor = MaterialColors.getColor(dateView, com.google.android.material.R.attr.colorOnSurface)
                val todayTextColor = MaterialColors.getColor(dateView, com.google.android.material.R.attr.colorOnPrimary)
                val completedTextColor = MaterialColors.getColor(dateView, com.google.android.material.R.attr.colorSurface)

                dateView.setBackgroundResource(R.drawable.circle_background)
                dateView.setTextColor(defaultTextColor)

                if (today.get(Calendar.DAY_OF_MONTH) == dateCal.get(Calendar.DAY_OF_MONTH) &&
                    today.get(Calendar.MONTH) == dateCal.get(Calendar.MONTH) &&
                    today.get(Calendar.YEAR) == dateCal.get(Calendar.YEAR)) {
                    dateView.setBackgroundResource(R.drawable.circle_background_today)
                    dateView.setTextColor(todayTextColor)
                }

                val completed = checkIns.any { checkIn ->
                    val cal = Calendar.getInstance().apply { timeInMillis = checkIn.checkInDate }
                    cal.get(Calendar.DAY_OF_MONTH) == dateCal.get(Calendar.DAY_OF_MONTH) &&
                            cal.get(Calendar.MONTH) == dateCal.get(Calendar.MONTH) &&
                            cal.get(Calendar.YEAR) == dateCal.get(Calendar.YEAR)
                }

                if (completed) {
                    dateView.setBackgroundResource(R.drawable.circle_background_done)
                    dateView.setTextColor(completedTextColor)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        HabitViewHolder(ItemHabitBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun attachSwipeHelper(recyclerView: RecyclerView) {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val habit = getItem(vh.adapterPosition)
                onDeleteClick(habit)
            }

            override fun onChildDraw(
                c: android.graphics.Canvas,
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val alpha = 1 - (kotlin.math.abs(dX) / rv.width.toFloat())
                vh.itemView.alpha = alpha
                vh.itemView.translationX = dX
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }
}

class HabitDiffCallback : DiffUtil.ItemCallback<Habit>() {
    override fun areItemsTheSame(oldItem: Habit, newItem: Habit) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Habit, newItem: Habit) =
        oldItem.name == newItem.name &&
                oldItem.frequency == newItem.frequency &&
                oldItem.streakCount == newItem.streakCount &&
                oldItem.lastCheckInDate == newItem.lastCheckInDate
}
