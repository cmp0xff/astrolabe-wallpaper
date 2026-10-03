package io.github.cmp0xff.astrolabewallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

/** Native bitmap probes anchor the 24-hour hand, geometric plate, and independent display layers. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DialRendererTest {
    private val renderer = DialRenderer()
    private val prague = AstrolabeGeometry(localSiderealAngleDeg = 0.0, trueObliquityDeg = 23.44, latitudeDeg = 50.08)

    @Test
    fun paletteIsPinnedToLiteralArgb() {
        // The device's screenshot pipeline applies a colour transform, so a capture cannot
        // adjudicate the exact gold (docs/device-testing.md). Every other pixel assertion in this
        // class compares DialStyle to itself, so without this pin a palette edit passes the suite.
        assertEquals(0xFFD8B66A.toInt(), DialStyle.GOLD)
        assertEquals(0xFF101923.toInt(), DialStyle.BACKGROUND)
    }

    @Test
    fun civilHandMatchesCardinalHours() {
        val hours = listOf(12 to 0.0, 18 to 90.0, 0 to 180.0, 6 to 270.0)
        for ((hour, angle) in hours) {
            val bitmap = render(time = LocalTime.of(hour, 0))
            for ((_, probeAngle) in hours) {
                val color = if (probeAngle == angle) DialStyle.HAND else DialStyle.NIGHT
                assertEquals(color, pixelAt(bitmap, radialPoint(angleDeg = probeAngle, radius = 0.35)))
            }
            assertTrue(pixelAt(bitmap, radialPoint(angleDeg = angle, radius = 1.13)) != DialStyle.HAND)
        }
    }

    @Test
    fun civilHandIncludesMinutes() {
        val bitmap = render(time = LocalTime.of(15, 15, 36))
        // 3h 15m 36s since noon is 48.9 degrees on the 24-hour scale.
        assertEquals(DialStyle.HAND, pixelAt(bitmap, radialPoint(angleDeg = 48.9, radius = 0.35)))
        assertEquals(DialStyle.NIGHT, pixelAt(bitmap, radialPoint(angleDeg = 45.0, radius = 0.35)))
    }

    @Test
    fun romanNumeralsKeepGlyphSpacing() {
        val bitmap = render()
        // XII and XXIV must occupy several glyph advances, even though the plate uses unit radii.
        assertTrue(textSpan(bitmap, -1.205) > 24)
        assertTrue(textSpan(bitmap, 1.205) > 40)
    }

    @Test
    fun missingLocationOmitsGeometry() {
        val bitmap = render()
        assertEquals(DialStyle.NIGHT, pixelAt(bitmap, DialPoint(x = 0.1, y = -0.8)))
        assertEquals(DialStyle.NIGHT, pixelAt(bitmap, DialPoint(x = -0.7, y = 0.2)))
        assertEquals(DialStyle.NIGHT, pixelAt(bitmap, DialPoint(x = 0.1, y = 0.8)))
        assertFalse(containsColor(bitmap, DialStyle.SKY))
        assertFalse(containsColor(bitmap, DialStyle.TWILIGHT))
    }

    @Test
    fun pragueHasDayTwilightAndNight() {
        val bitmap = render(geometry = prague, layers = DialLayers(isZodiacRingEnabled = false))
        // These interior points have geometric altitudes about +52, -6, and -27 degrees.
        assertEquals(DialStyle.SKY, pixelAt(bitmap, DialPoint(x = 0.1, y = -0.8)))
        assertEquals(DialStyle.TWILIGHT, pixelAt(bitmap, DialPoint(x = -0.7, y = 0.2)))
        assertEquals(DialStyle.NIGHT, pixelAt(bitmap, DialPoint(x = 0.1, y = 0.8)))
    }

    @Test
    fun dayNightToggleKeepsPlainPlate() {
        val bitmap =
            render(
                geometry = prague,
                layers = DialLayers(isZodiacRingEnabled = false, isDayAndNightEnabled = false),
            )
        assertEquals(DialStyle.NIGHT, pixelAt(bitmap, DialPoint(x = 0.1, y = -0.8)))
        assertEquals(DialStyle.NIGHT, pixelAt(bitmap, DialPoint(x = -0.7, y = 0.2)))
        assertFalse(containsColor(bitmap, DialStyle.SKY))
        assertFalse(containsColor(bitmap, DialStyle.TWILIGHT))
    }

    @Test
    fun zodiacRemainsBelowHorizon() {
        val projection = OrlojProjection(prague)
        val point = projection.eclipticPoint(210.0)
        assertTrue(projection.altitudeDeg(point) < -18.0)
        val enabled = render(geometry = prague)
        val disabled = render(geometry = prague, layers = DialLayers(isZodiacRingEnabled = false))
        assertTrue(
            "The below-horizon sign must be drawn",
            changedPixelsNear(first = enabled, second = disabled, point = point) > 20,
        )
    }

    @Test
    fun zodiacRotatesWithSiderealTime() {
        val later = prague.copy(localSiderealAngleDeg = 90.0)
        val original = render(geometry = prague)
        val rotated = render(geometry = later)
        val sign = OrlojProjection(prague).eclipticPoint(210.0)
        assertTrue(changedPixelsNear(first = original, second = rotated, point = sign) > 20)
        // The background plate depends on latitude and obliquity, not sidereal rotation.
        assertTrue(
            render(geometry = prague, layers = DialLayers(isZodiacRingEnabled = false))
                .sameAs(render(geometry = later, layers = DialLayers(isZodiacRingEnabled = false))),
        )
    }

    @Test
    fun landscapeUsesShortestSide() {
        val landscape = Bitmap.createBitmap(1200, SIZE, Bitmap.Config.ARGB_8888)
        renderer.renderDial(
            canvas = Canvas(landscape),
            state = clockState(LocalTime.NOON),
            geometry = prague,
            layers = DialLayers(isZodiacRingEnabled = false),
        )
        val centered = Bitmap.createBitmap(landscape, 200, 0, SIZE, SIZE)
        assertEquals(DialStyle.SKY, pixelAt(centered, DialPoint(x = 0.1, y = -0.8)))
        assertEquals(DialStyle.TWILIGHT, pixelAt(centered, DialPoint(x = -0.7, y = 0.2)))
        assertEquals(DialStyle.NIGHT, pixelAt(centered, DialPoint(x = 0.1, y = 0.8)))
        assertEquals(DialStyle.RIM, pixelAt(centered, DialPoint(x = 1.32, y = 0.0)))
        assertEquals(DialStyle.BACKGROUND, pixelAt(centered, DialPoint(x = 1.4, y = 0.0)))
        assertEquals(DialStyle.BACKGROUND, landscape.getPixel(1, SIZE / 2))
        assertEquals(DialStyle.BACKGROUND, landscape.getPixel(1198, SIZE / 2))
    }

    @Test
    fun degenerateCanvasIsLogged() {
        ShadowLog.clear()
        renderer.renderDial(Canvas(), clockState(LocalTime.NOON))
        val tiny = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        renderer.renderDial(Canvas(tiny), clockState(LocalTime.NOON))
        assertEquals(DialStyle.BACKGROUND, tiny.getPixel(10, 10))
        assertEquals(2, ShadowLog.getLogsForTag("DialRenderer").count { it.type == Log.WARN })
    }

    @Test
    fun renderFailuresAreContained() {
        ShadowLog.clear()
        containRenderFailure { throw IllegalArgumentException("invalid argument") }
        containRenderFailure { throw IllegalStateException("invalid state") }
        assertEquals(2, ShadowLog.getLogsForTag("DialRenderer").count { it.type == Log.ERROR })
        var hasDrawn = false
        containRenderFailure { hasDrawn = true }
        assertTrue(hasDrawn)
    }

    @Test
    fun unrelatedFailuresPropagate() {
        assertThrows(UnsupportedOperationException::class.java) {
            containRenderFailure { throw UnsupportedOperationException("not contained") }
        }
    }

    @Test
    fun rendererReuseIsIndependent() {
        // A live engine keeps one DialRenderer for its lifetime while the saved site changes, and
        // OrlojPlateRenderer keeps one mutable Path, so a frame must not depend on the previous draw.
        val sydney = prague.copy(latitudeDeg = -33.87)
        val reused = DialRenderer()
        val firstPrague = drawInto(reused, prague)
        val sydneyPass = drawInto(reused, sydney)
        assertTrue(firstPrague.sameAs(drawInto(DialRenderer(), prague)))
        assertTrue(sydneyPass.sameAs(drawInto(DialRenderer(), sydney)))
        assertTrue(firstPrague.sameAs(drawInto(reused, prague)))
    }

    private fun drawInto(target: DialRenderer, geometry: AstrolabeGeometry): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        target.renderDial(canvas = Canvas(bitmap), state = clockState(LocalTime.NOON), geometry = geometry)
        return bitmap
    }

    private fun render(
        time: LocalTime = LocalTime.NOON,
        geometry: AstrolabeGeometry? = null,
        layers: DialLayers = DialLayers(),
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        renderer.renderDial(canvas = Canvas(bitmap), state = clockState(time), geometry = geometry, layers = layers)
        assertEquals(DialStyle.BACKGROUND, bitmap.getPixel(1, 1))
        return bitmap
    }

    private fun pixelAt(bitmap: Bitmap, point: DialPoint): Int =
        bitmap.getPixel((CENTER + point.x * SKY_RADIUS).roundToInt(), (CENTER + point.y * SKY_RADIUS).roundToInt())

    private fun radialPoint(angleDeg: Double, radius: Double): DialPoint {
        val radians = Math.toRadians(angleDeg)
        return DialPoint(x = sin(radians) * radius, y = -cos(radians) * radius)
    }

    private fun containsColor(bitmap: Bitmap, color: Int): Boolean {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return color in pixels
    }

    private fun textSpan(bitmap: Bitmap, normalizedY: Double): Int {
        val baseline = (CENTER + normalizedY * SKY_RADIUS).roundToInt()
        val columns =
            (360..440).filter { x ->
                (baseline - 20..baseline + 20).any { y -> bitmap.getPixel(x, y) == DialStyle.GOLD }
            }
        return if (columns.isEmpty()) 0 else columns.last() - columns.first() + 1
    }

    private fun changedPixelsNear(first: Bitmap, second: Bitmap, point: DialPoint): Int {
        val x = (CENTER + point.x * SKY_RADIUS).roundToInt()
        val y = (CENTER + point.y * SKY_RADIUS).roundToInt()
        var changed = 0
        for (dx in -PROBE_RADIUS..PROBE_RADIUS) {
            for (dy in -PROBE_RADIUS..PROBE_RADIUS) {
                if (first.getPixel(x + dx, y + dy) != second.getPixel(x + dx, y + dy)) {
                    changed++
                }
            }
        }
        return changed
    }

    private companion object {
        const val SIZE = 800
        const val CENTER = 400.0
        const val SKY_RADIUS = SIZE * 0.43 / 1.37
        const val PROBE_RADIUS = 12
    }
}
