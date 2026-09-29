package dev.suspension.app.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong

/** The language the UI is currently shown in (follows the in-app language choice). */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

private fun Locale.decimalSeparator(): Char = DecimalFormatSymbols.getInstance(this).decimalSeparator

/**
 * Spec §7: integer steps render without decimals (`105`); fractional steps render with the
 * language's decimal separator and no trailing zero (German `1,75` / `19,5`, English `1.75` / `19.5`).
 */
fun formatStepValue(value: Double, step: Double, locale: Locale): String {
    if (step % 1.0 == 0.0) {
        return value.roundToLong().toString()
    }
    var text = String.format(Locale.ROOT, "%.2f", value)
    text = text.trimEnd('0').trimEnd('.')
    if (text.isEmpty() || text == "-0") text = "0"
    return text.replace('.', locale.decimalSeparator())
}

/** Spec §6: derived sag percentage, one decimal, e.g. German `17,8`, English `17.8`. */
fun formatPercent(value: Double, divisor: Double, locale: Locale): String {
    val percent = value / divisor * 100.0
    return String.format(Locale.ROOT, "%.1f", percent).replace('.', locale.decimalSeparator())
}
