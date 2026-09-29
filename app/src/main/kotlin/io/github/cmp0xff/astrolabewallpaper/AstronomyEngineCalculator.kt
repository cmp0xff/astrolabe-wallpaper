package io.github.cmp0xff.astrolabewallpaper

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.Topocentric
import io.github.cosinekitty.astronomy.Vector
import io.github.cosinekitty.astronomy.constellation
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.illumination
import io.github.cosinekitty.astronomy.moonPhase
import io.github.cosinekitty.astronomy.rotationEqjHor
import io.github.cosinekitty.astronomy.searchAltitude
import io.github.cosinekitty.astronomy.searchRiseSet
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.cos
import kotlin.math.sin

/**
 * The production [AstronomyCalculator], backed by Astronomy Engine.
 *
 * Every Solar System body is reduced the same way: the engine gives topocentric equatorial
 * coordinates of date with aberration corrected, [horizon] turns those into azimuth and altitude,
 * and the altitude is adjusted for the standard atmosphere with [Refraction.Normal]. Fixed stars
 * cannot go through [equator], because they are not Solar System bodies, so [StarCatalog] supplies
 * their J2000 place and proper motion and [rotationEqjHor] carries it to the horizon. See
 * `docs/astronomy.md` for frames, units, and tolerances.
 *
 * Three small conventions recur below.
 *
 * - An [Instant] becomes an engine [Time] through `fromMillisecondsSince1970`, which drops the
 *   sub-millisecond part. Nothing here is sensitive to less than a millisecond, and `Instant`
 *   stores milliseconds anyway.
 * - A computed azimuth is folded into `0..360` with `mod`, because the engine can return a
 *   negative zero, which the [Horizontal] range check would accept but which reads badly.
 * - The observer sits at zero height above the ellipsoid. The observing location is a coarse
 *   position, so the difference a few metres of altitude makes is far below the tolerances here.
 */
internal class AstronomyEngineCalculator : AstronomyCalculator {
    override fun sky(time: Instant, location: ObservingLocation): Sky {
        val instant = Time.fromMillisecondsSince1970(time.toEpochMilli())
        val observer = Observer(latitude = location.latitude, longitude = location.longitude, height = 0.0)
        return Sky(
            sun = sunState(instant, observer),
            moon = moonState(instant, observer),
            planets = Planet.entries.map { planetState(it, instant, observer) },
            stars = starStates(instant, observer),
            events = EventKind.entries.map { RiseSetEvent(kind = it, time = eventTime(it, time, observer)) },
        )
    }

    private fun sunState(time: Time, observer: Observer): SolarState {
        val position = horizontalPosition(body = Body.Sun, time = time, observer = observer)
        val constellation = j2000ConstellationSymbol(body = Body.Sun, time = time, observer = observer)
        return SolarState(position = position, constellation = constellation)
    }

    private fun moonState(time: Time, observer: Observer): MoonState {
        val light = illumination(body = Body.Moon, time = time)
        return MoonState(
            position = horizontalPosition(body = Body.Moon, time = time, observer = observer),
            phaseLongitudeDeg = moonPhase(time),
            phaseFraction = light.phaseFraction,
            phaseAngleDeg = light.phaseAngle,
            magnitude = light.mag,
        )
    }

    private fun planetState(planet: Planet, time: Time, observer: Observer): PlanetState {
        val position = horizontalPosition(body = planet.body, time = time, observer = observer)
        val magnitude = illumination(body = planet.body, time = time).mag
        return PlanetState(planet = planet, position = position, magnitude = magnitude)
    }

    private fun starStates(time: Time, observer: Observer): List<StarState> {
        // One rotation matrix serves every star, and building it is the expensive part.
        val toHorizon = rotationEqjHor(time, observer)
        val yearsSinceJ2000 = time.ut / DAYS_PER_JULIAN_YEAR
        return StarCatalog.stars.map { star ->
            val raDeg = star.rightAscensionDegAfter(years = yearsSinceJ2000)
            val decDeg = star.declinationDegAfter(years = yearsSinceJ2000)
            val j2000 =
                j2000UnitVectorFromRaDec(
                    rightAscensionDeg = raDeg,
                    declinationDeg = decDeg,
                    time = time,
                )
            val horizontal =
                toHorizon
                    .rotate(j2000)
                    .toHorizontal(Refraction.Normal)
            StarState(
                name = star.name,
                constellation = constellation(ra = raDeg / HOURS_PER_DEGREE, dec = decDeg).symbol,
                position =
                    Horizontal(
                        azimuthDeg = horizontal.lon.mod(FULL_TURN_DEGREES),
                        altitudeDeg = horizontal.lat,
                    ),
                magnitude = star.magnitude,
            )
        }
    }

    private fun horizontalPosition(body: Body, time: Time, observer: Observer): Horizontal {
        val ofDate =
            equator(
                body = body,
                time = time,
                observer = observer,
                equdate = EquatorEpoch.OfDate,
                aberration = Aberration.Corrected,
            )
        val topocentric: Topocentric =
            horizon(
                time = time,
                observer = observer,
                ra = ofDate.ra,
                dec = ofDate.dec,
                refraction = Refraction.Normal,
            )
        return Horizontal(
            azimuthDeg = topocentric.azimuth.mod(FULL_TURN_DEGREES),
            altitudeDeg = topocentric.altitude,
        )
    }

    private fun j2000ConstellationSymbol(body: Body, time: Time, observer: Observer): String {
        // The IAU boundaries are fixed in the J2000 frame, so this asks for the body's J2000
        // position rather than the of-date one used for the horizon: precession since 2000 is
        // already about a third of a degree, enough to cross a boundary.
        val j2000 =
            equator(
                body = body,
                time = time,
                observer = observer,
                equdate = EquatorEpoch.J2000,
                aberration = Aberration.Corrected,
            )
        return constellation(ra = j2000.ra, dec = j2000.dec).symbol
    }

    private fun eventTime(kind: EventKind, time: Instant, observer: Observer): Instant? {
        val dayStart = Time.fromMillisecondsSince1970(time.truncatedTo(ChronoUnit.DAYS).toEpochMilli())
        val direction = if (kind.isRising) Direction.Rise else Direction.Set
        val centerAltitude = kind.centerAltitudeDeg
        // `searchRiseSet` adds the Sun's angular radius to the -34-arcminute horizon refraction,
        // so the upper limb crosses while the centre sits near -50 arcminutes. A twilight is
        // instead defined by the airless centre, so it searches for that altitude directly.
        val found =
            if (centerAltitude == null) {
                searchRiseSet(
                    body = Body.Sun,
                    observer = observer,
                    direction = direction,
                    startTime = dayStart,
                    limitDays = EVENT_WINDOW_DAYS,
                )
            } else {
                searchAltitude(
                    body = Body.Sun,
                    observer = observer,
                    direction = direction,
                    startTime = dayStart,
                    limitDays = EVENT_WINDOW_DAYS,
                    altitude = centerAltitude,
                )
            }
        return found?.let { Instant.ofEpochMilli(it.toMillisecondsSince1970()) }
    }

    private fun j2000UnitVectorFromRaDec(rightAscensionDeg: Double, declinationDeg: Double, time: Time): Vector {
        // In the J2000 mean equator and equinox frame (the engine's EQJ), the time only travels
        // along; the rotation later applied to this vector is what dates it.
        val ra = Math.toRadians(rightAscensionDeg)
        val dec = Math.toRadians(declinationDeg)
        return Vector(x = cos(dec) * cos(ra), y = cos(dec) * sin(ra), z = sin(dec), t = time)
    }
}

private const val EVENT_WINDOW_DAYS = 1.0
private const val FULL_TURN_DEGREES = 360.0
private const val HOURS_PER_DEGREE = 15.0
private const val DAYS_PER_JULIAN_YEAR = 365.25
