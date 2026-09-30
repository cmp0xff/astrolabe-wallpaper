package io.github.cmp0xff.astrolabewallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalTime
import kotlin.math.roundToInt

/** Latitude-extreme bitmap assertions and reviewable native Canvas image artifacts. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OrlojRenderLatitudeTest {
    @Test
    fun equatorialHorizonIsALine() {
        val bitmap = renderPlate(0.0)
        assertColor(bitmap = bitmap, x = 0.2, y = -0.5, color = DialStyle.SKY)
        assertColor(bitmap = bitmap, x = 0.2, y = 0.5, color = DialStyle.NIGHT)
    }

    @Test
    fun polarPlatesRetainAnnuli() {
        val north = renderPlate(90.0)
        val south = renderPlate(-90.0)
        assertColor(bitmap = north, x = 0.2, y = 0.1, color = DialStyle.NIGHT)
        assertColor(bitmap = north, x = 0.85, y = 0.15, color = DialStyle.SKY)
        assertColor(bitmap = south, x = 0.2, y = 0.1, color = DialStyle.SKY)
        assertColor(bitmap = south, x = 0.94, y = 0.1, color = DialStyle.NIGHT)
    }

    @Test
    fun southernPlateHasDayAtCenter() {
        val bitmap = renderPlate(-33.87)
        assertColor(bitmap = bitmap, x = 0.2, y = 0.1, color = DialStyle.SKY)
        assertColor(bitmap = bitmap, x = 0.1, y = 0.8, color = DialStyle.NIGHT)
    }

    @Test
    fun exportRepresentativeImages() {
        val directory = File("build/reports/orloj")
        assertTrue(directory.isDirectory || directory.mkdirs())
        val sites =
            listOf(
                "prague" to 50.08,
                "sydney" to -33.87,
                "equator" to 0.0,
                "north-pole" to 90.0,
                "south-pole" to -90.0,
            )
        for ((name, latitude) in sites) {
            val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)
            DialRenderer().renderDial(
                canvas = Canvas(bitmap),
                state = clockState(LocalTime.of(15, 15, 36)),
                geometry =
                    AstrolabeGeometry(
                        localSiderealAngleDeg = 37.0,
                        trueObliquityDeg = 23.44,
                        latitudeDeg = latitude,
                    ),
            )
            File(directory, "$name-api${Build.VERSION.SDK_INT}.png").outputStream().use { output ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        }
    }

    private fun renderPlate(latitude: Double): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        DialRenderer().renderDial(
            canvas = Canvas(bitmap),
            state = clockState(LocalTime.NOON),
            geometry = AstrolabeGeometry(localSiderealAngleDeg = 0.0, trueObliquityDeg = 23.44, latitudeDeg = latitude),
            layers = DialLayers(isZodiacRingEnabled = false),
        )
        return bitmap
    }

    private fun assertColor(bitmap: Bitmap, x: Double, y: Double, color: Int) {
        assertEquals(
            color,
            bitmap
                .getPixel((CENTER + x * SKY_RADIUS).roundToInt(), (CENTER + y * SKY_RADIUS).roundToInt()),
        )
    }

    private companion object {
        const val SIZE = 800
        const val CENTER = 400.0
        const val SKY_RADIUS = SIZE * 0.43 / 1.37
        const val IMAGE_WIDTH = 1080
        const val IMAGE_HEIGHT = 1600
    }
}
