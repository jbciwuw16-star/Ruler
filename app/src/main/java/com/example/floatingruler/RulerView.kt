package com.example.floatingruler

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Penggaris berbentuk garis dengan 2 ujung (p1 & p2) dalam koordinat layar.
 * Window overlay otomatis di-resize mengikuti bounding box garis,
 * jadi area di luar garis tetap bisa disentuh (tembus ke aplikasi di bawahnya).
 */
class RulerView(
    context: Context,
    private val onLayoutChanged: (x: Int, y: Int, w: Int, h: Int) -> Unit
) : View(context) {

    private val dm = resources.displayMetrics
    private val d = dm.density
    private val pxPerMm = (dm.xdpi + dm.ydpi) / 2f / 25.4f   // kalibrasi dari DPI fisik layar

    private val pad = 56 * d          // ruang ekstra di sekeliling garis
    private val halfBand = 20 * d     // setengah lebar pita penggaris
    private val handleR = 14 * d
    private val hitHandle = 30 * d
    private val hitBody = 26 * d

    private var p1x = dm.widthPixels * 0.2f
    private var p1y = dm.heightPixels * 0.4f
    private var p2x = dm.widthPixels * 0.8f
    private var p2y = p1y

    private var originX = 0
    private var originY = 0

    private enum class Mode { NONE, P1, P2, BODY }
    private var mode = Mode.NONE
    private var lastX = 0f
    private var lastY = 0f

    private val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x88FFC107.toInt() }
    private val bandStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8D6E00.toInt(); style = Paint.Style.STROKE; strokeWidth = 1.5f * d
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF222222.toInt(); strokeWidth = 1f * d
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE53935.toInt(); strokeWidth = 1.5f * d
    }
    private val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF222222.toInt(); textSize = 11 * d
    }
    private val handleFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE53935.toInt() }
    private val handleStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 2.5f * d
    }
    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xDD212121.toInt() }
    private val pillText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt(); textSize = 13 * d; textAlign = Paint.Align.CENTER
    }

    /** Hitung ulang bounding box lalu kabari service supaya window di-resize. */
    fun relayout() {
        val left = min(p1x, p2x) - pad
        val top = min(p1y, p2y) - pad
        val right = max(p1x, p2x) + pad
        val bottom = max(p1y, p2y) + pad
        originX = left.toInt()
        originY = top.toInt()
        onLayoutChanged(originX, originY, (right - left).toInt() + 1, (bottom - top).toInt() + 1)
        invalidate()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.rawX
        val y = e.rawY
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mode = when {
                    hypot(x - p1x, y - p1y) <= hitHandle -> Mode.P1
                    hypot(x - p2x, y - p2y) <= hitHandle -> Mode.P2
                    distToSegment(x, y) <= hitBody -> Mode.BODY
                    else -> Mode.NONE
                }
                lastX = x; lastY = y
                return mode != Mode.NONE
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = x - lastX
                val dy = y - lastY
                lastX = x; lastY = y
                when (mode) {
                    Mode.P1 -> { p1x += dx; p1y += dy }
                    Mode.P2 -> { p2x += dx; p2y += dy }
                    Mode.BODY -> { p1x += dx; p1y += dy; p2x += dx; p2y += dy }
                    Mode.NONE -> return false
                }
                relayout()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mode = Mode.NONE
                return true
            }
        }
        return false
    }

    private fun distToSegment(px: Float, py: Float): Float {
        val vx = p2x - p1x
        val vy = p2y - p1y
        val len2 = vx * vx + vy * vy
        if (len2 == 0f) return hypot(px - p1x, py - p1y)
        val t = (((px - p1x) * vx + (py - p1y) * vy) / len2).coerceIn(0f, 1f)
        return hypot(px - (p1x + t * vx), py - (p1y + t * vy))
    }

    override fun onDraw(canvas: Canvas) {
        val dx = p2x - p1x
        val dy = p2y - p1y
        val len = hypot(dx, dy)
        val angle = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()

        canvas.translate(-originX.toFloat(), -originY.toFloat())

        // --- pita penggaris + tick (digambar di frame yang diputar) ---
        canvas.save()
        canvas.translate(p1x, p1y)
        canvas.rotate(angle)

        val band = RectF(0f, -halfBand, len, halfBand)
        canvas.drawRect(band, bandPaint)
        canvas.drawRect(band, bandStroke)

        val totalMm = floor(len / pxPerMm).toInt()
        val step = if (pxPerMm < 3f) 5 else 1
        var mm = 0
        while (mm <= totalMm) {
            val x = mm * pxPerMm
            val tl = when {
                mm % 10 == 0 -> 16 * d
                mm % 5 == 0 -> 11 * d
                else -> 7 * d
            }
            canvas.drawLine(x, -halfBand, x, -halfBand + tl, tickPaint)
            if (mm % 10 == 0 && mm != 0) {
                val label = (mm / 10).toString()
                val w = numPaint.measureText(label)
                canvas.drawText(label, x - w / 2f, -halfBand + tl + 12 * d, numPaint)
            }
            mm += step
        }

        canvas.drawLine(0f, 0f, len, 0f, centerPaint)   // garis tengah merah
        canvas.restore()

        // --- handle ujung ---
        for ((hx, hy) in listOf(p1x to p1y, p2x to p2y)) {
            canvas.drawCircle(hx, hy, handleR, handleFill)
            canvas.drawCircle(hx, hy, handleR, handleStroke)
        }

        // --- label panjang & sudut di tengah ---
        val text = "%.2f cm  |  %.0f°".format(len / pxPerMm / 10f, angle)
        val cx = (p1x + p2x) / 2f
        val cy = (p1y + p2y) / 2f
        val tw = pillText.measureText(text)
        val rect = RectF(cx - tw / 2f - 10 * d, cy - 14 * d, cx + tw / 2f + 10 * d, cy + 14 * d)
        canvas.drawRoundRect(rect, 14 * d, 14 * d, pillPaint)
        canvas.drawText(text, cx, cy + 5 * d, pillText)
    }
}
