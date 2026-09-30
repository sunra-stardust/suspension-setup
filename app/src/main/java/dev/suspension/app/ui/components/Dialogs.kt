package dev.suspension.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.suspension.app.R
import dev.suspension.app.ui.theme.AppTheme

/** Dialog frame in the app's own look (surface card, no Material chrome). */
@Composable
private fun AppDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val colors = AppTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .padding(16.dp),
        ) { content() }
    }
}

@Composable
private fun DialogButtons(confirmLabel: String, confirmEnabled: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        DialogButton(stringResource(R.string.action_cancel), primary = false, enabled = true, modifier = Modifier.weight(1f), onClick = onDismiss)
        DialogButton(confirmLabel, primary = true, enabled = confirmEnabled, modifier = Modifier.weight(1f), onClick = onConfirm)
    }
}

@Composable
private fun DialogButton(label: String, primary: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (primary) colors.hit else colors.surface)
            .clickable(enabled = enabled, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(text = label, style = AppTheme.type.rowLabel, color = if (enabled) colors.ink else colors.dim)
    }
}

/** Asks for a name (new/renamed bike or Vorlage). Blank names can't be saved. */
@Composable
fun NameDialog(title: String, hint: String?, initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    var name by rememberSaveable { mutableStateOf(initial) }
    val label = stringResource(R.string.name_label)
    AppDialog(onDismiss) {
        Text(text = title, style = type.groupHeading, color = colors.ink)
        if (hint != null) Text(text = hint, style = type.rowHint, color = colors.dim)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.hit)
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            if (name.isEmpty()) Text(text = label, style = type.body, color = colors.dim)
            BasicTextField(
                value = name,
                onValueChange = { name = it.take(MAX_NAME_LENGTH) },
                singleLine = true,
                textStyle = TextStyle(color = colors.ink, fontSize = type.body.fontSize, fontFamily = type.body.fontFamily),
                cursorBrush = SolidColor(colors.ink),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            )
        }
        DialogButtons(
            confirmLabel = stringResource(R.string.action_save),
            confirmEnabled = name.isNotBlank(),
            onConfirm = { onConfirm(name.trim()) },
            onDismiss = onDismiss,
        )
    }
}

/** Yes/no for destructive actions (delete). */
@Composable
fun ConfirmDialog(text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AppDialog(onDismiss) {
        Text(text = text, style = AppTheme.type.body, color = AppTheme.colors.ink)
        DialogButtons(confirmLabel = confirmLabel, confirmEnabled = true, onConfirm = onConfirm, onDismiss = onDismiss)
    }
}

private const val MAX_NAME_LENGTH = 40
