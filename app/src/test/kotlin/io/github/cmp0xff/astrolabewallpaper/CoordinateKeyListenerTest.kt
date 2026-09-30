package io.github.cmp0xff.astrolabewallpaper

import android.text.SpannableStringBuilder
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/** Rejects grouping commas rather than interpreting them as a US decimal separator. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 36])
class CoordinateKeyListenerTest {
    @Test
    fun usStripsGroupingComma() {
        val listener = CoordinateKeyListener(Locale.US)

        assertEquals("15", filter(listener, "1,5")?.toString())
    }

    private fun filter(listener: CoordinateKeyListener, source: String): CharSequence? =
        listener.filter(source, 0, source.length, SpannableStringBuilder(), 0, 0)
}
