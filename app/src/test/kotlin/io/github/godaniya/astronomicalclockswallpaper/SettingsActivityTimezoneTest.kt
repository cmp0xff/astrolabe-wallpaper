package io.github.godaniya.astronomicalclockswallpaper

import android.Manifest
import android.annotation.SuppressLint
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

/** Captures the phone timezone on a first save and keeps a saved site's zone when refreshing. */
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

    // The first-save case: nothing is stored yet, so there is no zone to preserve and the phone
    // zone standing when the asynchronous fix arrives is what gets saved.
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

    // Acquiring on top of an already-saved site updates the coordinates but must not overwrite the
    // site's zone with the phone's. Every other acquisition test starts from an empty store, where
    // the capturing and preserving paths agree, so only this case can catch the data loss.
    @Test
    fun acquisitionPreservesSavedZone() {
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        val manager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationShadow = shadowOf(manager)
        locationShadow.enableNetworkProvider()
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
            // Both buttons funnel through the same acquisition and differ only in forceFresh; both
            // must preserve the zone, so one case loops over them instead of duplicating the test.
            for (buttonId in listOf(R.id.use_current_location, R.id.refresh_location)) {
                val fix = Location(LocationManager.NETWORK_PROVIDER)
                fix.latitude = 37.42
                fix.longitude = -122.08
                activity.findViewById<Button>(buttonId).performClick()
                locationShadow.simulateLocation(LocationManager.NETWORK_PROVIDER, fix)
                shadowOf(Looper.getMainLooper()).idle()

                assertSavedTimezone(activity, "Australia/Sydney")
                val saved = LocationStore(application).load()
                assertEquals(37.42, saved?.latitude)
                assertEquals(-122.08, saved?.longitude)
                assertEquals(ObservingLocation.Source.CURRENT_COARSE, saved?.source)

                // Refresh-then-save untouched must preserve CURRENT_COARSE and Australia/Sydney
                activity.findViewById<Button>(R.id.save_location).performClick()
                assertSavedTimezone(activity, "Australia/Sydney")
                val savedAfterSave = LocationStore(application).load()
                assertEquals(37.42, savedAfterSave?.latitude)
                assertEquals(-122.08, savedAfterSave?.longitude)
                assertEquals(ObservingLocation.Source.CURRENT_COARSE, savedAfterSave?.source)
            }
        }
    }

    @Test
    fun coldOpenSavePreservesCurrent() {
        LocationStore(application).save(
            ObservingLocation(
                latitude = 50.0875,
                longitude = 14.4206,
                source = ObservingLocation.Source.CURRENT_COARSE,
                zoneId = ZoneId.of("Europe/Prague"),
            ),
        )
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"))
        Robolectric.buildActivity(SettingsActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            activity.findViewById<Button>(R.id.save_location).performClick()

            assertSavedTimezone(activity, "Europe/Prague")
            val saved = LocationStore(application).load()
            assertEquals(ObservingLocation.Source.CURRENT_COARSE, saved?.source)
            assertEquals(ZoneId.of("Europe/Prague"), saved?.zoneId)
            assertTrue(activity.findViewById<TextView>(R.id.location_current).text.contains("(current)"))
        }
    }

    @Test
    @SuppressLint("SetTextI18n")
    fun editedCoordsCaptureSaveZone() {
        LocationStore(application).save(
            ObservingLocation(
                latitude = 50.0875,
                longitude = 14.4206,
                source = ObservingLocation.Source.CURRENT_COARSE,
                zoneId = ZoneId.of("Europe/Prague"),
            ),
        )
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"))
        Robolectric.buildActivity(SettingsActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            activity.findViewById<EditText>(R.id.latitude_input).setText("51.5074")
            activity.findViewById<Button>(R.id.save_location).performClick()

            assertSavedTimezone(activity, "Pacific/Auckland")
            val saved = LocationStore(application).load()
            assertEquals(ObservingLocation.Source.MANUAL, saved?.source)
            assertEquals(ZoneId.of("Pacific/Auckland"), saved?.zoneId)
            assertTrue(activity.findViewById<TextView>(R.id.location_current).text.contains("(manual)"))
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
