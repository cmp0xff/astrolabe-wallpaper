package io.github.cmp0xff.astrolabewallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Independent analytic fixtures for the north-pole construction documented at
 * https://astro.cas.cz/bh2010/files/praha.pdf, printed pages 4–5.
 * Unit-radius Cancer coordinates use an artificial 30-degree obliquity so the expected
 * values follow exact 30/60/90-degree triangles, independently of Astronomy Engine.
 */
class OrlojProjectionTest {
    @Test
    fun equinoxAndSolsticeFixtures() {
        val projection = projection(0.0)
        val equator = 1 / sqrt(3.0)
        assertEquals(equator, projection.equatorRadius, TOLERANCE)
        assertEquals(1.0 / 3, projection.capricornRadius, TOLERANCE)
        assertPoint(expected = DialPoint(x = 0.0, y = -equator), actual = projection.eclipticPoint(0.0))
        assertPoint(expected = DialPoint(x = -1.0, y = 0.0), actual = projection.eclipticPoint(90.0))
        assertPoint(expected = DialPoint(x = 0.0, y = equator), actual = projection.eclipticPoint(180.0))
        assertPoint(expected = DialPoint(x = 1.0 / 3, y = 0.0), actual = projection.eclipticPoint(270.0))
    }

    @Test
    fun completeZodiacIsTangent() {
        val projection = projection(37.0)
        val circle = projection.zodiacCircle
        val offset = hypot(x = circle.center.x, y = circle.center.y)
        assertEquals(1.0, offset + circle.radius, TOLERANCE)
        assertEquals(projection.capricornRadius, circle.radius - offset, TOLERANCE)
        for (longitude in 0 until 360 step 15) {
            val point = projection.eclipticPoint(longitude.toDouble())
            assertEquals(circle.radius, hypot(x = point.x - circle.center.x, y = point.y - circle.center.y), TOLERANCE)
        }
    }

    @Test
    fun siderealRotationIsClockwise() {
        val initial = projection(0.0)
        val rotated = projection(90.0)
        for (longitude in 0 until 360 step 30) {
            val point = initial.eclipticPoint(longitude.toDouble())
            assertPoint(
                expected = DialPoint(x = -point.y, y = point.x),
                actual =
                    rotated
                        .eclipticPoint(longitude.toDouble()),
            )
        }
        val point = initial.zodiacCircle.center
        assertPoint(expected = DialPoint(x = -point.y, y = point.x), actual = rotated.zodiacCircle.center)
    }

    @Test
    fun fullRingIncludesBelowHorizon() {
        val projection = projection(0.0)
        val signs = (0 until 360 step 30).map { projection.eclipticPoint(it.toDouble()) }
        assertTrue(signs.any { projection.altitudeDeg(it) < 0 })
        assertTrue(signs.any { projection.altitudeDeg(it) > 0 })
        assertEquals(12, signs.size)
    }

    private fun projection(sidereal: Double): OrlojProjection {
        val geometry = AstrolabeGeometry(localSiderealAngleDeg = sidereal, trueObliquityDeg = 30.0, latitudeDeg = 50.0)
        return OrlojProjection(geometry)
    }

    private fun assertPoint(expected: DialPoint, actual: DialPoint) {
        assertEquals(expected.x, actual.x, TOLERANCE)
        assertEquals(expected.y, actual.y, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1e-12
    }
}
