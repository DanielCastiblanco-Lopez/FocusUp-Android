package com.example.focusup

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup

/**
 * Crea un Dialog "limpio": sin el marco/padding gris por defecto de AlertDialog,
 * para que solo se vea la tarjeta blanca con esquinas redondeadas que diseñamos
 * en cada layout (dialog_add_task.xml, dialog_edit_profile.xml, etc).
 */
object DialogUtils {

    fun createCustomDialog(context: Context, layoutResId: Int): Dialog {
        val dialog = Dialog(context)
        val view = LayoutInflater.from(context).inflate(layoutResId, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return dialog
    }
}
