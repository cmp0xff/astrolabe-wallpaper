package io.github.cmp0xff.astrolabewallpaper

import android.widget.Button
import android.widget.EditText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import java.util.Locale

/** Parses the keyboard's decimal format without accepting grouping or partially valid input. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 36], qualifiers = "de-rDE")
class SettingsActivityLocaleTest {
    @Test
    fun germanCoordinatesArePersisted() {
        Robolectric.buildActivity(SettingsActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            enterCoordinates(activity, latitude = "45,5", longitude = "-120,25")
            activity.findViewById<Button>(R.id.save_location).performClick()

            assertEquals(
                ObservingLocation(latitude = 45.5, longitude = -120.25, source = ObservingLocation.Source.MANUAL),
                LocationStore(activity).load(),
            )
        }
    }

    @Test
    @Config(qualifiers = "ar-rEG")
    fun arabicDigitsArePersisted() {
        Robolectric.buildActivity(SettingsActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            enterCoordinates(
                activity = activity,
                latitude = "٤٥٫٥",
                longitude = "١٢٠٫٢٥",
                expectedLocale = Locale.forLanguageTag("ar-EG"),
            )
            activity.findViewById<Button>(R.id.save_location).performClick()

            assertEquals(
                ObservingLocation(latitude = 45.5, longitude = 120.25, source = ObservingLocation.Source.MANUAL),
                LocationStore(activity).load(),
            )
        }
    }

    @Test
    fun positiveSignIsAccepted() {
        Robolectric.buildActivity(SettingsActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            enterCoordinates(activity, latitude = "+45,5", longitude = "-120,25")
            activity.findViewById<Button>(R.id.save_location).performClick()

            assertEquals(
                ObservingLocation(latitude = 45.5, longitude = -120.25, source = ObservingLocation.Source.MANUAL),
                LocationStore(activity).load(),
            )
        }
    }

    @Test
    fun repeatedDecimalIsRejected() {
        assertRejectedLatitude("45,5,1")
    }

    @Test
    fun trailingTextIsRejected() {
        assertRejectedLatitude("45,5north")
    }

    @Test
    fun groupingSeparatorIsRejected() {
        // Grouping-enabled parsing would produce 85, which passes latitude validation.
        assertRejectedLatitude("8.5")
    }

    @Test
    fun invalidLongitudeIsRejected() {
        Robolectric.buildActivity(SettingsActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            enterCoordinates(activity, latitude = "45,5", longitude = "181,0")
            activity.findViewById<Button>(R.id.save_location).performClick()

            assertNull(LocationStore(activity).load())
            assertEquals(activity.getString(R.string.location_invalid), ShadowToast.getTextOfLatestToast())
        }
    }

    private fun assertRejectedLatitude(latitude: String) {
        Robolectric.buildActivity(SettingsActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            // Inject malformed restored/input text without the keyboard removing the invalid characters first.
            activity.findViewById<EditText>(R.id.latitude_input).keyListener = null
            enterCoordinates(activity, latitude = latitude, longitude = "-120,25")
            activity.findViewById<Button>(R.id.save_location).performClick()

            assertNull(LocationStore(activity).load())
            assertEquals(activity.getString(R.string.location_invalid), ShadowToast.getTextOfLatestToast())
        }
    }

    private fun enterCoordinates(
        activity: SettingsActivity,
        latitude: String,
        longitude: String,
        expectedLocale: Locale = Locale.GERMANY,
    ) {
        val latitudeInput = activity.findViewById<EditText>(R.id.latitude_input)
        val longitudeInput = activity.findViewById<EditText>(R.id.longitude_input)
        assertEquals(expectedLocale, latitudeInput.textLocale)
        assertEquals(expectedLocale, longitudeInput.textLocale)
        latitudeInput.setText(latitude)
        longitudeInput.setText(longitude)
        assertEquals(latitude, latitudeInput.text.toString())
        assertEquals(longitude, longitudeInput.text.toString())
    }
}
