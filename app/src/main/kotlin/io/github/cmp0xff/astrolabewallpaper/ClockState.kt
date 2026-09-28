package io.github.cmp0xff.astrolabewallpaper

import java.time.LocalTime

/** Clock-hand angles in degrees, measured clockwise from 12 o'clock. */
internal data class ClockState(val hourAngle: Float, val minuteAngle: Float, val secondAngle: Float) {
    init {
        require(hourAngle in 0f..DEGREES_PER_REVOLUTION && hourAngle.isFinite()) { "Invalid hourAngle: $hourAngle" }
        require(
            minuteAngle in 0f..DEGREES_PER_REVOLUTION && minuteAngle.isFinite(),
        ) { "Invalid minuteAngle: $minuteAngle" }
        require(
            secondAngle in 0f..DEGREES_PER_REVOLUTION && secondAngle.isFinite(),
        ) { "Invalid secondAngle: $secondAngle" }
    }
}

/** Derives the three hand angles for a wall-clock time. */
internal fun clockState(time: LocalTime): ClockState {
    val secondOfDay = time.hour * SECONDS_PER_HOUR + time.minute * SECONDS_PER_MINUTE + time.second
    val secondOfHalfDay = secondOfDay % SECONDS_PER_HALF_DAY
    val secondOfHour = secondOfDay % SECONDS_PER_HOUR
    return ClockState(
        hourAngle = secondOfHalfDay * HOUR_HAND_DEGREES_PER_SECOND,
        minuteAngle = secondOfHour * MINUTE_HAND_DEGREES_PER_SECOND,
        secondAngle = time.second * SECOND_HAND_DEGREES_PER_SECOND,
    )
}

private const val DEGREES_PER_REVOLUTION = 360f
private const val HOURS_PER_REVOLUTION = 12
private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HOUR = 60 * 60
private const val SECONDS_PER_HALF_DAY = HOURS_PER_REVOLUTION * SECONDS_PER_HOUR
private const val SECOND_HAND_DEGREES_PER_SECOND = 6f
private const val MINUTE_HAND_DEGREES_PER_SECOND = 0.1f
private const val HOUR_HAND_DEGREES_PER_SECOND = 1f / 120f
