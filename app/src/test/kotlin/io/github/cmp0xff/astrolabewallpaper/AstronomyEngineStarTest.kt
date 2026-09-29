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
    fun properMotionReproducesEpoch() {
        // Hipparcos publishes each star at its 1991.25 observing epoch and again at J2000, 8.75
        // Julian years later. Stepping the J2000 place back by the star's proper motion has to
        // land on the published earlier place: the two epochs agree only if the catalogue's
        // `mu_alpha * cos(delta)` is divided by `cos(delta)` before it is added to a right
        // ascension. Ignoring that factor for Rigil Kentaurus, the fastest star here, would put
        // the result out by 34 arcseconds instead of the four thousandths seen at this tolerance.
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
        // Bright means a lower magnitude number, so every entry has to beat the cut.
        for (star in StarCatalog.stars) {
            assertTrue("${star.name} magnitude ${star.magnitude}", star.magnitude < BRIGHTEST_CUTOFF)
        }
        // Alpha Centauri B is four arcseconds from A and would draw a second label on the same
        // point of the dial, so the catalogue carries only the primary.
        assertTrue("Rigil Kentaurus is expected", "Rigil Kentaurus" in names)
        assertTrue("Alpha Centauri B must not be listed", names.none { it.contains("Centauri B") })

        val sky = calculator.sky(FIXTURE_INSTANT, GREENWICH.location)
        assertEquals(names.toSet(), sky.stars.map { it.name }.toSet())
    }

    @Test
    fun catalogMagnitudesAreConstant() {
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
         * one. Both are below a tenth of a degree, and the measured spread is under two
         * hundredths, so 0.1 degree leaves room while still failing on a real error.
         */
        const val STAR_TOLERANCE_DEG = 0.1

        /** J2000.0 is 8.75 Julian years after the Hipparcos epoch J1991.25. */
        const val EPOCH_GAP_YEARS = 8.75

        /** Four thousandths of an arcsecond measured; a tenth of one leaves ample room. */
        const val EPOCH_TOLERANCE_DEG = 0.0001
        const val BRIGHT_STAR_COUNT = 26
        const val BRIGHTEST_CUTOFF = 1.65
        const val EXACT = 0.0
    }
}
