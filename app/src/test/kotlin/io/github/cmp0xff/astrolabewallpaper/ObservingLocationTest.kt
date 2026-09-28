package io.github.cmp0xff.astrolabewallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks coordinate range validation and source classification for the observing location. */
class ObservingLocationTest {
    @Test
    fun validCoordinatesAccepted() {
        val location = ObservingLocation(latitude = 45.0, longitude = -120.0, source = ObservingLocation.Source.MANUAL)
        assertEquals(45.0, location.latitude, 0.0)
        assertEquals(-120.0, location.longitude, 0.0)
        assertEquals(ObservingLocation.Source.MANUAL, location.source)
    }

    @Test
    fun latitudeOutOfRangeRejected() {
        val exception =
            runCatching {
                ObservingLocation(latitude = 91.0, longitude = 0.0, source = ObservingLocation.Source.MANUAL)
            }.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
    }

    @Test
    fun longitudeOutOfRangeRejected() {
        val exception =
            runCatching {
                ObservingLocation(latitude = 0.0, longitude = -181.0, source = ObservingLocation.Source.MANUAL)
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
}
