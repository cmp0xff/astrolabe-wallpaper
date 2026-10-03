package io.github.cmp0xff.astronomicalclockswallpaper

import java.time.Instant

/*
 * Independent reference angles generated 2026-09-30 with pyerfa 2.0.1.5 (ERFA 2.0.1,
 * SOFA 20231011), Python 3.13.15. ERFA is derived from SOFA; no Astronomy Engine result
 * or formula was used to generate these values. All angles below are degrees.
 *
 * Primary sources:
 * https://github.com/liberfa/erfa/blob/v2.0.1/src/gst06a.c
 * https://github.com/liberfa/erfa/blob/v2.0.1/src/obl06.c
 * https://github.com/liberfa/erfa/blob/v2.0.1/src/nut06a.c
 * https://github.com/liberfa/pyerfa/tree/v2.0.1.5
 *
 * Reproduce each timestamp's calendar fields (y, m, d, hh, mm, ss) in Python:
 *   import erfa, math
 *   utc = erfa.dtf2d("UTC", y, m, d, hh, mm, ss)
 *   ut1 = erfa.utcut1(*utc, 0.0)
 *   tt = erfa.taitt(*erfa.utctai(*utc))
 *   gast = math.degrees(erfa.gst06a(*ut1, *tt))
 *   obliquity = math.degrees(erfa.obl06(*tt) + erfa.nut06a(*tt)[1])
 *
 * GAST uses IAU 2006 precession and IAU 2000A nutation. True obliquity is IAU 2006
 * mean obliquity plus the nutation in obliquity. There is no refraction. Longitude is
 * east-positive; local apparent sidereal angle is (GAST + longitude) modulo 360.
 * UT1-UTC is explicitly zero, matching the engine's rotation approximation rather than
 * measured Earth orientation. UTC-to-TT uses the leap-second table: TT-UTC = 69.184 s
 * for these dates. The engine instead predicts Delta-T around 75.05-75.65 s here and
 * truncates its nutation series to five terms. The tests allow these model differences:
 * 0.0001 degree (0.36 arcsecond) in sidereal angle and 0.00003 degree (0.108 arcsecond)
 * in obliquity. These are comparison tolerances, not claimed accuracy against measured
 * UT1: ignoring DUT1 can itself cost up to about 13.5 arcseconds of rotation.
 *
 * The rows cover all four 2026 seasons plus adjacent instants across a UTC date boundary.
 * Times label evaluation instants; they do not claim to be exact equinox/solstice events.
 */

/** Greenwich true-of-date angles from the independent ERFA computation described above. */
internal data class DialGeometryFixture(
    val instant: Instant,
    val greenwichSiderealAngleDeg: Double,
    val trueObliquityDeg: Double,
)

internal val geometryFixtures: List<DialGeometryFixture> =
    listOf(
        DialGeometryFixture(
            instant = Instant.parse("2026-01-01T00:00:00Z"),
            greenwichSiderealAngleDeg = 100.662223880875,
            trueObliquityDeg = 23.438137227782,
        ),
        DialGeometryFixture(
            instant = Instant.parse("2026-03-20T14:46:00Z"),
            greenwichSiderealAngleDeg = 39.649369936616,
            trueObliquityDeg = 23.438406235100,
        ),
        DialGeometryFixture(
            instant = Instant.parse("2026-06-21T00:00:00Z"),
            greenwichSiderealAngleDeg = 269.208537339434,
            trueObliquityDeg = 23.437975731881,
        ),
        DialGeometryFixture(
            instant = Instant.parse("2026-09-23T00:05:00Z"),
            greenwichSiderealAngleDeg = 3.113096710875,
            trueObliquityDeg = 23.438125248960,
        ),
        DialGeometryFixture(
            instant = Instant.parse("2026-12-21T20:50:00Z"),
            greenwichSiderealAngleDeg = 42.938088902050,
            trueObliquityDeg = 23.437637299363,
        ),
        DialGeometryFixture(
            instant = Instant.parse("2026-03-20T23:59:59Z"),
            greenwichSiderealAngleDeg = 178.524381893801,
            trueObliquityDeg = 23.438400611018,
        ),
        DialGeometryFixture(
            instant = Instant.parse("2026-03-21T00:00:00Z"),
            greenwichSiderealAngleDeg = 178.528559968142,
            trueObliquityDeg = 23.438400610836,
        ),
    )
