package io.github.cmp0xff.astronomicalclockswallpaper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/** Checks coordinate range boundaries and rejection of non-finite observing locations. */
class ObservingLocationTest {
    @Test
    fun latitudeOutOfRangeRejected() {
        val exception =
            runCatching {
                ObservingLocation(
                    latitude = 91.0,
                    longitude = 0.0,
                    source = ObservingLocation.Source.MANUAL,
                    zoneId = ZONE,
                )
            }.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
    }

    @Test
    fun longitudeOutOfRangeRejected() {
        val exception =
            runCatching {
                ObservingLocation(
                    latitude = 0.0,
                    longitude = -181.0,
                    source = ObservingLocation.Source.MANUAL,
                    zoneId = ZONE,
                )
            }.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
    }

    @Test
    fun rangeHelpersRejectOutOfRange() {
        assertTrue(ObservingLocation.isValidLatitude(90.0))
        assertTrue(ObservingLocation.isValidLongitude(-180.0))
        assertFalse(ObservingLocation.isValidLatitude(90.1))
        assertFalse(ObservingLocation.isValidLongitude(180.1))
    }

    @Test
    fun boundaryAndNonFiniteRejected() {
        assertTrue(ObservingLocation.isValidLatitude(-90.0))
        assertTrue(ObservingLocation.isValidLongitude(180.0))
        assertFalse(ObservingLocation.isValidLatitude(-90.1))
        assertFalse(ObservingLocation.isValidLongitude(-180.1))
        assertFalse(ObservingLocation.isValidLatitude(Double.NaN))
        assertFalse(ObservingLocation.isValidLongitude(Double.POSITIVE_INFINITY))
        assertTrue(
            runCatching {
                ObservingLocation(
                    latitude = Double.NaN,
                    longitude = 0.0,
                    source = ObservingLocation.Source.MANUAL,
                    zoneId = ZONE,
                )
            }.exceptionOrNull() is IllegalArgumentException,
        )
    }

    private companion object {
        val ZONE: ZoneId = ZoneId.of("Europe/Prague")
    }
}
