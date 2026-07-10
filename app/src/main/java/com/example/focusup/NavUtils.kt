package com.example.focusup

import android.app.Activity
import android.content.Intent

/**
 * Funciones de ayuda para navegar entre pantallas con una animacion
 * de transicion suave (slide + fade), en vez del salto seco por defecto.
 */
object NavUtils {

    fun goTo(activity: Activity, destino: Class<*>, terminarActual: Boolean = true) {
        val intent = Intent(activity, destino)
        activity.startActivity(intent)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        if (terminarActual) activity.finish()
    }
}
