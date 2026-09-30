package io.github.cmp0xff.astrolabewallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

/** Geometric altitude fixtures use elementary meridian/equator/pole identities, in degrees. */
class OrlojAltitudeTest {
    @Test
    fun meridianAltitudeFixtures() {
        for (latitude in listOf(50.0755, -33.8688, 0.0, 90.0, -90.0)) {
            val projection = projection(latitude)
            val radius = projection.equatorRadius
            assertEquals(90 - abs(latitude), projection.altitudeDeg(DialPoint(x = 0.0, y = -radius)), ANGLE_TOLERANCE)
            assertEquals(abs(latitude) - 90, projection.altitudeDeg(DialPoint(x = 0.0, y = radius)), ANGLE_TOLERANCE)
            assertEquals(-latitude, projection.altitudeDeg(DialPoint(x = 0.0, y = 0.0)), ANGLE_TOLERANCE)
            assertEquals(0.0, projection.altitudeDeg(DialPoint(x = radius, y = 0.0)), ANGLE_TOLERANCE)
        }
    }

    @Test
    fun equatorialHorizonIsLine() {
        val projection = projection(0.0)
        val boundary = projection.altitudeBoundary(0.0).single()
        assertTrue(boundary.all { abs(it.y) < COORDINATE_TOLERANCE })
        assertEquals(-1.0, boundary.first().x, COORDINATE_TOLERANCE)
        assertEquals(1.0, boundary.last().x, COORDINATE_TOLERANCE)
        assertTrue(projection.isAboveAltitude(DialPoint(x = 0.0, y = -0.5), 0.0))
        assertFalse(projection.isAboveAltitude(DialPoint(x = 0.0, y = 0.5), 0.0))
    }

    @Test
    fun polarHorizonsAreCircles() {
        for (latitude in listOf(90.0, -90.0)) {
            val projection = projection(latitude)
            val boundary = projection.altitudeBoundary(0.0).single()
            assertTrue(
                boundary.all { abs(hypot(x = it.x, y = it.y) - projection.equatorRadius) < COORDINATE_TOLERANCE },
            )
            assertEquals(latitude > 0, projection.isAboveAltitude(DialPoint(x = 0.9, y = 0.0), 0.0))
            assertEquals(latitude < 0, projection.isAboveAltitude(DialPoint(x = 0.0, y = 0.0), 0.0))
        }
    }

    @Test
    fun polarDayNightClassification() {
        val north = projection(90.0)
        val south = projection(-90.0)
        assertFalse(north.isAboveAltitude(DialPoint(x = 0.0, y = 0.0), -18.0))
        assertTrue(south.isAboveAltitude(DialPoint(x = 0.0, y = 0.0), 0.0))
        assertTrue(north.isAboveAltitude(DialPoint(x = 1.0, y = 0.0), 0.0))
        assertFalse(south.isAboveAltitude(DialPoint(x = 1.0, y = 0.0), -18.0))
        assertEquals(2, north.altitudeRegion(0.0).size)
        assertEquals(1, south.altitudeRegion(0.0).size)
    }

    @Test
    fun contoursStayBoundedAndExact() {
        for (latitude in LATITUDES) {
            val projection = projection(latitude)
            for (threshold in listOf(0.0, -18.0)) {
                val boundaries = projection.altitudeBoundary(threshold)
                for (point in boundaries.flatten()) {
                    assertTrue(point.x.isFinite() && point.y.isFinite())
                    assertTrue(hypot(x = point.x, y = point.y) <= 1 + COORDINATE_TOLERANCE)
                    assertEquals("latitude $latitude", threshold, projection.altitudeDeg(point), ANGLE_TOLERANCE)
                }
                val fills = projection.altitudeRegion(threshold)
                assertTrue(fills.flatten().all { hypot(x = it.x, y = it.y) <= 1 + COORDINATE_TOLERANCE })
            }
        }
    }

    @Test
    fun fillContoursMatchAltitude() {
        for (latitude in LATITUDES) {
            val projection = projection(latitude)
            for (threshold in listOf(0.0, -18.0)) {
                val contours = projection.altitudeRegion(threshold)
                for (point in GRID_POINTS) {
                    val altitude = projection.altitudeDeg(point)
                    if (abs(altitude - threshold) > 0.05) {
                        val isInside = contours.count { contains(it, point) } % 2 == 1
                        assertEquals(
                            "latitude $latitude, altitude $altitude, threshold $threshold",
                            altitude >= threshold,
                            isInside,
                        )
                    }
                }
            }
        }
    }

    private fun projection(latitude: Double): OrlojProjection {
        val geometry = AstrolabeGeometry(localSiderealAngleDeg = 0.0, trueObliquityDeg = 23.44, latitudeDeg = latitude)
        return OrlojProjection(geometry)
    }

    private fun contains(contour: List<DialPoint>, point: DialPoint): Boolean {
        var isInside = false
        var previous = contour.last()
        for (current in contour) {
            val isCurrentAbove = current.y > point.y
            val isPreviousAbove = previous.y > point.y
            if (isCurrentAbove != isPreviousAbove) {
                val edgeX = (previous.x - current.x) * (point.y - current.y) / (previous.y - current.y) + current.x
                if (point.x < edgeX) isInside = !isInside
            }
            previous = current
        }
        return isInside
    }

    private companion object {
        val GRID_POINTS =
            (-12..12).flatMap { x ->
                (-12..12).map { y -> DialPoint(x = x / 13.0, y = y / 13.0) }.filter { hypot(x = it.x, y = it.y) < 0.99 }
            }
        val LATITUDES = listOf(50.0755, -33.8688, 0.0, 90.0, -90.0, 1e-8, -1e-8, -18.0, -18.0 - 1e-8, -18.0 + 1e-8)
        const val ANGLE_TOLERANCE = 1e-6
        const val COORDINATE_TOLERANCE = 1e-12
    }
}
