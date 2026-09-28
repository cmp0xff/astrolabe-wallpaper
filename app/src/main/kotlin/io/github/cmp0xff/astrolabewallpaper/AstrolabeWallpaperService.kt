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
        private val dialRenderer = DialRenderer()
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

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            handler.removeCallbacksAndMessages(null)
            super.onSurfaceDestroyed(holder)
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
                    try {
                        drawFrame()
                    } finally {
                        scheduleNextTick()
                    }
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
            val canvas =
                try {
                    holder.lockCanvas()
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "skipping frame: lockCanvas failed", e)
                    null
                }
            if (canvas == null) {
                Log.w(TAG, "skipping frame: lockCanvas returned null")
                return
            }
            try {
                dialRenderer.renderDial(canvas, clockState(LocalTime.now()))
            } finally {
                try {
                    holder.unlockCanvasAndPost(canvas)
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "unlockCanvasAndPost failed: surface already released", e)
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "unlockCanvasAndPost failed: invalid surface state", e)
                }
            }
        }
    }

    private companion object {
        const val TAG = "AstrolabeWallpaperService"
        const val MILLIS_PER_SECOND = 1000L
    }
}
