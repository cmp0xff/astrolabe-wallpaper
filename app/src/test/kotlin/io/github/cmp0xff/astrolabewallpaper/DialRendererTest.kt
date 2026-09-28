package io.github.cmp0xff.astrolabewallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.sin

/** Renders the dial to a bitmap and checks the drawn hands land on the expected numerals. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DialRendererTest {
    @Test
    fun twelveOClockAllHandsAtNorth() {
        val time = LocalTime.of(12, 0, 0)
        val bitmap = render(time)
        assertBackground(bitmap)
        assertHandsAt(bitmap, time)
        // No hand points at the 3, 6, or 9 numerals at noon.
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 90f, distance = HOUR_SAMPLE_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 180f, distance = HOUR_SAMPLE_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 270f, distance = HOUR_SAMPLE_DISTANCE)
    }

    @Test
    fun threeOClockHourHandAtEast() {
        val time = LocalTime.of(3, 0, 0)
        val bitmap = render(time)
        assertBackground(bitmap)
        assertHandsAt(bitmap, time)
        // The hour hand is at 3; nothing points at 6 or 9.
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 180f, distance = HOUR_SAMPLE_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 270f, distance = HOUR_SAMPLE_DISTANCE)
    }

    @Test
    fun sixOClockHourHandAtSouth() {
        val time = LocalTime.of(6, 0, 0)
        val bitmap = render(time)
        assertBackground(bitmap)
        assertHandsAt(bitmap, time)
        // The hour hand is at 6; nothing points at 3 or 9.
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 90f, distance = HOUR_SAMPLE_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 270f, distance = HOUR_SAMPLE_DISTANCE)
    }

    private fun render(time: LocalTime): Bitmap {
        val bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
        renderDial(Canvas(bitmap), clockState(time))
        return bitmap
    }

    private fun assertHandsAt(bitmap: Bitmap, time: LocalTime) {
        val state = clockState(time)
        assertDialPixel(bitmap = bitmap, angleDegrees = state.hourAngle, distance = HOUR_SAMPLE_DISTANCE)
        assertDialPixel(bitmap = bitmap, angleDegrees = state.minuteAngle, distance = MINUTE_SAMPLE_DISTANCE)
        assertDialPixel(bitmap = bitmap, angleDegrees = state.secondAngle, distance = SECOND_SAMPLE_DISTANCE)
    }

    private fun assertDialPixel(bitmap: Bitmap, angleDegrees: Float, distance: Float) {
        val (x, y) = samplePoint(angleDegrees, distance)
        val isDialPixel = containsDialPixel(bitmap, x, y)
        assertTrue("no dial pixel near ($x, $y) for hand at $angleDegrees degrees", isDialPixel)
    }

    private fun containsDialPixel(bitmap: Bitmap, x: Int, y: Int): Boolean {
        for (dx in -SEARCH_RADIUS..SEARCH_RADIUS) {
            for (dy in -SEARCH_RADIUS..SEARCH_RADIUS) {
                // A dial stroke is the only non-background colour; anti-aliasing blends its edges
                // toward BACKGROUND_COLOR, so a thin (1 px) hand may have no exact DIAL_COLOR pixel.
                if (bitmap.getPixel(x + dx, y + dy) != BACKGROUND_COLOR) {
                    return true
                }
            }
        }
        return false
    }

    private fun assertBackgroundPixel(bitmap: Bitmap, angleDegrees: Float, distance: Float) {
        val (x, y) = samplePoint(angleDegrees, distance)
        assertEquals("expected background at ($x, $y) for angle $angleDegrees", BACKGROUND_COLOR, bitmap.getPixel(x, y))
    }

    private fun assertBackground(bitmap: Bitmap) {
        assertEquals(BACKGROUND_COLOR, bitmap.getPixel(CORNER, CORNER))
    }

    // Mirrors drawRadiusLine: a hand at angleDegrees passes through this pixel at the given distance.
    private fun samplePoint(angleDegrees: Float, distance: Float): Pair<Int, Int> {
        val center = BITMAP_SIZE / CENTER_DIVISOR
        val radians = Math.toRadians(angleDegrees.toDouble())
        val x = center + distance * sin(radians).toFloat()
        val y = center - distance * cos(radians).toFloat()
        return Pair(first = Math.round(x), second = Math.round(y))
    }

    private companion object {
        const val BITMAP_SIZE = 200

        // Sample each hand inside its length but clear of the tick annulus (tick marks start at
        // radius - HOUR_TICK_LENGTH = 50 px for the 200 px bitmap), so only the target hand is hit.
        const val HOUR_SAMPLE_DISTANCE = 15f
        const val MINUTE_SAMPLE_DISTANCE = 40f
        const val SECOND_SAMPLE_DISTANCE = 48f
        const val SEARCH_RADIUS = 1
        const val CORNER = 1
    }
}
