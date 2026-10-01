package dev.suspension.app.ui.care

import android.app.DatePickerDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.suspension.app.R
import dev.suspension.app.care.CareFormat
import dev.suspension.app.care.CareTag
import dev.suspension.app.care.DueState
import dev.suspension.app.care.RemainingKind
import dev.suspension.app.care.RemainingPart
import dev.suspension.app.ui.theme.AppTheme
import java.time.LocalDate
import java.time.ZoneId

/** Card title with its optional card-level source tag underneath. Same look as the Basics cards. */
@Composable
fun CareCardFrame(title: String, tag: CareTag?, content: @Composable () -> Unit) {
    val colors = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text(text = title, style = AppTheme.type.groupHeading, color = colors.ink, modifier = Modifier.semantics { heading() })
            if (tag != null) TagLine(tag)
        }
        content()
    }
}

/** `· Faustregel`: plain dim text, never a chip. */
@Composable
fun TagLine(tag: CareTag, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.care_tag_line, stringResource(tag.labelResId)),
        style = AppTheme.type.valueUnit,
        color = AppTheme.colors.dim,
        modifier = modifier,
    )
}

/** Status in words: OVERDUE filled, SOON outlined, everything else plain dim. Never colour-coded. */
@Composable
fun StatusPill(state: DueState) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    val label = statusLabel(state)
    when (state) {
        DueState.OVERDUE -> Text(
            text = label,
            style = type.rowHint.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            color = colors.bg,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(colors.ink).padding(horizontal = 10.dp, vertical = 3.dp),
        )
        DueState.SOON -> Text(
            text = label,
            style = type.rowHint.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            color = colors.ink,
            modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, colors.ink, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        )
        else -> Text(text = label, style = type.rowHint, color = colors.dim)
    }
}

@Composable
fun statusLabel(state: DueState): String = stringResource(
    when (state) {
        DueState.OVERDUE -> R.string.care_status_overdue
        DueState.SOON -> R.string.care_status_soon
        DueState.OK -> R.string.care_status_ok
        DueState.DONE -> R.string.care_status_done
        DueState.UNKNOWN -> R.string.care_status_unknown
    },
)

/** `in 230 km · in 41 Tagen`, overdue `seit 80 km`; empty when nothing is left to show. */
@Composable
fun remainingText(parts: List<RemainingPart>): String = parts.map { part ->
    when (part.kind) {
        RemainingKind.KM -> stringResource(
            if (part.overdue) R.string.care_remaining_since_km else R.string.care_remaining_in_km,
            CareFormat.km(part.amount),
        )
        RemainingKind.DAYS -> pluralStringResource(
            if (part.overdue) R.plurals.care_remaining_since_days else R.plurals.care_remaining_in_days,
            part.amount, part.amount,
        )
        RemainingKind.MONTHS -> pluralStringResource(R.plurals.care_remaining_in_months, part.amount, part.amount)
    }
}.joinToString(" · ")

/** Whole row toggles; the switch itself is only the picture, so a screen reader announces one control with its state. */
@Composable
fun CareSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 12.dp,
) {
    val colors = AppTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = horizontalPadding, vertical = 6.dp),
    ) {
        Text(text = label, style = AppTheme.type.rowLabel, color = colors.ink, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.bg,
                checkedTrackColor = colors.ink,
                checkedBorderColor = colors.ink,
                uncheckedThumbColor = colors.dim,
                uncheckedTrackColor = colors.hit,
                uncheckedBorderColor = colors.line,
            ),
        )
    }
}

/** Full-width button in the app's flat style. */
@Composable
fun CareButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false) {
    val colors = AppTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (primary) colors.ink else colors.hit)
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(text = label, style = AppTheme.type.rowLabel, color = if (primary) colors.bg else colors.ink)
    }
}

/**
 * Platform date picker. [max] caps the selectable day; [onClear] adds a "clear" button
 * (labelled [clearLabel]) for dates that may be unset.
 */
fun showDatePicker(
    context: Context,
    initial: LocalDate,
    max: LocalDate?,
    clearLabel: String,
    onClear: (() -> Unit)?,
    onPicked: (LocalDate) -> Unit,
) {
    val dialog = DatePickerDialog(
        context,
        { _, year, month, day -> onPicked(LocalDate.of(year, month + 1, day)) },
        initial.year, initial.monthValue - 1, initial.dayOfMonth,
    )
    if (max != null) {
        dialog.datePicker.maxDate = max.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
    }
    if (onClear != null) {
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, clearLabel) { _, _ -> onClear() }
    }
    dialog.show()
}

fun Context.findActivity(): ComponentActivity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is ComponentActivity) return c
        c = c.baseContext
    }
    return null
}
