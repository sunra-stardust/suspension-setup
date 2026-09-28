package dev.suspension.app.ui.format

import java.util.Locale
import kotlin.math.roundToLong

/**
 * Spec §7: integer steps render without decimals (`105`); fractional steps render with a
 * German comma and no trailing zero (`1.75` → `1,75`, `19.5` → `19,5`, `1.8` → `1,8`).
 */
fun formatStepValue(value: Double, step: Double): String {
    if (step % 1.0 == 0.0) {
        return value.roundToLong().toString()
    }
    var text = String.format(Locale.ROOT, "%.2f", value)
    text = text.trimEnd('0').trimEnd('.')
    if (text.isEmpty() || text == "-0") text = "0"
    return text.replace('.', ',')
}

/** Spec §6: derived sag percentage, one decimal, German comma, e.g. `17,8`. */
fun formatPercent(value: Double, divisor: Double): String {
    val percent = value / divisor * 100.0
    return String.format(Locale.ROOT, "%.1f", percent).replace('.', ',')
}
