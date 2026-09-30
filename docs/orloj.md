# Orloj dial foundation

The wallpaper uses accurate astronomical geometry with an original Canvas design inspired by
the Prague Orloj. This is the foundation for #5. It includes the civil clock, zodiac,
equator, tropics, horizon, and astronomical-night boundary. Sun rendering is tracked in #27;
Moon position and illuminated phase are tracked in #28. Other astronomy layers, display size,
position and brightness controls, calendar artwork, apostles, historical hour systems, and
mechanical approximations remain outside this slice. It does not complete all of #5.

## Astronomical frame

`AstronomyCalculator.astrolabeGeometry(instant, location)` returns an Android-free
`AstrolabeGeometry`: local apparent sidereal angle, true obliquity of date, and observer latitude,
all in degrees. The pinned engine's `siderealTime` gives Greenwich apparent sidereal hours;
multiplying by 15 and adding east-positive longitude gives the local angle. The public
`rotationEctEqd` rotation of the ecliptic y-axis into the true equator of date gives true obliquity.
No engine types cross the new geometry interface. Existing body positions and UTC event windows
retain their earlier contracts; the foundation does not compute the full body/event list per frame.

## Projection

The [Astronomical Institute's Prague guide](https://astro.cas.cz/bh2010/files/praha.pdf), printed
pages 4–5 (PDF pages 4–5), describes the unusual north-pole projection: Cancer is the outer sky
boundary, Capricorn the inner tropic, and the equator lies between them. The complete ecliptic is
a rotating offset circle, including the part below the horizon. We preserve this projection
worldwide and adapt the horizon to the saved latitude.

For hour angle `H = local apparent sidereal angle − right ascension` and declination `δ`:

```text
r = tan(45° + δ/2)
x = r sin(H)
y = −r cos(H)
```

Screen x increases rightward and y downward. Divide both coordinates by
`tan(45° + trueObliquity/2)` so Cancer has radius 1. The equator radius is the inverse of that
factor, and Capricorn is its square. Increasing sidereal angle rotates the projected sky clockwise.
Ecliptic longitude is converted through the true obliquity into equatorial coordinates before
projection; equal longitude intervals are not equal intervals around the offset circle. The twelve
labels denote tropical zodiac signs, rather than the unequal IAU constellations.

The plate is geometric and unrefracted. For raw projected coordinates before Cancer normalization,
observer latitude `φ`, and `q = x² + y²`, its altitude satisfies:

```text
sin(altitude) = [sin(φ) (q − 1) − 2 cos(φ) y] / (q + 1)
```

Day is altitude ≥ 0°, twilight is −18° ≤ altitude < 0°, and astronomical night is below −18°.
These are sky regions on the fixed plate, not a whole-screen tint based on the current Sun.
Equatorial horizons are lines; polar horizons are circles. The projected horizon and night
contours are bounded to the Cancer disk so nearly equatorial sites do not generate enormous
Canvas coordinates. Contour samples are at most one degree apart on the sphere; for terrestrial
obliquity below 24°, their chord error is below 0.25 pixels at a 500-pixel sky radius. The plate
fills these sampled contours; which side of a threshold a point lies on is decided analytically
instead. The zodiac remains complete over every plate region.

## Clock, settings, and lifecycle

The Roman scale shows 24 civil hours: XII at the top, XXIV at the bottom, VI on the left, XVIII
on the right. One hand follows saved-site civil time including DST. Its angle is independent of
the zodiac's sidereal rotation; it is not a solar position marker.

**Zodiac ring** and **Day and night** default to enabled and persist across recreation. The first
controls the rotating zodiac and its labels; the second controls the plate's day/twilight/night
colors. The reference horizon, night boundary, equator, and tropics remain visible when a site is
saved. Without a saved site, only the civil clock is shown, using the phone timezone. Settings
explains that an observing location is required for sky geometry.

Each engine listens for location and layer changes, maintains one immutable settings snapshot,
and draws each frame from one instant. Updates take effect on the next visible tick. Hidden
engines do not start rendering, and destroyed engines unregister both preference listeners.
Rendering stays at one frame per second while visible.

## Verification

The geometry reference tests use independently generated ERFA/SOFA fixtures with their generator,
time-scale conventions, and tolerances documented in
[`AstrolabeGeometryFixture.kt`](../app/src/test/kotlin/io/github/cmp0xff/astrolabewallpaper/AstrolabeGeometryFixture.kt).
They assume UT1 = UTC and TT − UTC = 69.184 seconds; the pinned engine uses modeled DeltaT.
The comparison tolerances (0.0001° sidereal angle, 0.00003° obliquity) describe agreement with
those fixtures, not physical UT1 accuracy. Omitting measured DUT1 can shift sidereal angle by
up to 13.5 arcseconds. Analytic projection
tests cover equinoxes/solstices, circle tangencies, rotation direction, northern/southern sites,
the equator, poles, and day/night classification. Robolectric tests cover layer persistence,
missing-location behavior, same-instant frame calculation, and hidden/destroyed engines.
Canvas tests inspect representative renders and export PNGs under `app/build/reports/orloj`.

Run `./gradlew qualityGate :app:assembleDebug` and `scripts/verify-apk.sh`. These tests do not
establish physical-device home or lit-lock-screen correctness, frame cost, or battery behavior.
See [device-testing.md](device-testing.md) for the unrun physical-device checks.
