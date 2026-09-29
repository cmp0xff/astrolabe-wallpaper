package io.github.cmp0xff.astrolabewallpaper

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/** Persists the observing location in framework [SharedPreferences]. */
internal class LocationStore(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): ObservingLocation? {
        // Read one snapshot without getString's ClassCastException for wrongly typed stored data.
        val stored = preferences.all
        val latitude = (stored[KEY_LATITUDE] as? String)?.toDoubleOrNull()
        val longitude = (stored[KEY_LONGITUDE] as? String)?.toDoubleOrNull()
        val sourceText = stored[KEY_SOURCE] as? String
        val source = ObservingLocation.Source.entries.firstOrNull { it.name == sourceText }
        if (latitude == null || longitude == null || source == null) {
            if (stored.keys.any { it == KEY_LATITUDE || it == KEY_LONGITUDE || it == KEY_SOURCE }) {
                Log.w(TAG, "discarding malformed observing location")
            }
            return null
        }
        val location =
            try {
                ObservingLocation(latitude, longitude, source)
            } catch (_: IllegalArgumentException) {
                null
            }
        if (location == null) {
            Log.w(TAG, "discarding out-of-range observing location")
        }
        return location
    }

    fun save(location: ObservingLocation) {
        val editor = preferences.edit()
        editor.putString(KEY_LATITUDE, location.latitude.toString())
        editor.putString(KEY_LONGITUDE, location.longitude.toString())
        editor.putString(KEY_SOURCE, location.source.name)
        editor.apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "observing_location"
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
        const val KEY_SOURCE = "source"
        private const val TAG = "LocationStore"
    }
}
