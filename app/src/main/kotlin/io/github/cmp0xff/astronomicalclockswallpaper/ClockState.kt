package io.github.cmp0xff.astronomicalclockswallpaper

import java.time.LocalTime

/** One civil-time hand, clockwise from screen up; noon is up and midnight is down. */
internal data class ClockState(val hourAngle: Float) {
    init {
        require(hourAngle in 0f..CivilDialConstants.DEGREES_PER_REVOLUTION) { "Invalid hourAngle: $hourAngle" }
    }
}

/** Derives the 24-hour hand angle from civil time, dropping sub-second nanoseconds. */
internal fun clockState(time: LocalTime): ClockState {
    val angle =
        (time.toSecondOfDay() / CivilDialConstants.SECONDS_PER_DEGREE + CivilDialConstants.MIDNIGHT_ANGLE_DEG_F) %
            CivilDialConstants.DEGREES_PER_REVOLUTION
    return ClockState(angle)
}
