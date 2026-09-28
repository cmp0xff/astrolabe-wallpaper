package io.github.cmp0xff.astrolabewallpaper

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/** Checks the pure hand-angle mapping independently of Android drawing and lifecycle code. */
class ClockStateTest {
    @Test
    fun midnightAlignsAllHands() {
        assertEquals(
            ClockState(hourAngle = 0f, minuteAngle = 0f, secondAngle = 0f),
            clockState(LocalTime.of(12, 0, 0)),
        )
    }

    @Test
    fun threeOClockHourHandAt90() {
        assertEquals(
            ClockState(hourAngle = 90f, minuteAngle = 0f, secondAngle = 0f),
            clockState(LocalTime.of(3, 0, 0)),
        )
    }

    @Test
    fun sixOClockHourHandAt180() {
        assertEquals(
            ClockState(hourAngle = 180f, minuteAngle = 0f, secondAngle = 0f),
            clockState(LocalTime.of(6, 0, 0)),
        )
    }

    @Test
    fun secondHandAtThirtySeconds() {
        assertEquals(
            ClockState(hourAngle = 0f, minuteAngle = 0f, secondAngle = 180f),
            clockState(LocalTime.of(0, 0, 30)),
        )
    }

    @Test
    fun elevenFiftyNineBeforeMidnight() {
        assertEquals(
            ClockState(hourAngle = 359.5f, minuteAngle = 354f, secondAngle = 354f),
            clockState(LocalTime.of(11, 59, 59)),
        )
    }
}
