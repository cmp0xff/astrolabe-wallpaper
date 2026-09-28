package io.github.cmp0xff.astrolabewallpaper

import android.graphics.Canvas
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
        private val dialRenderer = DialRenderer()
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

        // Failure containment lives in drawFrame, which logs the reason and returns; it does not
        // rethrow, so there is nothing here for a try/finally to keep alive.
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
                // Expected while the engine is alive but the surface is gone; debug level avoids
                // spamming logcat once per tick for a long-lived invalid surface.
                Log.d(TAG, "skipping frame: surface not ready")
                return
            }
            val canvas = lockCanvasOrNull(holder) ?: return
            try {
                containRenderFailure { dialRenderer.renderDial(canvas, clockState(LocalTime.now())) }
            } finally {
                unlockCanvasAndPost(holder, canvas)
            }
        }

        // Locks the canvas, logging the reason exactly once when no canvas is available: either the
        // exception that stopped the lock, or a genuine null return.
        private fun lockCanvasOrNull(holder: SurfaceHolder): Canvas? {
            try {
                val canvas = holder.lockCanvas()
                if (canvas == null) {
                    Log.w(TAG, "skipping frame: lockCanvas returned null")
                }
                return canvas
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "skipping frame: lockCanvas failed (surface released)", e)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "skipping frame: lockCanvas failed (invalid surface state)", e)
            }
            return null
        }

        // Posts the canvas back, logging either failure mode rather than propagating it.
        private fun unlockCanvasAndPost(holder: SurfaceHolder, canvas: Canvas) {
            try {
                holder.unlockCanvasAndPost(canvas)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "unlockCanvasAndPost failed: surface already released", e)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "unlockCanvasAndPost failed: invalid surface state", e)
            }
        }
    }

    private companion object {
        const val TAG = "AstrolabeWallpaperService"
        const val MILLIS_PER_SECOND = 1000L
    }
}
