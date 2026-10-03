package io.github.godaniya.astronomicalclockswallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/** Independent civil-time anchors for the Orloj's clockwise, noon-up 24-hour scale. */
class ClockStateTest {
    @Test
    fun noonIsUpAndMidnightDown() {
        assertAngle(LocalTime.NOON, 0f)
        assertAngle(LocalTime.MIDNIGHT, 180f)
    }

    @Test
    fun sixAndEighteenAreOpposite() {
        assertAngle(LocalTime.of(6, 0), 270f)
        assertAngle(LocalTime.of(18, 0), 90f)
    }

    @Test
    fun minutesAndSecondsSweepHand() {
        assertAngle(LocalTime.of(3, 15), 228.75f)
        assertAngle(LocalTime.of(12, 20, 43), 5.1791667f)
        assertAngle(LocalTime.of(23, 59, 59), 179.99583f)
    }

    @Test
    fun noonWrapsWithoutTimeLoss() {
        assertAngle(LocalTime.of(11, 59, 59), 359.99583f)
        assertAngle(LocalTime.of(12, 0, 1), 0.0041667f)
    }

    @Test
    fun subSecondNanosAreIgnored() {
        assertEquals(clockState(LocalTime.of(0, 0, 30)), clockState(LocalTime.of(0, 0, 30, 500_000_000)))
    }

    @Test
    fun boundaryAnglesAreAccepted() {
        assertEquals(0f, ClockState(0f).hourAngle, ANGLE_TOLERANCE)
        assertEquals(360f, ClockState(360f).hourAngle, ANGLE_TOLERANCE)
    }

    @Test
    fun invalidAnglesAreRejected() {
        for (angle in listOf(-1f, 360.5f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertTrue(runCatching { ClockState(angle) }.exceptionOrNull() is IllegalArgumentException)
        }
    }

    private fun assertAngle(time: LocalTime, angle: Float) {
        assertEquals(angle, clockState(time).hourAngle, ANGLE_TOLERANCE)
    }

    private companion object {
        const val ANGLE_TOLERANCE = 0.0001f
    }
}
