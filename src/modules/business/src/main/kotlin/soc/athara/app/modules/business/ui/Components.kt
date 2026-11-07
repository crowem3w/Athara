package soc.athara.app.modules.business.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import soc.athara.app.shared.designsystem.theme.Background
import soc.athara.app.shared.designsystem.theme.Divider as DividerColor
import soc.athara.app.shared.designsystem.theme.Teal
import soc.athara.app.shared.designsystem.theme.TealLight
import soc.athara.app.shared.designsystem.theme.TextPrimary
import soc.athara.app.shared.designsystem.theme.TextSecondary
import soc.athara.app.shared.designsystem.theme.TextTertiary

/** Status colours. Always paired with a text label, never colour alone. */
enum class Tone(val fg: Color, val bg: Color) {
    Neutral(TextSecondary, Color(0xFFF1F4F4)),
    Good(Color(0xFF1E7D4F), Color(0xFFE3F4EA)),
    Warn(Color(0xFF8A5A00), Color(0xFFFFF1D6)),
    Bad(Color(0xFFB3263E), Color(0xFFFDE6EA)),
    Info(Teal, TealLight),
}

@Composable
fun StatusChip(label: String, tone: Tone, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = tone.fg,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(tone.bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun PageTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null) TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

/** Standard bordered surface. Pass padding = 0.dp when the content is a list of [IconRow]s. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val border = BorderStroke(1.dp, DividerColor)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, color = Background, border = border) {
            Column(Modifier.padding(padding), content = content)
        }
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = Background, border = border) {
            Column(Modifier.padding(padding), content = content)
        }
    }
}

@Composable
fun RowDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(DividerColor))
}

/** Icon + title + optional subtitle + trailing content (chevron by default when clickable). */
@Composable
fun IconRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tint: Color = Teal,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (tint == Teal) TealLight else tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextTertiary)
        }
    }
}

@Composable
fun ToggleRow(title: String, checked: Boolean, onChecked: (Boolean) -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(TealLight), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Teal, modifier = Modifier.size(28.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
        if (actionLabel != null) Button(onClick = onAction, modifier = Modifier.padding(top = 8.dp)) { Text(actionLabel) }
    }
}

/** Minimal trend line. Keep it secondary to the operational cards above it. */
@Composable
fun TrendChart(values: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(96.dp)) {
        if (values.size < 2) return@Canvas
        val lo = values.min()
        val span = (values.max() - lo).takeIf { it > 0f } ?: 1f
        val pad = 6.dp.toPx()
        val step = size.width / (values.size - 1)
        val points = values.mapIndexed { i, v ->
            Offset(i * step, size.height - pad - ((v - lo) / span) * (size.height - 2 * pad))
        }
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, Teal, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(Teal, radius = 5.dp.toPx(), center = points.last())
    }
}
