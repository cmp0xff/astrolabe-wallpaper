package io.github.cmp0xff.astrolabewallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

/** A static Canvas placeholder; clock animation and device qualification belong to issue #2. */
class AstrolabeWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = StaticEngine()

    // Engine is a non-static Java inner class and requires the enclosing service instance.
    @Suppress("UnnecessaryInnerClass")
    private inner class StaticEngine : Engine() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                drawFrame()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            if (isVisible) {
                drawFrame()
            }
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            if (holder.surface.isValid) {
                val canvas = holder.lockCanvas()
                if (canvas != null) {
                    try {
                        drawDial(canvas)
                    } finally {
                        holder.unlockCanvasAndPost(canvas)
                    }
                }
            }
        }

        private fun drawDial(canvas: Canvas) {
            canvas.drawColor(BACKGROUND_COLOR)
            val centerX = canvas.width / CENTER_DIVISOR
            val centerY = canvas.height / CENTER_DIVISOR
            val radius = minOf(a = canvas.width, b = canvas.height) * RADIUS_FRACTION
            paint.color = DIAL_COLOR
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = radius * STROKE_FRACTION
            canvas.drawCircle(centerX, centerY, radius, paint)
            canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, paint)
            canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, paint)
        }
    }

    private companion object {
        val BACKGROUND_COLOR: Int = Color.rgb(17, 25, 35)
        val DIAL_COLOR: Int = Color.rgb(216, 182, 106)
        const val CENTER_DIVISOR = 2f
        const val RADIUS_FRACTION = 0.3f
        const val STROKE_FRACTION = 0.01f
    }
}
