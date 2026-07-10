package com.example.focusup

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.example.focusup.data.PomodoroStorage
import com.example.focusup.data.TaskStorage
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar

class StatisticsActivity : AppCompatActivity() {

    private var periodoActual = PomodoroStorage.Period.WEEK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)



        val btnPeriodWeek = findViewById<TextView>(R.id.btnPeriodWeek)
        val btnPeriodMonth = findViewById<TextView>(R.id.btnPeriodMonth)
        val btnPeriodTotal = findViewById<TextView>(R.id.btnPeriodTotal)

        btnPeriodWeek.setOnClickListener {
            periodoActual = PomodoroStorage.Period.WEEK
            actualizarBotonesPeriodo(btnPeriodWeek, btnPeriodMonth, btnPeriodTotal)
            cargarEstadisticas()
        }
        btnPeriodMonth.setOnClickListener {
            periodoActual = PomodoroStorage.Period.MONTH
            actualizarBotonesPeriodo(btnPeriodMonth, btnPeriodWeek, btnPeriodTotal)
            cargarEstadisticas()
        }
        btnPeriodTotal.setOnClickListener {
            periodoActual = PomodoroStorage.Period.ALL
            actualizarBotonesPeriodo(btnPeriodTotal, btnPeriodWeek, btnPeriodMonth)
            cargarEstadisticas()
        }

        val cardViewFullSummary = findViewById<CardView>(R.id.cardViewFullSummary)
        cardViewFullSummary.setOnClickListener {
            startActivity(Intent(this, FullSummaryActivity::class.java))
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_statistics

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    NavUtils.goTo(this, HomeActivity::class.java)
                    true
                }
                R.id.nav_tasks -> {
                    NavUtils.goTo(this, DashboardActivity::class.java)
                    true
                }
                R.id.nav_pomodoro -> {
                    NavUtils.goTo(this, PomodoroActivity::class.java)
                    true
                }
                R.id.nav_statistics -> true
                R.id.nav_profile -> {
                    NavUtils.goTo(this, ProfileActivity::class.java)
                    true
                }
                else -> false
            }
        }

        cargarEstadisticas()
    }

    override fun onResume() {
        super.onResume()
        cargarEstadisticas()
    }

    /** Marca visualmente cual boton de periodo esta activo (fondo turquesa) y cuales no */
    private fun actualizarBotonesPeriodo(seleccionado: TextView, vararg otros: TextView) {
        seleccionado.setBackgroundResource(R.drawable.bg_period_selected)
        seleccionado.setTextColor(Color.parseColor("#FFFFFF"))
        for (boton in otros) {
            boton.background = null
            boton.setTextColor(Color.parseColor("#7F8C8D"))
        }
    }

    private fun cargarEstadisticas() {
        val tvPeriodTitle = findViewById<TextView>(R.id.tvPeriodTitle)
        tvPeriodTitle.text = when (periodoActual) {
            PomodoroStorage.Period.WEEK -> "Tu progreso esta semana"
            PomodoroStorage.Period.MONTH -> "Tu progreso este mes"
            PomodoroStorage.Period.ALL -> "Tu progreso total"
        }

        val tareasCompletadas = TaskStorage.getCompletedCountForPeriod(this, periodoActual)
        val minutosTotales = PomodoroStorage.getTotalMinutesForPeriod(this, periodoActual)
        val totalSesiones = PomodoroStorage.getSessionCountForPeriod(this, periodoActual)

        findViewById<TextView>(R.id.tvCompletedCount).text = tareasCompletadas.toString()
        findViewById<TextView>(R.id.tvMinutes).text = minutosTotales.toString()
        findViewById<TextView>(R.id.tvSessions).text = totalSesiones.toString()

        val cardWeekChart = findViewById<CardView>(R.id.cardWeekChart)
        val cardViewFullSummary = findViewById<CardView>(R.id.cardViewFullSummary)

        when (periodoActual) {
            PomodoroStorage.Period.WEEK -> {
                cardWeekChart.visibility = View.VISIBLE
                cardViewFullSummary.visibility = View.GONE
                dibujarGraficoSemanal()
            }
            PomodoroStorage.Period.ALL -> {
                cardWeekChart.visibility = View.GONE
                cardViewFullSummary.visibility = View.VISIBLE
            }
            PomodoroStorage.Period.MONTH -> {
                cardWeekChart.visibility = View.GONE
                cardViewFullSummary.visibility = View.GONE
            }
        }

        mostrarUltimasSesiones()
    }

    private fun dibujarGraficoSemanal() {
        val container = findViewById<LinearLayout>(R.id.containerWeekChart)
        container.removeAllViews()

        val minutosPorDia = PomodoroStorage.getMinutesByDayOfWeek(this)
        val diasOrdenados = listOf(
            2 to "Lun", 3 to "Mar", 4 to "Mie", 5 to "Jue", 6 to "Vie", 7 to "Sab", 1 to "Dom"
        )

        val maxMinutos = (minutosPorDia.values.maxOrNull() ?: 0).coerceAtLeast(30)
        val alturaMaximaPx = dpToPx(95)

        val hoyDiaSemana = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        for ((diaCalendar, etiqueta) in diasOrdenados) {
            val minutos = minutosPorDia[diaCalendar] ?: 0
            val esHoy = diaCalendar == hoyDiaSemana

            val columna = LinearLayout(this)
            columna.orientation = LinearLayout.VERTICAL
            columna.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            val columnaParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT)
            columnaParams.weight = 1f
            columna.layoutParams = columnaParams

            val tvValor = TextView(this)
            tvValor.text = if (minutos > 0) minutos.toString() else ""
            tvValor.textSize = 10f
            tvValor.gravity = Gravity.CENTER
            tvValor.setTextColor(Color.parseColor("#2C3E50"))
            columna.addView(tvValor)

            val alturaBarra = if (maxMinutos > 0) {
                ((minutos.toFloat() / maxMinutos.toFloat()) * alturaMaximaPx).toInt().coerceAtLeast(dpToPx(4))
            } else dpToPx(4)

            val barra = View(this)
            val barraParams = LinearLayout.LayoutParams(dpToPx(20), alturaBarra)
            barraParams.topMargin = dpToPx(4)
            barra.layoutParams = barraParams
            barra.setBackgroundResource(R.drawable.bg_chart_bar)
            barra.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (esHoy) Color.parseColor("#18BC9C") else Color.parseColor("#A9DFD8")
            )
            columna.addView(barra)

            val tvDia = TextView(this)
            tvDia.text = etiqueta
            tvDia.textSize = 11f
            tvDia.gravity = Gravity.CENTER
            tvDia.setTextColor(
                if (esHoy) Color.parseColor("#18BC9C") else Color.parseColor("#7F8C8D")
            )
            if (esHoy) tvDia.setTypeface(null, android.graphics.Typeface.BOLD)
            val diaParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            diaParams.topMargin = dpToPx(6)
            tvDia.layoutParams = diaParams
            columna.addView(tvDia)

            container.addView(columna)
        }
    }

    private fun mostrarUltimasSesiones() {
        val container = findViewById<LinearLayout>(R.id.containerRecentSessions)
        val tvNoSessions = findViewById<TextView>(R.id.tvNoSessions)
        container.removeAllViews()

        val sesiones = PomodoroStorage.getSessionsForPeriod(this, periodoActual).take(5)

        if (sesiones.isEmpty()) {
            tvNoSessions.visibility = View.VISIBLE
            return
        }
        tvNoSessions.visibility = View.GONE

        for (sesion in sesiones) {
            val card = CardView(this)
            val cardParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            cardParams.bottomMargin = dpToPx(10)
            card.layoutParams = cardParams
            card.radius = dpToPx(12).toFloat()
            card.cardElevation = dpToPx(3).toFloat()
            card.setCardBackgroundColor(Color.parseColor("#FFFFFF"))

            val fila = LinearLayout(this)
            fila.orientation = LinearLayout.HORIZONTAL
            fila.gravity = Gravity.CENTER_VERTICAL
            fila.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))

            val columnaTexto = LinearLayout(this)
            columnaTexto.orientation = LinearLayout.VERTICAL
            val columnaParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT)
            columnaParams.weight = 1f
            columnaTexto.layoutParams = columnaParams

            val tvNombre = TextView(this)
            tvNombre.text = sesion.taskName
            tvNombre.setTextColor(Color.parseColor("#2C3E50"))
            tvNombre.textSize = 14f
            tvNombre.setTypeface(null, android.graphics.Typeface.BOLD)
            columnaTexto.addView(tvNombre)

            val tvFecha = TextView(this)
            tvFecha.text = DateUtils.formatSessionDate(sesion.timestamp)
            tvFecha.setTextColor(Color.parseColor("#7F8C8D"))
            tvFecha.textSize = 12f
            columnaTexto.addView(tvFecha)

            fila.addView(columnaTexto)

            val tvMinutos = TextView(this)
            tvMinutos.text = "${sesion.minutes} min"
            tvMinutos.setTextColor(Color.parseColor("#18BC9C"))
            tvMinutos.textSize = 16f
            tvMinutos.setTypeface(null, android.graphics.Typeface.BOLD)
            fila.addView(tvMinutos)

            card.addView(fila)
            container.addView(card)
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
