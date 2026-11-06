package com.poyankee.note.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.poyankee.note.data.NotebookInfo
import com.poyankee.note.data.Settings
import java.util.Calendar

private class Slot(val day: Int, val hour: Int, val label: String, val nbId: String)

private fun decode(s: String): List<Slot> = s.split('\u0001').mapNotNull {
    val p = it.split('|')
    if (p.size < 4) null else try { Slot(p[0].toInt(), p[1].toInt(), p[2], p[3]) } catch (_: Exception) { null }
}

private fun encode(l: List<Slot>) = l.joinToString("\u0001") { "${it.day}|${it.hour}|${it.label.replace("|", "/").replace("\u0001", "")}|${it.nbId}" }

/**
 * Haftalık ders programı: gün × ders saati tablosu. Bir derse dokununca bağlı defter açılır;
 * uzun basınca ders adı ve defter seçilir. Bugünün sütunu vurgulanır.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimetableDialog(notebooks: List<NotebookInfo>, onOpen: (String) -> Unit, onDismiss: () -> Unit) {
    val s = Settings.get(LocalContext.current)
    val slots = decode(s.timetable.value)
    val days = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt")
    val today = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7 // Pazartesi = 0
    var edit by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = UI.Surface,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(24.dp),
        title = { Text("Ders programı") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Derse dokunun: bağlı defter açılır. Uzun basın: ders adı ve defteri seçin.", fontSize = 13.sp, color = UI.TextSoft)
                Spacer(Modifier.height(10.dp))
                Row {
                    Box(Modifier.width(34.dp))
                    days.forEachIndexed { d, name ->
                        Text(name, fontWeight = FontWeight.SemiBold, color = if (d == today) UI.Primary else UI.Text, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }
                for (h in 1..8) {
                    Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("$h", color = UI.TextFaint, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
                        for (d in days.indices) {
                            val slot = slots.firstOrNull { it.day == d && it.hour == h }
                            val nb = slot?.nbId?.let { id -> notebooks.firstOrNull { it.id == id } }
                            Box(
                                Modifier.weight(1f).height(52.dp).padding(2.dp).clip(RoundedCornerShape(8.dp))
                                    .background(if (slot != null) UI.PrimarySoft else if (d == today) UI.SurfaceAlt else UI.Surface)
                                    .border(1.dp, UI.Border, RoundedCornerShape(8.dp))
                                    .combinedClickable(
                                        onClick = { if (nb != null) onOpen(nb.id) else edit = d to h },
                                        onLongClick = { edit = d to h },
                                    ).padding(4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (slot != null) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(slot.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = UI.PrimaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (nb != null) Text(nb.name, fontSize = 10.sp, color = UI.TextSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } },
    )
    edit?.let { (d, h) ->
        val cur = slots.firstOrNull { it.day == d && it.hour == h }
        var label by remember(d, h) { mutableStateOf(cur?.label ?: "") }
        var nbId by remember(d, h) { mutableStateOf(cur?.nbId ?: "") }
        AlertDialog(
            onDismissRequest = { edit = null }, containerColor = UI.Surface,
            title = { Text("${days[d]}, $h. ders") },
            text = {
                Column {
                    OutlinedTextField(label, { label = it.take(24) }, label = { Text("Ders / sınıf (ör. 9-A Matematik)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("Bağlı defter", fontSize = 13.sp, color = UI.TextSoft)
                    LazyColumn(Modifier.heightIn(max = 260.dp)) {
                        item { FolderPick("Defter yok", nbId.isEmpty()) { nbId = "" } }
                        items(notebooks, key = { it.id }) { n -> FolderPick(n.name, nbId == n.id) { nbId = n.id; if (label.isBlank()) label = n.name.take(24) } }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val rest = slots.filter { !(it.day == d && it.hour == h) }
                    s.timetable.value = encode(if (label.isBlank()) rest else rest + Slot(d, h, label.trim(), nbId))
                    edit = null
                }) { Text("Kaydet") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (cur != null) TextButton(onClick = { s.timetable.value = encode(slots.filter { !(it.day == d && it.hour == h) }); edit = null }) { Text("Sil", color = UI.Danger) }
                    TextButton(onClick = { edit = null }) { Text("Vazgeç", color = UI.TextSoft) }
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderPick(name: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        (if (selected) "✓ " else "   ") + name, color = if (selected) UI.Primary else UI.Text, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (selected) UI.PrimarySoft else UI.Surface)
            .combinedClickable(onClick = onClick).padding(10.dp),
    )
}
