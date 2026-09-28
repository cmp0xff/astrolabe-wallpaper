package io.github.cmp0xff.astrolabewallpaper

import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import java.time.LocalTime

/** An animated astrolabe-style clock. */
class AstrolabeWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = ClockEngine()

    // Engine is a non-static Java inner class and requires the enclosing service instance.
    @Suppress("UnnecessaryInnerClass")
    private inner class ClockEngine : Engine() {
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
                renderDial(canvas, clockState(LocalTime.now()))
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    private companion object {
        const val TAG = "AstrolabeWallpaperService"
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

private const val HOURS_PER_REVOLUTION = 12
private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HOUR = 60 * 60
private const val SECONDS_PER_HALF_DAY = HOURS_PER_REVOLUTION * SECONDS_PER_HOUR
private const val SECOND_HAND_DEGREES_PER_SECOND = 6f
private const val MINUTE_HAND_DEGREES_PER_SECOND = 0.1f
private const val HOUR_HAND_DEGREES_PER_SECOND = 1f / 120f
