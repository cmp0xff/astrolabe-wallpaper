package io.github.cmp0xff.astronomicalclockswallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalTime
import kotlin.math.roundToInt

/** Latitude-extreme bitmap assertions. */
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
        // Both plates are projected from the pole below the horizon, so night is innermost at each.
        assertColor(bitmap = north, x = 0.2, y = 0.1, color = DialStyle.NIGHT)
        assertColor(bitmap = north, x = 0.85, y = 0.15, color = DialStyle.SKY)
        assertColor(bitmap = south, x = 0.2, y = 0.1, color = DialStyle.NIGHT)
        assertColor(bitmap = south, x = 0.85, y = 0.15, color = DialStyle.SKY)
    }

    @Test
    fun southernPlateHasNightAtCenter() {
        val bitmap = renderPlate(-33.87)
        assertColor(bitmap = bitmap, x = 0.2, y = 0.1, color = DialStyle.NIGHT)
        assertColor(bitmap = bitmap, x = 0.75, y = 0.0, color = DialStyle.SKY)
    }

    @Test
    fun southernAndPolarBandsNest() {
        val expected = listOf(DialStyle.NIGHT, DialStyle.TWILIGHT, DialStyle.SKY)
        assertEquals(expected, bandSequence(-33.87))
        assertEquals(expected, bandSequence(90.0))
        assertEquals(expected, bandSequence(-90.0))
    }

    private fun renderPlate(latitude: Double): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        DialRenderer().renderDial(
            canvas = Canvas(bitmap),
            state = clockState(LocalTime.NOON),
            geometry = DialGeometry(localSiderealAngleDeg = 0.0, trueObliquityDeg = 23.44, latitudeDeg = latitude),
            layers = DialLayers(isZodiacRingEnabled = false),
        )
        return bitmap
    }

    private fun bandSequence(latitude: Double): List<Int> {
        val bitmap = renderPlate(latitude)
        val fills = listOf(DialStyle.NIGHT, DialStyle.TWILIGHT, DialStyle.SKY)
        // Distinct fill colours crossed walking east from the dial centre; grid strokes are skipped.
        val bands = mutableListOf<Int>()
        var radius = 0.06
        while (radius < 1.0) {
            val color = bitmap.getPixel((CENTER + radius * SKY_RADIUS).roundToInt(), CENTER.roundToInt())
            if (color in fills && bands.lastOrNull() != color) bands.add(color)
            radius += 0.005
        }
        return bands
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
    }
}
