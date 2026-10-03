package io.github.cmp0xff.astrolabewallpaper

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

/**
 * Paints the geometric plate inside the sky boundary. The Sun layer adds the day/twilight/night
 * fills and draws their horizon and night contour strokes; the tropics, equator, and rim stay.
 */
internal class OrlojPlateRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    fun draw(canvas: Canvas, projection: OrlojProjection?, isSunEnabled: Boolean) {
        paint.style = Paint.Style.FILL
        paint.color = DialStyle.NIGHT
        canvas.drawCircle(0f, 0f, SKY_RADIUS, paint)
        if (projection != null) {
            if (isSunEnabled) {
                fillRegion(canvas, projection.altitudeRegion(NIGHT_ALTITUDE), DialStyle.TWILIGHT)
                fillRegion(canvas, projection.altitudeRegion(HORIZON_ALTITUDE), DialStyle.SKY)
            }
            drawGrid(canvas, projection, isSunEnabled)
        }
        paint.color = DialStyle.GOLD
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = OUTER_WIDTH
        canvas.drawCircle(0f, 0f, SKY_RADIUS, paint)
    }

    private fun fillRegion(canvas: Canvas, contours: List<List<DialPoint>>, color: Int) {
        path.reset()
        path.fillType = Path.FillType.EVEN_ODD
        for (contour in contours) {
            traceContour(contour)
            path.close()
        }
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawPath(path, paint)
    }

    private fun drawGrid(canvas: Canvas, projection: OrlojProjection, isSunEnabled: Boolean) {
        paint.style = Paint.Style.STROKE
        paint.color = DialStyle.MUTED_GOLD
        paint.strokeWidth = GRID_WIDTH
        canvas.drawCircle(0f, 0f, projection.capricornRadius.toFloat(), paint)
        paint.color = DialStyle.GOLD
        canvas.drawCircle(0f, 0f, projection.equatorRadius.toFloat(), paint)
        if (isSunEnabled) {
            drawBoundary(canvas, projection.altitudeBoundary(NIGHT_ALTITUDE), DialStyle.MUTED_GOLD)
            drawBoundary(canvas, projection.altitudeBoundary(HORIZON_ALTITUDE), DialStyle.GOLD)
        }
    }

    private fun drawBoundary(canvas: Canvas, contours: List<List<DialPoint>>, color: Int) {
        path.reset()
        for (contour in contours) {
            traceContour(contour)
        }
        paint.color = color
        paint.strokeWidth = BOUNDARY_WIDTH
        canvas.drawPath(path, paint)
    }

    private fun traceContour(contour: List<DialPoint>) {
        val first = contour.firstOrNull() ?: return
        path.moveTo(first.x.toFloat(), first.y.toFloat())
        for (point in contour.drop(1)) {
            path.lineTo(point.x.toFloat(), point.y.toFloat())
        }
    }

    private companion object {
        const val SKY_RADIUS = 1f
        const val NIGHT_ALTITUDE = -18.0
        const val HORIZON_ALTITUDE = 0.0
        const val GRID_WIDTH = 0.0035f
        const val BOUNDARY_WIDTH = 0.006f
        const val OUTER_WIDTH = 0.008f
    }
}
