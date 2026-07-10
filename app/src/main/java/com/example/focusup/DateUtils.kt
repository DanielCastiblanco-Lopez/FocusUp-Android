package com.example.focusup

import java.util.Calendar

/**
 * Funciones de ayuda para mostrar fechas de forma legible (Hoy, Ayer, o fecha completa),
 * siempre calculadas a partir del timestamp real, nunca de un texto guardado previamente.
 */
object DateUtils {

    private val MESES = arrayOf(
        "ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sep", "oct", "nov", "dic"
    )

    /** Devuelve algo como "Hoy, 04:21 PM", "Ayer, 09:50 AM" o "19 jun, 03:00 PM" */
    fun formatSessionDate(timestampMillis: Long): String {
        val fecha = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val hoy = Calendar.getInstance()
        val ayer = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val horaFormateada = formatHora(fecha)

        return when {
            esMismoDia(fecha, hoy) -> "Hoy, $horaFormateada"
            esMismoDia(fecha, ayer) -> "Ayer, $horaFormateada"
            else -> {
                val dia = fecha.get(Calendar.DAY_OF_MONTH)
                val mes = MESES[fecha.get(Calendar.MONTH)]
                "$dia $mes, $horaFormateada"
            }
        }
    }

    private fun formatHora(cal: Calendar): String {
        val horas = cal.get(Calendar.HOUR)
        val minutos = cal.get(Calendar.MINUTE)
        val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
        return String.format("%02d:%02d %s", if (horas == 0) 12 else horas, minutos, amPm)
    }

    private fun esMismoDia(a: Calendar, b: Calendar): Boolean {
        return a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR) &&
            a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
    }
}
