package dev.suspension.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.suspension.app.ui.theme.AppColors
import dev.suspension.app.ui.theme.AppTheme

private val CardShape = RoundedCornerShape(10.dp)
private const val MIN_TOUCH_TARGET_DP = 44

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

@Composable
fun StepperRow(
    stripe: Color,
    label: String,
    unit: String?,
    hint: String?,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .width(4.dp)
                .height(if (hint != null) 64.dp else 52.dp)
                .background(stripe),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(text = label, style = type.rowLabel, color = colors.ink)
            if (hint != null) {
                Text(text = hint, style = type.rowHint, color = colors.dim)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 12.dp),
        ) {
            StepButton(symbol = "−", contentDescription = "$label verringern", onClick = onDecrement, colors = colors)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(56.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Text(text = valueText, style = type.valueNumeral, color = colors.ink)
                if (unit != null) {
                    Text(text = unit, style = type.valueUnit, color = colors.dim)
                }
            }
            StepButton(symbol = "+", contentDescription = "$label erhöhen", onClick = onIncrement, colors = colors)
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
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = type.body, color = colors.ink)
        Text(text = value, style = type.tableValue, color = colors.ink)
    }
}

@Composable
fun ToggleRow(
    stripe: Color,
    label: String,
    hint: String?,
    valueText: String,
    onToggle: () -> Unit,
) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .width(4.dp)
                .height(if (hint != null) 64.dp else 52.dp)
                .background(stripe),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(text = label, style = type.rowLabel, color = colors.ink)
            if (hint != null) {
                Text(text = hint, style = type.rowHint, color = colors.dim)
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(end = 12.dp)
                .heightIn(min = MIN_TOUCH_TARGET_DP.dp)
                .widthIn(min = 72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.hit)
                .clickable(onClickLabel = "$label wechseln", onClick = onToggle)
                .semantics { contentDescription = "$label: $valueText" }
                .padding(horizontal = 12.dp),
        ) {
            Text(text = valueText, style = type.rowLabel, color = colors.ink)
        }
    }
}
