package io.github.cmp0xff.astrolabewallpaper

import android.content.Context
import android.content.SharedPreferences

/** Persists the observing location in framework [SharedPreferences]. */
internal class LocationStore(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): ObservingLocation? {
        val latitude = preferences.getString(KEY_LATITUDE, null)
        val longitude = preferences.getString(KEY_LONGITUDE, null)
        val source = preferences.getString(KEY_SOURCE, null)
        if (latitude == null || longitude == null || source == null) {
            return null
        }
        return ObservingLocation(
            latitude = latitude.toDouble(),
            longitude = longitude.toDouble(),
            source = ObservingLocation.Source.valueOf(source),
        )
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
    }
}
