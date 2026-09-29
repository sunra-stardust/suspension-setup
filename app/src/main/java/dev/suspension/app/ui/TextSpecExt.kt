package dev.suspension.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.suspension.app.data.TextSpec
import dev.suspension.app.ui.format.currentLocale
import dev.suspension.app.ui.format.formatStepValue

@Composable
fun TextSpec.resolve(): String = when (this) {
    is TextSpec.Res -> stringResource(id)
    is TextSpec.Format -> {
        val locale = currentLocale()
        val formatted = args.map { if (it is TextSpec.Decimal) formatStepValue(it.value, it.step, locale) else it }
        stringResource(id, *formatted.toTypedArray())
    }
}
