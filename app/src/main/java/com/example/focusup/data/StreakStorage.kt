package com.example.focusup.data

import android.content.Context
import java.util.Calendar

/**
 * Calcula y guarda la racha de dias consecutivos con actividad
 * (al menos una tarea completada o una sesion Pomodoro hecha ese dia).
 */
object StreakStorage {

    private const val PREFS_NAME = "focusup_prefs"
    private const val KEY_LAST_ACTIVE_DAY = "streak_last_active_day"   // dia del año (1-366)
    private const val KEY_LAST_ACTIVE_YEAR = "streak_last_active_year"
    private const val KEY_CURRENT_STREAK = "streak_current_count"
    private const val KEY_MAX_STREAK = "streak_max_count"

    /**
     * Marca el dia de hoy como activo. Si ayer tambien fue activo o ya se
     * marco hoy, la racha sigue/se mantiene. Si hubo un salto de mas de
     * un dia, la racha se reinicia a 1.
     * Llamar esto cada vez que se completa una tarea o una sesion Pomodoro.
     */
    fun markActiveToday(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = Calendar.getInstance()
        val todayDay = today.get(Calendar.DAY_OF_YEAR)
        val todayYear = today.get(Calendar.YEAR)

        val lastDay = prefs.getInt(KEY_LAST_ACTIVE_DAY, -1)
        val lastYear = prefs.getInt(KEY_LAST_ACTIVE_YEAR, -1)
        val currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)

        if (lastDay == todayDay && lastYear == todayYear) {
            // Ya se marco hoy, no hacer nada
            return
        }

        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayDay = yesterday.get(Calendar.DAY_OF_YEAR)
        val yesterdayYear = yesterday.get(Calendar.YEAR)

        val nuevaRacha = if (lastDay == yesterdayDay && lastYear == yesterdayYear) {
            currentStreak + 1
        } else {
            1
        }

        val rachaMaxima = prefs.getInt(KEY_MAX_STREAK, 0)
        val nuevaRachaMaxima = if (nuevaRacha > rachaMaxima) nuevaRacha else rachaMaxima

        prefs.edit()
            .putInt(KEY_LAST_ACTIVE_DAY, todayDay)
            .putInt(KEY_LAST_ACTIVE_YEAR, todayYear)
            .putInt(KEY_CURRENT_STREAK, nuevaRacha)
            .putInt(KEY_MAX_STREAK, nuevaRachaMaxima)
            .apply()
    }

    /** Devuelve la racha mas larga que el usuario ha logrado alguna vez */
    fun getMaxStreak(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val maxGuardada = prefs.getInt(KEY_MAX_STREAK, 0)
        val actual = getCurrentStreak(context)
        return maxOf(maxGuardada, actual)
    }

    /**
     * Devuelve la racha actual, ya considerando si el ultimo dia activo
     * fue hoy o ayer (vigente) o hace mas tiempo (rota, devuelve 0).
     */
    fun getCurrentStreak(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastDay = prefs.getInt(KEY_LAST_ACTIVE_DAY, -1)
        val lastYear = prefs.getInt(KEY_LAST_ACTIVE_YEAR, -1)
        val currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)

        if (lastDay == -1) return 0

        val today = Calendar.getInstance()
        val todayDay = today.get(Calendar.DAY_OF_YEAR)
        val todayYear = today.get(Calendar.YEAR)

        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayDay = yesterday.get(Calendar.DAY_OF_YEAR)
        val yesterdayYear = yesterday.get(Calendar.YEAR)

        val sigueVigente = (lastDay == todayDay && lastYear == todayYear) ||
            (lastDay == yesterdayDay && lastYear == yesterdayYear)

        return if (sigueVigente) currentStreak else 0
    }
}
