package com.example.focusup.data

import android.content.Context

/**
 * Guarda el estado del temporizador Pomodoro para que sobreviva a cambios
 * de pantalla, minimizar la app, o incluso cerrarla y volver a abrirla.
 *
 * En vez de guardar "milisegundos restantes" (que se queda desactualizado
 * en cuanto sales de la pantalla), guardamos CUANDO debe terminar la sesion
 * (timestamp absoluto). Asi, en cualquier momento podemos calcular el tiempo
 * restante real con: tiempoFin - ahora.
 */
object PomodoroState {

    private const val PREFS_NAME = "focusup_prefs"
    private const val KEY_IS_RUNNING = "pomodoro_is_running"
    private const val KEY_IS_PAUSED = "pomodoro_is_paused"
    private const val KEY_END_TIME = "pomodoro_end_time"          // timestamp en el que termina, si esta corriendo
    private const val KEY_REMAINING_MILLIS = "pomodoro_remaining_millis"  // tiempo restante guardado, si esta pausado o detenido
    private const val KEY_TOTAL_MILLIS = "pomodoro_total_millis"
    private const val KEY_TASK_NAME = "pomodoro_task_name"

    fun saveRunning(context: Context, endTimeMillis: Long, totalMillis: Long, taskName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_RUNNING, true)
            .putBoolean(KEY_IS_PAUSED, false)
            .putLong(KEY_END_TIME, endTimeMillis)
            .putLong(KEY_TOTAL_MILLIS, totalMillis)
            .putString(KEY_TASK_NAME, taskName)
            .apply()
    }

    fun savePaused(context: Context, remainingMillis: Long, totalMillis: Long, taskName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_RUNNING, false)
            .putBoolean(KEY_IS_PAUSED, true)
            .putLong(KEY_REMAINING_MILLIS, remainingMillis)
            .putLong(KEY_TOTAL_MILLIS, totalMillis)
            .putString(KEY_TASK_NAME, taskName)
            .apply()
    }

    fun saveStopped(context: Context, totalMillis: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_RUNNING, false)
            .putBoolean(KEY_IS_PAUSED, false)
            .putLong(KEY_REMAINING_MILLIS, totalMillis)
            .putLong(KEY_TOTAL_MILLIS, totalMillis)
            .apply()
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_RUNNING, false)
            .putBoolean(KEY_IS_PAUSED, false)
            .apply()
    }

    data class SavedState(
        val isRunning: Boolean,
        val isPaused: Boolean,
        val remainingMillis: Long,
        val totalMillis: Long,
        val taskName: String,
        /** true si el tiempo ya se agoto mientras la app estaba cerrada/en otra pantalla */
        val alreadyFinished: Boolean
    )

    fun load(context: Context): SavedState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
        val isPaused = prefs.getBoolean(KEY_IS_PAUSED, false)
        val totalMillis = prefs.getLong(KEY_TOTAL_MILLIS, 25 * 60 * 1000L)
        val taskName = prefs.getString(KEY_TASK_NAME, "") ?: ""

        if (isRunning) {
            val endTime = prefs.getLong(KEY_END_TIME, 0L)
            val ahora = System.currentTimeMillis()
            val restante = endTime - ahora

            return if (restante <= 0) {
                // El tiempo ya se acabo mientras no estabamos viendo la pantalla
                SavedState(
                    isRunning = false,
                    isPaused = false,
                    remainingMillis = 0L,
                    totalMillis = totalMillis,
                    taskName = taskName,
                    alreadyFinished = true
                )
            } else {
                SavedState(
                    isRunning = true,
                    isPaused = false,
                    remainingMillis = restante,
                    totalMillis = totalMillis,
                    taskName = taskName,
                    alreadyFinished = false
                )
            }
        }

        val remaining = prefs.getLong(KEY_REMAINING_MILLIS, totalMillis)
        return SavedState(
            isRunning = false,
            isPaused = isPaused,
            remainingMillis = remaining,
            totalMillis = totalMillis,
            taskName = taskName,
            alreadyFinished = false
        )
    }
}
