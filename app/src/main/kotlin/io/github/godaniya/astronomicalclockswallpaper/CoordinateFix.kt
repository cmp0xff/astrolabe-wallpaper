package io.github.godaniya.astronomicalclockswallpaper

/** Validated coordinates from acquisition, before Settings chooses their source and saved timezone. */
internal data class CoordinateFix(val latitude: Double, val longitude: Double) {
    init {
        require(ObservingLocation.isValidLatitude(latitude)) { "invalid latitude: $latitude" }
        require(ObservingLocation.isValidLongitude(longitude)) { "invalid longitude: $longitude" }
    }
}
