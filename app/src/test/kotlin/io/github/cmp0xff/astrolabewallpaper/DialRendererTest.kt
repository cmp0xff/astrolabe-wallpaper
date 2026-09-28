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
import kotlin.math.roundToInt
import kotlin.math.sin

/** Renders the dial to a bitmap and checks the drawn hands land at the expected radial positions. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DialRendererTest {
    private val renderer = DialRenderer()

    @Test
    fun twelveOClockAllHandsAtNorth() {
        val bitmap = render(LocalTime.of(12, 0, 0))
        assertBackground(bitmap)
        assertHandsAt(
            bitmap = bitmap,
            expectedHourAngle = 0f,
            expectedMinuteAngle = 0f,
            expectedSecondAngle = 0f,
        )
        // No hand points at the 3, 6, or 9 o'clock positions (90°, 180°, 270°) at noon.
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 90f, distance = HOUR_SAMPLE_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 180f, distance = HOUR_SAMPLE_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 270f, distance = HOUR_SAMPLE_DISTANCE)
    }

    @Test
    fun threeOClockHourHandAtEast() {
        val bitmap = render(LocalTime.of(3, 0, 0))
        assertBackground(bitmap)
        assertHandsAt(
            bitmap = bitmap,
            expectedHourAngle = 90f,
            expectedMinuteAngle = 0f,
            expectedSecondAngle = 0f,
        )
        // The hour hand points at 3 (East); nothing points at 6 or 9.
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 180f, distance = HOUR_SAMPLE_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 270f, distance = HOUR_SAMPLE_DISTANCE)
        // Overdraw check: hour hand terminates before 35 px (length is 30 px).
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 90f, distance = HOUR_OVERDRAW_DISTANCE)
    }

    @Test
    fun sixOClockHourHandAtSouth() {
        val bitmap = render(LocalTime.of(6, 0, 0))
        assertBackground(bitmap)
        assertHandsAt(
            bitmap = bitmap,
            expectedHourAngle = 180f,
            expectedMinuteAngle = 0f,
            expectedSecondAngle = 0f,
        )
        // The hour hand points at 6 (South); nothing points at 3 or 9.
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 90f, distance = HOUR_SAMPLE_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 270f, distance = HOUR_SAMPLE_DISTANCE)
    }

    @Test
    fun nineOClockHourHandAtWest() {
        val bitmap = render(LocalTime.of(9, 0, 0))
        assertBackground(bitmap)
        assertHandsAt(
            bitmap = bitmap,
            expectedHourAngle = 270f,
            expectedMinuteAngle = 0f,
            expectedSecondAngle = 0f,
        )
        // The hour hand points at 9 (West / negative X); nothing points at 3 or 6.
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 90f, distance = HOUR_SAMPLE_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 180f, distance = HOUR_SAMPLE_DISTANCE)
    }

    @Test
    fun threeFifteenThirtyDispersed() {
        val bitmap = render(LocalTime.of(3, 15, 30))
        assertBackground(bitmap)
        // Hour: 3h + 15m 30s = 97.75°, Minute: 15m 30s = 93°, Second: 30s = 180°.
        assertHandsAt(
            bitmap = bitmap,
            expectedHourAngle = 97.75f,
            expectedMinuteAngle = 93f,
            expectedSecondAngle = 180f,
        )
    }

    private fun render(time: LocalTime): Bitmap {
        val bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
        renderer.renderDial(Canvas(bitmap), clockState(time))
        return bitmap
    }

    private fun assertHandsAt(
        bitmap: Bitmap,
        expectedHourAngle: Float,
        expectedMinuteAngle: Float,
        expectedSecondAngle: Float,
    ) {
        assertDialPixel(bitmap = bitmap, angleDegrees = expectedHourAngle, distance = HOUR_SAMPLE_DISTANCE)
        assertDialPixel(bitmap = bitmap, angleDegrees = expectedMinuteAngle, distance = MINUTE_SAMPLE_DISTANCE)
        assertDialPixel(bitmap = bitmap, angleDegrees = expectedSecondAngle, distance = SECOND_SAMPLE_DISTANCE)
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
        return Pair(first = x.roundToInt(), second = y.roundToInt())
    }

    private companion object {
        const val BITMAP_SIZE = 200
        const val CENTER_DIVISOR = 2f

        // Dial radius for a 200 px bitmap is 60 px (BITMAP_SIZE * RADIUS_FRACTION).
        // Hand lengths: hour = 30 px (0.5 * 60), minute = 45 px (0.75 * 60), second = 51 px (0.85 * 60).
        // Hour ticks span radius [50, 60] px, with a 3 px round cap extending inward to 48.5 px.
        //
        // Sample distances are chosen to:
        // 1) Lie within the target hand's length (15 < 30, 40 < 45, 46.5 < 51).
        // 2) Isolate longer hands from shorter ones when divergent (40 > 30, 46.5 > 45).
        // 3) Sample the second hand clear of the inward round cap of the 12 o'clock tick mark (< 48.5 px).
        const val HOUR_SAMPLE_DISTANCE = 15f
        const val MINUTE_SAMPLE_DISTANCE = 40f
        const val SECOND_SAMPLE_DISTANCE = 46.5f
        const val HOUR_OVERDRAW_DISTANCE = 35f
        const val SEARCH_RADIUS = 1
        const val CORNER = 1
    }
}
