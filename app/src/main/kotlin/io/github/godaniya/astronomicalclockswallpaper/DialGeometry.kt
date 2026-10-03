package io.github.godaniya.astronomicalclockswallpaper

/**
 * Geometric reference frame for the dial, independent of Android and the ephemeris engine.
 *
 * [localSiderealAngleDeg] is local apparent sidereal time in degrees, east-positive longitude
 * added to Greenwich apparent sidereal time, normalized to `[0, 360)`. It measures the local
 * meridian against the true equinox of date. [trueObliquityDeg] is the angle between the true
 * equator and ecliptic of date, including nutation. [latitudeDeg] is the observer's geographic
 * latitude, positive north. [moonLongitudeDeg] is the Moon's apparent ecliptic longitude of date
 * in degrees, normalized to `[0, 360)`, or `null` when uncalculated. [moonPhaseLongitudeDeg] is
 * the Moon's ecliptic phase longitude in degrees (`0` is new, `90` first quarter, `180` full,
 * `270` last quarter), normalized to `[0, 360)`, or `null` when uncalculated. These reference
 * angles include no atmospheric refraction.
 */
internal data class DialGeometry(
    val localSiderealAngleDeg: Double,
    val trueObliquityDeg: Double,
    val latitudeDeg: Double,
    val moonLongitudeDeg: Double? = null,
    val moonPhaseLongitudeDeg: Double? = null,
) {
    init {
        require(localSiderealAngleDeg >= 0.0 && localSiderealAngleDeg < FULL_TURN_DEGREES) {
            "local sidereal angle must be in [0, 360)"
        }
        require(trueObliquityDeg > 0.0 && trueObliquityDeg < RIGHT_ANGLE_DEGREES) {
            "true obliquity must be in (0, 90)"
        }
        require(ObservingLocation.isValidLatitude(latitudeDeg)) { "latitude must be in [-90, 90]" }
        moonLongitudeDeg?.let {
            require(it >= 0.0 && it < FULL_TURN_DEGREES) { "moon longitude must be in [0, 360)" }
        }
        moonPhaseLongitudeDeg?.let {
            require(it >= 0.0 && it < FULL_TURN_DEGREES) { "moon phase longitude must be in [0, 360)" }
        }
    }
}
