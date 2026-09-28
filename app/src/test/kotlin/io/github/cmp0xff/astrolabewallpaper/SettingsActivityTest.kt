package io.github.cmp0xff.astrolabewallpaper

import android.Manifest
import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.location.LocationManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Exercises the launcher lifecycle, the preview button, and manual location persistence. */
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
    fun activityCanBeRecreated() {
        Robolectric.buildActivity(SettingsActivity::class.java).use { controller ->
            controller.setup().recreate()
            assertEquals(
                controller.get().getString(R.string.open_preview),
                controller.get().findViewById<Button>(R.id.open_preview).text,
            )
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
            val expected = "45.5, -120.25 (manual)"
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
            val expected = "10.0, 20.0 (manual)"

            shadowOf(activity.application).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
            val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            shadowOf(locationManager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, false)
            shadowOf(locationManager).setProviderEnabled(LocationManager.GPS_PROVIDER, false)
            activity.findViewById<Button>(R.id.refresh_location).performClick()

            assertEquals(expected, activity.findViewById<TextView>(R.id.location_current).text.toString())
            assertEquals(
                ObservingLocation(latitude = 10.0, longitude = 20.0, source = ObservingLocation.Source.MANUAL),
                LocationStore(activity).load(),
            )
        }
    }
}
