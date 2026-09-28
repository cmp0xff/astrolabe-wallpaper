package io.github.cmp0xff.astrolabewallpaper

import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders the astrolabe clock dial onto a [Canvas].
 *
 * [ClockState] angles are degrees measured clockwise from 12 o'clock (screen up; the dial carries no
 * geographic orientation). The canvas is cleared to [BACKGROUND_COLOR] before the tick marks and
 * hands are drawn, and a degenerate canvas or dial is skipped with a log line. The renderer owns a
 * mutable [Paint], so callers must confine an instance to a single thread. Rendering is best-effort:
 * a skipped frame is reported through the log rather than a return value.
 */
internal class DialRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** Clears [canvas] and draws the centered dial ticks and hands for [state]. */
    fun renderDial(canvas: Canvas, state: ClockState) {
        if (canvas.width <= 0 || canvas.height <= 0) {
            Log.w(TAG, "skipping render: empty canvas ${canvas.width}x${canvas.height}")
            return
        }
        canvas.drawColor(BACKGROUND_COLOR)
        val centerX = canvas.width / CENTER_DIVISOR
        val centerY = canvas.height / CENTER_DIVISOR
        val radius = minOf(a = canvas.width, b = canvas.height) * RADIUS_FRACTION
        // Reject a dial too small for its own tick geometry before building it.
        if (radius <= MIN_DIAL_RADIUS) {
            Log.w(TAG, "skipping dial: radius $radius <= minimum $MIN_DIAL_RADIUS")
            return
        }
        val dial = Dial(centerX = centerX, centerY = centerY, radius = radius)
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
        for (index in 0 until TICKS_PER_REVOLUTION) {
            val isHourTick = index % TICKS_PER_HOUR == 0
            val tickLength = if (isHourTick) HOUR_TICK_LENGTH else MINUTE_TICK_LENGTH
            val tickWidth = if (isHourTick) HOUR_TICK_WIDTH else MINUTE_TICK_WIDTH
            configureDialPaint(tickWidth)
            drawRadiusLine(
                canvas = canvas,
                dial = dial,
                innerRadius = dial.radius - tickLength,
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

    internal companion object {
        /** Background colour of the wallpaper dial; exposed so the pixel test can assert it. */
        internal const val BACKGROUND_COLOR: Int = 0xFF111923.toInt()

        /** Dial stroke colour; exposed so the pixel test can pin the palette. */
        internal const val DIAL_COLOR: Int = 0xFFD8B26A.toInt()

        private const val CENTER_DIVISOR = 2f
        private const val RADIUS_FRACTION = 0.3f
        private const val HOUR_HAND_LENGTH = 0.5f
        private const val MINUTE_HAND_LENGTH = 0.75f
        private const val SECOND_HAND_LENGTH = 0.85f
        private const val HOUR_HAND_WIDTH = 5f
        private const val MINUTE_HAND_WIDTH = 3f
        private const val SECOND_HAND_WIDTH = 1f
        private const val TICKS_PER_REVOLUTION = 60
        private const val TICKS_PER_HOUR = 5
        private const val DEGREES_PER_TICK = 6f
        private const val HOUR_TICK_LENGTH = 10f
        private const val MINUTE_TICK_LENGTH = 5f
        private const val HOUR_TICK_WIDTH = 3f
        private const val MINUTE_TICK_WIDTH = 1f

        // Derived from the tick constants on purpose: a dial must clear the longest tick and its
        // round cap, so retuning the ticks also moves the radius below which the dial is skipped and
        // only the cleared background is drawn.
        private const val MIN_DIAL_RADIUS = HOUR_TICK_LENGTH + HOUR_TICK_WIDTH / 2f
    }
}

/**
 * Runs one dial draw, containing the exceptions the draw path can raise so a single bad frame cannot
 * kill the per-second tick. The exception is logged with its stack trace, so the failure stays
 * diagnosable instead of being lost.
 */
internal fun containRenderFailure(draw: () -> Unit) {
    try {
        draw()
    } catch (e: IllegalArgumentException) {
        Log.e(TAG, "skipping frame: invalid render argument", e)
    } catch (e: IllegalStateException) {
        Log.e(TAG, "skipping frame: canvas in an invalid state", e)
    }
}

private const val TAG = "DialRenderer"
