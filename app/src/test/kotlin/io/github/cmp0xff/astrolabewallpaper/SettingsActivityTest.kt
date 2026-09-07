package io.github.cmp0xff.astrolabewallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.widget.Button
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Exercises the launcher lifecycle and the actual preview button. */
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
}
