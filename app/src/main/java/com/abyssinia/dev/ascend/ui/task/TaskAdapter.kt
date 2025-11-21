package com.abyssinia.dev.ascend.ui.task

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abyssinia.dev.ascend.R
import com.abyssinia.dev.ascend.data.model.Task
import com.abyssinia.dev.ascend.databinding.ItemTaskBinding
import com.abyssinia.dev.ascend.viewmodel.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val taskViewModel: TaskViewModel,
    private val onTaskClick: (Task) -> Unit,
    private val onTaskCompletionChange: (Task, Boolean) -> Unit,
    private val onTaskDelete: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    inner class TaskViewHolder(val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            binding.textTaskTitle.text = task.title

            val dueDateText = task.dueDate?.let {
                val dueDate = Date(it)
                val today = Calendar.getInstance().time
                val isOverdue = !task.isCompleted && dueDate.before(today)
                binding.textTaskDueDate.setTextColor(if (isOverdue) Color.RED else Color.DKGRAY)
                "Due: ${dateFormat.format(dueDate)}"
            } ?: "No due date"
            binding.textTaskDueDate.text = dueDateText

            // ---- Completion Checkbox Animation ----
            binding.checkboxCompleted.setOnCheckedChangeListener(null)
            binding.checkboxCompleted.isChecked = task.isCompleted
            binding.checkboxCompleted.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != task.isCompleted) {
                    // Scale animation
                    binding.checkboxCompleted.scaleX = 0.8f
                    binding.checkboxCompleted.scaleY = 0.8f
                    binding.checkboxCompleted.animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .setDuration(150)
                        .withEndAction {
                            binding.checkboxCompleted.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(150)
                                .start()
                        }.start()
                    onTaskCompletionChange(task, isChecked)
                }
            }

            // Click to edit with ripple + elevation animation
            binding.root.setOnClickListener {
                val elevationAnim = AnimatorInflater.loadAnimator(binding.root.context, R.animator.card_elevation) as AnimatorSet
                elevationAnim.setTarget(binding.root)
                elevationAnim.start()
                onTaskClick(task)
            }

            // Long press → delete
            binding.root.setOnLongClickListener {
                AlertDialog.Builder(binding.root.context)
                    .setTitle("Delete Task")
                    .setMessage("Are you sure you want to delete this task?")
                    .setPositiveButton("Delete") { _, _ -> onTaskDelete(task) }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }

            // Load tags asynchronously
            binding.textTaskTags.text = "Loading tags..."
            lifecycleOwner.lifecycleScope.launch {
                val tags = withContext(Dispatchers.IO) { taskViewModel.getTagsByTask(task.id) }
                val tagText = if (tags.isEmpty()) "No tags" else tags.joinToString(", ") { it.name }
                binding.textTaskTags.text = tagText
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        TaskViewHolder(ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ---- Swipe-to-delete helper ----
    fun attachSwipeHelper(recyclerView: RecyclerView) {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val task = getItem(vh.adapterPosition)
                onTaskDelete(task)
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

class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
    override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
}
