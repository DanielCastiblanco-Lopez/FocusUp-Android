package com.example.focusup.data

/**
 * Representa una sesion Pomodoro completada.
 */
data class PomodoroSession(
    val id: Long = System.currentTimeMillis(),
    val taskName: String,
    val minutes: Int,
    val dateLabel: String,    // ej: "Hoy, 10:00 AM" o "19 jun, 3:00 PM"
    val dayOfWeek: Int,       // 1 = Domingo ... 7 = Sabado (Calendar.DAY_OF_WEEK)
    val timestamp: Long = System.currentTimeMillis()
)
