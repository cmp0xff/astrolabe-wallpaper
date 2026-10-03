package io.github.cmp0xff.astrolabewallpaper

import android.content.SharedPreferences
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.time.Clock
import java.time.ZoneId

/** An animated astrolabe-style clock. */
class AstrolabeWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        val dialRenderer = DialRenderer()
        return createEngine(
            draw = { canvas, state, geometry, layers ->
                dialRenderer.renderDial(canvas = canvas, state = state, geometry = geometry, layers = layers)
            },
        )
    }

    /** Creates an engine with a frame draw operation and optional controlled surface holder. */
    internal fun createEngine(
        draw: (Canvas, ClockState, AstrolabeGeometry?, DialLayers) -> Unit,
        holder: SurfaceHolder? = null,
        clock: Clock = Clock.systemUTC(),
        deviceZone: () -> ZoneId = ZoneId::systemDefault,
        calculator: AstronomyCalculator = AstronomyEngineCalculator(),
    ): Engine {
        val engine =
            ClockEngine(
                draw = draw,
                frameHolder = holder,
                clock = clock,
                deviceZone = deviceZone,
                calculator = calculator,
            )
        return engine
    }

    // Engine is a non-static Java inner class and requires the enclosing service instance.
    @Suppress("UnnecessaryInnerClass")
    private inner class ClockEngine(
        private val draw: (Canvas, ClockState, AstrolabeGeometry?, DialLayers) -> Unit,
        private val frameHolder: SurfaceHolder?,
        private val clock: Clock,
        private val deviceZone: () -> ZoneId,
        private val calculator: AstronomyCalculator,
    ) : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val locationStore = LocationStore(applicationContext, deviceZone)
        private val dialSettingsStore = DialSettingsStore(applicationContext)
        private var settings = loadSettings()
        private var isDestroyed = false
        private val settingsListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                if (!isDestroyed) {
                    settings = loadSettings()
                }
            }

        // Mirrors the visibility the framework reports through onVisibilityChanged; kept here so
        // onSurfaceChanged can decide whether to resume ticking without a framework-only getter.
        private var isEngineVisible = false

        init {
            // Keep a strong listener reference for this engine's lifetime. Updates only replace the
            // cached snapshot; hidden engines must not acquire a surface or schedule a tick.
            locationStore.registerListener(settingsListener)
            dialSettingsStore.registerListener(settingsListener)
        }

        private fun loadSettings(): WallpaperSettings =
            WallpaperSettings(location = locationStore.load(), layers = dialSettingsStore.load())

        override fun onVisibilityChanged(visible: Boolean) {
            if (isDestroyed) {
                return
            }
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
            isDestroyed = true
            isEngineVisible = false
            locationStore.unregisterListener(settingsListener)
            dialSettingsStore.unregisterListener(settingsListener)
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
            val millisInSecond = Math.floorMod(clock.millis(), MILLIS_PER_SECOND)
            return MILLIS_PER_SECOND - millisInSecond
        }

        private fun drawFrame() {
            drawWallpaperFrame(frameHolder ?: surfaceHolder) { canvas ->
                val instant = clock.instant()
                val snapshot = settings
                val location = snapshot.location
                val civilTime = instant.atZone(location?.zoneId ?: deviceZone()).toLocalTime()
                val geometry = location?.let { calculator.astrolabeGeometry(instant, it) }
                draw(canvas, clockState(civilTime), geometry, snapshot.layers)
            }
        }
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
    }
}
