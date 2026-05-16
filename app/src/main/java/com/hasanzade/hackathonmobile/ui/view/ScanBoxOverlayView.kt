package com.hasanzade.hackathonmobile.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class ScanBoxOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val cornerPaint = Paint().apply {
        color = Color.parseColor("#52B788")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val cornerLength = 48f  // künc xəttinin uzunluğu

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val r = 12f  // künc radiusu

        // ── Yuxarı sol künc ──
        canvas.drawLine(0f, r + cornerLength, 0f, r, cornerPaint)
        canvas.drawLine(0f, r, r, 0f, cornerPaint)
        canvas.drawLine(r, 0f, r + cornerLength, 0f, cornerPaint)

        // ── Yuxarı sağ künc ──
        canvas.drawLine(w - r - cornerLength, 0f, w - r, 0f, cornerPaint)
        canvas.drawLine(w - r, 0f, w, r, cornerPaint)
        canvas.drawLine(w, r, w, r + cornerLength, cornerPaint)

        // ── Aşağı sol künc ──
        canvas.drawLine(0f, h - r - cornerLength, 0f, h - r, cornerPaint)
        canvas.drawLine(0f, h - r, r, h, cornerPaint)
        canvas.drawLine(r, h, r + cornerLength, h, cornerPaint)

        // ── Aşağı sağ künc ──
        canvas.drawLine(w - r - cornerLength, h, w - r, h, cornerPaint)
        canvas.drawLine(w - r, h, w, h - r, cornerPaint)
        canvas.drawLine(w, h - r, w, h - r - cornerLength, cornerPaint)
    }
}