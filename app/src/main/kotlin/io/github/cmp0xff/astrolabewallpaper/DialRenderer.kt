package io.github.cmp0xff.astrolabewallpaper

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders the astrolabe clock dial onto a [Canvas].
 *
 * Angles in [ClockState] are expected in degrees measured clockwise from 12 o'clock (North).
 * The canvas background is cleared to [BACKGROUND_COLOR] before drawing dial tick marks and hands.
 */
internal class DialRenderer(private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)) {
    /** Clears [canvas] with [BACKGROUND_COLOR] and draws the centered dial ticks and hands for [state]. */
    fun renderDial(canvas: Canvas, state: ClockState) {
        if (canvas.width <= 0 || canvas.height <= 0) return
        canvas.drawColor(BACKGROUND_COLOR)
        val dial =
            Dial(
                centerX = canvas.width / CENTER_DIVISOR,
                centerY = canvas.height / CENTER_DIVISOR,
                radius = minOf(a = canvas.width, b = canvas.height) * RADIUS_FRACTION,
            )
        if (dial.radius <= HOUR_TICK_LENGTH) return
        drawTickMarks(canvas, dial)
        drawHand(
            canvas = canvas,
            dial = dial,
            length = dial.radius * HOUR_HAND_LENGTH,
            angleDegrees = state.hourAngle,
            strokeWidth = HOUR_HAND_WIDTH,
        )
        drawHand(
            canvas = canvas,
            dial = dial,
            length = dial.radius * MINUTE_HAND_LENGTH,
            angleDegrees = state.minuteAngle,
            strokeWidth = MINUTE_HAND_WIDTH,
        )
        drawHand(
            canvas = canvas,
            dial = dial,
            length = dial.radius * SECOND_HAND_LENGTH,
            angleDegrees = state.secondAngle,
            strokeWidth = SECOND_HAND_WIDTH,
        )
    }

    private fun drawTickMarks(canvas: Canvas, dial: Dial) {
        configureDialPaint(MINUTE_TICK_WIDTH)
        for (index in 0 until TICKS_PER_REVOLUTION) {
            val isHourTick = index % TICKS_PER_HOUR == 0
            val tickLength = if (isHourTick) HOUR_TICK_LENGTH else MINUTE_TICK_LENGTH
            val innerRadius = dial.radius - tickLength
            paint.strokeWidth = if (isHourTick) HOUR_TICK_WIDTH else MINUTE_TICK_WIDTH
            drawRadiusLine(
                canvas = canvas,
                dial = dial,
                innerRadius = innerRadius,
                outerRadius = dial.radius,
                angleDegrees = index * DEGREES_PER_TICK,
            )
        }
    }

    private fun drawHand(canvas: Canvas, dial: Dial, length: Float, angleDegrees: Float, strokeWidth: Float) {
        configureDialPaint(strokeWidth)
        drawRadiusLine(
            canvas = canvas,
            dial = dial,
            innerRadius = 0f,
            outerRadius = length,
            angleDegrees = angleDegrees,
        )
    }

    private fun configureDialPaint(strokeWidth: Float) {
        paint.color = DIAL_COLOR
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = strokeWidth
    }

    private fun drawRadiusLine(
        canvas: Canvas,
        dial: Dial,
        innerRadius: Float,
        outerRadius: Float,
        angleDegrees: Float,
    ) {
        val radians = Math.toRadians(angleDegrees.toDouble())
        val sinAngle = sin(radians).toFloat()
        val cosAngle = cos(radians).toFloat()
        canvas.drawLine(
            dial.centerX + innerRadius * sinAngle,
            dial.centerY - innerRadius * cosAngle,
            dial.centerX + outerRadius * sinAngle,
            dial.centerY - outerRadius * cosAngle,
            paint,
        )
    }

    private data class Dial(val centerX: Float, val centerY: Float, val radius: Float)

    private companion object {
        const val DIAL_COLOR: Int = 0xFFD8B26A.toInt()
        const val CENTER_DIVISOR = 2f
        const val RADIUS_FRACTION = 0.3f
        const val HOUR_HAND_LENGTH = 0.5f
        const val MINUTE_HAND_LENGTH = 0.75f
        const val SECOND_HAND_LENGTH = 0.85f
        const val HOUR_HAND_WIDTH = 5f
        const val MINUTE_HAND_WIDTH = 3f
        const val SECOND_HAND_WIDTH = 1f
        const val TICKS_PER_REVOLUTION = 60
        const val TICKS_PER_HOUR = 5
        const val DEGREES_PER_TICK = 6f
        const val HOUR_TICK_LENGTH = 10f
        const val MINUTE_TICK_LENGTH = 5f
        const val HOUR_TICK_WIDTH = 3f
        const val MINUTE_TICK_WIDTH = 1f
    }
}

internal const val BACKGROUND_COLOR: Int = 0xFF111923.toInt()
