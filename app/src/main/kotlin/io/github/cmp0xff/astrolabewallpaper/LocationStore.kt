package io.github.cmp0xff.astrolabewallpaper

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/** Persists the observing location in framework [SharedPreferences]. */
internal class LocationStore(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): ObservingLocation? {
        val latitudeText = preferences.getString(KEY_LATITUDE, null)
        val longitudeText = preferences.getString(KEY_LONGITUDE, null)
        val sourceText = preferences.getString(KEY_SOURCE, null)
        val latitude = latitudeText?.toDoubleOrNull()
        val longitude = longitudeText?.toDoubleOrNull()
        val source = sourceText?.let { value -> ObservingLocation.Source.entries.firstOrNull { it.name == value } }
        if (latitude == null || longitude == null || source == null) {
            if (latitudeText != null || longitudeText != null || sourceText != null) {
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
