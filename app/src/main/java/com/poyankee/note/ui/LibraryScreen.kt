@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.poyankee.note.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.poyankee.note.core.PageSize
import com.poyankee.note.core.Template
import com.poyankee.note.data.NotebookInfo
import com.poyankee.note.data.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen(
    notebooks: List<NotebookInfo>,
    busy: String?,
    settings: Settings,
    onOpen: (String) -> Unit,
    onCreate: (String, Template, Boolean, PageSize) -> Unit,
    onImportPdf: () -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onSettings: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var showNew by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<NotebookInfo?>(null) }
    var deleteTarget by remember { mutableStateOf<NotebookInfo?>(null) }
    val fmt = remember { SimpleDateFormat("d MMM yyyy HH:mm", Locale("tr")) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("Defterlerim") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink, titleContentColor = Color.White, actionIconContentColor = Color.White),
            actions = { IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Ayarlar") } },
        )
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { showNew = true }) {
                Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Yeni defter")
            }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(onClick = onImportPdf) {
                Icon(Icons.Filled.PictureAsPdf, null); Spacer(Modifier.width(6.dp)); Text("PDF aç")
            }
            Spacer(Modifier.weight(1f))
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text("Defter ara") }, singleLine = true,
                modifier = Modifier.width(280.dp),
            )
        }
        val list = notebooks.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (notebooks.isEmpty()) "Henüz defter yok.\n\"Yeni defter\" ile boş bir defter açın veya \"PDF aç\" ile bir PDF'in üzerine yazın."
                    else "Aramaya uygun defter yok.",
                    color = Color(0xFF607D8B), style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(170.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(list, key = { it.id }) { nb ->
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        Card(
                            modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onOpen(nb.id) }, onLongClick = { menu = true }),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                        ) {
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(0.9f).background(if (nb.isPdf) Color(0xFFFFEBEE) else Color(0xFFE3F2FD)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (nb.isPdf) Icons.Filled.PictureAsPdf else Icons.Filled.Description, null,
                                    tint = if (nb.isPdf) Color(0xFFC62828) else Accent, modifier = Modifier.size(56.dp),
                                )
                            }
                            Column(Modifier.padding(10.dp)) {
                                Text(nb.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${nb.pageCount} sayfa · ${fmt.format(Date(nb.updated))}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF78909C))
                            }
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Aç") }, onClick = { menu = false; onOpen(nb.id) })
                            DropdownMenuItem(text = { Text("Yeniden adlandır") }, onClick = { menu = false; renameTarget = nb })
                            DropdownMenuItem(text = { Text("Sil") }, onClick = { menu = false; deleteTarget = nb })
                        }
                    }
                }
            }
        }
    }

    if (showNew) NewNotebookDialog(settings, onDismiss = { showNew = false }) { name, t, dark, size ->
        showNew = false; onCreate(name, t, dark, size)
    }
    renameTarget?.let { nb ->
        var name by remember(nb.id) { mutableStateOf(nb.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Yeniden adlandır") },
            text = { OutlinedTextField(name, { name = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { onRename(nb.id, name.ifBlank { nb.name }); renameTarget = null }) { Text("Kaydet") } },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Vazgeç") } },
        )
    }
    deleteTarget?.let { nb ->
        ConfirmDialog("Defteri sil", "\"${nb.name}\" ve içindeki tüm yazılar kalıcı olarak silinecek.", "Sil", { onDelete(nb.id) }, { deleteTarget = null })
    }
    if (busy != null) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            text = { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(28.dp)); Spacer(Modifier.width(16.dp)); Text(busy) } },
        )
    }
}

@Composable
fun NewNotebookDialog(settings: Settings, onDismiss: () -> Unit, onCreate: (String, Template, Boolean, PageSize) -> Unit) {
    var name by remember { mutableStateOf("") }
    var template by remember { mutableStateOf(runCatching { Template.valueOf(settings.newTemplate.value) }.getOrDefault(Template.GRID)) }
    var size by remember { mutableStateOf(runCatching { PageSize.valueOf(settings.newSize.value) }.getOrDefault(PageSize.A4_PORTRAIT)) }
    var dark by remember { mutableStateOf(settings.newDark.value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni defter") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Defter adı") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                SectionTitle("Sayfa şablonu")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Template.entries.forEach { t -> FilterChip(selected = template == t, onClick = { template = t }, label = { Text(t.title) }) }
                }
                SectionTitle("Sayfa boyutu")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PageSize.entries.forEach { s -> FilterChip(selected = size == s, onClick = { size = s }, label = { Text(s.title) }) }
                }
                LabeledRow("Kara tahta (koyu sayfa)", "Projeksiyonda göz yormaz") { Switch(dark, { dark = it }) }
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                settings.newTemplate.value = template.name; settings.newSize.value = size.name; settings.newDark.value = dark
                onCreate(name.ifBlank { "Defter " + SimpleDateFormat("d MMM", Locale("tr")).format(Date()) }, template, dark, size)
            }) { Text("Oluştur") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}
