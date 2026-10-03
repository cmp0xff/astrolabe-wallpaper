package io.github.godaniya.astronomicalclockswallpaper

import java.time.Instant

/**
 * Computes the sky over an observing location.
 *
 * The interface is deliberately small and free of Android types, so calculations can be tested
 * on the JVM and can outlive the wallpaper service that calls them.
 *
 * ## Times
 *
 * Everything is UTC. [Instant] carries no zone, and this interface never consults
 * [java.time.ZoneId]: the observing location comes from the caller, not from the phone's
 * timezone. Turning an instant into the civil time a user reads is the caller's job, so that
 * the same [Sky] serves any display timezone.
 *
 * ## Event window
 *
 * Bodies are placed at [time] exactly. Solar events instead belong to a day: each
 * [EventKind] reports its **first** occurrence at or after the start of the UTC day containing
 * [time], searched up to 24 hours later. The window therefore depends on the date but not on
 * the time of day, and a `null` event time means the event genuinely does not happen in that
 * window — polar day, polar night, or a twilight band the Sun never reaches. An event that
 * falls past the end of the window belongs to the next UTC day and is reported by a call with
 * an instant in that day.
 *
 * ## Failure contract
 *
 * [sky] does not catch anything, does not log, and has no error result: a null is always a
 * statement about the sky, never a failed calculation. It may throw, and a caller in a render
 * loop has to decide what to do about that — degrade to a partial frame, or let the frame fail.
 *
 * - `ArithmeticException` from `Instant.toEpochMilli` for an instant outside the roughly ±292
 *   million years a `long` of epoch milliseconds spans. The documented range is 2026; this is
 *   the boundary of what the signature accepts at all.
 * - `IllegalArgumentException` from [Horizontal]'s range checks, which reject a non-finite
 *   azimuth or altitude, and from the engine's `constellation` when the declination it is given
 *   is outside -90..+90 degrees — reachable only through a star whose proper-motion-extrapolated
 *   place has left the sphere.
 * - `ExceptionInInitializerError` from [StarCatalog] on first use if any bundled row violates
 *   its own invariants, followed by `NoClassDefFoundError` on later attempts in the same
 *   process. A malformed row is a build-time data error, but it surfaces here, on the first
 *   frame that needs a star.
 * - `io.github.cosinekitty.astronomy.InternalError` when an engine iteration, search, or
 *   boundary lookup cannot converge. It is the engine's own class, which extends `Exception`,
 *   and is **not** `java.lang.InternalError`; `catch (e: Exception)` covers it.
 *
 * The two catalogue failures, `ExceptionInInitializerError` and its `NoClassDefFoundError`
 * follow-on, are `Error`s rather than `Exception`s, so a caller that wraps `sky()` in
 * `catch (e: Exception)` will not contain a malformed catalogue. That is deliberate: a
 * build-time data error should not look like a runtime one.
 *
 * Logging belongs to the caller, not here: this layer is deliberately free of Android types so
 * it can be tested on the JVM, and it owns no logger. #5 owns the render loop and therefore
 * owns recording these failures where a field report can find them.
 */
internal interface AstronomyCalculator {
    /** The sky at [time] over [location], with the UTC day's solar events. */
    fun sky(time: Instant, location: ObservingLocation): Sky

    /**
     * Unrefracted reference geometry at [time], using the saved coordinates and ignoring its zone.
     * Apparent sidereal time and true obliquity both refer to the true equinox/equator of date.
     * The engine approximates UT1 with UTC and derives TT from its built-in Delta-T model.
     */
    fun dialGeometry(time: Instant, location: ObservingLocation): DialGeometry
}
