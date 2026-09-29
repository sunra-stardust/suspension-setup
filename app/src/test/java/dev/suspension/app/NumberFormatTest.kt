package dev.suspension.app

import dev.suspension.app.ui.format.formatPercent
import dev.suspension.app.ui.format.formatStepValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

/** Decimal separator follows the UI language: German comma, English point. */
class NumberFormatTest {

    @Test
    fun `German uses a decimal comma without trailing zeros`() {
        assertEquals("1,75", formatStepValue(1.75, 0.05, Locale.GERMAN))
        assertEquals("1,8", formatStepValue(1.8, 0.05, Locale.GERMAN))
        assertEquals("19,5", formatStepValue(19.5, 0.5, Locale.GERMAN))
        assertEquals("17,8", formatPercent(32.0, 180.0, Locale.GERMAN))
    }

    @Test
    fun `English uses a decimal point`() {
        assertEquals("1.75", formatStepValue(1.75, 0.05, Locale.ENGLISH))
        assertEquals("1.8", formatStepValue(1.8, 0.05, Locale.ENGLISH))
        assertEquals("17.8", formatPercent(32.0, 180.0, Locale.ENGLISH))
    }

    @Test
    fun `integer steps have no decimals in any language`() {
        assertEquals("105", formatStepValue(105.0, 1.0, Locale.GERMAN))
        assertEquals("105", formatStepValue(105.0, 1.0, Locale.ENGLISH))
        assertEquals("2", formatStepValue(2.0, 0.5, Locale.ENGLISH))
    }
}
