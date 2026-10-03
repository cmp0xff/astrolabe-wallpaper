package io.github.cmp0xff.astrolabewallpaper

import java.time.LocalTime

/** One civil-time hand, clockwise from screen up; noon is up and midnight is down. */
internal data class ClockState(val hourAngle: Float) {
    init {
        require(hourAngle in 0f..DEGREES_PER_REVOLUTION) { "Invalid hourAngle: $hourAngle" }
    }
}

/** Derives the 24-hour hand angle from civil time, dropping sub-second nanoseconds. */
internal fun clockState(time: LocalTime): ClockState =
    ClockState((time.toSecondOfDay() / SECONDS_PER_DEGREE + MIDNIGHT_ANGLE) % DEGREES_PER_REVOLUTION)

private const val DEGREES_PER_REVOLUTION = 360f
private const val SECONDS_PER_DEGREE = 240f
private const val MIDNIGHT_ANGLE = 180f
