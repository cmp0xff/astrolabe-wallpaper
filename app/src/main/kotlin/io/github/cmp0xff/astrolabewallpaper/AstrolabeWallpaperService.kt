package io.github.cmp0xff.astrolabewallpaper

import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.time.LocalTime

/** An animated astrolabe-style clock. */
class AstrolabeWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        val dialRenderer = DialRenderer()
        return createEngine(draw = { canvas -> dialRenderer.renderDial(canvas, clockState(LocalTime.now())) })
    }

    /** Creates an engine with a frame draw operation and optional controlled surface holder. */
    internal fun createEngine(draw: (Canvas) -> Unit, holder: SurfaceHolder? = null): Engine = ClockEngine(draw, holder)

    // Engine is a non-static Java inner class and requires the enclosing service instance.
    @Suppress("UnnecessaryInnerClass")
    private inner class ClockEngine(private val draw: (Canvas) -> Unit, private val frameHolder: SurfaceHolder?) :
        Engine() {
        private val handler = Handler(Looper.getMainLooper())

        // Mirrors the visibility the framework reports through onVisibilityChanged; kept here so
        // onSurfaceChanged can decide whether to resume ticking without a framework-only getter.
        private var isEngineVisible = false

        override fun onVisibilityChanged(visible: Boolean) {
            isEngineVisible = visible
            if (visible) {
                startTicking()
            } else {
                stopTicking()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            // Redraw for the new surface and restart the tick. The framework can destroy and
            // recreate the surface without a visibility change, and onSurfaceDestroyed cancels the
            // loop, so this is the only place that can resume it in that case.
            if (isEngineVisible) {
                startTicking()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            stopTicking()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            stopTicking()
            super.onDestroy()
        }

        // Draw one frame and post the next tick. Safe to call repeatedly: scheduleNextTick clears any
        // pending callback first, so the loop is never double-scheduled.
        private fun startTicking() {
            drawFrame()
            scheduleNextTick()
        }

        // The handler is dedicated to ticks, so cancelling all messages stops the loop.
        private fun stopTicking() {
            handler.removeCallbacksAndMessages(null)
        }

        // Handled argument/state failures return from drawFrame normally so the next tick is posted.
        // Other failures propagate rather than being hidden by the scheduling loop.
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
            drawWallpaperFrame(frameHolder ?: surfaceHolder, draw)
        }
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
    }
}
