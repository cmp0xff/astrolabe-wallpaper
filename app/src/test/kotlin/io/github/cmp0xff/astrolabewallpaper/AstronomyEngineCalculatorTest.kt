package io.github.cmp0xff.astrolabewallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.math.abs

/**
 * Checks the engine-backed calculator against independent reference data in
 * [AstronomyReferenceFixtures]. These are plain JUnit tests: [AstronomyEngineCalculator] and
 * everything it reaches are free of Android types, so no Robolectric environment is needed.
 *
 * The tolerances are set below what the acceptance criteria allow and well above what the two
 * implementations actually disagree by, so a real regression fails while a rounding difference
 * does not. `docs/astronomy.md` records the measured spreads.
 */
class AstronomyEngineCalculatorTest {
    private val calculator = AstronomyEngineCalculator()

    @Test
    fun sunPositionsMatchHorizons() {
        assertPositions(expectedBodies = arrayOf("Sun"), toleranceDeg = AZIMUTH_TOLERANCE_DEG)
    }

    @Test
    fun moonPositionsMatchHorizons() {
        // The Moon moves fastest and its model is the coarsest, so it gets the widest bound the
        // acceptance criteria allow; the measured spread in these fixtures is under 0.001 degree.
        assertPositions(expectedBodies = arrayOf("Moon"), toleranceDeg = MOON_TOLERANCE_DEG)
    }

    @Test
    fun planetPositionsMatchHorizons() {
        val planets = Planet.entries.map { it.body.name }.toTypedArray()
        assertPositions(expectedBodies = planets, toleranceDeg = AZIMUTH_TOLERANCE_DEG)
    }

    @Test
    fun magnitudesMatchHorizons() {
        var compared = 0
        for (fixture in positionFixtures) {
            val sky = calculator.sky(fixture.instant, fixture.site.location)
            // A null magnitude is a body this app models no magnitude for. The Sun is the one
            // such body: its fixture rows record a position and nothing else, because there is
            // nothing in [SolarState] to compare a solar magnitude against.
            val comparable = fixture.bodies.filter { it.magnitude != null }
            for (body in comparable) {
                assertEquals(
                    "${fixture.label}/${body.body} magnitude",
                    requireNotNull(body.magnitude),
                    magnitudeOf(sky, body.body),
                    MAGNITUDE_TOLERANCE,
                )
                compared++
            }
        }
        assertTrue("no magnitudes were compared", compared >= Planet.entries.size)
    }

    @Test
    fun lunarPhasesMatchUsnoInstants() {
        // The table is the only external reference for the Moon's phase, and an empty or
        // one-sided table would make every comparison below vanish without failing.
        assertTrue("no full-moon fixture", lunarPhaseFixtures.any { it.isFull })
        assertTrue("no new-moon fixture", lunarPhaseFixtures.any { !it.isFull })
        for (fixture in lunarPhaseFixtures) {
            val sky = calculator.sky(fixture.instant, GREENWICH.location)
            val where = "${fixture.phase} at ${fixture.instant}"
            val phaseFromNew =
                abs(angleDifferenceDeg(first = sky.moon.phaseLongitudeDeg, second = NEW_MOON_LONGITUDE_DEG))
            val phaseFromFull =
                abs(angleDifferenceDeg(first = sky.moon.phaseLongitudeDeg, second = FULL_MOON_LONGITUDE_DEG))
            if (fixture.isFull) {
                assertTrue("$where phase longitude", phaseFromFull <= PHASE_TOLERANCE_DEG)
                assertTrue("$where illuminated fraction", sky.moon.phaseFraction >= FULL_FRACTION)
                assertTrue("$where phase angle", sky.moon.phaseAngleDeg <= FULL_PHASE_ANGLE_DEG)
                assertTrue("$where magnitude", sky.moon.magnitude < FULL_MOON_MAGNITUDE_LIMIT)
            } else {
                assertTrue("$where phase longitude", phaseFromNew <= PHASE_TOLERANCE_DEG)
                assertTrue("$where illuminated fraction", sky.moon.phaseFraction <= NEW_FRACTION)
                assertTrue("$where phase angle", sky.moon.phaseAngleDeg >= NEW_PHASE_ANGLE_DEG)
            }
        }
    }

    @Test
    fun eventsMatchHorizonsCrossings() {
        // The table is the only external reference for solar events, and these assertions are the
        // properties #4 asks the event check to cover: both hemispheres, and the polar cases where
        // an event does not happen at all. Without them a trimmed table would pass vacuously.
        assertTrue("no northern event fixture", eventFixtures.any { it.site.latitudeDeg > 0 })
        assertTrue("no southern event fixture", eventFixtures.any { it.site.latitudeDeg < 0 })
        assertTrue("no polar event fixture", eventFixtures.any { it.usnoPolarNote != null })
        for (fixture in eventFixtures) {
            val sky = calculator.sky(utcMidnight(fixture.date), fixture.site.location)
            for (kind in EventKind.entries) {
                val where = "${fixture.site.name} ${fixture.date} $kind"
                val expected = expectedEvent(fixture, kind)
                val actual = sky.eventTime(kind)
                if (expected == null) {
                    assertNull("$where should not occur", actual)
                } else {
                    assertNotNull("$where should occur", actual)
                    assertWithin(
                        where = where,
                        expected = expected,
                        actual = requireNotNull(actual),
                        seconds = EVENT_TOLERANCE_SECONDS,
                    )
                }
            }
        }
    }

    @Test
    fun horizonEventsAgreeWithUsno() {
        for (fixture in eventFixtures) {
            val sky = calculator.sky(utcMidnight(fixture.date), fixture.site.location)
            val published =
                listOf(
                    EventKind.SUNRISE to fixture.usnoSunrise,
                    EventKind.SUNSET to fixture.usnoSunset,
                    EventKind.CIVIL_DAWN to fixture.usnoCivilDawn,
                    EventKind.CIVIL_DUSK to fixture.usnoCivilDusk,
                )
            for ((kind, time) in published) {
                assertUsnoAgreement(fixture = fixture, kind = kind, published = time, sky = sky)
            }
        }
    }

    @Test
    fun sunMatchesIauConstellations() {
        // Four instants that resolve to four different constellations: trimming the table to one
        // row would leave the check passing against a single boundary region.
        assertEquals(
            "the fixtures should resolve to four different constellations",
            SUN_CONSTELLATION_COUNT,
            sunConstellationFixtures.map { it.constellation }.toSet().size,
        )
        for (fixture in sunConstellationFixtures) {
            val sky = calculator.sky(fixture.instant, fixture.site.location)
            assertEquals(
                "Sun constellation at ${fixture.instant}",
                fixture.constellation,
                sky.sun.constellation,
            )
        }
    }

    @Test
    fun eventWindowIsTheUtcDay() {
        // Quito's nautical dusk falls just after midnight UTC and its nautical dawn late in the
        // UTC morning, so both belong to the same UTC day. The window depends on the date alone:
        // asking at either end of the day must give the same answer.
        val early = calculator.sky(Instant.parse("2026-03-20T00:00:00Z"), QUITO.location)
        val late = calculator.sky(Instant.parse("2026-03-20T23:59:59Z"), QUITO.location)
        for (kind in EventKind.entries) {
            assertEquals(
                "$kind must not depend on the time of day",
                early.eventTime(kind),
                late.eventTime(kind),
            )
        }
        val dusk = requireNotNull(early.eventTime(EventKind.NAUTICAL_DUSK)) { "no nautical dusk" }
        val dawn = requireNotNull(early.eventTime(EventKind.NAUTICAL_DAWN)) { "no nautical dawn" }
        assertTrue("nautical dusk $dusk should precede dawn $dawn on one UTC day", dusk < dawn)
        val dayStart = Instant.parse("2026-03-20T00:00:00Z")
        val dayEnd = Instant.parse("2026-03-21T00:00:00Z")
        assertTrue("nautical dusk $dusk should be inside the UTC day", dusk >= dayStart && dusk < dayEnd)

        // The following UTC day is a different window with its own times.
        val nextDay = calculator.sky(Instant.parse("2026-03-21T12:00:00Z"), QUITO.location)
        val sunrise = requireNotNull(early.eventTime(EventKind.SUNRISE)) { "no sunrise" }
        val nextSunrise = requireNotNull(nextDay.eventTime(EventKind.SUNRISE)) { "no next sunrise" }
        assertTrue("the next UTC day should shift its sunrise", nextSunrise > sunrise)
    }

    @Test
    fun polarDayAndNightReportNoEvent() {
        // Longyearbyen in June never sees the Sun set, and McMurdo in December is in its own
        // polar day. Nothing may invent a time for an event that does not happen.
        val cases = listOf(SVALBARD to "2026-06-21", MCMURDO to "2026-12-21")
        for ((site, date) in cases) {
            val sky = calculator.sky(utcMidnight(LocalDate.parse(date)), site.location)
            for (kind in EventKind.entries) {
                assertNull("$date at ${site.name}: $kind must not occur", sky.eventTime(kind))
            }
        }
    }

    @Test
    fun positionSpreadsAreEnforced() {
        // docs/astronomy.md publishes, per quantity, the spread this fixture set actually shows
        // against its reference. Those are claims about these files and nothing else measures
        // them, so these tests do. Each bound is that spread with room to spare: loose enough that
        // last-bit arithmetic cannot move it, tight enough that dropping a reduction step cannot
        // hide inside it. A failure means the documentation has to move as well.
        val sun = worstSpreadOfOneBody(SUN)
        val moon = worstSpreadOfOneBody(MOON)
        val planets = worstSpreadOfPlanets()
        assertTrue("Sun azimuth spread ${sun.azimuth}", sun.azimuth <= SUN_SPREAD_LIMIT_DEG)
        assertTrue("Sun altitude spread ${sun.altitude}", sun.altitude <= SUN_SPREAD_LIMIT_DEG)
        assertTrue("Moon azimuth spread ${moon.azimuth}", moon.azimuth <= MOON_SPREAD_LIMIT_DEG)
        assertTrue("Moon altitude spread ${moon.altitude}", moon.altitude <= MOON_SPREAD_LIMIT_DEG)
        assertTrue("planet azimuth spread ${planets.azimuth}", planets.azimuth <= PLANET_SPREAD_LIMIT_DEG)
        assertTrue("planet altitude spread ${planets.altitude}", planets.altitude <= PLANET_SPREAD_LIMIT_DEG)
        val magnitude = worstMagnitude()
        assertTrue("magnitude spread $magnitude", magnitude <= MAGNITUDE_SPREAD_LIMIT)
    }

    @Test
    fun eventSpreadIsEnforced() {
        var worst = 0L
        for (fixture in eventFixtures) {
            val sky = calculator.sky(utcMidnight(fixture.date), fixture.site.location)
            for (kind in EventKind.entries) {
                val expected = expectedEvent(fixture, kind)
                val actual = sky.eventTime(kind)
                if (expected != null && actual != null) {
                    worst = maxOf(a = worst, b = abs(Duration.between(expected, actual).seconds))
                }
            }
        }
        assertTrue("event spread $worst s", worst <= EVENT_SPREAD_LIMIT_SECONDS)
    }

    @Test
    fun phaseSpreadIsEnforced() {
        // This is also the phase-wrap case: both new-moon fixtures land just below 360 degrees of
        // ecliptic longitude, so a raw subtraction would report a 360-degree disagreement rather
        // than the 0.0052 degrees measured here.
        var worst = 0.0
        for (fixture in lunarPhaseFixtures) {
            val sky = calculator.sky(fixture.instant, GREENWICH.location)
            val target = if (fixture.isFull) FULL_MOON_LONGITUDE_DEG else NEW_MOON_LONGITUDE_DEG
            worst = maxOf(a = worst, b = abs(angleDifferenceDeg(first = sky.moon.phaseLongitudeDeg, second = target)))
        }
        assertTrue("lunar phase spread $worst", worst <= PHASE_SPREAD_LIMIT_DEG)
    }

    @Test
    fun skyIsDefinedAtBothPoles() {
        // Azimuth is not a meaningful direction at a pole — every bearing is south from the
        // north pole — so the risk is a reduction that returns NaN or an out-of-range angle
        // there, which [Horizontal] would reject and turn into a thrown exception in the middle
        // of a render. The engine's azimuth is a normalised atan2 and stays finite, so the
        // calculator is expected to answer. Nothing else covers a latitude so far from the
        // fixtures' 78.22 degrees, and [ObservingLocation] accepts exactly 90.
        for (latitude in listOf(90.0, -90.0)) {
            val site =
                ObservingLocation(
                    latitude = latitude,
                    longitude = 0.0,
                    source = ObservingLocation.Source.MANUAL,
                )
            for (instant in POLAR_INSTANTS) {
                val sky = calculator.sky(instant, site)
                val where = "latitude $latitude at $instant"
                assertEquals("$where stars", StarCatalog.stars.size, sky.stars.size)
                assertEquals("$where planets", Planet.entries.size, sky.planets.size)
                // A pole sees no solar event at all: over one UTC day the Sun's altitude there
                // moves only by the day's change in declination, under half a degree, and the
                // nearest of the eight thresholds is 50 arcminutes away.
                for (kind in EventKind.entries) {
                    assertNull("$where $kind must not occur", sky.eventTime(kind))
                }
            }
        }
    }

    @Test
    fun skyReportsEveryBodyAndEvent() {
        val sky = calculator.sky(Instant.parse("2026-06-21T12:00:00Z"), GREENWICH.location)
        assertEquals(Planet.entries.size, sky.planets.size)
        assertEquals(StarCatalog.stars.size, sky.stars.size)
        assertEquals(EventKind.entries.size, sky.events.size)
        for (kind in EventKind.entries) {
            assertEquals("$kind appears once", 1, sky.events.count { it.kind == kind })
        }
    }

    private data class Spread(val azimuth: Double, val altitude: Double)

    private fun worstSpreadOfOneBody(body: String): Spread = worstSpread { it == body }

    private fun worstSpreadOfPlanets(): Spread =
        worstSpread { name -> Planet.entries.any { planet -> planet.body.name == name } }

    private fun worstSpread(matches: (String) -> Boolean): Spread {
        var azimuth = 0.0
        var altitude = 0.0
        for (fixture in positionFixtures) {
            val sky = calculator.sky(fixture.instant, fixture.site.location)
            for (row in fixture.bodies) {
                if (!matches(row.body)) continue
                val actual = positionOf(sky, row.body)
                val bearingOff = abs(angleDifferenceDeg(first = actual.azimuthDeg, second = row.azimuthDeg))
                azimuth = maxOf(a = azimuth, b = bearingOff)
                // The altitude comparison stops below -1 degree for the reason the position tests
                // give, so this spread covers the same rows they compare.
                if (row.altitudeDeg >= REFRACTION_COMPARABLE_ALTITUDE_DEG) {
                    altitude = maxOf(a = altitude, b = abs(actual.altitudeDeg - row.altitudeDeg))
                }
            }
        }
        return Spread(azimuth = azimuth, altitude = altitude)
    }

    private fun worstMagnitude(): Double {
        var worst = 0.0
        for (fixture in positionFixtures) {
            val sky = calculator.sky(fixture.instant, fixture.site.location)
            for (row in fixture.bodies) {
                val reference = row.magnitude ?: continue
                worst = maxOf(a = worst, b = abs(magnitudeOf(sky, row.body) - reference))
            }
        }
        return worst
    }

    private fun assertPositions(expectedBodies: Array<String>, toleranceDeg: Double) {
        var compared = 0
        for (fixture in positionFixtures) {
            val sky = calculator.sky(fixture.instant, fixture.site.location)
            for (body in fixture.bodies) {
                if (body.body !in expectedBodies) continue
                val actual = positionOf(sky, body.body)
                val where = "${fixture.label}/${body.body}"
                assertTrue(
                    "$where azimuth: expected ${body.azimuthDeg}, found ${actual.azimuthDeg}",
                    abs(angleDifferenceDeg(first = actual.azimuthDeg, second = body.azimuthDeg)) <= toleranceDeg,
                )
                // The app applies Refraction.Normal, which fades toward the nadir below -1
                // degree, while the JPL Horizons convention holds the -1 degree value. The two
                // models agree above that limit; below it refraction lifts altitude but does not
                // turn bearing, so only azimuth is compared there.
                if (body.altitudeDeg >= REFRACTION_COMPARABLE_ALTITUDE_DEG) {
                    assertEquals("$where altitude", body.altitudeDeg, actual.altitudeDeg, toleranceDeg)
                }
                compared++
            }
        }
        assertTrue("no positions were compared for ${expectedBodies.joinToString()}", compared > 0)
    }

    private fun assertUsnoAgreement(fixture: EventFixture, kind: EventKind, published: LocalTime?, sky: Sky) {
        val where = "${fixture.site.name} ${fixture.date} ${kind.name.lowercase()}"
        val note = fixture.usnoPolarNote?.let { " ($it)" }.orEmpty()
        val actual = sky.eventTime(kind)
        if (published == null) {
            assertNull("$where: USNO reports no event$note", actual)
            return
        }
        assertNotNull("$where: USNO publishes $published$note", actual)
        assertWithin(
            where = "$where against USNO",
            expected = requireNotNull(utcInstant(date = fixture.date, time = published)),
            actual = requireNotNull(actual),
            seconds = USNO_TOLERANCE_SECONDS,
        )
    }

    private fun expectedEvent(fixture: EventFixture, kind: EventKind): Instant? {
        val timeOfDay =
            when (kind) {
                EventKind.SUNRISE -> fixture.sunrise
                EventKind.SUNSET -> fixture.sunset
                EventKind.CIVIL_DAWN -> fixture.civilDawn
                EventKind.CIVIL_DUSK -> fixture.civilDusk
                EventKind.NAUTICAL_DAWN -> fixture.nauticalDawn
                EventKind.NAUTICAL_DUSK -> fixture.nauticalDusk
                EventKind.ASTRONOMICAL_DAWN -> fixture.astronomicalDawn
                EventKind.ASTRONOMICAL_DUSK -> fixture.astronomicalDusk
            }
        return utcInstant(date = fixture.date, time = timeOfDay)
    }

    private fun positionOf(sky: Sky, body: String): Horizontal {
        val planet = Planet.entries.firstOrNull { it.body.name == body }
        return when (body) {
            SUN -> sky.sun.position
            MOON -> sky.moon.position
            else -> sky.planets.single { state -> state.planet == requireNotNull(planet) }.position
        }
    }

    private fun magnitudeOf(sky: Sky, body: String): Double {
        require(body != SUN) { "the Sun has no modelled magnitude" }
        return if (body == MOON) sky.moon.magnitude else planetOf(sky, body).magnitude
    }

    private fun planetOf(sky: Sky, body: String): PlanetState {
        val planet = Planet.entries.single { it.body.name == body }
        return sky.planets.single { it.planet == planet }
    }

    private fun assertWithin(where: String, expected: Instant, actual: Instant, seconds: Long) {
        val delta = Duration.between(expected, actual).seconds
        assertTrue("$where: expected $expected, found $actual (${delta}s)", abs(delta) <= seconds)
    }

    private fun utcMidnight(date: LocalDate): Instant = date.atStartOfDay(ZoneOffset.UTC).toInstant()

    private companion object {
        const val SUN = "Sun"
        const val MOON = "Moon"
        const val AZIMUTH_TOLERANCE_DEG = 0.05
        const val MOON_TOLERANCE_DEG = 0.1
        const val MAGNITUDE_TOLERANCE = 0.25
        const val PHASE_TOLERANCE_DEG = 0.05
        const val NEW_MOON_LONGITUDE_DEG = 0.0
        const val FULL_MOON_LONGITUDE_DEG = 180.0
        const val FULL_FRACTION = 0.99
        const val NEW_FRACTION = 0.01
        const val FULL_PHASE_ANGLE_DEG = 5.0
        const val NEW_PHASE_ANGLE_DEG = 175.0
        const val FULL_MOON_MAGNITUDE_LIMIT = -12.0
        const val EVENT_TOLERANCE_SECONDS = 60L
        const val USNO_TOLERANCE_SECONDS = 60L
        const val REFRACTION_COMPARABLE_ALTITUDE_DEG = -1.0

        /** The four IAU constellations the Sun is checked in: Pisces, Taurus, Virgo, Sagittarius. */
        const val SUN_CONSTELLATION_COUNT = 4

        /**
         * The spreads `docs/astronomy.md` publishes, as bounds for the spread tests. Each is
         * above the spread measured over the fixtures — Sun 0.0008 degrees, Moon 0.0014,
         * planets 0.0041, magnitudes 0.13 — and well below the tolerances asserted elsewhere, so
         * a dropped term fails here first.
         *
         * [MAGNITUDE_SPREAD_LIMIT] is different in kind: magnitudes pass straight through from
         * the engine, so no change to this repository's code can move them. It pins the engine
         * revision and the transcription, not a reduction step.
         */
        const val SUN_SPREAD_LIMIT_DEG = 0.002
        const val MOON_SPREAD_LIMIT_DEG = 0.003
        const val PLANET_SPREAD_LIMIT_DEG = 0.008
        const val MAGNITUDE_SPREAD_LIMIT = 0.2
        const val EVENT_SPREAD_LIMIT_SECONDS = 5L
        const val PHASE_SPREAD_LIMIT_DEG = 0.01

        /** Instants spread across the year for [skyIsDefinedAtBothPoles]. */
        val POLAR_INSTANTS: List<Instant> =
            listOf(
                Instant.parse("2026-03-20T12:00:00Z"),
                Instant.parse("2026-06-21T00:00:00Z"),
                Instant.parse("2026-12-21T12:00:00Z"),
            )
    }
}
