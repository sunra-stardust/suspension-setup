package dev.suspension.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.suspension.app.data.RotationDirection
import dev.suspension.app.ui.theme.AppTheme
import kotlin.math.cos
import kotlin.math.sin

/** A ~270° arc with an arrowhead, drawn (not text) so it renders identically on every device. */
@Composable
fun RotationIcon(direction: RotationDirection, tint: Color, modifier: Modifier = Modifier) {
    val clockwise = direction == RotationDirection.CLOCKWISE
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.14f
        val radius = size.minDimension / 2f - strokeWidth
        val center = Offset(size.width / 2f, size.height / 2f)
        val sweep = 270f
        val startAngleDeg = if (clockwise) -225f else 45f
        val sweepDeg = if (clockwise) sweep else -sweep

        drawArc(
            color = tint,
            startAngle = startAngleDeg,
            sweepAngle = sweepDeg,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )

        val endAngleRad = Math.toRadians((startAngleDeg + sweepDeg).toDouble())
        val tipX = center.x + radius * cos(endAngleRad).toFloat()
        val tipY = center.y + radius * sin(endAngleRad).toFloat()
        val tangentRad = endAngleRad + (if (clockwise) Math.PI / 2 else -Math.PI / 2)
        val arrowLen = strokeWidth * 2.4f
        val backX = tipX - arrowLen * cos(tangentRad).toFloat()
        val backY = tipY - arrowLen * sin(tangentRad).toFloat()
        val perpRad = tangentRad + Math.PI / 2
        val spread = strokeWidth * 1.15f
        val leftX = backX + spread * cos(perpRad).toFloat()
        val leftY = backY + spread * sin(perpRad).toFloat()
        val rightX = backX - spread * cos(perpRad).toFloat()
        val rightY = backY - spread * sin(perpRad).toFloat()

        val arrowhead = Path().apply {
            moveTo(tipX, tipY)
            lineTo(leftX, leftY)
            lineTo(rightX, rightY)
            close()
        }
        drawPath(arrowhead, color = tint)
    }
}

/**
 * The single canonical way to show a rotation direction anywhere in the app (Change 01 §5):
 * icon + explanatory text, laid out horizontally (chips, table cells) or vertically (buttons).
 */
@Composable
fun RotationLabel(
    direction: RotationDirection,
    text: String,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
    iconSize: Dp = 16.dp,
    iconTint: Color = AppTheme.colors.ink,
    textStyle: TextStyle = AppTheme.type.rowHint,
    textColor: Color = AppTheme.colors.dim,
) {
    if (vertical) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
            RotationIcon(direction, iconTint, Modifier.size(iconSize))
            Text(text = text, style = textStyle, color = textColor)
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = modifier,
        ) {
            RotationIcon(direction, iconTint, Modifier.size(iconSize))
            Text(text = text, style = textStyle, color = textColor)
        }
    }
}

private val ROTATION_BUTTON_WIDTH = 56.dp
private val ROTATION_BUTTON_HEIGHT = 52.dp

/** The tappable ↺/↻ control replacing `+`/`−` on damping rows (Change 01 §5). */
@Composable
fun RotationButton(
    direction: RotationDirection,
    caption: String,
    enabled: Boolean,
    contentDescription: String,
    disabledStateDescription: String,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(ROTATION_BUTTON_WIDTH)
            .height(ROTATION_BUTTON_HEIGHT)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.hit)
            .clickable(enabled = enabled, onClickLabel = contentDescription, onClick = onClick)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                if (!enabled) {
                    stateDescription = disabledStateDescription
                    disabled()
                }
            },
    ) {
        RotationLabel(
            direction = direction,
            text = caption,
            vertical = true,
            iconSize = 24.dp,
            iconTint = colors.ink,
            textStyle = AppTheme.type.valueUnit,
            textColor = colors.dim,
        )
    }
}
