package io.github.cmp0xff.astrolabewallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/** Checks the pure hand-angle mapping and its invariants independently of Android code. */
class ClockStateTest {
    @Test
    fun twelveOClockAlignsAllHands() {
        assertAngles(time = LocalTime.of(12, 0, 0), hourAngle = 0f, minuteAngle = 0f, secondAngle = 0f)
    }

    @Test
    fun threeOClockHourHandAt90() {
        assertAngles(time = LocalTime.of(3, 0, 0), hourAngle = 90f, minuteAngle = 0f, secondAngle = 0f)
    }

    @Test
    fun sixOClockHourHandAt180() {
        assertAngles(time = LocalTime.of(6, 0, 0), hourAngle = 180f, minuteAngle = 0f, secondAngle = 0f)
    }

    @Test
    fun threeFifteenSweepsHourHand() {
        assertAngles(time = LocalTime.of(3, 15, 0), hourAngle = 97.5f, minuteAngle = 90f, secondAngle = 0f)
    }

    @Test
    fun secondHandAtThirtySeconds() {
        assertAngles(time = LocalTime.of(0, 0, 30), hourAngle = 0.25f, minuteAngle = 3f, secondAngle = 180f)
    }

    @Test
    fun elevenFiftyNineBeforeMidnight() {
        assertAngles(time = LocalTime.of(23, 59, 59), hourAngle = 359.9917f, minuteAngle = 359.9f, secondAngle = 354f)
    }

    @Test
    fun subSecondNanosAreIgnored() {
        assertEquals(
            clockState(LocalTime.of(0, 0, 30)),
            clockState(LocalTime.of(0, 0, 30, 500_000_000)),
        )
    }

    @Test
    fun boundaryAnglesAreAccepted() {
        // Constructing the inclusive bounds is the assertion: a rejected value would throw here.
        val zero = ClockState(hourAngle = 0f, minuteAngle = 0f, secondAngle = 0f)
        val full = ClockState(hourAngle = 360f, minuteAngle = 360f, secondAngle = 360f)
        assertEquals(0f, zero.hourAngle, ANGLE_TOLERANCE)
        assertEquals(360f, full.hourAngle, ANGLE_TOLERANCE)
        assertEquals(360f, full.minuteAngle, ANGLE_TOLERANCE)
        assertEquals(360f, full.secondAngle, ANGLE_TOLERANCE)
    }

    @Test
    fun outOfRangeAnglesAreRejected() {
        assertRejected { it.copy(hourAngle = -1f) }
        assertRejected { it.copy(minuteAngle = -0.1f) }
        assertRejected { it.copy(secondAngle = 360.5f) }
        assertRejected { it.copy(hourAngle = 1000f) }
    }

    @Test
    fun nonFiniteAnglesAreRejected() {
        assertRejected { it.copy(hourAngle = Float.NaN) }
        assertRejected { it.copy(minuteAngle = Float.POSITIVE_INFINITY) }
        assertRejected { it.copy(secondAngle = Float.NEGATIVE_INFINITY) }
    }

    private fun assertAngles(time: LocalTime, hourAngle: Float, minuteAngle: Float, secondAngle: Float) {
        val state = clockState(time)
        assertEquals(hourAngle, state.hourAngle, ANGLE_TOLERANCE)
        assertEquals(minuteAngle, state.minuteAngle, ANGLE_TOLERANCE)
        assertEquals(secondAngle, state.secondAngle, ANGLE_TOLERANCE)
    }

    // A modified copy is the reachable path that re-runs the constructor's invariants.
    private fun assertRejected(modify: (ClockState) -> ClockState) {
        val state = clockState(LocalTime.of(12, 0, 0))
        val failure = runCatching { modify(state) }.exceptionOrNull()
        val actualType = failure?.let { it::class.simpleName } ?: "no exception"
        assertTrue("expected IllegalArgumentException but got $actualType", failure is IllegalArgumentException)
    }

    private companion object {
        const val ANGLE_TOLERANCE = 0.001f
    }
}
