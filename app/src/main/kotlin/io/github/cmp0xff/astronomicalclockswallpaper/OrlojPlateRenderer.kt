package io.github.cmp0xff.astronomicalclockswallpaper

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.abs

/**
 * Paints the geometric plate inside the sky boundary. The Sun layer adds the day/twilight/night
 * fills and draws their horizon and night contour strokes; the tropics, equator, and rim stay.
 *
 * Caches static plate geometry keyed by observer latitude and true obliquity, pre-allocating Path
 * objects so per-second ticks allocate zero heap objects.
 */
internal class OrlojPlateRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var cachedPlate: CachedPlate? = null

    fun draw(canvas: Canvas, projection: OrlojProjection?, isSunEnabled: Boolean) {
        paint.style = Paint.Style.FILL
        paint.color = DialStyle.NIGHT
        canvas.drawCircle(0f, 0f, SKY_RADIUS, paint)
        if (projection != null) {
            val plate = getOrCreatePlate(projection)
            if (isSunEnabled) {
                paint.style = Paint.Style.FILL
                paint.color = DialStyle.TWILIGHT
                canvas.drawPath(plate.paths.twilightFill, paint)
                paint.color = DialStyle.SKY
                canvas.drawPath(plate.paths.dayFill, paint)
            }
            drawGrid(canvas, plate, isSunEnabled)
        }
        paint.color = DialStyle.GOLD
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = OUTER_WIDTH
        canvas.drawCircle(0f, 0f, SKY_RADIUS, paint)
    }

    private fun getOrCreatePlate(projection: OrlojProjection): CachedPlate {
        val current = cachedPlate
        if (current != null &&
            current.matches(
                latitude = projection.geometry.latitudeDeg,
                obliquity = projection.geometry.trueObliquityDeg,
            )
        ) {
            return current
        }
        val newPlate = buildPlate(projection)
        cachedPlate = newPlate
        return newPlate
    }

    private fun buildPlate(projection: OrlojProjection): CachedPlate {
        val twilightPath = Path().apply { fillType = Path.FillType.EVEN_ODD }
        for (contour in projection.altitudeRegion(NIGHT_ALTITUDE)) {
            traceContour(twilightPath, contour)
            twilightPath.close()
        }

        val dayPath = Path().apply { fillType = Path.FillType.EVEN_ODD }
        for (contour in projection.altitudeRegion(HORIZON_ALTITUDE)) {
            traceContour(dayPath, contour)
            dayPath.close()
        }

        val nightBoundaryPath = Path()
        for (contour in projection.altitudeBoundary(NIGHT_ALTITUDE)) {
            traceContour(nightBoundaryPath, contour)
        }

        val horizonBoundaryPath = Path()
        for (contour in projection.altitudeBoundary(HORIZON_ALTITUDE)) {
            traceContour(horizonBoundaryPath, contour)
        }

        return CachedPlate(
            latitudeDeg = projection.geometry.latitudeDeg,
            obliquityDeg = projection.geometry.trueObliquityDeg,
            paths =
                PlatePaths(
                    twilightFill = twilightPath,
                    dayFill = dayPath,
                    nightBoundary = nightBoundaryPath,
                    horizonBoundary = horizonBoundaryPath,
                ),
            capricornRadius = projection.capricornRadius.toFloat(),
            equatorRadius = projection.equatorRadius.toFloat(),
        )
    }

    private fun drawGrid(canvas: Canvas, plate: CachedPlate, isSunEnabled: Boolean) {
        paint.style = Paint.Style.STROKE
        paint.color = DialStyle.MUTED_GOLD
        paint.strokeWidth = GRID_WIDTH
        canvas.drawCircle(0f, 0f, plate.capricornRadius, paint)
        paint.color = DialStyle.GOLD
        canvas.drawCircle(0f, 0f, plate.equatorRadius, paint)
        if (isSunEnabled) {
            paint.color = DialStyle.MUTED_GOLD
            paint.strokeWidth = BOUNDARY_WIDTH
            canvas.drawPath(plate.paths.nightBoundary, paint)
            paint.color = DialStyle.GOLD
            canvas.drawPath(plate.paths.horizonBoundary, paint)
        }
    }

    private fun traceContour(path: Path, contour: List<DialPoint>) {
        if (contour.isEmpty()) return
        val first = contour[0]
        path.moveTo(first.x.toFloat(), first.y.toFloat())
        for (i in 1 until contour.size) {
            val point = contour[i]
            path.lineTo(point.x.toFloat(), point.y.toFloat())
        }
    }

    private data class PlatePaths(
        val twilightFill: Path,
        val dayFill: Path,
        val nightBoundary: Path,
        val horizonBoundary: Path,
    )

    private class CachedPlate(
        val latitudeDeg: Double,
        val obliquityDeg: Double,
        val paths: PlatePaths,
        val capricornRadius: Float,
        val equatorRadius: Float,
    ) {
        fun matches(latitude: Double, obliquity: Double): Boolean =
            abs(latitudeDeg - latitude) < EPSILON_LATITUDE && abs(obliquityDeg - obliquity) < EPSILON_OBLIQUITY

        private companion object {
            const val EPSILON_LATITUDE = 1e-7
            const val EPSILON_OBLIQUITY = 1e-4
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
