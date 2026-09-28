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
import android.util.Log

/**
 * Fetches a single approximate observing location with a bounded timeout.
 *
 * The callback is always invoked on the main thread: synchronously for the permission-denied,
 * cached, and no-provider paths, and asynchronously for a fresh network request and its timeout.
 */
internal class LocationProvider(
    private val context: Context,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var activeListener: LocationListener? = null
    private var activeTimeout: Runnable? = null
    private var requestGeneration = 0

    fun fetch(forceFresh: Boolean = false, callback: (ObservingLocation?) -> Unit) {
        cancel()
        if (!hasCoarsePermission()) {
            Log.w(TAG, "location fetch skipped: ACCESS_COARSE_LOCATION not granted")
            callback(null)
            return
        }
        val cached = if (forceFresh) null else lastKnownLocation()
        if (cached != null) {
            callback(cached)
            return
        }
        if (!locationManager.isProviderEnabled(NETWORK_PROVIDER)) {
            Log.w(TAG, "network location provider disabled")
            callback(null)
        } else {
            requestFreshLocation(callback)
        }
    }

    /** Cancels any in-flight request so a finished setup does not deliver a late callback. */
    fun cancel() {
        requestGeneration++
        activeTimeout?.let { handler.removeCallbacks(it) }
        activeListener?.let { locationManager.removeUpdates(it) }
        activeListener = null
        activeTimeout = null
    }

    private fun hasCoarsePermission(): Boolean =
        context.checkSelfPermission(ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // Only reached after hasCoarsePermission() passed in fetch().
    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(): ObservingLocation? {
        val location =
            try {
                locationManager.getLastKnownLocation(NETWORK_PROVIDER)
            } catch (e: SecurityException) {
                Log.w(TAG, "getLastKnownLocation denied", e)
                null
            }
        return location?.toObservingLocation()
    }

    private fun requestFreshLocation(callback: (ObservingLocation?) -> Unit) {
        val generation = requestGeneration
        var isDelivered = false
        var timeout = Runnable {}
        val listener =
            object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (generation == requestGeneration && !isDelivered) {
                        isDelivered = true
                        handler.removeCallbacks(timeout)
                        activeListener = null
                        activeTimeout = null
                        callback(location.toObservingLocation())
                    }
                }

                // These are abstract below API 30 but default on API 30+, so they must be
                // overridden for devices at the API 26 minimum even though this slice never
                // reacts to them.
                @Suppress("OVERRIDE_DEPRECATION")
                override fun onProviderDisabled(provider: String) = Unit

                @Suppress("OVERRIDE_DEPRECATION")
                override fun onProviderEnabled(provider: String) = Unit

                @Suppress("OVERRIDE_DEPRECATION")
                override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) = Unit
            }
        activeListener = listener
        timeout =
            Runnable {
                if (generation == requestGeneration && !isDelivered) {
                    isDelivered = true
                    locationManager.removeUpdates(listener)
                    activeListener = null
                    activeTimeout = null
                    Log.w(TAG, "network location update timed out after ${timeoutMillis}ms")
                    callback(null)
                }
            }
        activeTimeout = timeout
        handler.postDelayed(timeout, timeoutMillis)
        try {
            requestSingleUpdate(NETWORK_PROVIDER, listener)
        } catch (e: SecurityException) {
            Log.w(TAG, "requestSingleUpdate denied", e)
            if (generation == requestGeneration && !isDelivered) {
                isDelivered = true
                handler.removeCallbacks(timeout)
                activeListener = null
                activeTimeout = null
                callback(null)
            }
        }
    }

    // requestSingleUpdate is the single-update API available since API 9; the API 30 replacement
    // (getCurrentLocation) is unavailable on devices at the API 26 minimum. See docs/development.md.
    // Only reached after hasCoarsePermission() passed in fetch().
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun requestSingleUpdate(provider: String, listener: LocationListener) {
        locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
    }

    private companion object {
        const val ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION"
        const val DEFAULT_TIMEOUT_MILLIS = 10_000L

        // Coarse permission cannot access the GPS provider (it requires ACCESS_FINE_LOCATION),
        // so the network provider is the only usable source for approximate location.
        val NETWORK_PROVIDER = LocationManager.NETWORK_PROVIDER
        private const val TAG = "LocationProvider"
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
