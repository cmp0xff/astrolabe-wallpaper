package io.github.godaniya.astronomicalclockswallpaper

import android.Manifest
import android.content.Context
import android.location.LocationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Checks immediate failures before a location request can be registered. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 36])
class LocationProviderTest {
    private val application = RuntimeEnvironment.getApplication()
    private val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Before
    fun grantPermission() {
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    @Test
    fun fetchDeniedWithoutPermission() {
        shadowOf(application).denyPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        assertImmediateFailure()
    }

    @Test
    fun fetchNullWhenNetworkDisabled() {
        shadowOf(locationManager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, false)
        assertImmediateFailure()
    }

    @Test
    fun fetchNullWithoutNetwork() {
        shadowOf(locationManager).removeProvider(LocationManager.NETWORK_PROVIDER)
        assertImmediateFailure()
    }

    private fun assertImmediateFailure() {
        val results = mutableListOf<CoordinateFix?>()
        LocationProvider(application).fetch { results.add(it) }
        assertEquals(listOf<CoordinateFix?>(null), results)
        assertTrue(shadowOf(locationManager).networkListeners().isEmpty())
    }
}
