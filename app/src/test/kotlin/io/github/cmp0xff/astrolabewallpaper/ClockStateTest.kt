package io.github.cmp0xff.astrolabewallpaper

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/** Checks the pure hand-angle mapping independently of Android drawing and lifecycle code. */
class ClockStateTest {
    @Test
    fun twelveOClockAlignsAllHands() {
        assertAngles(
            actual = clockState(LocalTime.of(12, 0, 0)),
            expected = ClockState(hourAngle = 0f, minuteAngle = 0f, secondAngle = 0f),
        )
    }

    @Test
    fun threeOClockHourHandAt90() {
        assertAngles(
            actual = clockState(LocalTime.of(3, 0, 0)),
            expected = ClockState(hourAngle = 90f, minuteAngle = 0f, secondAngle = 0f),
        )
    }

    @Test
    fun sixOClockHourHandAt180() {
        assertAngles(
            actual = clockState(LocalTime.of(6, 0, 0)),
            expected = ClockState(hourAngle = 180f, minuteAngle = 0f, secondAngle = 0f),
        )
    }

    @Test
    fun secondHandAtThirtySeconds() {
        assertAngles(
            actual = clockState(LocalTime.of(0, 0, 30)),
            expected = ClockState(hourAngle = 0.25f, minuteAngle = 3f, secondAngle = 180f),
        )
    }

    @Test
    fun elevenFiftyNineBeforeMidnight() {
        assertAngles(
            actual = clockState(LocalTime.of(11, 59, 59)),
            expected = ClockState(hourAngle = 359.9917f, minuteAngle = 359.9f, secondAngle = 354f),
        )
    }

    private fun assertAngles(actual: ClockState, expected: ClockState) {
        assertEquals(expected.hourAngle, actual.hourAngle, ANGLE_TOLERANCE)
        assertEquals(expected.minuteAngle, actual.minuteAngle, ANGLE_TOLERANCE)
        assertEquals(expected.secondAngle, actual.secondAngle, ANGLE_TOLERANCE)
    }

    private companion object {
        const val ANGLE_TOLERANCE = 0.001f
    }
}
