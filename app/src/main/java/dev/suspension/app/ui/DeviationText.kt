package dev.suspension.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.suspension.app.R
import dev.suspension.app.data.Deviation
import dev.suspension.app.ui.format.currentLocale
import dev.suspension.app.ui.format.formatStepValue
import kotlin.math.abs

/** "2 Klicks weiter zu als Fox-Empfehlung", "+6 psi gegenüber Startwert", "Startwert: Offen". */
@Composable
fun Deviation.text(optionLabel: @Composable (String) -> String = { it }): String {
    val reference = referenceMaker?.let { stringResource(R.string.reference_maker, it) } ?: stringResource(R.string.reference_start)
    return when (this) {
        is Deviation.Clicks -> pluralStringResource(
            if (closer) R.plurals.deviation_clicks_closer else R.plurals.deviation_clicks_opener,
            clicks, clicks, reference,
        )
        is Deviation.Amount -> {
            // Quantity rows: explicit sign; "−" is the typographic minus used throughout the UI.
            val sign = if (delta > 0) "+" else "−"
            val number = sign + formatStepValue(abs(delta), step, currentLocale())
            val unit = unitResId?.let { stringResource(it) }.orEmpty()
            stringResource(R.string.deviation_amount, number, unit, reference).replace("  ", " ")
        }
        is Deviation.Option -> {
            val option = optionLabel(referenceOption)
            referenceMaker?.let { stringResource(R.string.deviation_option_maker, option, it) }
                ?: stringResource(R.string.deviation_option_start, option)
        }
    }
}
