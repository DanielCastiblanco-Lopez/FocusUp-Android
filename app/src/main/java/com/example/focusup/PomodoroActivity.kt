package com.example.focusup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.app.NotificationCompat
import com.example.focusup.data.PomodoroSession
import com.example.focusup.data.PomodoroState
import com.example.focusup.data.PomodoroStorage
import com.example.focusup.data.StreakStorage
import com.example.focusup.data.TaskStorage
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import java.util.Calendar

class PomodoroActivity : AppCompatActivity() {

    private lateinit var tvTimer: TextView
    private lateinit var tvSessionStatus: TextView
    private lateinit var progressTimer: CircularProgressView
    private lateinit var btnStart: MaterialButton
    private lateinit var btnPause: MaterialButton
    private lateinit var btnReset: MaterialButton
    private lateinit var actvTask: AutoCompleteTextView

    private var timer: android.os.CountDownTimer? = null
    private var totalMillis: Long = 25 * 60 * 1000L   // 25 minutos por defecto
    private var remainingMillis: Long = totalMillis
    private var isRunning = false
    private var isPaused = false

    private val CHANNEL_ID = "focusup_pomodoro_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pomodoro)

        val toolbar = findViewById<Toolbar>(R.id.toolbarPomodoro)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            NavUtils.goTo(this, DashboardActivity::class.java)
        }

        tvTimer = findViewById(R.id.tvTimer)
        tvSessionStatus = findViewById(R.id.tvSessionStatus)
        progressTimer = findViewById(R.id.progressTimer)
        btnStart = findViewById(R.id.btnStart)
        btnPause = findViewById(R.id.btnPause)
        btnReset = findViewById(R.id.btnReset)
        actvTask = findViewById(R.id.actvTask)

        createNotificationChannel()
        setupTaskDropdown()

        val timerCircle = findViewById<CardView>(R.id.cardTimerCenter)
        timerCircle.setOnClickListener {
            if (!isRunning) {
                showEditTimeDialog()
            }
        }

        btnStart.setOnClickListener {
            if (isRunning) {
                pauseTimer()
            } else {
                startTimer()
            }
        }

        btnReset.setOnClickListener {
            resetTimer()
        }

        btnPause.setOnClickListener {
            pauseTimer()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_pomodoro

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
                R.id.nav_pomodoro -> true
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

        restaurarEstadoGuardado()
    }

    override fun onResume() {
        super.onResume()
        mostrarUltimasSesiones()
    }

    override fun onPause() {
        super.onPause()
        guardarEstadoActual()
    }

    private fun restaurarEstadoGuardado() {
        val estado = PomodoroState.load(this)

        totalMillis = estado.totalMillis
        remainingMillis = estado.remainingMillis

        if (estado.taskName.isNotEmpty()) {
            actvTask.setText(estado.taskName, false)
        }

        when {
            estado.alreadyFinished -> {
                isRunning = false
                isPaused = false
                remainingMillis = 0
                updateTimerText()
                updateProgressRing()
                actualizarEstadoVisual()
                onPomodoroFinished()
                resetTimer()
            }
            estado.isRunning -> {
                isRunning = false
                isPaused = false
                updateTimerText()
                updateProgressRing()
                startTimer()
            }
            estado.isPaused -> {
                isRunning = false
                isPaused = true
                updateTimerText()
                updateProgressRing()
                actualizarEstadoVisual()
            }
            else -> {
                isRunning = false
                isPaused = false
                updateTimerText()
                updateProgressRing()
                actualizarEstadoVisual()
            }
        }

        mostrarUltimasSesiones()
    }

    private fun guardarEstadoActual() {
        val tareaNombre = actvTask.text?.toString() ?: ""
        when {
            isRunning -> {
                val tiempoFin = System.currentTimeMillis() + remainingMillis
                PomodoroState.saveRunning(this, tiempoFin, totalMillis, tareaNombre)
            }
            isPaused -> {
                PomodoroState.savePaused(this, remainingMillis, totalMillis, tareaNombre)
            }
            else -> {
                PomodoroState.saveStopped(this, totalMillis)
            }
        }
    }

    private fun setupTaskDropdown() {
        val pendientes = TaskStorage.getTasks(this).filter { !it.isDone }.map { it.title }
        val opciones = if (pendientes.isEmpty()) listOf("Sesion libre") else pendientes + "Sesion libre"
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, opciones)
        actvTask.setAdapter(adapter)
        if (actvTask.text.isNullOrEmpty()) {
            actvTask.setText(opciones.first(), false)
        }
    }

    private fun startTimer() {
        timer?.cancel()
        isRunning = true
        isPaused = false
        actualizarEstadoVisual()

        timer = object : android.os.CountDownTimer(remainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished
                updateTimerText()
                updateProgressRing()
            }

            override fun onFinish() {
                remainingMillis = 0
                isRunning = false
                isPaused = false
                updateTimerText()
                updateProgressRing()
                actualizarEstadoVisual()
                onPomodoroFinished()
            }
        }.start()

        guardarEstadoActual()
    }

    private fun pauseTimer() {
        timer?.cancel()
        isRunning = false
        isPaused = true
        actualizarEstadoVisual()
        guardarEstadoActual()
    }

    private fun resetTimer() {
        timer?.cancel()
        isRunning = false
        isPaused = false
        remainingMillis = totalMillis
        updateTimerText()
        updateProgressRing()
        actualizarEstadoVisual()
        guardarEstadoActual()
    }

    private fun actualizarEstadoVisual() {
        when {
            isRunning -> {
                btnStart.text = "Pausar"
                btnStart.icon = androidx.core.content.ContextCompat.getDrawable(this, android.R.drawable.ic_media_pause)
                tvSessionStatus.text = "En curso"
                tvSessionStatus.background.setTint(Color.parseColor("#18BC9C"))
            }
            isPaused -> {
                btnStart.text = "Reanudar"
                btnStart.icon = androidx.core.content.ContextCompat.getDrawable(this, android.R.drawable.ic_media_play)
                tvSessionStatus.text = "En pausa"
                tvSessionStatus.background.setTint(Color.parseColor("#F39C12"))
            }
            else -> {
                btnStart.text = "Iniciar sesion"
                btnStart.icon = androidx.core.content.ContextCompat.getDrawable(this, android.R.drawable.ic_media_play)
                tvSessionStatus.text = "Listo para empezar"
                tvSessionStatus.background.setTint(Color.parseColor("#7F8C8D"))
            }
        }
    }

    private fun updateTimerText() {
        val totalSegundos = remainingMillis / 1000
        val horas = totalSegundos / 3600
        val minutos = (totalSegundos % 3600) / 60
        val segundos = totalSegundos % 60

        tvTimer.text = if (horas > 0) {
            String.format("%d:%02d:%02d", horas, minutos, segundos)
        } else {
            String.format("%02d:%02d", minutos, segundos)
        }
    }

    private fun updateProgressRing() {
        if (totalMillis <= 0) return
        val porcentajeRestante = (remainingMillis.toFloat() / totalMillis.toFloat())
        progressTimer.progress = porcentajeRestante
    }

    private fun showEditTimeDialog() {
        val totalMinutosActuales = (totalMillis / 1000 / 60).toInt()
        val horasActuales = totalMinutosActuales / 60
        val minutosActuales = totalMinutosActuales % 60

        val view = layoutInflater.inflate(R.layout.dialog_edit_time, null)
        val pickerHours = view.findViewById<android.widget.NumberPicker>(R.id.pickerHours)
        val pickerMinutes = view.findViewById<android.widget.NumberPicker>(R.id.pickerMinutes)

        pickerHours.minValue = 0
        pickerHours.maxValue = 5
        pickerHours.value = horasActuales

        pickerMinutes.minValue = 0
        pickerMinutes.maxValue = 59
        pickerMinutes.value = minutosActuales

        android.app.AlertDialog.Builder(this)
            .setTitle("Duracion de la sesion")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val horas = pickerHours.value
                val minutos = pickerMinutes.value
                val totalMinutos = horas * 60 + minutos

                if (totalMinutos > 0) {
                    totalMillis = totalMinutos * 60 * 1000L
                    remainingMillis = totalMillis
                    updateTimerText()
                    updateProgressRing()
                    guardarEstadoActual()
                } else {
                    android.widget.Toast.makeText(this, "La sesion debe durar al menos 1 minuto", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun onPomodoroFinished() {
        playSound()
        vibrate()
        showNotification()
        saveCompletedSession()
        PomodoroState.clear(this)
        mostrarUltimasSesiones()
    }

    /** Guarda la sesion completada. Ya NO se guarda un texto de fecha fijo:
     * la fecha se calcula siempre al mostrarla, usando el timestamp real. */
    private fun saveCompletedSession() {
        val minutosCompletados = (totalMillis / 1000 / 60).toInt()
        val tareaNombre = actvTask.text.toString().ifEmpty { "Sesion libre" }
        val calendar = Calendar.getInstance()

        PomodoroStorage.addSession(
            this,
            PomodoroSession(
                taskName = tareaNombre,
                minutes = minutosCompletados,
                dateLabel = "",
                dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            )
        )
        StreakStorage.markActiveToday(this)
    }

    private fun mostrarUltimasSesiones() {
        val container = findViewById<LinearLayout>(R.id.containerRecentSessionsPomodoro)
        val tvNoSessions = findViewById<TextView>(R.id.tvNoSessionsPomodoro)
        container.removeAllViews()

        val sesiones = PomodoroStorage.getSessions(this).take(3)

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

    private fun playSound() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, notification)
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(500)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pomodoro",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notificaciones del temporizador Pomodoro"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("FocusUp")
            .setContentText("Tu sesion Pomodoro ha terminado!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val manager = androidx.core.app.NotificationManagerCompat.from(this)
        if (Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            manager.notify(1, builder.build())
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
