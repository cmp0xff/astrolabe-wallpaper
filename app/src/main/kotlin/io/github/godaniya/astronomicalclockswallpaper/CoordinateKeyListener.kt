package io.github.godaniya.astronomicalclockswallpaper

import android.text.InputType
import android.text.method.NumberKeyListener
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Admits the characters a coordinate can contain, derived from the field's text locale.
 *
 * The layout's `numberDecimal` hint filters through a `DigitsKeyListener` that accepts `.` but drops
 * the locale decimal separator (the comma in German), while [SettingsActivity.parseCoordinate] reads
 * the locale separator and treats `.` as an alias for it. Both sides key off the same locale here, so
 * the filter can no longer drop a separator the parser is prepared to accept.
 */
internal class CoordinateKeyListener(locale: Locale) : NumberKeyListener() {
    private val accepted: CharArray = buildAccepted(locale)

    override fun getAcceptedChars(): CharArray = accepted

    override fun getInputType(): Int = INPUT_TYPE

    private companion object {
        const val INPUT_TYPE =
            InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL or
                InputType.TYPE_NUMBER_FLAG_SIGNED

        fun buildAccepted(locale: Locale): CharArray {
            val symbols = DecimalFormatSymbols.getInstance(locale)
            return buildString {
                for (digit in '0'..'9') {
                    append(digit)
                    append(symbols.zeroDigit + (digit - '0'))
                }
                append('.')
                append(symbols.decimalSeparator)
                append('+')
                append('-')
                if (symbols.minusSign != '-') append(symbols.minusSign)
            }.toCharArray()
        }
    }
}
