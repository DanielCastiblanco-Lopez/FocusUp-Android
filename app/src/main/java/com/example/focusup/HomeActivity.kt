package com.example.focusup

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.focusup.data.PomodoroStorage
import com.example.focusup.data.StreakStorage
import com.example.focusup.data.Task
import com.example.focusup.data.TaskStorage
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import java.util.Calendar

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val ivProfile = findViewById<android.widget.ImageView>(R.id.ivProfileHome)
        ivProfile.setOnClickListener {
            NavUtils.goTo(this, ProfileActivity::class.java, terminarActual = false)
        }

        val cardQuickPomodoro = findViewById<CardView>(R.id.cardQuickPomodoro)
        cardQuickPomodoro.setOnClickListener {
            NavUtils.goTo(this, PomodoroActivity::class.java, terminarActual = false)
        }

        val cardQuickAddTask = findViewById<CardView>(R.id.cardQuickAddTask)
        cardQuickAddTask.setOnClickListener {
            NavUtils.goTo(this, DashboardActivity::class.java, terminarActual = false)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_tasks -> {
                    NavUtils.goTo(this, DashboardActivity::class.java)
                    true
                }
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

        cargarSaludo()
        cargarResumen()
        construirCalendarioSemanal()
    }

    override fun onResume() {
        super.onResume()
        cargarSaludo()
        cargarResumen()
        construirCalendarioSemanal()
    }

    private fun cargarSaludo() {
        val usuario = com.example.focusup.data.UserStorage.getCurrentUser(this)
        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)
        tvGreeting.text = if (usuario != null) "Hola, ${usuario.nombres}" else "Hola"

        val pendientesHoy = TaskStorage.getTasks(this).count { !it.isDone }
        val minutosSemana = calcularMinutosSemana()
        val racha = StreakStorage.getCurrentStreak(this)

        val tvSubGreeting = findViewById<TextView>(R.id.tvSubGreeting)
        tvSubGreeting.text = when {
            racha >= 2 -> "Tu racha sigue viva: $racha dias seguidos"
            pendientesHoy > 0 -> "Tienes $pendientesHoy ${if (pendientesHoy == 1) "tarea pendiente" else "tareas pendientes"}"
            minutosSemana > 0 -> "Has estudiado $minutosSemana min esta semana"
            else -> "Listo para empezar tu dia"
        }
    }

    private fun calcularMinutosSemana(): Int {
        val haceUnaSemana = Calendar.getInstance()
        haceUnaSemana.add(Calendar.DAY_OF_YEAR, -7)
        return PomodoroStorage.getSessions(this)
            .filter { it.timestamp >= haceUnaSemana.timeInMillis }
            .sumOf { it.minutes }
    }

    private fun cargarResumen() {
        val tareas = TaskStorage.getTasks(this)
        val pendientes = tareas.filter { !it.isDone }

        findViewById<TextView>(R.id.tvHomePendingCount).text = pendientes.size.toString()

        val minutosHoy = PomodoroStorage.getMinutesToday(this)
        findViewById<TextView>(R.id.tvHomeMinutesToday).text = minutosHoy.toString()

        val racha = StreakStorage.getCurrentStreak(this)
        findViewById<TextView>(R.id.tvStreakCount).text = racha.toString()
        findViewById<TextView>(R.id.tvStreakLabel).text =
            if (racha == 1) "dia de racha activa" else "dias de racha activa"

        mostrarProximaTarea(pendientes)
    }

    private fun mostrarProximaTarea(pendientes: List<Task>) {
        val tvNextTask = findViewById<TextView>(R.id.tvHomeNextTask)
        val tvNextTaskDetail = findViewById<TextView>(R.id.tvHomeNextTaskDetail)
        val barraColor = findViewById<View>(R.id.viewNextTaskPriorityBar)

        if (pendientes.isEmpty()) {
            tvNextTask.text = "No tienes tareas pendientes"
            tvNextTaskDetail.visibility = View.GONE
            barraColor.setBackgroundColor(Color.parseColor("#BDC3C7"))
            return
        }

        val conFecha = pendientes.filter { it.deadlineMillis > 0 }.sortedBy { it.deadlineMillis }
        val proxima = conFecha.firstOrNull() ?: pendientes.last()

        tvNextTask.text = proxima.title

        val colorPrioridad = when (proxima.priority) {
            "Alta" -> Color.parseColor("#E74C3C")
            "Media" -> Color.parseColor("#F39C12")
            else -> Color.parseColor("#27AE60")
        }
        barraColor.setBackgroundColor(colorPrioridad)

        val emoji = when (proxima.priority) {
            "Alta" -> "\uD83D\uDD34"
            "Media" -> "\uD83D\uDFE1"
            else -> "\uD83D\uDFE2"
        }

        val faltante = if (proxima.deadlineMillis > 0) {
            " · " + calcularTiempoRestante(proxima.deadlineMillis)
        } else ""

        tvNextTaskDetail.text = "$emoji ${proxima.priority} prioridad$faltante"
        tvNextTaskDetail.visibility = View.VISIBLE
    }

    private fun calcularTiempoRestante(deadlineMillis: Long): String {
        val hoy = Calendar.getInstance()
        hoy.set(Calendar.HOUR_OF_DAY, 0)
        hoy.set(Calendar.MINUTE, 0)
        hoy.set(Calendar.SECOND, 0)
        hoy.set(Calendar.MILLISECOND, 0)

        val dias = ((deadlineMillis - hoy.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

        return when {
            dias < 0 -> "Vencida"
            dias == 0 -> "Hoy"
            dias == 1 -> "Mañana"
            else -> "en $dias dias"
        }
    }

    /** Construye los 7 dias de la semana actual (Lunes a Domingo), marcando los que tienen tareas */
    private fun construirCalendarioSemanal() {
        val container = findViewById<LinearLayout>(R.id.containerWeekCalendar)
        container.removeAllViews()

        val hoy = Calendar.getInstance()
        val hoyDiaAno = hoy.get(Calendar.DAY_OF_YEAR)
        val hoyAno = hoy.get(Calendar.YEAR)

        val inicioSemana = Calendar.getInstance()
        val diaSemanaActual = inicioSemana.get(Calendar.DAY_OF_WEEK)
        val diasParaRetroceder = if (diaSemanaActual == Calendar.SUNDAY) 6 else diaSemanaActual - Calendar.MONDAY
        inicioSemana.add(Calendar.DAY_OF_YEAR, -diasParaRetroceder)

        val etiquetas = arrayOf("Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom")

        for (i in 0 until 7) {
            val diaCal = inicioSemana.clone() as Calendar
            diaCal.add(Calendar.DAY_OF_YEAR, i)
            diaCal.set(Calendar.HOUR_OF_DAY, 0)
            diaCal.set(Calendar.MINUTE, 0)
            diaCal.set(Calendar.SECOND, 0)
            diaCal.set(Calendar.MILLISECOND, 0)

            val esHoy = diaCal.get(Calendar.DAY_OF_YEAR) == hoyDiaAno &&
                diaCal.get(Calendar.YEAR) == hoyAno

            val tareasDelDia = TaskStorage.getTasksForDay(this, diaCal.timeInMillis)

            val columna = LinearLayout(this)
            columna.orientation = LinearLayout.VERTICAL
            columna.gravity = Gravity.CENTER
            val columnaParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT)
            columnaParams.weight = 1f
            columna.layoutParams = columnaParams
            columna.isClickable = true
            columna.isFocusable = true

            val tvEtiqueta = TextView(this)
            tvEtiqueta.text = etiquetas[i]
            tvEtiqueta.textSize = 11f
            tvEtiqueta.gravity = Gravity.CENTER
            tvEtiqueta.setTextColor(
                if (esHoy) Color.parseColor("#18BC9C") else Color.parseColor("#7F8C8D")
            )
            if (esHoy) tvEtiqueta.setTypeface(null, Typeface.BOLD)
            columna.addView(tvEtiqueta)

            val tvNumero = TextView(this)
            tvNumero.text = diaCal.get(Calendar.DAY_OF_MONTH).toString()
            tvNumero.textSize = 13f
            tvNumero.gravity = Gravity.CENTER
            val numeroParams = LinearLayout.LayoutParams(dpToPx(30), dpToPx(30))
            numeroParams.topMargin = dpToPx(4)
            tvNumero.layoutParams = numeroParams

            if (esHoy) {
                tvNumero.setBackgroundResource(R.drawable.bg_avatar_circle)
                tvNumero.setTextColor(Color.WHITE)
                tvNumero.setTypeface(null, Typeface.BOLD)
            } else {
                tvNumero.setTextColor(Color.parseColor("#2C3E50"))
            }
            columna.addView(tvNumero)

            val puntoIndicador = View(this)
            val puntoParams = LinearLayout.LayoutParams(dpToPx(6), dpToPx(6))
            puntoParams.topMargin = dpToPx(4)
            puntoIndicador.layoutParams = puntoParams
            if (tareasDelDia.isNotEmpty()) {
                puntoIndicador.setBackgroundResource(R.drawable.bg_avatar_circle)
                val tieneAltaPrioridad = tareasDelDia.any { it.priority == "Alta" && !it.isDone }
                puntoIndicador.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    if (tieneAltaPrioridad) Color.parseColor("#E74C3C") else Color.parseColor("#18BC9C")
                )
            } else {
                puntoIndicador.visibility = View.INVISIBLE
            }
            columna.addView(puntoIndicador)

            columna.setOnClickListener {
                mostrarTareasDelDia(diaCal, tareasDelDia, etiquetas[i])
            }

            container.addView(columna)
        }
    }

    /** Dialogo personalizado (estilo institucional) con las tareas que vencen el dia seleccionado */
    private fun mostrarTareasDelDia(dia: Calendar, tareas: List<Task>, etiquetaDia: String) {
        val nombreDiaCompleto = nombreDiaCompleto(dia.get(Calendar.DAY_OF_WEEK))
        val fechaTexto = "$nombreDiaCompleto, ${dia.get(Calendar.DAY_OF_MONTH)} de ${mesEnTexto(dia.get(Calendar.MONTH))}"

        val dialog = DialogUtils.createCustomDialog(this, R.layout.dialog_day_tasks)

        val tvDayDialogTitle = dialog.findViewById<TextView>(R.id.tvDayDialogTitle)
        val tvDayDialogSubtitle = dialog.findViewById<TextView>(R.id.tvDayDialogSubtitle)
        val containerNoTasksDay = dialog.findViewById<LinearLayout>(R.id.containerNoTasksDay)
        val containerDayTasks = dialog.findViewById<LinearLayout>(R.id.containerDayTasks)
        val btnClose = dialog.findViewById<MaterialButton>(R.id.btnCloseDayDialog)
        val btnSeeAll = dialog.findViewById<MaterialButton>(R.id.btnSeeAllTasks)

        tvDayDialogTitle.text = fechaTexto
        tvDayDialogSubtitle.text = if (tareas.isEmpty()) "Sin tareas" else
            "${tareas.size} ${if (tareas.size == 1) "tarea" else "tareas"}"

        containerDayTasks.removeAllViews()

        if (tareas.isEmpty()) {
            containerNoTasksDay.visibility = View.VISIBLE
        } else {
            containerNoTasksDay.visibility = View.GONE
            val inflater = LayoutInflater.from(this)

            for (task in tareas) {
                val itemView = inflater.inflate(R.layout.item_day_task, containerDayTasks, false)
                val tvTitle = itemView.findViewById<TextView>(R.id.tvDayTaskTitle)
                val tvStatus = itemView.findViewById<TextView>(R.id.tvDayTaskStatus)
                val priorityBar = itemView.findViewById<View>(R.id.viewDayTaskPriorityBar)

                tvTitle.text = task.title
                tvStatus.text = if (task.isDone) "Completada" else "Pendiente"

                val color = when (task.priority) {
                    "Alta" -> Color.parseColor("#E74C3C")
                    "Media" -> Color.parseColor("#F39C12")
                    else -> Color.parseColor("#27AE60")
                }
                priorityBar.setBackgroundColor(color)

                if (task.isDone) {
                    tvTitle.paintFlags = tvTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                }

                containerDayTasks.addView(itemView)
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnSeeAll.setOnClickListener {
            dialog.dismiss()
            NavUtils.goTo(this, DashboardActivity::class.java, terminarActual = false)
        }

        dialog.show()
    }

    private fun nombreDiaCompleto(diaSemana: Int): String {
        val nombres = arrayOf(
            "Domingo", "Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado"
        )
        return nombres[diaSemana - 1]
    }

    private fun mesEnTexto(mes: Int): String {
        val meses = arrayOf(
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
        )
        return meses[mes]
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
