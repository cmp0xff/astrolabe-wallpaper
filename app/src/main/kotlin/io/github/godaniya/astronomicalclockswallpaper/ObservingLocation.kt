package io.github.godaniya.astronomicalclockswallpaper

import java.time.ZoneId

/** A validated observing location, its saved civil timezone, and how its coordinates were chosen. */
internal data class ObservingLocation(
    val latitude: Double,
    val longitude: Double,
    val source: Source,
    val zoneId: ZoneId,
) {
    /** Names are persisted by [LocationStore]; renaming a value requires a storage migration. */
    enum class Source { CURRENT_COARSE, MANUAL }

    init {
        require(isValidLatitude(latitude)) { "latitude $latitude outside [-$MAX_LATITUDE, $MAX_LATITUDE]" }
        require(isValidLongitude(longitude)) { "longitude $longitude outside [-$MAX_LONGITUDE, $MAX_LONGITUDE]" }
    }

    companion object {
        const val MAX_LATITUDE = 90.0
        const val MAX_LONGITUDE = 180.0

        fun isValidLatitude(latitude: Double): Boolean = latitude in -MAX_LATITUDE..MAX_LATITUDE

        fun isValidLongitude(longitude: Double): Boolean = longitude in -MAX_LONGITUDE..MAX_LONGITUDE
    }
}
