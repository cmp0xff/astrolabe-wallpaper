package io.github.godaniya.astronomicalclockswallpaper

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
            assertEquals(-abs(latitude), projection.altitudeDeg(DialPoint(x = 0.0, y = 0.0)), ANGLE_TOLERANCE)
            assertEquals(0.0, projection.altitudeDeg(DialPoint(x = radius, y = 0.0)), ANGLE_TOLERANCE)
        }
    }

    @Test
    fun southernPlateMirrorsNorthern() {
        // A south-pole plate at -phi draws the same altitude field over the same points as a
        // north-pole plate at +phi, so both hemispheres nest night inside twilight inside day.
        for (latitude in listOf(33.8688, 66.56, 90.0)) {
            val north = projection(latitude)
            val south = projection(-latitude)
            for (point in GRID_POINTS) {
                assertEquals(
                    "latitude $latitude at $point",
                    north.altitudeDeg(point),
                    south.altitudeDeg(point),
                    ANGLE_TOLERANCE,
                )
            }
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
            // At either pole the pole below the horizon sits at the dial centre, so the centre is
            // night and the outer sky is day.
            assertFalse(projection.isAboveAltitude(DialPoint(x = 0.0, y = 0.0), 0.0))
            assertTrue(projection.isAboveAltitude(DialPoint(x = 0.9, y = 0.0), 0.0))
        }
    }

    @Test
    fun polarDayNightClassification() {
        for (latitude in listOf(90.0, -90.0)) {
            val projection = projection(latitude)
            assertFalse(projection.isAboveAltitude(DialPoint(x = 0.0, y = 0.0), -18.0))
            assertTrue(projection.isAboveAltitude(DialPoint(x = 0.5, y = 0.0), -18.0))
            assertFalse(projection.isAboveAltitude(DialPoint(x = 0.5, y = 0.0), 0.0))
            assertTrue(projection.isAboveAltitude(DialPoint(x = 0.7, y = 0.0), 0.0))
            assertEquals(2, projection.altitudeRegion(0.0).size)
            assertEquals(2, projection.altitudeRegion(-18.0).size)
        }
    }

    @Test
    fun southernBandsNestOutward() {
        // Walking outward from the dial centre: night, then the twilight annulus, then day.
        for (latitude in listOf(-33.8688, 90.0, -90.0)) {
            val projection = projection(latitude)
            val bands = mutableListOf<String>()
            var radius = 0.0
            while (radius <= 1.0) {
                val altitude = projection.altitudeDeg(DialPoint(x = radius, y = 0.0))
                val band =
                    when {
                        altitude >= 0.0 -> "day"
                        altitude >= -18.0 -> "twilight"
                        else -> "night"
                    }
                if (bands.lastOrNull() != band) bands.add(band)
                radius += 0.005
            }
            assertEquals("latitude $latitude", listOf("night", "twilight", "day"), bands)
        }
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
        val geometry = DialGeometry(localSiderealAngleDeg = 0.0, trueObliquityDeg = 23.44, latitudeDeg = latitude)
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

        // visibleArcStart() leaves its acos branch exactly where the altitude circle becomes tangent
        // to the Tropic of Capricorn, at latitude 90 - obliquity + altitude: 66.56 for the horizon
        // and 48.56 for the -18 degree night contour. Pin both sides of each knife edge.
        val TANGENT_LATITUDES =
            listOf(66.56, 48.56).flatMap { latitude ->
                listOf(latitude - 1e-8, latitude, latitude + 1e-8, -latitude)
            }
        val LATITUDES =
            listOf(50.0755, -33.8688, 0.0, 90.0, -90.0, 1e-8, -1e-8, -18.0, -18.0 - 1e-8, -18.0 + 1e-8) +
                TANGENT_LATITUDES
        const val ANGLE_TOLERANCE = 1e-6
        const val COORDINATE_TOLERANCE = 1e-12
    }
}
