package io.github.cmp0xff.astrolabewallpaper

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper

/** Fetches a single approximate observing location with a bounded timeout. */
internal class LocationProvider(
    private val context: Context,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun fetch(callback: (ObservingLocation?) -> Unit) {
        if (!hasCoarsePermission()) {
            callback(null)
            return
        }
        fetchWithPermission(callback)
    }

    private fun fetchWithPermission(callback: (ObservingLocation?) -> Unit) {
        val cached = lastKnownLocation()
        if (cached != null) {
            callback(cached)
            return
        }
        val provider = firstEnabledProvider()
        if (provider == null) {
            callback(null)
            return
        }
        requestFreshLocation(provider, callback)
    }

    private fun hasCoarsePermission(): Boolean =
        context.checkSelfPermission(ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // Only reached after hasCoarsePermission() passed in fetch().
    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(): ObservingLocation? {
        for (provider in PROVIDERS) {
            val location = locationManager.getLastKnownLocation(provider)
            if (location != null) {
                return location.toObservingLocation()
            }
        }
        return null
    }

    private fun firstEnabledProvider(): String? = PROVIDERS.firstOrNull { locationManager.isProviderEnabled(it) }

    private fun requestFreshLocation(provider: String, callback: (ObservingLocation?) -> Unit) {
        var isDelivered = false
        var timeout = Runnable {}
        val listener =
            object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (!isDelivered) {
                        isDelivered = true
                        handler.removeCallbacks(timeout)
                        callback(location.toObservingLocation())
                    }
                }

                // These are abstract on API 26 but default on API 30+, so they must be overridden
                // for the min SDK 26 target even though this slice never reacts to them.
                @Suppress("OVERRIDE_DEPRECATION")
                override fun onProviderDisabled(provider: String) = Unit

                @Suppress("OVERRIDE_DEPRECATION")
                override fun onProviderEnabled(provider: String) = Unit

                @Suppress("OVERRIDE_DEPRECATION")
                override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) = Unit
            }
        timeout =
            Runnable {
                if (!isDelivered) {
                    isDelivered = true
                    locationManager.removeUpdates(listener)
                    callback(null)
                }
            }
        handler.postDelayed(timeout, timeoutMillis)
        try {
            requestSingleUpdate(provider, listener)
        } catch (_: SecurityException) {
            if (!isDelivered) {
                isDelivered = true
                handler.removeCallbacks(timeout)
                callback(null)
            }
        }
    }

    // requestSingleUpdate is the single-update API available since API 9; the API 30 replacement
    // (getCurrentLocation) is unavailable on the min SDK 26 target. See docs/development.md.
    // Only reached after hasCoarsePermission() passed in fetch().
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun requestSingleUpdate(provider: String, listener: LocationListener) {
        locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
    }

    private companion object {
        const val ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION"
        const val DEFAULT_TIMEOUT_MILLIS = 10_000L
        val PROVIDERS = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
    }
}

internal fun Location.toObservingLocation(): ObservingLocation {
    val location =
        ObservingLocation(
            latitude = latitude,
            longitude = longitude,
            source = ObservingLocation.Source.CURRENT_COARSE,
        )
    return location
}
