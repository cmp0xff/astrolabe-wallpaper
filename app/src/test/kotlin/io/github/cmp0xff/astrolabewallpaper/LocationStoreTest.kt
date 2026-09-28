package io.github.cmp0xff.astrolabewallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Checks observing-location persistence through the framework SharedPreferences store. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 36])
class LocationStoreTest {
    @Test
    fun saveThenLoadRoundTrips() {
        val store = LocationStore(RuntimeEnvironment.getApplication())
        val location =
            ObservingLocation(latitude = 12.5, longitude = -77.0, source = ObservingLocation.Source.CURRENT_COARSE)
        store.save(location)
        assertEquals(location, store.load())
    }

    @Test
    fun loadReturnsNullWhenEmpty() {
        assertNull(LocationStore(RuntimeEnvironment.getApplication()).load())
    }
}
