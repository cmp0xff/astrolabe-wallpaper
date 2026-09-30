package io.github.cmp0xff.astrolabewallpaper

import android.Manifest
import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import java.time.ZoneId

/** Exercises launcher and preview behavior, permission results, and manual/current location persistence. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 36])
class SettingsActivityTest {
    @Test
    fun previewTargetsWallpaper() {
        Robolectric.buildActivity(SettingsActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            assertTrue(activity.findViewById<Button>(R.id.open_preview).performClick())
            val intent = shadowOf(activity).nextStartedActivity
            assertEquals(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER, intent.action)
            // The typed overload requires API 33; this test also runs on API 26.
            @Suppress("DEPRECATION")
            val component = intent.getParcelableExtra<ComponentName>(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT)
            assertEquals(
                ComponentName(activity, AstrolabeWallpaperService::class.java),
                component,
            )
            controller.pause().stop().destroy()
        }
    }

    @Test
    @SuppressLint("SetTextI18n")
    fun manualCoordsSurviveRecreate() {
        Robolectric.buildActivity(SettingsActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            activity.findViewById<EditText>(R.id.latitude_input).setText("45.5")
            activity.findViewById<EditText>(R.id.longitude_input).setText("-120.25")
            activity.findViewById<Button>(R.id.save_location).performClick()
            val expected = "45.5, -120.25 (manual)\nTimezone: ${ZoneId.systemDefault().id}"
            assertEquals(expected, activity.findViewById<TextView>(R.id.location_current).text.toString())
            controller.recreate()
            assertEquals(
                expected,
                controller
                    .get()
                    .findViewById<TextView>(R.id.location_current)
                    .text
                    .toString(),
            )
        }
    }

    @Test
    @SuppressLint("SetTextI18n")
    fun refreshPreservesOnFailure() {
        Robolectric.buildActivity(SettingsActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            activity.findViewById<EditText>(R.id.latitude_input).setText("10.0")
            activity.findViewById<EditText>(R.id.longitude_input).setText("20.0")
            activity.findViewById<Button>(R.id.save_location).performClick()
            val expected = "10.0, 20.0 (manual)\nTimezone: ${ZoneId.systemDefault().id}"

            shadowOf(activity.application).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
            val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            shadowOf(locationManager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, false)
            activity.findViewById<Button>(R.id.refresh_location).performClick()

            assertEquals(expected, activity.findViewById<TextView>(R.id.location_current).text.toString())
            assertEquals(
                ObservingLocation(
                    latitude = 10.0,
                    longitude = 20.0,
                    source = ObservingLocation.Source.MANUAL,
                    zoneId = ZoneId.systemDefault(),
                ),
                LocationStore(activity).load(),
            )
        }
    }

    @Test
    fun useCurrentLocationSavesCoarse() {
        Robolectric.buildActivity(SettingsActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            shadowOf(activity.application).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
            val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            shadowOf(locationManager).enableNetworkProvider()
            activity.findViewById<Button>(R.id.use_current_location).performClick()
            shadowOf(locationManager).simulateLocation(
                LocationManager.NETWORK_PROVIDER,
                location(latitude = 37.42, longitude = -122.08),
            )
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(
                "37.42, -122.08 (current)\nTimezone: ${ZoneId.systemDefault().id}",
                activity.findViewById<TextView>(R.id.location_current).text.toString(),
            )
            assertEquals(
                ObservingLocation(
                    latitude = 37.42,
                    longitude = -122.08,
                    source = ObservingLocation.Source.CURRENT_COARSE,
                    zoneId = ZoneId.systemDefault(),
                ),
                LocationStore(activity).load(),
            )
        }
    }

    @Test
    fun denyCallbackShowsToast() {
        Robolectric.buildActivity(SettingsActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            activity.onRequestPermissionsResult(
                REQUEST_LOCATION_PERMISSION,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                intArrayOf(PackageManager.PERMISSION_DENIED),
            )
            assertEquals(activity.getString(R.string.location_permission_denied), ShadowToast.getTextOfLatestToast())
            assertNull(LocationStore(activity).load())
        }
    }

    @Test
    @SuppressLint("SetTextI18n")
    fun invalidManualInputRejected() {
        Robolectric.buildActivity(SettingsActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            activity.findViewById<EditText>(R.id.latitude_input).setText("91")
            activity.findViewById<EditText>(R.id.longitude_input).setText("20")
            activity.findViewById<Button>(R.id.save_location).performClick()
            assertEquals(activity.getString(R.string.location_invalid), ShadowToast.getTextOfLatestToast())
            assertNull(LocationStore(activity).load())
        }
    }

    private fun location(latitude: Double, longitude: Double): Location {
        val location = Location(LocationManager.NETWORK_PROVIDER)
        location.setLatitude(latitude)
        location.setLongitude(longitude)
        return location
    }

    private companion object {
        const val REQUEST_LOCATION_PERMISSION = 1
    }
}
