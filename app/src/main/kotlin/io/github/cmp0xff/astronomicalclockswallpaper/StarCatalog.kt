package io.github.cmp0xff.astronomicalclockswallpaper

import kotlin.math.cos

/**
 * One entry of the bundled bright-star catalogue.
 *
 * Coordinates and motions are the Hipparcos main catalogue (ESA 1997, CDS I/239/hip_main), read
 * through VizieR. They are expressed in the ICRS, which is aligned with the J2000 mean equator
 * and equinox to well below the accuracy of this app:
 *
 * - [rightAscensionDeg] and [declinationDeg] are the catalogue place at epoch J2000, with the
 *   catalogue's own proper motion already carried from its 1991.25 observing epoch.
 * - [properMotionRaMasPerYear] is `mu_alpha * cos(delta)`, matching the catalogue's `pmRA`
 *   column; the `cos(delta)` factor is applied when the motion is converted to a change in
 *   right ascension. [properMotionDecMasPerYear] is `mu_delta`.
 * - [magnitude] is the Johnson V magnitude, the catalogue's `Vmag`, carried unchanged.
 *   Variability is a deliberate omission, not a claim that these stars are steady: Betelgeuse
 *   and Antares both swing by more than the app's 0.25 magnitude tolerance. The constructor
 *   requires it to be strictly below the catalogue cut, so a fainter row fails the first time
 *   the catalogue is touched rather than quietly joining the dial.
 *
 * The Sun is absent because it is not an entry of this catalogue at all. Alpha Centauri B (HIP
 * 71681) is dropped: it trails Rigil Kentaurus by about fifteen arcseconds at the bundled epoch,
 * which is one naked-eye point to an observer and two labels drawn on top of each other to a dial.
 */
internal data class CatalogStar(
    /** IAU proper name, for example `Sirius`. */
    val name: String,
    /** Bayer designation as the Hipparcos catalogue writes it, for example `alf CMa`. */
    val designation: String,
    val rightAscensionDeg: Double,
    val declinationDeg: Double,
    val properMotionRaMasPerYear: Double,
    val properMotionDecMasPerYear: Double,
    val magnitude: Double,
) {
    init {
        require(rightAscensionDeg in 0.0..FULL_TURN_DEGREES) {
            "rightAscensionDeg $rightAscensionDeg not in 0..360"
        }
        require(declinationDeg in -RIGHT_ANGLE_DEGREES..RIGHT_ANGLE_DEGREES) {
            "declinationDeg $declinationDeg not in -90..+90"
        }
        require(magnitude < MAX_CATALOG_MAGNITUDE) {
            "magnitude $magnitude does not beat the V < $MAX_CATALOG_MAGNITUDE catalogue cut"
        }
    }
}

/**
 * J2000 right ascension in degrees after [years] of linear proper motion.
 *
 * [years] is Julian years since J2000.0, negative for instants before it; see
 * [AstronomyEngineCalculator] for how the instant is turned into that count.
 *
 * The motion is linear in `mu_alpha`, so the catalogue's `mu_alpha * cos(delta)` is divided by
 * `cos(delta)` here. Applying the catalogue value directly to right ascension would understate
 * the drift of every star away from the equator — by a factor of two for Rigil Kentaurus, which
 * sits at a declination of -60 degrees.
 */
internal fun CatalogStar.rightAscensionDegAfter(years: Double): Double =
    rightAscensionDeg + properMotionRaMasPerYear * years / (MAS_PER_DEGREE * cos(Math.toRadians(declinationDeg)))

/**
 * J2000 declination in degrees after [years] of linear proper motion, with [years] in Julian
 * years since J2000.0.
 */
internal fun CatalogStar.declinationDegAfter(years: Double): Double =
    declinationDeg + properMotionDecMasPerYear * years / MAS_PER_DEGREE

/**
 * The app's bundled bright stars: every Hipparcos main-catalogue entry with `Vmag < 1.65`,
 * except Alpha Centauri B (see [CatalogStar]) — 26 stars from a query result of 27 rows.
 *
 * The cut at 1.65 is a rendering choice, not a requirement: 26 stars is enough to orient a dial
 * drawn for a screen a few centimetres across, and the list stays small enough to review by
 * eye. That leaves one star sitting exactly on the boundary, Elnath (HIP 25428, `Vmag` 1.65),
 * which both the query and this file exclude with a strict comparison. Completeness here is a
 * claim about the recorded query rather than about the sky: `docs/dependencies.md` records it,
 * and re-running it returns these 26 rows plus HIP 71681.
 */
internal object StarCatalog {
    val stars: List<CatalogStar> =
        listOf(
            CatalogStar(
                name = "Sirius",
                designation = "alf CMa",
                rightAscensionDeg = 101.28715539,
                declinationDeg = -16.71611582,
                properMotionRaMasPerYear = -546.01,
                properMotionDecMasPerYear = -1223.08,
                magnitude = -1.44,
            ),
            CatalogStar(
                name = "Canopus",
                designation = "alf Car",
                rightAscensionDeg = 95.9879578,
                declinationDeg = -52.69566046,
                properMotionRaMasPerYear = 19.99,
                properMotionDecMasPerYear = 23.67,
                magnitude = -0.62,
            ),
            CatalogStar(
                name = "Arcturus",
                designation = "alf Boo",
                rightAscensionDeg = 213.9153001,
                declinationDeg = 19.1824103,
                properMotionRaMasPerYear = -1093.45,
                properMotionDecMasPerYear = -1999.4,
                magnitude = -0.05,
            ),
            CatalogStar(
                name = "Rigil Kentaurus",
                designation = "alf Cen A",
                rightAscensionDeg = 219.90206584,
                declinationDeg = -60.83397468,
                properMotionRaMasPerYear = -3678.19,
                properMotionDecMasPerYear = 481.84,
                magnitude = -0.01,
            ),
            CatalogStar(
                name = "Vega",
                designation = "alf Lyr",
                rightAscensionDeg = 279.23473511,
                declinationDeg = 38.7836918,
                properMotionRaMasPerYear = 201.02,
                properMotionDecMasPerYear = 287.46,
                magnitude = 0.03,
            ),
            CatalogStar(
                name = "Capella",
                designation = "alf Aur",
                rightAscensionDeg = 79.1723294,
                declinationDeg = 45.99799111,
                properMotionRaMasPerYear = 75.52,
                properMotionDecMasPerYear = -427.13,
                magnitude = 0.08,
            ),
            CatalogStar(
                name = "Rigel",
                designation = "bet Ori",
                rightAscensionDeg = 78.63446812,
                declinationDeg = -8.20164055,
                properMotionRaMasPerYear = 1.87,
                properMotionDecMasPerYear = -0.56,
                magnitude = 0.18,
            ),
            CatalogStar(
                name = "Procyon",
                designation = "alf CMi",
                rightAscensionDeg = 114.82549301,
                declinationDeg = 5.22499306,
                properMotionRaMasPerYear = -716.57,
                properMotionDecMasPerYear = -1034.58,
                magnitude = 0.4,
            ),
            CatalogStar(
                name = "Achernar",
                designation = "alf Eri",
                rightAscensionDeg = 24.42852736,
                declinationDeg = -57.23675749,
                properMotionRaMasPerYear = 88.02,
                properMotionDecMasPerYear = -40.08,
                magnitude = 0.45,
            ),
            CatalogStar(
                name = "Betelgeuse",
                designation = "alf Ori",
                rightAscensionDeg = 88.7929386,
                declinationDeg = 7.40706274,
                properMotionRaMasPerYear = 27.33,
                properMotionDecMasPerYear = 10.86,
                magnitude = 0.45,
            ),
            CatalogStar(
                name = "Hadar",
                designation = "bet Cen",
                rightAscensionDeg = 210.95585201,
                declinationDeg = -60.37303931,
                properMotionRaMasPerYear = -33.96,
                properMotionDecMasPerYear = -25.06,
                magnitude = 0.61,
            ),
            CatalogStar(
                name = "Altair",
                designation = "alf Aql",
                rightAscensionDeg = 297.69582916,
                declinationDeg = 8.86832198,
                properMotionRaMasPerYear = 536.82,
                properMotionDecMasPerYear = 385.54,
                magnitude = 0.76,
            ),
            CatalogStar(
                name = "Acrux",
                designation = "alf Cru",
                rightAscensionDeg = 186.64956584,
                declinationDeg = -63.09909166,
                properMotionRaMasPerYear = -35.37,
                properMotionDecMasPerYear = -14.73,
                magnitude = 0.77,
            ),
            CatalogStar(
                name = "Aldebaran",
                designation = "alf Tau",
                rightAscensionDeg = 68.9801611,
                declinationDeg = 16.50930139,
                properMotionRaMasPerYear = 62.78,
                properMotionDecMasPerYear = -189.36,
                magnitude = 0.87,
            ),
            CatalogStar(
                name = "Spica",
                designation = "alf Vir",
                rightAscensionDeg = 201.29824701,
                declinationDeg = -11.16132203,
                properMotionRaMasPerYear = -42.5,
                properMotionDecMasPerYear = -31.73,
                magnitude = 0.98,
            ),
            CatalogStar(
                name = "Antares",
                designation = "alf Sco",
                rightAscensionDeg = 247.35192046,
                declinationDeg = -26.43200249,
                properMotionRaMasPerYear = -10.16,
                properMotionDecMasPerYear = -23.21,
                magnitude = 1.06,
            ),
            CatalogStar(
                name = "Pollux",
                designation = "bet Gem",
                rightAscensionDeg = 116.32895983,
                declinationDeg = 28.02619862,
                properMotionRaMasPerYear = -625.69,
                properMotionDecMasPerYear = -45.95,
                magnitude = 1.16,
            ),
            CatalogStar(
                name = "Fomalhaut",
                designation = "alf PsA",
                rightAscensionDeg = 344.41269372,
                declinationDeg = -29.62223615,
                properMotionRaMasPerYear = 329.22,
                properMotionDecMasPerYear = -164.22,
                magnitude = 1.17,
            ),
            CatalogStar(
                name = "Mimosa",
                designation = "bet Cru",
                rightAscensionDeg = 191.93026305,
                declinationDeg = -59.68876362,
                properMotionRaMasPerYear = -48.24,
                properMotionDecMasPerYear = -12.82,
                magnitude = 1.25,
            ),
            CatalogStar(
                name = "Deneb",
                designation = "alf Cyg",
                rightAscensionDeg = 310.35797809,
                declinationDeg = 45.280338,
                properMotionRaMasPerYear = 1.56,
                properMotionDecMasPerYear = 1.55,
                magnitude = 1.25,
            ),
            CatalogStar(
                name = "Regulus",
                designation = "alf Leo",
                rightAscensionDeg = 152.0929611,
                declinationDeg = 11.96720706,
                properMotionRaMasPerYear = -249.4,
                properMotionDecMasPerYear = 4.91,
                magnitude = 1.36,
            ),
            CatalogStar(
                name = "Adhara",
                designation = "eps CMa",
                rightAscensionDeg = 104.65645182,
                declinationDeg = -28.97208374,
                properMotionRaMasPerYear = 2.63,
                properMotionDecMasPerYear = 2.29,
                magnitude = 1.5,
            ),
            CatalogStar(
                name = "Castor",
                designation = "alf Gem",
                rightAscensionDeg = 113.64942834,
                declinationDeg = 31.88827629,
                properMotionRaMasPerYear = -206.33,
                properMotionDecMasPerYear = -148.18,
                magnitude = 1.58,
            ),
            CatalogStar(
                name = "Gacrux",
                designation = "gam Cru",
                rightAscensionDeg = 187.79149709,
                declinationDeg = -57.11321169,
                properMotionRaMasPerYear = 27.94,
                properMotionDecMasPerYear = -264.33,
                magnitude = 1.59,
            ),
            CatalogStar(
                name = "Shaula",
                designation = "lam Sco",
                rightAscensionDeg = 263.40216661,
                declinationDeg = -37.10382115,
                properMotionRaMasPerYear = -8.9,
                properMotionDecMasPerYear = -29.95,
                magnitude = 1.62,
            ),
            CatalogStar(
                name = "Bellatrix",
                designation = "gam Ori",
                rightAscensionDeg = 81.28276276,
                declinationDeg = 6.34970223,
                properMotionRaMasPerYear = -8.75,
                properMotionDecMasPerYear = -13.28,
                magnitude = 1.64,
            ),
        )
}

private const val MAS_PER_DEGREE = 3_600_000.0
private const val MAX_CATALOG_MAGNITUDE = 1.65
