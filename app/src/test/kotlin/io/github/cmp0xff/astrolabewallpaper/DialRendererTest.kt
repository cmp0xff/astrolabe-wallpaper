package io.github.cmp0xff.astrolabewallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLog
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Renders the dial to a bitmap and checks that the hands land where they should.
 *
 * Probe radii are chosen so each assertion can only be satisfied by the feature under test: at
 * [dispersedTime] the three hands are more than 100 degrees apart, presence probes sit inside the
 * target hand but beyond the next shorter hand, and overdraw probes sit past the target tip but
 * before the first tick that crosses the probe ray (radius 50 for the 12 hour ticks, 55 for the
 * other 48).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DialRendererTest {
    private val renderer = DialRenderer()

    // 12:20:43 -> hour 1243/120 = 10.358 deg, minute 1243/10 = 124.3 deg, second 43 * 6 = 258 deg.
    private val dispersedTime = LocalTime.of(12, 20, 43)

    @Test
    fun twelveOClockAllHandsPointUp() {
        val bitmap = render(LocalTime.of(12, 0, 0))
        assertBackgroundOutsideDial(bitmap)
        // Noon is the convention anchor: all three hands must coincide at 12 o'clock.
        assertDialPixel(bitmap = bitmap, angleDegrees = 0f, distance = HOUR_SAMPLE_DISTANCE)
        assertDialPixel(bitmap = bitmap, angleDegrees = 0f, distance = MINUTE_SAMPLE_DISTANCE)
        assertDialPixel(bitmap = bitmap, angleDegrees = 0f, distance = SECOND_SAMPLE_DISTANCE)
        // Nothing dial-coloured points at 3, 6 or 9 o'clock.
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 90f, distance = HOUR_SAMPLE_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 180f, distance = HOUR_SAMPLE_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 270f, distance = HOUR_SAMPLE_DISTANCE)
    }

    @Test
    fun threeOClockHourHandAt90() {
        val bitmap = render(LocalTime.of(3, 0, 0))
        assertBackgroundOutsideDial(bitmap)
        assertDialPixel(bitmap = bitmap, angleDegrees = 90f, distance = HOUR_SAMPLE_DISTANCE)
        // Minute and second hands point up; nothing points at 6 or 9 o'clock.
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 180f, distance = HOUR_SAMPLE_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 270f, distance = HOUR_SAMPLE_DISTANCE)
        // The hour hand is the shortest, so it must not reach the overdraw probe.
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 90f, distance = HOUR_OVERDRAW_DISTANCE)
    }

    @Test
    fun sixOClockHourHandAt180() {
        val bitmap = render(LocalTime.of(6, 0, 0))
        assertBackgroundOutsideDial(bitmap)
        assertDialPixel(bitmap = bitmap, angleDegrees = 180f, distance = HOUR_SAMPLE_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 90f, distance = HOUR_SAMPLE_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 270f, distance = HOUR_SAMPLE_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 180f, distance = HOUR_OVERDRAW_DISTANCE)
    }

    @Test
    fun nineOClockHourHandAt270() {
        val bitmap = render(LocalTime.of(9, 0, 0))
        assertBackgroundOutsideDial(bitmap)
        assertDialPixel(bitmap = bitmap, angleDegrees = 270f, distance = HOUR_SAMPLE_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 90f, distance = HOUR_SAMPLE_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 180f, distance = HOUR_SAMPLE_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 270f, distance = HOUR_OVERDRAW_DISTANCE)
    }

    @Test
    fun dispersedHandsAreIsolated() {
        val bitmap = render(dispersedTime)
        // Hour hand: present uniquely at 20 px, absent past its 32.5 px cap-extended tip.
        assertDialPixel(bitmap = bitmap, angleDegrees = DISPERSED_HOUR_ANGLE, distance = HOUR_ONLY_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = DISPERSED_HOUR_ANGLE, distance = HOUR_OVERDRAW_DISTANCE)
        // Minute hand: uniquely present at 40 px (past the hour hand), absent past its 46.5 px tip.
        assertDialPixel(bitmap = bitmap, angleDegrees = DISPERSED_MINUTE_ANGLE, distance = MINUTE_ONLY_DISTANCE)
        assertBackgroundPixel(
            bitmap = bitmap,
            angleDegrees = DISPERSED_MINUTE_ANGLE,
            distance = MINUTE_OVERDRAW_DISTANCE,
        )
        // Second hand: only hand that reaches 49 px, absent before its ray's first tick at 55 px.
        assertDialPixel(bitmap = bitmap, angleDegrees = DISPERSED_SECOND_ANGLE, distance = SECOND_ONLY_DISTANCE)
        assertBackgroundPixel(
            bitmap = bitmap,
            angleDegrees = DISPERSED_SECOND_ANGLE,
            distance = SECOND_OVERDRAW_DISTANCE,
        )
    }

    @Test
    fun minuteTicksAreSixDegreesApart() {
        val bitmap = render(dispersedTime)
        // Minute ticks sit at multiples of 6 degrees; 3 and 9 degrees are gaps. No hand is near
        // these rays at 12:20:43, and radius 58 is inside the minute-tick band [55, 60].
        assertDialPixel(bitmap = bitmap, angleDegrees = 6f, distance = TICK_PROBE_DISTANCE)
        assertDialPixel(bitmap = bitmap, angleDegrees = 12f, distance = TICK_PROBE_DISTANCE)
        assertDialPixel(bitmap = bitmap, angleDegrees = 354f, distance = TICK_PROBE_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 3f, distance = TICK_PROBE_DISTANCE)
        assertBackgroundPixel(bitmap = bitmap, angleDegrees = 9f, distance = TICK_PROBE_DISTANCE)
    }

    @Test
    fun dialAndBackgroundColoursPinned() {
        assertEquals(EXPECTED_BACKGROUND_COLOR, DialRenderer.BACKGROUND_COLOR)
        assertEquals(EXPECTED_DIAL_COLOR, DialRenderer.DIAL_COLOR)
        val bitmap = render(dispersedTime)
        assertTrue(
            "no exact dial colour near the hour hand at 12:20:43",
            containsExactColour(
                bitmap = bitmap,
                angleDegrees = DISPERSED_HOUR_ANGLE,
                distance = HOUR_ONLY_DISTANCE,
                colour = EXPECTED_DIAL_COLOR,
            ),
        )
    }

    @Test
    fun oneRendererIsFrameIndependent() {
        val fresh = DialRenderer()
        val expectedThree = renderWith(renderer = fresh, time = LocalTime.of(3, 0, 0))
        val expectedDispersed = renderWith(renderer = fresh, time = dispersedTime)

        // The shared renderer draws a different frame first; output must not depend on draw history.
        val actualDispersed = render(dispersedTime)
        val actualThree = render(LocalTime.of(3, 0, 0))

        assertTrue("3:00 frame depends on the renderer's previous draw", expectedThree.sameAs(actualThree))
        assertTrue("12:20:43 frame depends on the renderer's previous draw", expectedDispersed.sameAs(actualDispersed))
    }

    @Test
    fun landscapeDialUsesShorterAxis() {
        val bitmap =
            renderWith(
                renderer = renderer,
                time = LocalTime.of(12, 0, 0),
                width = LANDSCAPE_WIDTH,
                height = LANDSCAPE_HEIGHT,
            )
        val centerX = LANDSCAPE_WIDTH / CENTER_DIVISOR
        val centerY = LANDSCAPE_HEIGHT / CENTER_DIVISOR
        val angles = listOf(0f, 90f, 180f, 270f)
        for (angleDegrees in angles) {
            // The dial radius comes from the shorter (height) axis, so hour ticks reach radius 60.
            assertDialPixel(
                bitmap = bitmap,
                angleDegrees = angleDegrees,
                distance = TICK_BAND_DISTANCE,
                centerX = centerX,
                centerY = centerY,
            )
            assertBackgroundPixel(
                bitmap = bitmap,
                angleDegrees = angleDegrees,
                distance = OUTSIDE_DIAL_DISTANCE,
                centerX = centerX,
                centerY = centerY,
            )
        }
    }

    @Test
    fun degenerateAndSmallDials() {
        // A 0x0 canvas is skipped without touching the bitmap.
        renderer.renderDial(Canvas(), clockState(LocalTime.of(12, 0, 0)))

        val belowMinimum = Bitmap.createBitmap(BELOW_MIN_DIAL_SIZE, BELOW_MIN_DIAL_SIZE, Bitmap.Config.ARGB_8888)
        renderer.renderDial(Canvas(belowMinimum), clockState(LocalTime.of(12, 0, 0)))
        assertEquals(
            "a canvas below the minimum dial radius is cleared but not drawn",
            DialRenderer.BACKGROUND_COLOR,
            belowMinimum.getPixel(BELOW_MIN_DIAL_SIZE / 2, BELOW_MIN_DIAL_SIZE / 2),
        )

        val aboveMinimum = Bitmap.createBitmap(ABOVE_MIN_DIAL_SIZE, ABOVE_MIN_DIAL_SIZE, Bitmap.Config.ARGB_8888)
        renderer.renderDial(Canvas(aboveMinimum), clockState(LocalTime.of(12, 0, 0)))
        assertTrue(
            "a canvas above the minimum dial radius is drawn",
            aboveMinimum.getPixel(ABOVE_MIN_DIAL_SIZE / 2, ABOVE_MIN_DIAL_SIZE / 2) != DialRenderer.BACKGROUND_COLOR,
        )
    }

    @Test
    fun renderFailureIsContained() {
        ShadowLog.clear()
        containRenderFailure { throw IllegalArgumentException("invalid argument") }
        containRenderFailure { throw IllegalStateException("invalid state") }
        val failures = ShadowLog.getLogs().filter { it.type == Log.ERROR && it.tag == DIAL_RENDERER_TAG }
        assertEquals("both contained failures must be logged at error level", 2, failures.size)

        var hasDrawn = false
        containRenderFailure { hasDrawn = true }
        assertTrue("a successful draw must still run", hasDrawn)
    }

    @Test
    fun unrelatedFailuresPropagate() {
        // The catch is narrow: exceptions outside the handled types surface rather than being lost.
        assertThrows(UnsupportedOperationException::class.java) {
            containRenderFailure { throw UnsupportedOperationException("not contained") }
        }
    }

    private fun render(time: LocalTime): Bitmap = renderWith(renderer = renderer, time = time)

    private fun renderWith(
        renderer: DialRenderer,
        time: LocalTime,
        width: Int = BITMAP_SIZE,
        height: Int = BITMAP_SIZE,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        renderer.renderDial(Canvas(bitmap), clockState(time))
        return bitmap
    }

    private fun assertDialPixel(
        bitmap: Bitmap,
        angleDegrees: Float,
        distance: Float,
        centerX: Float = CENTER,
        centerY: Float = CENTER,
    ) {
        val (x, y) = samplePoint(angleDegrees = angleDegrees, distance = distance, centerX = centerX, centerY = centerY)
        assertTrue(
            "no dial pixel near ($x, $y) for angle $angleDegrees at distance $distance",
            containsDialPixel(bitmap, x, y),
        )
    }

    private fun assertBackgroundPixel(
        bitmap: Bitmap,
        angleDegrees: Float,
        distance: Float,
        centerX: Float = CENTER,
        centerY: Float = CENTER,
    ) {
        val (x, y) = samplePoint(angleDegrees = angleDegrees, distance = distance, centerX = centerX, centerY = centerY)
        assertEquals(
            "expected background at ($x, $y) for angle $angleDegrees at distance $distance",
            DialRenderer.BACKGROUND_COLOR,
            pixelOrBackground(bitmap = bitmap, x = x, y = y),
        )
    }

    private fun assertBackgroundOutsideDial(bitmap: Bitmap) {
        val points =
            listOf(
                Pair(first = 1, second = 1),
                Pair(first = bitmap.width - 2, second = 1),
                Pair(first = 1, second = bitmap.height - 2),
                Pair(first = bitmap.width - 2, second = bitmap.height - 2),
                Pair(first = bitmap.width / 2, second = 1),
                Pair(first = bitmap.width / 2, second = bitmap.height - 2),
                Pair(first = 1, second = bitmap.height / 2),
                Pair(first = bitmap.width - 2, second = bitmap.height / 2),
            )
        for ((x, y) in points) {
            assertEquals(
                "expected background at ($x, $y)",
                DialRenderer.BACKGROUND_COLOR,
                pixelOrBackground(bitmap = bitmap, x = x, y = y),
            )
        }
    }

    private fun containsDialPixel(bitmap: Bitmap, x: Int, y: Int): Boolean {
        for (dx in -SEARCH_RADIUS..SEARCH_RADIUS) {
            for (dy in -SEARCH_RADIUS..SEARCH_RADIUS) {
                // A dial stroke is the only non-background colour; anti-aliasing blends its edges
                // toward BACKGROUND_COLOR, so a thin (1 px) hand may have no exact DIAL_COLOR pixel.
                if (pixelOrBackground(bitmap = bitmap, x = x + dx, y = y + dy) != DialRenderer.BACKGROUND_COLOR) {
                    return true
                }
            }
        }
        return false
    }

    private fun containsExactColour(bitmap: Bitmap, angleDegrees: Float, distance: Float, colour: Int): Boolean {
        val (x, y) = samplePoint(angleDegrees = angleDegrees, distance = distance)
        for (dx in -PALETTE_WINDOW..PALETTE_WINDOW) {
            for (dy in -PALETTE_WINDOW..PALETTE_WINDOW) {
                if (pixelOrBackground(bitmap = bitmap, x = x + dx, y = y + dy) == colour) {
                    return true
                }
            }
        }
        return false
    }

    // Bitmap.getPixel throws for out-of-bounds coordinates; clamping keeps a future probe change a
    // test failure rather than a crash.
    private fun pixelOrBackground(bitmap: Bitmap, x: Int, y: Int): Int {
        val clampedX = x.coerceIn(minimumValue = 0, maximumValue = bitmap.width - 1)
        val clampedY = y.coerceIn(minimumValue = 0, maximumValue = bitmap.height - 1)
        return bitmap.getPixel(clampedX, clampedY)
    }

    // Mirrors drawRadiusLine: a hand at angleDegrees passes through this pixel at the given distance.
    private fun samplePoint(
        angleDegrees: Float,
        distance: Float,
        centerX: Float = CENTER,
        centerY: Float = CENTER,
    ): Pair<Int, Int> {
        val radians = Math.toRadians(angleDegrees.toDouble())
        val x = centerX + distance * sin(radians).toFloat()
        val y = centerY - distance * cos(radians).toFloat()
        return Pair(first = x.roundToInt(), second = y.roundToInt())
    }

    private companion object {
        const val BITMAP_SIZE = 200
        const val CENTER_DIVISOR = 2f
        const val CENTER = 100f

        // A 200 px bitmap gives a 60 px dial radius. Hand lengths: hour 30 px (cap-extended 32.5),
        // minute 45 px (46.5), second 51 px (51.5). Hour ticks span radii [50, 60] with a 3 px round
        // cap reaching inward to 48.5; minute ticks span [55, 60] with a 1 px cap reaching to 54.5.
        // Hand probes stay below 48.5 px, and the 49/53 px probes sit on rays that only carry a
        // minute tick (innermost point 54.5 px).
        const val HOUR_SAMPLE_DISTANCE = 15f
        const val MINUTE_SAMPLE_DISTANCE = 40f
        const val SECOND_SAMPLE_DISTANCE = 47f
        const val HOUR_OVERDRAW_DISTANCE = 35f

        // 12:20:43 separates the hands by more than 100 degrees so each probe is unambiguous.
        const val DISPERSED_HOUR_ANGLE = 10.358f
        const val DISPERSED_MINUTE_ANGLE = 124.3f
        const val DISPERSED_SECOND_ANGLE = 258f
        const val HOUR_ONLY_DISTANCE = 20f
        const val MINUTE_ONLY_DISTANCE = 40f
        const val MINUTE_OVERDRAW_DISTANCE = 49f
        const val SECOND_ONLY_DISTANCE = 49f
        const val SECOND_OVERDRAW_DISTANCE = 53f

        const val TICK_PROBE_DISTANCE = 58f

        // A 300x200 canvas must take its 60 px radius from the 200 px height, not the 300 px width.
        const val LANDSCAPE_WIDTH = 300
        const val LANDSCAPE_HEIGHT = 200
        const val TICK_BAND_DISTANCE = 55f
        const val OUTSIDE_DIAL_DISTANCE = 65f

        // Radius is 0.3 * size, so 30 px (radius 9) is below the 11.5 px minimum and 40 px (radius
        // 12) is above it.
        const val BELOW_MIN_DIAL_SIZE = 30
        const val ABOVE_MIN_DIAL_SIZE = 40

        const val SEARCH_RADIUS = 1
        const val PALETTE_WINDOW = 2
        const val EXPECTED_BACKGROUND_COLOR = 0xFF111923.toInt()
        const val EXPECTED_DIAL_COLOR = 0xFFD8B26A.toInt()
        const val DIAL_RENDERER_TAG = "DialRenderer"
    }
}
