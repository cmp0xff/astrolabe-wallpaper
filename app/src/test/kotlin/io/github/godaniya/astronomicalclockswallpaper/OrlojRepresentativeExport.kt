package io.github.godaniya.astronomicalclockswallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Generates representative Orloj dial Canvas PNGs for visual inspection.
 *
 * This harness is decoupled from the automated unit suite and invoked explicitly via
 * `./gradlew exportRepresentativeImages`. It does not make synthetic assertions on image encoding.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OrlojRepresentativeExport {
    @Test
    fun exportRepresentativeImages() {
        val directory = File("build/reports/orloj")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val sites =
            listOf(
                "prague" to 50.08,
                "sydney" to -33.87,
                "equator" to 0.0,
                "north-pole" to 90.0,
                "south-pole" to -90.0,
            )
        val calculator = AstronomyEngineCalculator()
        val exportInstant = Instant.parse("2026-10-04T15:15:36Z")
        for ((name, latitude) in sites) {
            val zoneId = if (name == "sydney") ZoneId.of("Australia/Sydney") else ZoneId.of("Europe/Prague")
            val longitude = if (name == "sydney") 151.21 else 14.42
            val site =
                ObservingLocation(
                    latitude = latitude,
                    longitude = longitude,
                    source = ObservingLocation.Source.MANUAL,
                    zoneId = zoneId,
                )
            val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)
            DialRenderer().renderDial(
                canvas = Canvas(bitmap),
                state = clockState(LocalTime.of(15, 15, 36)),
                geometry = calculator.dialGeometry(exportInstant, site),
            )
            File(directory, "$name-api${Build.VERSION.SDK_INT}.png").outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        }
    }

    private companion object {
        const val IMAGE_WIDTH = 1080
        const val IMAGE_HEIGHT = 1600
    }
}
