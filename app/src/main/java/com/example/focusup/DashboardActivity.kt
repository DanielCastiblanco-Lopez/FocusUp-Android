package com.example.focusup

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.focusup.data.StreakStorage
import com.example.focusup.data.Task
import com.example.focusup.data.TaskStorage
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Calendar

class DashboardActivity : AppCompatActivity() {

    private lateinit var adapter: TaskAdapter
    private lateinit var chipAll: Chip
    private lateinit var chipPending: Chip
    private lateinit var chipCompleted: Chip
    private var currentFilter = "Todas"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val rvTasks = findViewById<RecyclerView>(R.id.rvTasks)
        rvTasks.layoutManager = LinearLayoutManager(this)

        adapter = TaskAdapter(
            tasks = mutableListOf(),
            onCheckChanged = { task, isChecked ->
                TaskStorage.updateTask(this, task.copy(isDone = isChecked))
                if (isChecked) {
                    StreakStorage.markActiveToday(this)
                }
                refreshList()
            },
            onEditClick = { task -> showAddEditTaskDialog(task) },
            onDeleteClick = { task -> confirmDelete(task) },
            onCardClick = { task -> showTaskDetailDialog(task) }
        )
        rvTasks.adapter = adapter

        chipAll = findViewById(R.id.chipAll)
        chipPending = findViewById(R.id.chipPending)
        chipCompleted = findViewById(R.id.chipCompleted)

        chipAll.setOnClickListener {
            chipAll.isChecked = true
            currentFilter = "Todas"
            refreshList()
        }
        chipPending.setOnClickListener {
            chipPending.isChecked = true
            currentFilter = "Pendientes"
            refreshList()
        }
        chipCompleted.setOnClickListener {
            chipCompleted.isChecked = true
            currentFilter = "Completadas"
            refreshList()
        }

        val fabAddTask = findViewById<FloatingActionButton>(R.id.fabAddTask)
        fabAddTask.setOnClickListener {
            showAddEditTaskDialog(null)
        }

        val ivProfile = findViewById<android.widget.ImageView>(R.id.ivProfile)
        ivProfile.setOnClickListener {
            NavUtils.goTo(this, ProfileActivity::class.java, terminarActual = false)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_tasks

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    NavUtils.goTo(this, HomeActivity::class.java)
                    true
                }
                R.id.nav_tasks -> true
                R.id.nav_pomodoro -> {
                    NavUtils.goTo(this, PomodoroActivity::class.java)
                    true
                }
                R.id.nav_statistics -> {
                    NavUtils.goTo(this, StatisticsActivity::class.java)
                    true
                }
                R.id.nav_profile -> {
                    NavUtils.goTo(this, ProfileActivity::class.java)
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val allTasks = TaskStorage.getTasks(this)
        val filtered = when (currentFilter) {
            "Pendientes" -> allTasks.filter { !it.isDone }
            "Completadas" -> allTasks.filter { it.isDone }
            else -> allTasks
        }
        adapter.updateData(filtered)
        actualizarEstadoVacio(filtered.isEmpty())
    }

    private fun actualizarEstadoVacio(estaVacia: Boolean) {
        val containerEmptyState = findViewById<android.widget.LinearLayout>(R.id.containerEmptyState)
        val tvEmptyStateMessage = findViewById<TextView>(R.id.tvEmptyStateMessage)
        val rvTasks = findViewById<RecyclerView>(R.id.rvTasks)

        if (estaVacia) {
            containerEmptyState.visibility = View.VISIBLE
            rvTasks.visibility = View.GONE
            tvEmptyStateMessage.text = when (currentFilter) {
                "Pendientes" -> "No tienes tareas pendientes"
                "Completadas" -> "Aun no has completado ninguna tarea"
                else -> "No tienes tareas todavia. Toca + para crear la primera"
            }
        } else {
            containerEmptyState.visibility = View.GONE
            rvTasks.visibility = View.VISIBLE
        }
    }

    /** Dialogo para crear o editar una tarea, incluyendo el campo de notas opcional */
    private fun showAddEditTaskDialog(taskToEdit: Task?) {
        val dialog = DialogUtils.createCustomDialog(this, R.layout.dialog_add_task)

        val tvDialogTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val tilTitle = dialog.findViewById<TextInputLayout>(R.id.tilTaskTitle)
        val etTitle = dialog.findViewById<TextInputEditText>(R.id.etTaskTitle)
        val etDeadline = dialog.findViewById<TextInputEditText>(R.id.etTaskDeadline)
        val etNote = dialog.findViewById<TextInputEditText>(R.id.etTaskNote)
        val chipGroup = dialog.findViewById<ChipGroup>(R.id.chipGroupPriority)
        val chipHigh = dialog.findViewById<Chip>(R.id.chipHigh)
        val chipMedium = dialog.findViewById<Chip>(R.id.chipMedium)
        val chipLow = dialog.findViewById<Chip>(R.id.chipLow)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancelDialog)
        val btnSave = dialog.findViewById<MaterialButton>(R.id.btnSaveDialog)

        var deadlineMillisSeleccionado = taskToEdit?.deadlineMillis ?: 0L

        tvDialogTitle.text = if (taskToEdit == null) "Nueva tarea" else "Editar tarea"

        if (taskToEdit != null) {
            etTitle.setText(taskToEdit.title)
            etDeadline.setText(taskToEdit.deadline)
            etNote.setText(taskToEdit.note)
            when (taskToEdit.priority) {
                "Alta" -> chipHigh.isChecked = true
                "Media" -> chipMedium.isChecked = true
                "Baja" -> chipLow.isChecked = true
            }
        }

        etDeadline.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val meses = arrayOf(
                        "ene", "feb", "mar", "abr", "may", "jun",
                        "jul", "ago", "sep", "oct", "nov", "dic"
                    )
                    etDeadline.setText("$dayOfMonth ${meses[month]} $year")

                    val seleccion = Calendar.getInstance()
                    seleccion.set(year, month, dayOfMonth, 0, 0, 0)
                    deadlineMillisSeleccionado = seleccion.timeInMillis
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val deadline = etDeadline.text.toString().trim()
            val note = etNote.text.toString().trim()
            val priority = when (chipGroup.checkedChipId) {
                chipHigh.id -> "Alta"
                chipLow.id -> "Baja"
                else -> "Media"
            }

            if (title.isEmpty()) {
                tilTitle.error = "El titulo no puede estar vacio"
                etTitle.requestFocus()
                return@setOnClickListener
            }
            tilTitle.error = null

            if (taskToEdit == null) {
                TaskStorage.addTask(
                    this,
                    Task(
                        title = title,
                        deadline = deadline.ifEmpty { "Sin fecha" },
                        deadlineMillis = deadlineMillisSeleccionado,
                        priority = priority,
                        note = note
                    )
                )
            } else {
                TaskStorage.updateTask(
                    this,
                    taskToEdit.copy(
                        title = title,
                        deadline = deadline.ifEmpty { "Sin fecha" },
                        deadlineMillis = deadlineMillisSeleccionado,
                        priority = priority,
                        note = note
                    )
                )
            }
            refreshList()
            dialog.dismiss()
        }

        dialog.show()
    }

    /** Dialogo de detalle: se abre al tocar la tarjeta, muestra info y permite editar la nota */
    private fun showTaskDetailDialog(task: Task) {
        val dialog = DialogUtils.createCustomDialog(this, R.layout.dialog_task_detail)

        val tvDetailTitle = dialog.findViewById<TextView>(R.id.tvDetailTitle)
        val tvDetailPriority = dialog.findViewById<TextView>(R.id.tvDetailPriority)
        val tvDetailDeadline = dialog.findViewById<TextView>(R.id.tvDetailDeadline)
        val etDetailNote = dialog.findViewById<TextInputEditText>(R.id.etDetailNote)
        val btnClose = dialog.findViewById<MaterialButton>(R.id.btnCloseDetail)
        val btnSaveNote = dialog.findViewById<MaterialButton>(R.id.btnSaveNote)

        tvDetailTitle.text = task.title
        tvDetailPriority.text = task.priority
        val color = when (task.priority) {
            "Alta" -> android.graphics.Color.parseColor("#E74C3C")
            "Media" -> android.graphics.Color.parseColor("#F39C12")
            else -> android.graphics.Color.parseColor("#27AE60")
        }
        tvDetailPriority.background.setTint(color)
        tvDetailDeadline.text = "Vence: ${task.deadline}"
        etDetailNote.setText(task.note)

        btnClose.setOnClickListener { dialog.dismiss() }

        btnSaveNote.setOnClickListener {
            val nuevaNota = etDetailNote.text.toString().trim()
            TaskStorage.updateTask(this, task.copy(note = nuevaNota))
            refreshList()
            Toast.makeText(this, "Nota guardada", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun confirmDelete(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar tarea")
            .setMessage("Seguro que quieres eliminar \"${task.title}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                TaskStorage.deleteTask(this, task)
                refreshList()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
