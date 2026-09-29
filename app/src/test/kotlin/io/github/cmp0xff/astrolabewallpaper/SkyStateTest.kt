package io.github.cmp0xff.astrolabewallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Checks the pure data model's invariants, independently of any engine. */
class SkyStateTest {
    @Test
    fun horizontalAcceptsTheSphere() {
        // Constructing the inclusive bounds is the assertion: a rejected value would throw here.
        val corners =
            listOf(
                Horizontal(azimuthDeg = 0.0, altitudeDeg = -90.0),
                Horizontal(azimuthDeg = 360.0, altitudeDeg = 90.0),
                Horizontal(azimuthDeg = 359.999, altitudeDeg = 0.0),
            )
        assertEquals(3, corners.size)
    }

    @Test
    fun horizontalRejectsBadAngles() {
        assertRejected { Horizontal(azimuthDeg = -0.001, altitudeDeg = 0.0) }
        assertRejected { Horizontal(azimuthDeg = 360.001, altitudeDeg = 0.0) }
        assertRejected { Horizontal(azimuthDeg = 90.0, altitudeDeg = -90.001) }
        assertRejected { Horizontal(azimuthDeg = 90.0, altitudeDeg = 90.001) }
        assertRejected { Horizontal(azimuthDeg = Double.NaN, altitudeDeg = 0.0) }
        assertRejected { Horizontal(azimuthDeg = 0.0, altitudeDeg = Double.NEGATIVE_INFINITY) }
        assertRejected { Horizontal(azimuthDeg = Double.POSITIVE_INFINITY, altitudeDeg = 0.0) }
    }

    @Test
    fun eventTimeFindsItsKind() {
        val rise = Instant.parse("2026-06-21T03:42:45Z")
        val sky =
            sky(
                RiseSetEvent(EventKind.SUNRISE, rise),
                RiseSetEvent(EventKind.SUNSET, null),
            )
        assertEquals(rise, sky.eventTime(EventKind.SUNRISE))
        assertNull(sky.eventTime(EventKind.SUNSET))
    }

    @Test
    fun eventTimeRejectsIncompleteSky() {
        val failure = runCatching { sky(RiseSetEvent(EventKind.SUNRISE, null)).eventTime(EventKind.SUNSET) }
        assertTrue("expected a lookup failure", failure.isFailure)
    }

    @Test
    fun catalogStarRejectsBadValues() {
        val valid = StarCatalog.stars.first()
        assertRejected { valid.copy(rightAscensionDeg = -1.0) }
        assertRejected { valid.copy(rightAscensionDeg = 360.5) }
        assertRejected { valid.copy(declinationDeg = 90.5) }
        assertRejected { valid.copy(magnitude = 1.65) }
    }

    @Test
    fun properMotionIsLinearInTime() {
        val star = StarCatalog.stars.single { it.name == "Sirius" }
        assertEquals(star.rightAscensionDeg, star.rightAscensionDegAfter(years = 0.0), 0.0)
        assertEquals(star.declinationDeg, star.declinationDegAfter(years = 0.0), 0.0)
        val afterTen = star.rightAscensionDegAfter(years = 10.0) - star.rightAscensionDeg
        val afterTwenty = star.rightAscensionDegAfter(years = 20.0) - star.rightAscensionDeg
        assertEquals(afterTen * 2.0, afterTwenty, 1e-9)
        // Sirius moves south and west in right ascension, so its declination must decrease.
        assertTrue("declination should fall", star.declinationDegAfter(years = 100.0) < star.declinationDeg)
    }

    private fun sky(vararg events: RiseSetEvent): Sky {
        val sun = SolarState(position = UP, constellation = "Tau")
        val moon =
            MoonState(
                position = UP,
                phaseLongitudeDeg = 180.0,
                phaseFraction = 1.0,
                phaseAngleDeg = 0.0,
                magnitude = FULL_MOON_MAGNITUDE,
            )
        return Sky(
            sun = sun,
            moon = moon,
            planets = emptyList(),
            stars = emptyList(),
            events = events.toList(),
        )
    }

    // A modified copy is the reachable path that re-runs the constructor's invariants.
    private fun assertRejected(modify: () -> Any) {
        val failure = runCatching { modify() }.exceptionOrNull()
        val actual = failure?.let { it::class.simpleName } ?: "no exception"
        assertTrue("expected IllegalArgumentException but got $actual", failure is IllegalArgumentException)
    }

    private companion object {
        val UP = Horizontal(azimuthDeg = 0.0, altitudeDeg = 90.0)
        const val FULL_MOON_MAGNITUDE = -12.7
    }
}
