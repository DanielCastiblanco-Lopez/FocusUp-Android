package com.example.focusup

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.focusup.data.Task

class TaskAdapter(
    private var tasks: MutableList<Task>,
    private val onCheckChanged: (Task, Boolean) -> Unit,
    private val onEditClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit,
    private val onCardClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbTaskDone: CheckBox = itemView.findViewById(R.id.cbTaskDone)
        val tvTaskTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val tvTaskDeadline: TextView = itemView.findViewById(R.id.tvTaskDeadline)
        val tvPriority: TextView = itemView.findViewById(R.id.tvPriority)
        val ivHasNote: ImageView = itemView.findViewById(R.id.ivHasNote)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun getItemCount(): Int = tasks.size

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        holder.tvTaskTitle.text = task.title
        holder.tvTaskDeadline.text = "Vence: ${task.deadline}"
        holder.tvPriority.text = task.priority

        val color = when (task.priority) {
            "Alta" -> Color.parseColor("#E74C3C")
            "Media" -> Color.parseColor("#F39C12")
            else -> Color.parseColor("#27AE60")
        }
        holder.tvPriority.background.setTint(color)

        holder.ivHasNote.visibility = if (task.note.isNotBlank()) View.VISIBLE else View.GONE

        holder.cbTaskDone.setOnCheckedChangeListener(null)
        holder.cbTaskDone.isChecked = task.isDone

        if (task.isDone) {
            holder.tvTaskTitle.paintFlags = holder.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.tvTaskTitle.paintFlags = holder.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        holder.cbTaskDone.setOnCheckedChangeListener { _, isChecked ->
            onCheckChanged(task, isChecked)
        }

        holder.btnEdit.setOnClickListener { onEditClick(task) }
        holder.btnDelete.setOnClickListener { onDeleteClick(task) }

        // Toda la tarjeta es clickeable para abrir el detalle/nota, excepto los controles internos
        holder.itemView.setOnClickListener { onCardClick(task) }
    }

    fun updateData(newTasks: List<Task>) {
        tasks = newTasks.toMutableList()
        notifyDataSetChanged()
    }
}
