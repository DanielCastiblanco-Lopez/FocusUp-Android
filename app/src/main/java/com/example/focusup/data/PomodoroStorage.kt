package com.example.focusup.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * Guarda y lee las sesiones Pomodoro completadas, en SharedPreferences como JSON.
 * Incluye funciones para filtrar por periodo (semana actual, mes actual, o todo).
 */
object PomodoroStorage {

    private const val PREFS_NAME = "focusup_prefs"
    private const val KEY_SESSIONS = "pomodoro_sessions_json"

    enum class Period { WEEK, MONTH, ALL }

    fun getSessions(context: Context): MutableList<PomodoroSession> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SESSIONS, null) ?: return mutableListOf()

        val list = mutableListOf<PomodoroSession>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                PomodoroSession(
                    id = obj.getLong("id"),
                    taskName = obj.getString("taskName"),
                    minutes = obj.getInt("minutes"),
                    dateLabel = obj.getString("dateLabel"),
                    dayOfWeek = obj.getInt("dayOfWeek"),
                    timestamp = obj.getLong("timestamp")
                )
            )
        }
        return list
    }

    private fun saveSessions(context: Context, sessions: List<PomodoroSession>) {
        val array = JSONArray()
        for (s in sessions) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("taskName", s.taskName)
            obj.put("minutes", s.minutes)
            obj.put("dateLabel", s.dateLabel)
            obj.put("dayOfWeek", s.dayOfWeek)
            obj.put("timestamp", s.timestamp)
            array.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SESSIONS, array.toString()).apply()
    }

    fun addSession(context: Context, session: PomodoroSession) {
        val sessions = getSessions(context)
        sessions.add(0, session)
        saveSessions(context, sessions)
    }

    // ===================== Limites de periodo =====================

    /** Timestamp del Lunes 00:00:00 de la semana actual */
    private fun inicioSemanaActual(): Long {
        val cal = Calendar.getInstance()
        val diaSemana = cal.get(Calendar.DAY_OF_WEEK) // 1=Domingo .. 7=Sabado
        val diasARetroceder = if (diaSemana == Calendar.SUNDAY) 6 else diaSemana - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_YEAR, -diasARetroceder)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Timestamp del dia 1 00:00:00 del mes actual */
    private fun inicioMesActual(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Filtra las sesiones segun el periodo solicitado */
    fun getSessionsForPeriod(context: Context, period: Period): List<PomodoroSession> {
        val sessions = getSessions(context)
        return when (period) {
            Period.WEEK -> {
                val inicio = inicioSemanaActual()
                sessions.filter { it.timestamp >= inicio }
            }
            Period.MONTH -> {
                val inicio = inicioMesActual()
                sessions.filter { it.timestamp >= inicio }
            }
            Period.ALL -> sessions
        }
    }

    fun getTotalMinutesForPeriod(context: Context, period: Period): Int =
        getSessionsForPeriod(context, period).sumOf { it.minutes }

    fun getSessionCountForPeriod(context: Context, period: Period): Int =
        getSessionsForPeriod(context, period).size

    // ===================== Compatibilidad con codigo existente =====================

    fun getTotalMinutes(context: Context): Int = getTotalMinutesForPeriod(context, Period.ALL)

    fun getMinutesToday(context: Context): Int {
        val today = Calendar.getInstance()
        val todayDay = today.get(Calendar.DAY_OF_YEAR)
        val todayYear = today.get(Calendar.YEAR)

        return getSessions(context).filter { session ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = session.timestamp
            cal.get(Calendar.DAY_OF_YEAR) == todayDay &&
                cal.get(Calendar.YEAR) == todayYear
        }.sumOf { it.minutes }
    }

    fun getSessionCount(context: Context): Int = getSessionCountForPeriod(context, Period.ALL)

    /**
     * Minutos estudiados por dia, para la semana actual (Lunes a Domingo).
     * Devuelve un mapa donde la clave es Calendar.DAY_OF_WEEK (1=Domingo .. 7=Sabado)
     */
    fun getMinutesByDayOfWeek(context: Context): Map<Int, Int> {
        val sessions = getSessionsForPeriod(context, Period.WEEK)
        val result = mutableMapOf<Int, Int>()
        for (s in sessions) {
            result[s.dayOfWeek] = (result[s.dayOfWeek] ?: 0) + s.minutes
        }
        return result
    }

    // ===================== Metricas avanzadas para el resumen total =====================

    /** Numero de dias distintos (con al menos una sesion) en toda la historia */
    fun getTotalDaysStudied(context: Context): Int {
        val sessions = getSessions(context)
        val diasUnicos = sessions.map { session ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = session.timestamp
            Pair(cal.get(Calendar.DAY_OF_YEAR), cal.get(Calendar.YEAR))
        }.toSet()
        return diasUnicos.size
    }

    /** Promedio de minutos por dia estudiado (no por dia calendario, sino por dia con actividad) */
    fun getAverageMinutesPerDay(context: Context): Int {
        val totalMinutos = getTotalMinutes(context)
        val diasEstudiados = getTotalDaysStudied(context)
        return if (diasEstudiados > 0) totalMinutos / diasEstudiados else 0
    }

    /** El record de minutos estudiados en un solo dia */
    fun getRecordMinutesInADay(context: Context): Int {
        val sessions = getSessions(context)
        val minutosPorDia = mutableMapOf<Pair<Int, Int>, Int>()
        for (s in sessions) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = s.timestamp
            val clave = Pair(cal.get(Calendar.DAY_OF_YEAR), cal.get(Calendar.YEAR))
            minutosPorDia[clave] = (minutosPorDia[clave] ?: 0) + s.minutes
        }
        return minutosPorDia.values.maxOrNull() ?: 0
    }

    /** Minutos estudiados en la semana mas productiva de toda la historia */
    fun getBestWeekMinutes(context: Context): Int {
        val sessions = getSessions(context)
        if (sessions.isEmpty()) return 0

        val minutosPorSemana = mutableMapOf<Pair<Int, Int>, Int>()
        for (s in sessions) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = s.timestamp
            val clave = Pair(cal.get(Calendar.WEEK_OF_YEAR), cal.get(Calendar.YEAR))
            minutosPorSemana[clave] = (minutosPorSemana[clave] ?: 0) + s.minutes
        }
        return minutosPorSemana.values.maxOrNull() ?: 0
    }
}
