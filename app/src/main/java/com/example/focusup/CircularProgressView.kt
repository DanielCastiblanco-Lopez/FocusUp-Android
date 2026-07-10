package com.example.focusup

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Vista personalizada que dibuja un anillo de progreso circular.
 * No depende de ningun estilo del sistema, asi que se ve igual en
 * cualquier version de Android sin animaciones raras ni "loading" infinito.
 */
class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#D5DBDB")
        style = Paint.Style.STROKE
        strokeWidth = 18f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint().apply {
        color = Color.parseColor("#18BC9C")
        style = Paint.Style.STROKE
        strokeWidth = 18f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val arcRect = RectF()

    /** Progreso de 0.0 a 1.0 (1.0 = anillo completo) */
    var progress: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val strokeHalf = backgroundPaint.strokeWidth / 2f
        arcRect.set(strokeHalf, strokeHalf, w - strokeHalf, h - strokeHalf)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Pista de fondo, anillo completo
        canvas.drawArc(arcRect, 0f, 360f, false, backgroundPaint)
        // Progreso real, empezando arriba (-90 grados) en sentido horario
        val sweepAngle = 360f * progress
        canvas.drawArc(arcRect, -90f, sweepAngle, false, progressPaint)
    }
}
