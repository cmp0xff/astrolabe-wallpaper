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
        // The range checks are inclusive at both ends: 360 names the same bearing as 0, and a
        // body sits exactly on the horizon or exactly at the nadir rather than just beside it.
        // Constructing these is the assertion — a rejected value throws here — and the boundary
        // is what the rejections below pin, one thousandth of a degree outside it.
        val corners =
            listOf(
                Horizontal(azimuthDeg = 0.0, altitudeDeg = -RIGHT_ANGLE_DEGREES),
                Horizontal(azimuthDeg = FULL_TURN_DEGREES, altitudeDeg = RIGHT_ANGLE_DEGREES),
                Horizontal(azimuthDeg = 359.999, altitudeDeg = 0.0),
            )
        assertEquals(
            "due north is spelled two ways",
            0.0,
            angleDifferenceDeg(first = corners[0].azimuthDeg, second = corners[1].azimuthDeg),
            0.0,
        )
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
        // A Sky missing a kind is malformed, and the lookup says so instead of answering `null`:
        // null has to stay reserved for "the event does not happen", which is a fact about the
        // sky rather than a gap in the record.
        val thrown = failureOf { sky(RiseSetEvent(EventKind.SUNRISE, null)).eventTime(EventKind.SUNSET) }
        assertTrue("expected NoSuchElementException, got ${thrown.name()}", thrown is NoSuchElementException)
    }

    @Test
    fun eventTimeRejectsDuplicateKinds() {
        val thrown =
            failureOf {
                sky(RiseSetEvent(EventKind.SUNRISE, null), RiseSetEvent(EventKind.SUNRISE, null))
                    .eventTime(EventKind.SUNRISE)
            }
        assertTrue("expected IllegalArgumentException, got ${thrown.name()}", thrown is IllegalArgumentException)
    }

    @Test
    fun catalogStarRejectsBadValues() {
        val valid = StarCatalog.stars.first()
        assertRejected { valid.copy(rightAscensionDeg = -1.0) }
        assertRejected { valid.copy(rightAscensionDeg = 360.5) }
        assertRejected { valid.copy(declinationDeg = 90.5) }
        assertRejected { valid.copy(magnitude = 1.65) }
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
        val failure = failureOf(modify)
        assertTrue("expected IllegalArgumentException but got ${failure.name()}", failure is IllegalArgumentException)
    }

    private fun failureOf(block: () -> Any?): Throwable? = runCatching(block).exceptionOrNull()

    private fun Throwable?.name(): String = this?.let { it::class.simpleName } ?: "no exception"

    private companion object {
        val UP = Horizontal(azimuthDeg = 0.0, altitudeDeg = 90.0)
        const val FULL_MOON_MAGNITUDE = -12.7
    }
}
