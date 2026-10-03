package io.github.cmp0xff.astronomicalclockswallpaper

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface

/** Complete ecliptic ring, including the part below the horizon, with tropical longitude labels. */
internal class ZodiacRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(canvas: Canvas, projection: OrlojProjection) {
        val circle = projection.zodiacCircle
        paint.style = Paint.Style.STROKE
        paint.color = DialStyle.GOLD
        paint.strokeWidth = RING_OUTER_WIDTH
        canvas.drawCircle(circle.center.x.toFloat(), circle.center.y.toFloat(), circle.radius.toFloat(), paint)
        paint.color = DialStyle.NIGHT
        paint.strokeWidth = RING_INNER_WIDTH
        canvas.drawCircle(circle.center.x.toFloat(), circle.center.y.toFloat(), circle.radius.toFloat(), paint)
        drawSigns(canvas, projection)
    }

    private fun drawSigns(canvas: Canvas, projection: OrlojProjection) {
        val checkpoint = canvas.save()
        canvas.scale(1 / DialStyle.TEXT_UNITS, 1 / DialStyle.TEXT_UNITS)
        paint.style = Paint.Style.FILL
        paint.color = DialStyle.HAND
        paint.typeface = SIGNS_TYPEFACE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = SIGN_SIZE * DialStyle.TEXT_UNITS
        val textOffset = -(paint.ascent() + paint.descent()) / CENTER_DIVISOR
        for ((index, sign) in SIGNS.withIndex()) {
            val point = projection.eclipticPoint(index * DEGREES_PER_SIGN)
            canvas.drawText(
                sign,
                point.x.toFloat() * DialStyle.TEXT_UNITS,
                point.y.toFloat() * DialStyle.TEXT_UNITS + textOffset,
                paint,
            )
        }
        canvas.restoreToCount(checkpoint)
    }

    private companion object {
        const val RING_OUTER_WIDTH = 0.09f
        const val RING_INNER_WIDTH = 0.075f
        const val SIGN_SIZE = 0.044f
        const val CENTER_DIVISOR = 2f
        const val DEGREES_PER_SIGN = 30.0
        val SIGNS = listOf("ARI", "TAU", "GEM", "CAN", "LEO", "VIR", "LIB", "SCO", "SAG", "CAP", "AQU", "PIS")
        private val SIGNS_TYPEFACE: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
}
