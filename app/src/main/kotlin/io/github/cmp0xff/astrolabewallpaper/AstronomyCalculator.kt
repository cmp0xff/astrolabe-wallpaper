package io.github.cmp0xff.astrolabewallpaper

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
 */
internal interface AstronomyCalculator {
    /** The sky at [time] over [location], with the UTC day's solar events. */
    fun sky(time: Instant, location: ObservingLocation): Sky
}
