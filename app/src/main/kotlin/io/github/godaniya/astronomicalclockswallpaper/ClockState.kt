package io.github.godaniya.astronomicalclockswallpaper

import java.time.LocalTime

/** One civil-time hand, clockwise from screen up; noon is up and midnight is down. */
internal data class ClockState(val hourAngle: Float) {
    init {
        require(hourAngle in 0f..CivilDialConstants.DEGREES_PER_REVOLUTION) { "Invalid hourAngle: $hourAngle" }
    }
}

/** Derives the 24-hour hand angle from civil time, dropping sub-second nanoseconds. */
internal fun clockState(time: LocalTime): ClockState {
    // The shared constant is a Double, which the numeral placement needs; the hand angle is a Float.
    val midnightAngle = CivilDialConstants.MIDNIGHT_ANGLE_DEG.toFloat()
    val angle =
        (time.toSecondOfDay() / CivilDialConstants.SECONDS_PER_DEGREE + midnightAngle) %
            CivilDialConstants.DEGREES_PER_REVOLUTION
    return ClockState(angle)
}
