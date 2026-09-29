package dev.suspension.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.suspension.app.data.TextSpec

@Composable
fun TextSpec.resolve(): String = when (this) {
    is TextSpec.Res -> stringResource(id)
    is TextSpec.Format -> stringResource(id, *args.toTypedArray())
}
