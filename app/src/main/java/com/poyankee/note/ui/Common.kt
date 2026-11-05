package com.poyankee.note.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Ink = Color(0xFF1E2A3A)
val Accent = Color(0xFF1565C0)

@Composable
fun NoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Accent,
            onPrimary = Color.White,
            secondary = Color(0xFF00897B),
            surface = Color(0xFFFFFFFF),
            background = Color(0xFFF5F5F7),
            surfaceVariant = Color(0xFFE8ECF1),
        ),
        content = content,
    )
}

@Suppress("unused")
private val unusedDark = darkColorScheme()

/** Hazır renkler (renk seçici). */
val PresetColors: List<Int> = listOf(
    0xFF000000, 0xFF1A1A1A, 0xFF546E7A, 0xFF9E9E9E, 0xFFFFFFFF, 0xFFFFF59D,
    0xFFD32F2F, 0xFFE91E63, 0xFFF57C00, 0xFFFFC107, 0xFFFFEB3B, 0xFF8D6E63,
    0xFF2E7D32, 0xFF43A047, 0xFF00897B, 0xFF00BCD4, 0xFF1565C0, 0xFF42A5F5,
    0xFF283593, 0xFF6A1B9A, 0xFFAB47BC, 0xFFFF8A80, 0xFFB9F6CA, 0xFF80D8FF,
).map { it.toInt() }

@Composable
fun ColorDot(color: Int, selected: Boolean, size: Int = 28, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size.dp)
            .border(if (selected) 3.dp else 1.dp, if (selected) Accent else Color(0x33000000), CircleShape)
            .padding(if (selected) 4.dp else 2.dp)
            .background(Color(color or 0xFF000000.toInt()), CircleShape)
            .clickable(onClick = onClick),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = Accent, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
}

@Composable
fun LabeledRow(label: String, sub: String? = null, modifier: Modifier = Modifier, trailing: @Composable () -> Unit) {
    Row(modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = Color(0xFF607D8B))
        }
        trailing()
    }
}
