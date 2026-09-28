package io.github.cmp0xff.astrolabewallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.sin

/** An animated astrolabe-style clock. */
class AstrolabeWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = ClockEngine()

    // Engine is a non-static Java inner class and requires the enclosing service instance.
    @Suppress("UnnecessaryInnerClass")
    private inner class ClockEngine : Engine() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val handler = Handler(Looper.getMainLooper())

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                drawFrame()
                scheduleNextTick()
            } else {
                handler.removeCallbacksAndMessages(null)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            // Redraw immediately for the new surface; onSurfaceChanged never schedules ticks — the
            // per-second loop is (re)started only by onVisibilityChanged(true).
            if (isVisible) {
                drawFrame()
            }
        }

        override fun onDestroy() {
            handler.removeCallbacksAndMessages(null)
            super.onDestroy()
        }

        // Post a tick that draws once and re-schedules for the next whole second; the handler is dedicated to ticks.
        private fun scheduleNextTick() {
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed(
                Runnable {
                    drawFrame()
                    scheduleNextTick()
                },
                millisUntilNextWholeSecond(),
            )
        }

        private fun millisUntilNextWholeSecond(): Long {
            val millisInSecond = System.currentTimeMillis() % MILLIS_PER_SECOND
            return MILLIS_PER_SECOND - millisInSecond
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            val surface = holder.surface
            if (surface == null || !surface.isValid) {
                Log.d(TAG, "skipping frame: surface not ready")
                return
            }
            val canvas = holder.lockCanvas()
            if (canvas == null) {
                Log.w(TAG, "skipping frame: lockCanvas returned null")
                return
            }
            try {
                drawDial(canvas, clockState(LocalTime.now()))
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        private fun drawDial(canvas: Canvas, state: ClockState) {
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
    }

    private companion object {
        const val TAG = "AstrolabeWallpaperService"
        val BACKGROUND_COLOR: Int = Color.rgb(17, 25, 35)
        val DIAL_COLOR: Int = Color.rgb(216, 182, 106)
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
        const val MILLIS_PER_SECOND = 1000L
    }
}

/** Clock-hand angles in degrees, measured clockwise from 12 o'clock. */
internal data class ClockState(val hourAngle: Float, val minuteAngle: Float, val secondAngle: Float)

/** Derives the three hand angles for a wall-clock time. */
internal fun clockState(time: LocalTime): ClockState {
    val secondOfDay = time.hour * SECONDS_PER_HOUR + time.minute * SECONDS_PER_MINUTE + time.second
    val secondOfHalfDay = secondOfDay % SECONDS_PER_HALF_DAY
    val secondOfHour = secondOfDay % SECONDS_PER_HOUR
    return ClockState(
        hourAngle = secondOfHalfDay * HOUR_HAND_DEGREES_PER_SECOND,
        minuteAngle = secondOfHour * MINUTE_HAND_DEGREES_PER_SECOND,
        secondAngle = time.second * SECOND_HAND_DEGREES_PER_SECOND,
    )
}

private data class Dial(val centerX: Float, val centerY: Float, val radius: Float)

private const val HOURS_PER_REVOLUTION = 12
private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HOUR = 60 * 60
private const val SECONDS_PER_HALF_DAY = HOURS_PER_REVOLUTION * SECONDS_PER_HOUR
private const val SECOND_HAND_DEGREES_PER_SECOND = 6f
private const val MINUTE_HAND_DEGREES_PER_SECOND = 0.1f
private const val HOUR_HAND_DEGREES_PER_SECOND = 1f / 120f
