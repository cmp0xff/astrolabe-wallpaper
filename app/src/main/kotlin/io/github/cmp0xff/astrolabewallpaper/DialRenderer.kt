package io.github.cmp0xff.astrolabewallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin

/** Draws the astrolabe-style clock dial onto [canvas] for the given hand [state]. */
internal fun renderDial(canvas: Canvas, state: ClockState) {
    canvas.drawColor(BACKGROUND_COLOR)
    val dial =
        Dial(
            centerX = canvas.width / CENTER_DIVISOR,
            centerY = canvas.height / CENTER_DIVISOR,
            radius = minOf(a = canvas.width, b = canvas.height) * RADIUS_FRACTION,
        )
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
        val innerRadius = dial.radius - if (isHourTick) HOUR_TICK_LENGTH else MINUTE_TICK_LENGTH
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

private fun drawRadiusLine(canvas: Canvas, dial: Dial, innerRadius: Float, outerRadius: Float, angleDegrees: Float) {
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

private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

internal val BACKGROUND_COLOR: Int = Color.rgb(17, 25, 35)
internal val DIAL_COLOR: Int = Color.rgb(216, 182, 106)
internal const val CENTER_DIVISOR = 2f
internal const val RADIUS_FRACTION = 0.3f
internal const val HOUR_HAND_LENGTH = 0.5f
internal const val MINUTE_HAND_LENGTH = 0.75f
internal const val SECOND_HAND_LENGTH = 0.85f
internal const val HOUR_HAND_WIDTH = 5f
internal const val MINUTE_HAND_WIDTH = 3f
internal const val SECOND_HAND_WIDTH = 1f
internal const val TICKS_PER_REVOLUTION = 60
internal const val TICKS_PER_HOUR = 5
internal const val DEGREES_PER_TICK = 6f
internal const val HOUR_TICK_LENGTH = 10f
internal const val MINUTE_TICK_LENGTH = 5f
internal const val HOUR_TICK_WIDTH = 3f
internal const val MINUTE_TICK_WIDTH = 1f
