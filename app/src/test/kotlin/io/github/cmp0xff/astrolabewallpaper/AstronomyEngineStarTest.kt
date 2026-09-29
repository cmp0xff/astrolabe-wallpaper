package io.github.cmp0xff.astrolabewallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.math.abs

/**
 * Checks the bundled [StarCatalog] and the fixed-star reduction against independent data.
 *
 * The star path cannot reuse the planetary code: a star is not a Solar System body, so the
 * catalogue place and its proper motion replace the engine's ephemeris. These tests therefore
 * cover both the catalogued numbers and the J2000-to-horizon rotation applied to them, comparing
 * against the Hipparcos catalogue and an IAU SOFA reduction of the same input.
 *
 * The membership checks pin the recorded set, not completeness against the catalogue: the
 * expected names are the 26 rows of the query recorded in `docs/dependencies.md`. Re-running
 * that query is what establishes completeness, and it needs the network, so the query itself is
 * recorded rather than only its result.
 */
class AstronomyEngineStarTest {
    private val calculator = AstronomyEngineCalculator()

    @Test
    fun starPositionsMatchSofa() {
        var compared = 0
        for (fixture in starFixtures) {
            val sky = calculator.sky(fixture.instant, fixture.site.location)
            val star = sky.stars.single { it.name == fixture.name }
            val where = "${fixture.name} from ${fixture.site.name} at ${fixture.instant}"
            assertTrue(
                "$where azimuth: expected ${fixture.azimuthDeg}, found ${star.position.azimuthDeg}",
                abs(
                    angleDifferenceDeg(first = star.position.azimuthDeg, second = fixture.azimuthDeg),
                ) <= STAR_TOLERANCE_DEG,
            )
            assertEquals(
                "$where altitude",
                fixture.altitudeDeg,
                star.position.altitudeDeg,
                STAR_TOLERANCE_DEG,
            )
            compared++
        }
        assertTrue("no star positions were compared", compared > 0)
    }

    @Test
    fun starConstellationsMatchSimbad() {
        for (fixture in starFixtures) {
            val sky = calculator.sky(fixture.instant, fixture.site.location)
            val star = sky.stars.single { it.name == fixture.name }
            assertEquals(
                "${fixture.name} constellation",
                fixture.constellation,
                star.constellation,
            )
        }
    }

    @Test
    fun starSpreadsAreEnforced() {
        // The star row of the spread table in docs/astronomy.md, measured here rather than left
        // to prose. The spread is what the app's omitted annual aberration and parallax cost
        // against the SOFA chain, so it should stay where it is; a jump means one of the
        // reductions moved. Measured: 0.020 degrees of azimuth, 0.007 of altitude.
        var azimuth = 0.0
        var altitude = 0.0
        // Every bundled star has to appear somewhere in this table, which is what makes the
        // spread above a statement about the whole catalogue rather than about part of it.
        assertEquals(StarCatalog.stars.map { it.name }.toSet(), starFixtures.map { it.name }.toSet())
        for (fixture in starFixtures) {
            val sky = calculator.sky(fixture.instant, fixture.site.location)
            val star = sky.stars.single { it.name == fixture.name }
            azimuth =
                maxOf(
                    a = azimuth,
                    b = abs(angleDifferenceDeg(first = star.position.azimuthDeg, second = fixture.azimuthDeg)),
                )
            altitude = maxOf(a = altitude, b = abs(star.position.altitudeDeg - fixture.altitudeDeg))
        }
        assertTrue("star azimuth spread $azimuth", azimuth <= STAR_SPREAD_LIMIT_DEG)
        assertTrue("star altitude spread $altitude", altitude <= STAR_SPREAD_LIMIT_DEG)
    }

    @Test
    fun properMotionReproducesEpoch() {
        // Hipparcos publishes each star at its 1991.25 observing epoch and again at J2000, 8.75
        // Julian years later. Stepping the J2000 place back by the star's proper motion has to
        // land on the published earlier place: the two epochs agree only if the catalogue's
        // `mu_alpha * cos(delta)` is divided by `cos(delta)` before it is added to a right
        // ascension. Ignoring that factor for Rigil Kentaurus, the fastest star here, puts the
        // result out by 34 arcseconds instead of the 0.0045 arcsecond that this tolerance holds.
        //
        // This is also the only test that catches a missing `cos(delta)`: at the 2026 instants
        // the starPositionsMatchSofa fixtures use, that error is about 0.03 degrees, well inside
        // the 0.1 degree the position test allows. That makes the row set load-bearing, so it is
        // checked against the catalogue rather than trusted: a trimmed table would otherwise keep
        // passing while quietly dropping the comparison.
        assertEquals(StarCatalog.stars.map { it.name }.toSet(), catalogEpochFixtures.map { it.name }.toSet())
        for (fixture in catalogEpochFixtures) {
            val star = StarCatalog.stars.single { it.name == fixture.name }
            assertEquals(
                "${fixture.name} right ascension at J1991.25",
                fixture.rightAscensionDeg,
                star.rightAscensionDegAfter(years = -EPOCH_GAP_YEARS),
                EPOCH_TOLERANCE_DEG,
            )
            assertEquals(
                "${fixture.name} declination at J1991.25",
                fixture.declinationDeg,
                star.declinationDegAfter(years = -EPOCH_GAP_YEARS),
                EPOCH_TOLERANCE_DEG,
            )
        }
    }

    @Test
    fun catalogCoversBrightStarsOnce() {
        val names = StarCatalog.stars.map { it.name }
        assertEquals("duplicate star names", names.size, names.toSet().size)
        assertEquals(BRIGHT_STAR_COUNT, names.size)
        // Each row carries the Bayer designation the recorded catalogue query returned. It is
        // the only per-row link back to that query and nothing else in the app reads it, so
        // asserting it here is what keeps it from being free text — and it catches a duplicated
        // transcription row even when the two rows disagree about the name.
        val designations = StarCatalog.stars.map { it.designation }
        assertEquals("duplicate designations", designations.size, designations.toSet().size)
        for ((name, designation) in names.zip(designations)) {
            assertTrue("$name has a blank designation", designation.isNotBlank())
        }
        // Alpha Centauri B is about fifteen arcseconds from A and would draw a second label on the same
        // point of the dial, so the catalogue carries only the primary.
        assertTrue("Rigil Kentaurus is expected", "Rigil Kentaurus" in names)
        assertTrue("Alpha Centauri B must not be listed", names.none { it.contains("Centauri B") })

        val sky = calculator.sky(FIXTURE_INSTANT, GREENWICH.location)
        assertEquals(names.toSet(), sky.stars.map { it.name }.toSet())
    }

    @Test
    fun catalogMagnitudesAreConstant() {
        // The model only carries the catalogue value through: nothing here derives a star's
        // magnitude from the engine. That is the property under test, since a magnitude the
        // engine computed for a fixed star would be a different number entirely.
        val sky = calculator.sky(FIXTURE_INSTANT, GREENWICH.location)
        for (star in sky.stars) {
            val catalogued = StarCatalog.stars.single { it.name == star.name }
            assertEquals("${star.name} magnitude", catalogued.magnitude, star.magnitude, EXACT)
        }
    }

    private companion object {
        val FIXTURE_INSTANT: Instant = Instant.parse("2026-06-21T22:00:00Z")

        /**
         * The star fixtures compare an app that ignores annual aberration against a SOFA chain
         * that includes it, a 20-arcsecond effect, and a linear proper motion against a rigorous
         * one. Both are far below a tenth of a degree, and the measured spread is under two
         * hundredths, so 0.1 degree leaves room while still failing on a real error.
         *
         * This test alone would pass if the `cos(delta)` conversion were dropped;
         * [properMotionReproducesEpoch] is what catches that.
         */
        const val STAR_TOLERANCE_DEG = 0.1

        /** J2000.0 is 8.75 Julian years after the Hipparcos epoch J1991.25. */
        const val EPOCH_GAP_YEARS = 8.75

        /**
         * The largest measured residual over [catalogEpochFixtures] is 1.25e-6 degrees, or
         * 0.0045 arcseconds, for the declination of Rigil Kentaurus — a star with by far the
         * largest proper motion in the catalogue. The next largest is about seventy times
         * smaller, so this bound is set by that one star, and the other 25 sit at or below
         * 1e-8 degrees, at or under the precision the catalogue publishes its places to.
         *
         * 3e-6 degrees leaves 2.4 times the worst residual and still fails on a dropped
         * proper-motion term or a missing `cos(delta)`, which cost that star tens of arcseconds.
         * What it cannot see is a sub-arcsecond slip in one of the slower stars: this is one
         * bound for all 26 rows, not one bound per row.
         */
        const val EPOCH_TOLERANCE_DEG = 3.0e-6

        /**
         * The spread `docs/astronomy.md` publishes for stars, as a bound for
         * [starSpreadsAreEnforced]: twice the 0.020 degrees measured over [starFixtures].
         */
        const val STAR_SPREAD_LIMIT_DEG = 0.04
        const val BRIGHT_STAR_COUNT = 26
        const val EXACT = 0.0
    }
}
