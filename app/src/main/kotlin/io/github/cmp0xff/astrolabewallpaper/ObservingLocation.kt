package io.github.cmp0xff.astrolabewallpaper

/** A validated observing location: coordinates plus how they were chosen. */
internal data class ObservingLocation(val latitude: Double, val longitude: Double, val source: Source) {
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
