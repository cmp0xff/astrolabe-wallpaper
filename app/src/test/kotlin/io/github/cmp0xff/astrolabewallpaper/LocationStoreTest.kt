package io.github.cmp0xff.astrolabewallpaper

import android.content.Context
import android.content.SharedPreferences
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

    @Test
    fun loadNullWhenPartiallySet() {
        preferences().edit().putString(KEY_LATITUDE, "10.0").apply()
        assertNull(LocationStore(RuntimeEnvironment.getApplication()).load())
    }

    @Test
    fun loadRejectsNonNumeric() {
        preferences()
            .edit()
            .putString(KEY_LATITUDE, "abc")
            .putString(KEY_LONGITUDE, "10.0")
            .putString(KEY_SOURCE, ObservingLocation.Source.MANUAL.name)
            .apply()
        assertNull(LocationStore(RuntimeEnvironment.getApplication()).load())
    }

    @Test
    fun loadRejectsUnknownSource() {
        preferences()
            .edit()
            .putString(KEY_LATITUDE, "10.0")
            .putString(KEY_LONGITUDE, "20.0")
            .putString(KEY_SOURCE, "NOT_A_SOURCE")
            .apply()
        assertNull(LocationStore(RuntimeEnvironment.getApplication()).load())
    }

    @Test
    fun loadRejectsOutOfRange() {
        preferences()
            .edit()
            .putString(KEY_LATITUDE, "91.0")
            .putString(KEY_LONGITUDE, "20.0")
            .putString(KEY_SOURCE, ObservingLocation.Source.MANUAL.name)
            .apply()
        assertNull(LocationStore(RuntimeEnvironment.getApplication()).load())
    }

    private fun preferences(): SharedPreferences =
        RuntimeEnvironment.getApplication().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private companion object {
        const val PREFERENCES_NAME = "observing_location"
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
        const val KEY_SOURCE = "source"
    }
}
