package io.github.cmp0xff.astrolabewallpaper

import android.content.Context
import android.widget.CheckBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/** Verifies default layer choices, persistence, and the Settings controls that change them. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 36])
class DialSettingsStoreTest {
    private val application = RuntimeEnvironment.getApplication()

    @Test
    fun layersDefaultToEnabled() {
        assertEquals(
            DialLayers(isZodiacRingEnabled = true, isDayAndNightEnabled = true),
            DialSettingsStore(application)
                .load(),
        )
    }

    @Test
    fun choicesSurviveNewStore() {
        for (isZodiacEnabled in listOf(false, true)) {
            for (isDayNightEnabled in listOf(false, true)) {
                val expected =
                    DialLayers(
                        isZodiacRingEnabled = isZodiacEnabled,
                        isDayAndNightEnabled = isDayNightEnabled,
                    )
                DialSettingsStore(application).save(expected)
                assertEquals(expected, DialSettingsStore(application).load())
            }
        }
    }

    @Test
    fun badSettingKeepsOtherChoice() {
        val preferences = application.getSharedPreferences("dial_settings", Context.MODE_PRIVATE)
        preferences
            .edit()
            .putString("zodiac_ring", "broken")
            .putBoolean("day_and_night", false)
            .apply()
        assertEquals(
            DialLayers(isZodiacRingEnabled = true, isDayAndNightEnabled = false),
            DialSettingsStore(application)
                .load(),
        )
        assertEquals("broken", preferences.getString("zodiac_ring", null))
        assertEquals(1, ShadowLog.getLogsForTag("DialSettingsStore").size)
    }

    @Test
    fun settingsTogglesPersist() {
        Robolectric.buildActivity(SettingsActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            val zodiac = activity.findViewById<CheckBox>(R.id.zodiac_ring)
            val dayNight = activity.findViewById<CheckBox>(R.id.day_and_night)
            assertTrue(zodiac.isChecked)
            assertTrue(dayNight.isChecked)
            zodiac.performClick()
            assertEquals(
                DialLayers(isZodiacRingEnabled = false, isDayAndNightEnabled = true),
                DialSettingsStore(activity)
                    .load(),
            )
            dayNight.performClick()
            controller.recreate()
            val recreated = controller.get()
            assertFalse(recreated.findViewById<CheckBox>(R.id.zodiac_ring).isChecked)
            assertFalse(recreated.findViewById<CheckBox>(R.id.day_and_night).isChecked)
            recreated.findViewById<CheckBox>(R.id.zodiac_ring).performClick()
            assertEquals(
                DialLayers(isZodiacRingEnabled = true, isDayAndNightEnabled = false),
                DialSettingsStore(activity)
                    .load(),
            )
        }
    }
}
