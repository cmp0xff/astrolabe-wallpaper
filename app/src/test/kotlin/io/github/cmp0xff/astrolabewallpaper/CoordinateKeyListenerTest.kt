package io.github.cmp0xff.astrolabewallpaper

import android.text.SpannableStringBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/** Pins which characters the coordinate fields admit under each locale's numbering system. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 36])
class CoordinateKeyListenerTest {
    @Test
    fun germanAcceptsCommaAndDot() {
        val listener = CoordinateKeyListener(Locale.GERMANY)

        assertNull(filter(listener, "45,5"))
        assertNull(filter(listener, "45.5"))
    }

    @Test
    fun usStripsGroupingComma() {
        val listener = CoordinateKeyListener(Locale.US)

        assertEquals("15", filter(listener, "1,5")?.toString())
    }

    @Test
    fun arabicAcceptsItsDigitsAndDot() {
        val listener = CoordinateKeyListener(Locale.forLanguageTag("ar-EG"))

        assertNull(filter(listener, "٤٥٫٥"))
        assertNull(filter(listener, "45.5"))
    }

    private fun filter(listener: CoordinateKeyListener, source: String): CharSequence? =
        listener.filter(source, 0, source.length, SpannableStringBuilder(), 0, 0)
}
