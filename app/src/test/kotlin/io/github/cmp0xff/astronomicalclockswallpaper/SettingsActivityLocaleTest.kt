package io.github.cmp0xff.astronomicalclockswallpaper

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
import java.time.ZoneId
import java.util.Locale

/** Parses the keyboard's decimal format without accepting grouping or partially valid input. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 36], qualifiers = "de-rDE")
class SettingsActivityLocaleTest {
    @Test
    fun germanCoordinatesArePersisted() {
        assertPersisted(latitude = "45,5", longitude = "-120,25", expectedLatitude = 45.5, expectedLongitude = -120.25)
    }

    @Test
    @Config(qualifiers = "ar-rEG")
    fun arabicDigitsArePersisted() {
        assertPersisted(
            latitude = "٤٥٫٥",
            longitude = "١٢٠٫٢٥",
            expectedLatitude = 45.5,
            expectedLongitude = 120.25,
            expectedLocale = Locale.forLanguageTag("ar-EG"),
        )
    }

    @Test
    fun positiveSignIsAccepted() {
        assertPersisted(latitude = "+45,5", longitude = "-120,25", expectedLatitude = 45.5, expectedLongitude = -120.25)
    }

    @Test
    fun repeatedDecimalIsRejected() {
        assertRejected(latitude = "45,5,1", longitude = "-120,25", bypassKeyboard = true)
    }

    @Test
    fun trailingTextIsRejected() {
        assertRejected(latitude = "45,5north", longitude = "-120,25", bypassKeyboard = true)
    }

    @Test
    fun dotCoordinatesArePersisted() {
        // '.' is the canonical coordinate separator and an alias for the German comma.
        assertPersisted(latitude = "8.5", longitude = "-120.25", expectedLatitude = 8.5, expectedLongitude = -120.25)
    }

    @Test
    @Config(qualifiers = "ar-rEG")
    fun arabicDotCoordinatesPersist() {
        assertPersisted(
            latitude = "45.5",
            longitude = "120.5",
            expectedLatitude = 45.5,
            expectedLongitude = 120.5,
            expectedLocale = Locale.forLanguageTag("ar-EG"),
        )
    }

    @Test
    fun invalidLongitudeIsRejected() {
        assertRejected(latitude = "45,5", longitude = "181,0")
    }

    private fun assertPersisted(
        latitude: String,
        longitude: String,
        expectedLatitude: Double,
        expectedLongitude: Double,
        expectedLocale: Locale = Locale.GERMANY,
    ) {
        Robolectric.buildActivity(SettingsActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            enterCoordinates(
                activity = activity,
                latitude = latitude,
                longitude = longitude,
                expectedLocale = expectedLocale,
            )
            activity.findViewById<Button>(R.id.save_location).performClick()
            assertEquals(
                ObservingLocation(
                    latitude = expectedLatitude,
                    longitude = expectedLongitude,
                    source = ObservingLocation.Source.MANUAL,
                    zoneId = ZoneId.systemDefault(),
                ),
                LocationStore(activity).load(),
            )
        }
    }

    private fun assertRejected(latitude: String, longitude: String, bypassKeyboard: Boolean = false) {
        Robolectric.buildActivity(SettingsActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            if (bypassKeyboard) {
                // Inject malformed restored/input text without the keyboard removing invalid characters first.
                activity.findViewById<EditText>(R.id.latitude_input).keyListener = null
            }
            enterCoordinates(activity, latitude, longitude)
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
