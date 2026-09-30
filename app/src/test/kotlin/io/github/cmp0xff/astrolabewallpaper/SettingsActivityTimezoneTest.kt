package io.github.cmp0xff.astrolabewallpaper

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.ZoneId
import java.util.TimeZone

/** Captures the phone timezone when saving, including after an asynchronous coordinate acquisition. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 36])
class SettingsActivityTimezoneTest {
    private val originalTimezone = TimeZone.getDefault()
    private val application = RuntimeEnvironment.getApplication()

    @Before
    fun setInitialTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Prague"))
    }

    @After
    fun restoreTimezone() {
        TimeZone.setDefault(originalTimezone)
    }

    @Test
    fun acquisitionCapturesSaveZone() {
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        val manager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationShadow = shadowOf(manager)
        locationShadow.enableNetworkProvider()
        Robolectric.buildActivity(SettingsActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            activity.findViewById<Button>(R.id.refresh_location).performClick()
            assertEquals(1, locationShadow.networkListeners().size)

            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"))
            val fix = Location(LocationManager.NETWORK_PROVIDER)
            fix.latitude = 37.42
            fix.longitude = -122.08
            locationShadow.simulateLocation(LocationManager.NETWORK_PROVIDER, fix)
            shadowOf(Looper.getMainLooper()).idle()

            assertSavedTimezone(activity, "Pacific/Auckland")
        }
    }

    @Test
    fun manualEntryCapturesSaveZone() {
        Robolectric.buildActivity(SettingsActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            enterCoordinates(activity, latitude = "45.5", longitude = "-120.25")
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            activity.findViewById<Button>(R.id.save_location).performClick()

            assertSavedTimezone(activity, "America/New_York")
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
            controller.recreate()
            assertSavedTimezone(controller.get(), "America/New_York")
        }
    }

    @Test
    fun explicitSavedZoneIsDisplayed() {
        LocationStore(application).save(
            ObservingLocation(
                latitude = -33.87,
                longitude = 151.21,
                source = ObservingLocation.Source.MANUAL,
                zoneId = ZoneId.of("Australia/Sydney"),
            ),
        )
        Robolectric.buildActivity(SettingsActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            assertSavedTimezone(activity, "Australia/Sydney")
            assertEquals(
                activity.getString(R.string.location_timezone_help),
                activity.findViewById<TextView>(R.id.location_timezone_help).text.toString(),
            )
        }
    }

    private fun enterCoordinates(activity: SettingsActivity, latitude: String, longitude: String) {
        activity.findViewById<EditText>(R.id.latitude_input).setText(latitude)
        activity.findViewById<EditText>(R.id.longitude_input).setText(longitude)
    }

    private fun assertSavedTimezone(activity: SettingsActivity, expectedZone: String) {
        val saved = LocationStore(application).load()
        assertNotNull(saved)
        assertEquals(ZoneId.of(expectedZone), saved?.zoneId)
        assertTrue(activity.findViewById<TextView>(R.id.location_current).text.contains(expectedZone))
    }
}
