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

/**
 * Renders the dial to a bitmap and checks that the hands land where they should.
 *
 * Cardinal times anchor the screen orientation; dispersed times isolate each hand. At each
 * dispersed time the three hands are more than 100 degrees apart. Presence probes sit inside
 * the target hand, and overdraw probes sit past its tip. The second-hand probes use minute-tick
 * rays, safely separated from the longer hour ticks. Pixel rounding and antialiasing tolerances
 * are documented alongside the probe distances below.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DialRendererTest {
    private val renderer = DialRenderer()

    // Also used by palette, tick-spacing, and renderer-reuse checks.
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
    fun cardinalHourHandsAreIsolated() {
        for ((hour, angle) in listOf(3 to 90f, 6 to 180f, 9 to 270f)) {
            val bitmap = render(LocalTime.of(hour, 0, 0))
            assertBackgroundOutsideDial(bitmap)
            assertDialPixel(bitmap = bitmap, angleDegrees = angle, distance = HOUR_SAMPLE_DISTANCE)
            // Minute and second hands point up; the other two cardinal directions are empty.
            for (emptyAngle in listOf(90f, 180f, 270f).filter { it != angle }) {
                assertBackgroundPixel(bitmap = bitmap, angleDegrees = emptyAngle, distance = HOUR_SAMPLE_DISTANCE)
            }
            // The hour hand is the shortest, so it must not reach the overdraw probe.
            assertBackgroundPixel(bitmap = bitmap, angleDegrees = angle, distance = HOUR_OVERDRAW_DISTANCE)
        }
    }

    @Test
    fun dispersedHandsAreIsolated() {
        val cases =
            listOf(
                dispersedTime to ClockState(hourAngle = 10.3583f, minuteAngle = 124.3f, secondAngle = 258f),
                LocalTime.of(4, 42, 3) to ClockState(hourAngle = 141.025f, minuteAngle = 252.3f, secondAngle = 18f),
                LocalTime.of(8, 3, 23) to ClockState(hourAngle = 241.6917f, minuteAngle = 20.3f, secondAngle = 138f),
            )
        for ((time, angles) in cases) {
            val (hourAngle, minuteAngle, secondAngle) = angles
            val bitmap = render(time)
            // Each hand has its own presence and overdraw probes; it cannot borrow pixels from
            // another hand as it can when all three overlap at noon.
            assertDialPixel(bitmap = bitmap, angleDegrees = hourAngle, distance = HOUR_ONLY_DISTANCE)
            assertBackgroundPixel(bitmap = bitmap, angleDegrees = hourAngle, distance = HOUR_OVERDRAW_DISTANCE)
            assertDialPixel(bitmap = bitmap, angleDegrees = minuteAngle, distance = MINUTE_ONLY_DISTANCE)
            assertBackgroundPixel(bitmap = bitmap, angleDegrees = minuteAngle, distance = MINUTE_OVERDRAW_DISTANCE)
            assertDialPixel(bitmap = bitmap, angleDegrees = secondAngle, distance = SECOND_ONLY_DISTANCE)
            assertBackgroundPixel(bitmap = bitmap, angleDegrees = secondAngle, distance = SECOND_OVERDRAW_DISTANCE)
        }
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
                angleDegrees = 10.3583f,
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
        // An empty canvas is skipped.
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
            bitmap.getPixel(x, y),
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
                bitmap.getPixel(x, y),
            )
        }
    }

    private fun containsDialPixel(bitmap: Bitmap, x: Int, y: Int): Boolean {
        for (dx in -SEARCH_RADIUS..SEARCH_RADIUS) {
            for (dy in -SEARCH_RADIUS..SEARCH_RADIUS) {
                // A dial stroke is the only non-background colour; anti-aliasing blends its edges
                // toward BACKGROUND_COLOR, so a thin (1 px) hand may have no exact DIAL_COLOR pixel.
                if (bitmap.getPixel(x + dx, y + dy) != DialRenderer.BACKGROUND_COLOR) {
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
                if (bitmap.getPixel(x + dx, y + dy) == colour) {
                    return true
                }
            }
        }
        return false
    }

    // Project the literal expected angles into screen coordinates: clockwise from up. The cardinal
    // tests anchor this convention. Out-of-bounds probes fail rather than sampling a clamped edge.
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
        // Presence probes accept any antialiased stroke pixel in a 3x3 window: rounding can move
        // the expected location by 0.5 px per axis, and a 1 px stroke may have no exact dial colour.
        // Hour/minute windows remain inside radius 43, well before any tick. Second-hand windows
        // reach at most radius 52, before the minute tick's 54.5 px inner cap. Their 18/138/258 degree
        // rays are at least 12 degrees from an hour tick, leaving a gap even after the probe window
        // and tick width are included. Other hands are over 100 degrees away at every dispersed time.
        // Overdraw probes use one exact background pixel beyond the cap and its antialiased edge.
        const val HOUR_SAMPLE_DISTANCE = 15f
        const val MINUTE_SAMPLE_DISTANCE = 40f
        const val SECOND_SAMPLE_DISTANCE = 47f
        const val HOUR_OVERDRAW_DISTANCE = 35f

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
        const val EXPECTED_DIAL_COLOR = 0xFFD8B66A.toInt()
    }
}
