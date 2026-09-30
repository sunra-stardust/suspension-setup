package dev.suspension.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.suspension.app.R
import dev.suspension.app.data.RotationDirection
import dev.suspension.app.ui.theme.AppColors
import dev.suspension.app.ui.theme.AppTheme

private val CardShape = RoundedCornerShape(10.dp)
private const val MIN_TOUCH_TARGET_DP = 44
private val STACK_BELOW_WIDTH = 300.dp

/** Rows grouped in a card: surface fill, 1dp line border, 10dp radius, no shadow (spec §4). */
@Composable
fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.surface)
            .border(1.dp, colors.line, CardShape),
        content = content,
    )
}

@Composable
fun RowDivider() {
    HorizontalDivider(color = AppTheme.colors.line, thickness = 1.dp)
}

/**
 * Row with the 4dp semantic stripe (spec §4). IntrinsicSize.Min lets the stripe fill exactly
 * the row's height however far the text wraps — a fixed stripe height left gaps, and
 * fillMaxHeight without it collapsed the stripe to nothing.
 */
@Composable
private fun StripedRow(stripe: Color, content: @Composable RowScope.() -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(stripe))
        content()
    }
}

/**
 * Label, hint, and — when the rider moved off the starting value — the deviation line
 * ("2 Klicks weiter zu als Fox-Empfehlung"), in ink so it stands out from the grey hint.
 */
@Composable
private fun LabelAndHint(label: String, hint: String?, deviation: String? = null) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    Column {
        Text(text = label, style = type.rowLabel, color = colors.ink)
        if (hint != null) {
            Text(text = hint, style = type.rowHint, color = colors.dim)
        }
        if (deviation != null) {
            Text(text = "≠ $deviation", style = type.rowHint, color = colors.ink)
        }
    }
}

@Composable
fun StepperRow(
    stripe: Color,
    label: String,
    unit: String?,
    hint: String?,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    deviation: String? = null,
) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    StripedRow(stripe) {
        Box(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) { LabelAndHint(label, hint, deviation) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 12.dp).align(Alignment.CenterVertically),
        ) {
            StepButton(symbol = "−", contentDescription = stringResource(R.string.stepper_decrease, label), onClick = onDecrement, colors = colors)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .widthIn(min = 56.dp)
                    .padding(horizontal = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Text(text = valueText, style = type.valueNumeral, color = colors.ink)
                if (unit != null) {
                    Text(text = unit, style = type.valueUnit, color = colors.dim)
                }
            }
            StepButton(symbol = "+", contentDescription = stringResource(R.string.stepper_increase, label), onClick = onIncrement, colors = colors)
        }
    }
}

/**
 * Rotation-control row for the damping circuits (LSC/HSC/LSR/HSR or single rebound —
 * Change 01). Never mixes `+`/`−` with the dial's own clockwise/counter-clockwise convention;
 * see [RotationButton]. `valueText` is the numeral only — the unit line is "zu" / "offen".
 */
@Composable
fun DampingRow(
    stripe: Color,
    label: String,
    hint: String?,
    valueText: String,
    isOpen: Boolean,
    isRebound: Boolean,
    atZero: Boolean,
    atMax: Boolean,
    a11yBase: String,
    onClockwise: () -> Unit,
    onCounterClockwise: () -> Unit,
    deviation: String? = null,
) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    val ccwCaption = stringResource(if (isRebound) R.string.rotation_caption_faster else R.string.rotation_caption_softer)
    val cwCaption = stringResource(if (isRebound) R.string.rotation_caption_slower else R.string.rotation_caption_firmer)
    val unitText = stringResource(if (isOpen) R.string.damping_state_offen else R.string.damping_state_zu)
    val valueDescription = if (isOpen) {
        stringResource(R.string.rotation_value_description_open, a11yBase, valueText)
    } else {
        stringResource(R.string.rotation_value_description_closed, a11yBase)
    }
    val limitReached = stringResource(R.string.rotation_limit_reached)
    val cwDescription = stringResource(R.string.rotation_cw_description, a11yBase, cwCaption)
    val ccwDescription = stringResource(R.string.rotation_ccw_description, a11yBase, ccwCaption)

    val controlsRow: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RotationButton(
                direction = RotationDirection.COUNTER_CLOCKWISE,
                caption = ccwCaption,
                enabled = !atMax,
                contentDescription = ccwDescription,
                disabledStateDescription = limitReached,
                onClick = onCounterClockwise,
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .widthIn(min = 56.dp)
                    .padding(horizontal = 4.dp)
                    .semantics(mergeDescendants = true) {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = valueDescription
                    },
            ) {
                Text(text = valueText, style = type.valueNumeral, color = colors.ink)
                Text(text = unitText, style = type.valueUnit, color = colors.dim)
            }
            RotationButton(
                direction = RotationDirection.CLOCKWISE,
                caption = cwCaption,
                enabled = !atZero,
                contentDescription = cwDescription,
                disabledStateDescription = limitReached,
                onClick = onClockwise,
            )
        }
    }

    // Change 01 §5: at narrow widths / large font scale, the label+hint and the three-part
    // control cluster don't both fit on one line — stack instead of clipping either one.
    // Dividing by fontScale measures the width in "text units": 448dp at 200% behaves like 224dp.
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth / fontScale < STACK_BELOW_WIDTH) {
            StripedRow(stripe) {
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
                    LabelAndHint(label, hint, deviation)
                    Box(modifier = Modifier.padding(top = 8.dp)) { controlsRow() }
                }
            }
        } else {
            StripedRow(stripe) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) { LabelAndHint(label, hint, deviation) }
                Box(modifier = Modifier.padding(end = 12.dp).align(Alignment.CenterVertically)) { controlsRow() }
            }
        }
    }
}

@Composable
fun StepButton(
    symbol: String,
    contentDescription: String,
    onClick: () -> Unit,
    colors: AppColors = AppTheme.colors,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(MIN_TOUCH_TARGET_DP.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.hit)
            .clickable(onClickLabel = contentDescription, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Text(text = symbol, style = AppTheme.type.rowLabel, color = colors.ink)
    }
}

/** Two-column table row: label left, value right-aligned, tabular numerals (spec §10). */
@Composable
fun TwoColumnRow(label: String, value: String) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        Text(text = label, style = type.body, color = colors.ink, modifier = Modifier.weight(1f))
        Text(text = value, style = type.tableValue, color = colors.ink, textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
fun ToggleRow(
    stripe: Color,
    label: String,
    hint: String?,
    valueText: String,
    onToggle: () -> Unit,
    deviation: String? = null,
) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    val changeLabel = stringResource(R.string.toggle_change, label)
    StripedRow(stripe) {
        Box(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) { LabelAndHint(label, hint, deviation) }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = 12.dp)
                .heightIn(min = MIN_TOUCH_TARGET_DP.dp)
                .widthIn(min = 72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.hit)
                .clickable(onClickLabel = changeLabel, onClick = onToggle)
                .semantics { contentDescription = "$label: $valueText" }
                .padding(horizontal = 12.dp),
        ) {
            Text(text = valueText, style = type.rowLabel, color = colors.ink)
        }
    }
}
