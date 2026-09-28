package io.github.cmp0xff.astrolabewallpaper

import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowLocationManager

/** Exercises the provider's defensive [SecurityException] path through a throwing shadow. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 36], shadows = [ThrowingLocationManagerShadow::class])
class LocationProviderSecurityExceptionTest {
    private val application = RuntimeEnvironment.getApplication()

    @Before
    fun grantPermission() {
        shadowOf(application).grantPermissions(ACCESS_COARSE_LOCATION)
    }

    @Test
    fun securityExceptionReturnsNull() {
        val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        shadowOf(locationManager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        var isCalled = false
        var result: ObservingLocation? = ObservingLocation(0.0, 0.0, ObservingLocation.Source.MANUAL)
        LocationProvider(application).fetch { fetched ->
            isCalled = true
            result = fetched
        }
        assertTrue(isCalled)
        assertNull(result)
    }

    private companion object {
        const val ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION"
    }
}

@Implements(LocationManager::class)
class ThrowingLocationManagerShadow : ShadowLocationManager() {
    @Implementation
    override fun requestSingleUpdate(provider: String, listener: LocationListener, looper: Looper?): Unit =
        throw SecurityException("denied in test")
}
