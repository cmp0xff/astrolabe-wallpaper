package io.github.cmp0xff.astrolabewallpaper

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** Exercises the location provider branches: cached, denied, disabled, timeout, and fresh update. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 36])
class LocationProviderTest {
    private val application = RuntimeEnvironment.getApplication()
    private val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Before
    fun grantPermission() {
        shadowOf(application).grantPermissions(ACCESS_COARSE_LOCATION)
    }

    @Test
    @Suppress("DEPRECATION")
    fun fetchReturnsCachedLocation() {
        shadowOf(locationManager).setLastKnownLocation(
            LocationManager.NETWORK_PROVIDER,
            location(latitude = LATITUDE, longitude = LONGITUDE),
        )
        var result: ObservingLocation? = null
        LocationProvider(application).fetch { result = it }
        assertEquals(
            ObservingLocation(
                latitude = LATITUDE,
                longitude = LONGITUDE,
                source = ObservingLocation.Source.CURRENT_COARSE,
            ),
            result,
        )
    }

    @Test
    fun fetchDeniedWithoutPermission() {
        shadowOf(application).denyPermissions(ACCESS_COARSE_LOCATION)
        var isCalled = false
        var result: ObservingLocation? = ObservingLocation(0.0, 0.0, ObservingLocation.Source.MANUAL)
        LocationProvider(application).fetch { fetched ->
            isCalled = true
            result = fetched
        }
        assertTrue(isCalled)
        assertNull(result)
    }

    @Test
    fun fetchNullWhenNetworkDisabled() {
        shadowOf(locationManager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, false)
        var isCalled = false
        var result: ObservingLocation? = null
        LocationProvider(application).fetch { fetched ->
            isCalled = true
            result = fetched
        }
        assertTrue(isCalled)
        assertNull(result)
    }

    @Test
    fun fetchTimesOutWithoutUpdate() {
        shadowOf(locationManager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        var isCalled = false
        var result: ObservingLocation? = null
        LocationProvider(application, timeoutMillis = SHORT_TIMEOUT).fetch { fetched ->
            isCalled = true
            result = fetched
        }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SHORT_TIMEOUT + 1))
        assertTrue(isCalled)
        assertNull(result)
    }

    @Test
    fun fetchSingleUpdateFromProvider() {
        shadowOf(locationManager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        var result: ObservingLocation? = null
        LocationProvider(application, timeoutMillis = SHORT_TIMEOUT).fetch { result = it }
        shadowOf(locationManager).simulateLocation(
            LocationManager.NETWORK_PROVIDER,
            location(latitude = LATITUDE, longitude = LONGITUDE),
        )
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(
            ObservingLocation(
                latitude = LATITUDE,
                longitude = LONGITUDE,
                source = ObservingLocation.Source.CURRENT_COARSE,
            ),
            result,
        )
    }

    @Test
    @Suppress("DEPRECATION")
    fun fetchFreshIgnoresCache() {
        shadowOf(locationManager).setLastKnownLocation(
            LocationManager.NETWORK_PROVIDER,
            location(latitude = 1.0, longitude = 2.0),
        )
        shadowOf(locationManager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        var result: ObservingLocation? = null
        LocationProvider(application, timeoutMillis = SHORT_TIMEOUT).fetch(forceFresh = true) { result = it }
        shadowOf(locationManager).simulateLocation(
            LocationManager.NETWORK_PROVIDER,
            location(latitude = LATITUDE, longitude = LONGITUDE),
        )
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(
            ObservingLocation(
                latitude = LATITUDE,
                longitude = LONGITUDE,
                source = ObservingLocation.Source.CURRENT_COARSE,
            ),
            result,
        )
    }

    private fun location(latitude: Double, longitude: Double): Location {
        val location = Location(LocationManager.NETWORK_PROVIDER)
        location.setLatitude(latitude)
        location.setLongitude(longitude)
        return location
    }

    private companion object {
        const val ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION"
        const val LATITUDE = 37.42
        const val LONGITUDE = -122.08
        const val SHORT_TIMEOUT = 1_000L
    }
}
