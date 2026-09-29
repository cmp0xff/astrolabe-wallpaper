# Astronomy calculations

[Astronomy Calculator](../app/src/main/kotlin/io/github/cmp0xff/astrolabewallpaper/AstronomyCalculator.kt)
answers one question: *what does the sky look like from a saved observing location at a given
instant?* Issue #4. It returns the Sun, the Moon with its phase, the seven planets visible from
Earth, a bundled set of bright stars, and the day's sunrise, sunset, and twilights.

| File | Role |
| --- | --- |
| `SkyState.kt` | The result types: `Sky`, `Horizontal`, per-body state, `EventKind`, `RiseSetEvent` |
| `AstronomyCalculator.kt` | The interface, and the time and event-window contract |
| `AstronomyEngineCalculator.kt` | The implementation, backed by Astronomy Engine |
| `StarCatalog.kt` | The bundled Hipparcos bright stars and their proper-motion arithmetic |

Every one of them is free of `android.*` imports. Combined with `java.time` being available
natively at the API 26 minimum, that is what lets the tests in
`app/src/test/kotlin/.../AstronomyEngineCalculatorTest.kt` and `...StarTest.kt` run as plain
JUnit with no Robolectric environment.

## Units

| Quantity | Unit |
| --- | --- |
| Azimuth, altitude, phase angle, phase longitude, constellation boundaries | degrees |
| Right ascension, hour angle | sidereal hours — only inside the engine boundary, never in `Sky` |
| Magnitude | Johnson V, as the engine's or the catalogue's model reports it |
| Distance | astronomical units, internal to Astronomy Engine; not exposed |
| Time | `java.time.Instant`, UTC, millisecond resolution |

`Instant` carries no zone, and nothing under `AstronomyCalculator` reads a `ZoneId`. Converting
an instant to the civil time a user reads is the caller's job, so one `Sky` serves any display
timezone. The clock follows the phone's timezone; the astronomy follows the saved location.

## Coordinate frames

| Frame | Epoch | Where it appears |
| --- | --- | --- |
| Horizontal (HOR) | — | `Horizontal.azimuthDeg`, `altitudeDeg`: clockwise from north, up from the mathematical horizon |
| Equatorial of date (EQD) | Date and time of the observation | The intermediate step for the Sun, Moon, and planets |
| Equatorial J2000 (EQJ) | J2000.0 | The star catalogue's own frame, and where constellation labels are resolved |
| Equatorial B1875 | B1875.0 | Inside the engine only: IAU constellation boundaries are defined there |

Positions of the Sun, Moon, and planets are computed with `Aberration.Corrected` at
`EquatorEpoch.OfDate`, then converted with `Refraction.Normal`. This is the standard topocentric
path: aberration matters because the light takes time to arrive, and of-date coordinates are what
a horizon conversion needs.

**Constellation labels are resolved in J2000, not of date.** The IAU boundaries are fixed in
right ascension and declination at B1875, so feeding them of-date coordinates would introduce
precession error — about a third of a degree since 2000, enough to land on the wrong side of a
boundary for a body near one. `Sky` carries the three-letter IAU abbreviation (`Tau`, `CMa`),
not the full name, because that is what star charts and dials print; the full name is a
presentation choice for #5.

## Fixed stars

Stars are not Solar System bodies, so they cannot go through the planetary path. `StarCatalog`
carries each star's Hipparcos place at epoch J2000 and its proper motion, and the reduction is:

1. Advance the catalogue place linearly in time: `alpha = alpha_0 + mu_alpha * t` and
   `delta = delta_0 + mu_delta * t`, with `t` in Julian years since J2000.
2. Build the unit vector in J2000 mean equator and equinox (EQJ).
3. Rotate it to the horizontal frame with `rotationEqjHor` — precession, nutation, and Earth
   rotation in one public engine call — and convert with `Refraction.Normal`.

The catalogue's `pmRA` column is `mu_alpha * cos(delta)`, so it is divided by `cos(delta)` before
it is added to a right ascension. Skipping that would understate the drift of every star away
from the equator, by a factor of two for Rigil Kentaurus at declination -60 degrees. The
`properMotionReproducesEpoch` test pins this: it steps each star's J2000 place back 8.75 Julian
years and requires the result to match the catalogue's own published J1991.25 place.

The catalogue is every Hipparcos main-catalogue entry brighter than V = 1.65 — 26 stars after
dropping Alpha Centauri B, which trails A by four arcseconds and would draw two labels on one
point of the dial. Provenance and the query are in [dependencies.md](dependencies.md).

Two deliberate simplifications apply to stars only:

- **No annual aberration.** The planetary path corrects for it; the star rotation chain above
  does not. The effect is at most 20 arcseconds, 0.006 degrees.
- **No annual parallax.** The Earth's orbit displaces the nearest star by under an arcsecond.

Both are far inside the star tolerance below, and both are visible in the measured spread: the
star fixtures disagree with their SOFA reference by up to 0.02 degrees, against 0.005 degrees for
the Sun and planets.

## Refraction

Positions and the horizon events use `Refraction.Normal`: the Saemundsson formula for a standard
atmosphere, the same model family JPL Horizons uses. It assumes sea-level pressure, 15 °C, and a
standard atmosphere, so a very hot or a very low-pressure day will shift a horizon event by a few
seconds. That is well inside the tolerance, and modelling actual weather is out of scope.

Two consequences are worth recording:

- The engine fades refraction toward the nadir below -1 degree of altitude, while the Horizons
  reference holds it at the -1 degree value. The two therefore diverge by up to about a third of
  a degree for bodies well below the horizon, and the position tests compare **azimuth only**
  there: refraction lifts altitude but does not turn bearing. Above -1 degree everything is
  compared.
- The observer's height above the ellipsoid is fixed at zero. The saved observing location is a
  coarse position, and the horizon dip a few hundred metres of altitude produces is far below the
  tolerances here.

The rise/set convention is the standard one: the Sun's *upper limb* crosses the horizon,
including the conventional 34 arcminutes of near-horizon refraction. The three twilights are
defined by the Sun's **centre** crossing -6, -12, and -18 degrees airlessly. The engine uses
UT1 ≈ UTC, which can be off by up to 0.9 seconds from real UT1 — under 0.004 degrees of Earth
rotation, and again inside the tolerances.

## Supported date range

Astronomy Engine targets ±1 arcminute against the USNO's NOVAS reference, and its own
documentation validates equinoxes and solstices only for 1800–2100 (within 2 minutes). The
fixtures here span 2026. This app has no requirement outside the present era, so it inherits the
engine's range rather than narrowing it, and nothing in this repository has been verified outside
2026. Very large or negative `Instant` values are not rejected; they are simply unverified.

## Events and their window

Positions are computed at the requested instant. Solar events cannot be: they belong to a day.
Each `EventKind` reports its **first** occurrence at or after the start of the UTC day containing
the requested instant, searched up to 24 hours later. The window therefore depends on the date
but not on the time of day, and `eventTime` returns `null` when the event genuinely does not
happen — polar day, polar night, or a twilight band the Sun never reaches. A `null` is a
statement about the sky, not missing data; the dial must not substitute a guessed time.

The window is UTC, not civil, because the calculator is timezone-agnostic. An event that falls in
the next UTC day belongs to that day's window and appears in a call with an instant in it. The
`eventWindowIsTheUtcDay` test pins the contract using Quito, whose nautical dusk lands just after
midnight UTC.

## Accuracy against independent references

Expected values in the tests never come from Astronomy Engine. They come from the USNO and JPL
Horizons for the Sun, Moon, planets, and solar events; from the Hipparcos catalogue plus an IAU
SOFA reduction for the stars; and from published USNO lunar phases and season instants. The
fixture file records every source and request. Tolerances sit well above what the two
implementations actually disagree by, so a regression fails while a rounding difference does not.

| Quantity | Tolerance | Measured spread | Reference |
| --- | --- | --- | --- |
| Sun azimuth and altitude | 0.05° | under 0.001° | JPL Horizons, apparent and refracted |
| Planet azimuth and altitude | 0.05° | 0.004° (Neptune) | JPL Horizons |
| Moon azimuth and altitude | 0.1° | 0.001° | JPL Horizons |
| Star azimuth and altitude | 0.1° | 0.02° | Hipparcos catalogue reduced with IAU SOFA |
| Planet and Moon magnitudes | 0.25 mag | 0.13 mag (Neptune) | JPL Horizons apparent magnitude |
| Sunrise, sunset, and twilight | 60 s | 3 s | JPL Horizons crossings, cross-checked against USNO |
| Lunar phase at a published phase instant | 0.05° of ecliptic longitude | 0.005° | USNO lunar phases |
| Proper motion over the 8.75-year catalogue step | 0.0001° | 0.0000007° (0.0024 arcsec) | Hipparcos J1991.25 place |
| Sun constellation | exact match | — | IAU boundaries at the USNO season instants |

The altitude comparison is skipped for reference positions below -1 degree, for the refraction
reason above. The 60-second event tolerance sits at the tight end of the 1–2 minutes the
acceptance criteria allow, and the two implementations actually agree to within 3 seconds — part
of which is the reference's own, since the fixture crossing is interpolated from one-minute
samples.

`sky()` costs about 1.4 ms on the JVM for all 26 stars and the 8 event searches. The wallpaper
redraws about once a second, so this is not a battery concern, but #5 should compute the stars
once per frame rather than once per star.

## What is not verified here

- **Nothing on a device.** These are JVM tests against published reference data. Rendering the
  layers, waking, and lock-screen behaviour are #5 and #6, and need the physical device.
- **Nothing outside 2026** — see the date range above.
- **The Moon's topocentric parallax at the horizon** is the engine's, not independently checked
  here beyond the two polar and two moon-phase fixtures.
- **Asteroid, comet, and rise/set-for-the-Moon** cases are out of scope for #4.

Run the tests with `./gradlew qualityGate`, or `./gradlew :app:testDebugUnitTest` for the tests
alone. They need no Android SDK, no network, and no device.
