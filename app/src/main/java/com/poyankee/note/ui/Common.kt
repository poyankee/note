package com.poyankee.note.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ---------------- tasarım renkleri ---------------- */

object UI {
    val Primary = Color(0xFF4F46E5)          // indigo
    val PrimarySoft = Color(0xFFEEF0FF)
    val PrimaryText = Color(0xFF3730A3)
    val Bg = Color(0xFFF4F5F8)
    val Surface = Color(0xFFFFFFFF)
    val Border = Color(0xFFE4E7EC)
    val Text = Color(0xFF1D2330)
    val TextSoft = Color(0xFF667085)
    val TextFaint = Color(0xFF98A2B3)
    val Desk = Color(0xFFE9EBEF)
    val Danger = Color(0xFFE5484D)
    val Shadow = Color(0x1A101828)
}

// Eski adlar (diğer dosyalarla uyum)
val Ink = UI.Text
val Accent = UI.Primary

@Composable
fun NoteTheme(content: @Composable () -> Unit) {
    val base = Typography()
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = UI.Primary,
            onPrimary = Color.White,
            primaryContainer = UI.PrimarySoft,
            onPrimaryContainer = UI.PrimaryText,
            secondary = Color(0xFF0E9384),
            surface = UI.Surface,
            onSurface = UI.Text,
            background = UI.Bg,
            onBackground = UI.Text,
            surfaceVariant = Color(0xFFF2F4F7),
            onSurfaceVariant = UI.TextSoft,
            outline = UI.Border,
            outlineVariant = UI.Border,
            surfaceContainer = UI.Surface,
            surfaceContainerHigh = UI.Surface,
            surfaceContainerHighest = Color(0xFFF2F4F7),
            surfaceContainerLow = UI.Surface,
        ),
        typography = base.copy(
            headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
            titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        ),
        content = content,
    )
}

/** Gölgeli, yuvarlak köşeli yüzen panel. */
@Composable
fun FloatingCard(modifier: Modifier = Modifier, radius: Int = 18, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(radius.dp),
        color = UI.Surface,
        shadowElevation = 10.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, Color(0x0F101828)),
        content = content,
    )
}

/** Küçük, yuvarlak seçilebilir hap düğme. */
@Composable
fun Pill(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) UI.PrimarySoft else Color(0xFFF2F4F7))
            .border(1.dp, if (selected) UI.Primary.copy(alpha = 0.35f) else Color.Transparent, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 13.sp, color = if (selected) UI.PrimaryText else UI.Text, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
fun PillRow(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, content = content)
}

/** Hazır renkler (renk seçici). */
val PresetColors: List<Int> = listOf(
    0xFF111827, 0xFF374151, 0xFF6B7280, 0xFFD1D5DB, 0xFFFFFFFF, 0xFFFFF59D,
    0xFFDC2626, 0xFFE11D48, 0xFFEA580C, 0xFFF59E0B, 0xFFFACC15, 0xFF92400E,
    0xFF15803D, 0xFF22C55E, 0xFF0D9488, 0xFF06B6D4, 0xFF1D4ED8, 0xFF3B82F6,
    0xFF312E81, 0xFF7C3AED, 0xFFC026D3, 0xFFFCA5A5, 0xFFBBF7D0, 0xFFBAE6FD,
).map { it.toInt() }

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ColorDot(color: Int, selected: Boolean, size: Int = 28, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    val c = Color(color or 0xFF000000.toInt())
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .border(2.dp, if (selected) UI.Primary else Color.Transparent, CircleShape)
            .padding(if (selected) 4.dp else 2.dp)
            .clip(CircleShape)
            .background(c)
            .border(1.dp, Color(0x22000000), CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UI.Surface,
        title = { Text("Renk seç") },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PresetColors.forEach { c -> ColorDot(c, (c or 0xFF000000.toInt()) == (current or 0xFF000000.toInt()), 40) { onPick(c) } }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } },
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String = "Evet", onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UI.Surface,
        title = { Text(title) },
        text = { Text(text, color = UI.TextSoft) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm, color = if (confirm == "Sil") UI.Danger else UI.Primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç", color = UI.TextSoft) } },
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text.uppercase(), style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
        color = UI.TextFaint, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

@Composable
fun LabeledRow(label: String, sub: String? = null, modifier: Modifier = Modifier, trailing: @Composable () -> Unit) {
    Row(modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = UI.Text)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = UI.TextSoft)
        }
        trailing()
    }
}
