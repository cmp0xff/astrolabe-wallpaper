package io.github.cmp0xff.astrolabewallpaper

import io.github.cosinekitty.astronomy.Body
import java.time.Instant

/**
 * A direction in the observer's local sky.
 *
 * [azimuthDeg] runs clockwise from due north (`0` north, `90` east, `180` south, `270` west);
 * [altitudeDeg] is the elevation above the mathematical horizon, positive upwards. Altitude
 * `-90` is the nadir and `+90` the zenith.
 *
 * Both angles are **apparent** angles: they include the standard atmospheric refraction that
 * [AstronomyCalculator] applies, so a body exactly on the horizon and one lifted by refraction
 * are indistinguishable here. Non-finite values fail the range checks, because `NaN` never
 * compares inside a range.
 */
internal data class Horizontal(val azimuthDeg: Double, val altitudeDeg: Double) {
    init {
        require(azimuthDeg in 0.0..FULL_TURN_DEGREES) { "azimuthDeg $azimuthDeg not in 0..360" }
        require(altitudeDeg in -RIGHT_ANGLE_DEGREES..RIGHT_ANGLE_DEGREES) {
            "altitudeDeg $altitudeDeg not in -90..+90"
        }
    }
}

/**
 * The Sun as seen from the observing location at one instant.
 *
 * [constellation] is the IAU three-letter abbreviation of the constellation containing the Sun,
 * for example `Tau`, resolved from the Sun's J2000 position against the IAU boundaries. It is
 * the abbreviation rather than the full name because star charts and dials label with the
 * abbreviation; presentation is a rendering concern.
 */
internal data class SolarState(val position: Horizontal, val constellation: String)

/**
 * The Moon as seen from the observing location at one instant.
 *
 * [phaseLongitudeDeg] is the Moon's ecliptic longitude minus the Sun's, normalised to `0..360`:
 * `0` is new, `90` first quarter, `180` full, `270` last quarter. It is the only field that
 * distinguishes waxing from waning, which [phaseFraction] alone cannot. [phaseFraction] is the
 * illuminated fraction of the apparent disc and [phaseAngleDeg] the Sun-Moon-Earth angle; both
 * come from the same illumination model as [magnitude], the visual magnitude.
 */
internal data class MoonState(
    val position: Horizontal,
    val phaseLongitudeDeg: Double,
    val phaseFraction: Double,
    val phaseAngleDeg: Double,
    val magnitude: Double,
)

/**
 * A planet of the Solar System that can be observed from Earth.
 *
 * [body] is the Astronomy Engine body this value maps to. Earth is absent: these are observing
 * targets, and the engine rejects Earth as a target.
 */
internal enum class Planet(val body: Body) {
    MERCURY(Body.Mercury),
    VENUS(Body.Venus),
    MARS(Body.Mars),
    JUPITER(Body.Jupiter),
    SATURN(Body.Saturn),
    URANUS(Body.Uranus),
    NEPTUNE(Body.Neptune),
}

/** One planet at one instant. [magnitude] is the visual magnitude from the engine's model. */
internal data class PlanetState(val planet: Planet, val position: Horizontal, val magnitude: Double)

/**
 * One catalog star at one instant.
 *
 * [name] is the IAU proper name and [constellation] the IAU three-letter abbreviation of the
 * constellation holding the star. [magnitude] is the catalog V magnitude, which is constant:
 * these are fixed stars, and their variability and their distance are both below the accuracy
 * this app needs. Positions are reduced from the catalog epoch and include the star's proper
 * motion; see [StarCatalog].
 */
internal data class StarState(
    val name: String,
    val constellation: String,
    val position: Horizontal,
    val magnitude: Double,
)

/**
 * A solar event that an observer can watch for.
 *
 * The two fields carry the definition, so each member says what it means without a lookup:
 *
 * - [isRising] is true for the dawn and sunrise members and false for dusk and sunset.
 * - [centerAltitudeDeg] is the airless altitude the Sun's **centre** crosses, which is how the
 *   three twilights are defined: `-6` degrees for civil, `-12` nautical, and `-18` astronomical.
 *   It is `null` for [SUNRISE] and [SUNSET], which use the standard horizon convention instead:
 *   the Sun's *upper limb* crosses the horizon with the conventional 34 arcminutes of
 *   near-horizon refraction, so the centre sits near `-50` arcminutes.
 */
internal enum class EventKind(val isRising: Boolean, val centerAltitudeDeg: Double?) {
    SUNRISE(isRising = true, centerAltitudeDeg = null),
    SUNSET(isRising = false, centerAltitudeDeg = null),
    CIVIL_DAWN(isRising = true, centerAltitudeDeg = CIVIL_TWILIGHT_DEG),
    CIVIL_DUSK(isRising = false, centerAltitudeDeg = CIVIL_TWILIGHT_DEG),
    NAUTICAL_DAWN(isRising = true, centerAltitudeDeg = NAUTICAL_TWILIGHT_DEG),
    NAUTICAL_DUSK(isRising = false, centerAltitudeDeg = NAUTICAL_TWILIGHT_DEG),
    ASTRONOMICAL_DAWN(isRising = true, centerAltitudeDeg = ASTRONOMICAL_TWILIGHT_DEG),
    ASTRONOMICAL_DUSK(isRising = false, centerAltitudeDeg = ASTRONOMICAL_TWILIGHT_DEG),
}

/**
 * One solar event and when it happens.
 *
 * [time] is `null` when the event does not occur — polar day or polar night, or a twilight
 * band the Sun never reaches. A `null` is a statement about the sky, not missing data, so
 * callers must not substitute a guessed time. See [AstronomyCalculator] for the window each
 * event is searched in.
 */
internal data class RiseSetEvent(val kind: EventKind, val time: Instant?)

/**
 * The whole sky at one instant: where the bodies are and what solar events the day holds.
 *
 * [AstronomyCalculator.sky] emits exactly one [RiseSetEvent] per [EventKind], so [eventTime]
 * always finds a value; only the time inside it may be `null`.
 */
internal data class Sky(
    val sun: SolarState,
    val moon: MoonState,
    val planets: List<PlanetState>,
    val stars: List<StarState>,
    val events: List<RiseSetEvent>,
)

/**
 * The instant of [kind], or `null` when [kind] does not occur in the search window.
 *
 * An extension rather than a member so that [Sky] stays a plain record of what a calculation
 * returned.
 */
internal fun Sky.eventTime(kind: EventKind): Instant? = events.single { it.kind == kind }.time

private const val FULL_TURN_DEGREES = 360.0
private const val RIGHT_ANGLE_DEGREES = 90.0
private const val CIVIL_TWILIGHT_DEG = -6.0
private const val NAUTICAL_TWILIGHT_DEG = -12.0
private const val ASTRONOMICAL_TWILIGHT_DEG = -18.0
