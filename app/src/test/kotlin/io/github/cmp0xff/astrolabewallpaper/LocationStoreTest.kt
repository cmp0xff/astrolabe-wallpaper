package io.github.cmp0xff.astrolabewallpaper

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

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
        val store = savedStore()
        preferences().edit().putString(KEY_LATITUDE, "abc").apply()
        assertNull(store.load())
    }

    @Test
    fun loadRejectsUnknownSource() {
        val store = savedStore()
        preferences().edit().putString(KEY_SOURCE, "NOT_A_SOURCE").apply()
        assertNull(store.load())
    }

    @Test
    fun loadRejectsOutOfRange() {
        val store = savedStore()
        preferences().edit().putString(KEY_LATITUDE, "91.0").apply()
        assertNull(store.load())
    }

    @Test
    fun loadRejectsWrongFieldTypes() {
        val store = LocationStore(RuntimeEnvironment.getApplication())
        val location =
            ObservingLocation(latitude = 12.5, longitude = -77.0, source = ObservingLocation.Source.MANUAL)
        for (key in listOf(KEY_LATITUDE, KEY_LONGITUDE, KEY_SOURCE)) {
            store.save(location)
            preferences().edit().putInt(key, 123_456).apply()
            assertNull("Wrong type for $key", store.load())
        }
        assertEquals(
            List(3) { "discarding malformed observing location" },
            ShadowLog.getLogsForTag("LocationStore").map { it.msg },
        )
        assertTrue(ShadowLog.getLogsForTag("LocationStore").all { it.throwable == null })
    }

    @Test
    fun saveReplacesMalformedTypes() {
        preferences()
            .edit()
            .putBoolean(KEY_LATITUDE, true)
            .putFloat(KEY_LONGITUDE, 10.0f)
            .putStringSet(KEY_SOURCE, setOf("legacy"))
            .apply()
        val store = LocationStore(RuntimeEnvironment.getApplication())
        assertNull(store.load())
        val location = ObservingLocation(0.0, 0.0, ObservingLocation.Source.MANUAL)
        store.save(location)
        assertEquals(location, LocationStore(RuntimeEnvironment.getApplication()).load())
    }

    private fun preferences(): SharedPreferences =
        RuntimeEnvironment.getApplication().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun savedStore(): LocationStore {
        val store = LocationStore(RuntimeEnvironment.getApplication())
        val location =
            ObservingLocation(latitude = 10.0, longitude = 20.0, source = ObservingLocation.Source.MANUAL)
        store.save(location)
        assertEquals(location, store.load())
        return store
    }

    private companion object {
        const val PREFERENCES_NAME = "observing_location"
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
        const val KEY_SOURCE = "source"
    }
}
