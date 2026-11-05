@file:OptIn(ExperimentalFoundationApi::class)

package com.poyankee.note.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poyankee.note.data.NotebookInfo
import com.poyankee.note.data.SortMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Ana sayfa: solda klasörler, sağda defter kapakları. [folder] null = tüm defterler. */
@Suppress("DEPRECATION")
@Composable
fun LibraryScreen(
    notebooks: List<NotebookInfo>,
    folders: List<String>,
    folder: String?,
    busy: String?,
    onSelectFolder: (String?) -> Unit,
    onOpen: (String) -> Unit,
    onCreate: (String) -> Unit,
    onImportPdf: () -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (String, String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onSettings: () -> Unit,
    sort: SortMode = SortMode.UPDATED,
    onSort: (SortMode) -> Unit = {},
    onFavorite: (String, Boolean) -> Unit = { _, _ -> },
    onLock: (String, String) -> Unit = { _, _ -> },
    onQuickNote: () -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    var showNew by remember { mutableStateOf(false) }
    var newFolder by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<NotebookInfo?>(null) }
    var deleteTarget by remember { mutableStateOf<NotebookInfo?>(null) }
    var moveTarget by remember { mutableStateOf<NotebookInfo?>(null) }
    var folderRename by remember { mutableStateOf<String?>(null) }
    var folderDelete by remember { mutableStateOf<String?>(null) }
    var lockTarget by remember { mutableStateOf<NotebookInfo?>(null) }
    var sortMenu by remember { mutableStateOf(false) }
    val isFav = folder == FAVORITES

    BoxWithConstraints(Modifier.fillMaxSize().background(UI.Bg)) {
        val wide = maxWidth > 720.dp
        Row(Modifier.fillMaxSize()) {
            // ---------- kenar çubuğu ----------
            Column(
                Modifier.width(if (wide) 264.dp else 210.dp).fillMaxHeight().background(UI.Surface)
                    .padding(horizontal = 14.dp, vertical = 18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp, bottom = 22.dp)) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF4338CA)))),
                        contentAlignment = Alignment.Center,
                    ) { ToolGlyph(com.poyankee.note.editor.Tool.PEN, Color.White, Color(0xFFFDE68A), 22.dp) }
                    Spacer(Modifier.width(10.dp))
                    Text("Note", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp), color = UI.Text)
                }
                Button(
                    onClick = { onCreate("") }, modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                ) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("Yeni defter", fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onImportPdf, modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, UI.Border),
                ) { Icon(Icons.Rounded.PictureAsPdf, null, tint = Color(0xFFD92D20)); Spacer(Modifier.width(6.dp)); Text("PDF aç", color = UI.Text) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onQuickNote, modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, UI.Border),
                ) { Icon(Icons.Rounded.FlashOn, null, tint = Color(0xFFF59E0B)); Spacer(Modifier.width(6.dp)); Text("Hızlı not", color = UI.Text) }

                Spacer(Modifier.height(20.dp))
                NavItem(Icons.Rounded.LibraryBooks, "Tüm defterler", notebooks.size, folder == null) { onSelectFolder(null) }
                NavItem(Icons.Rounded.Star, "Favoriler", notebooks.count { it.favorite }, isFav) { onSelectFolder(FAVORITES) }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 18.dp, bottom = 4.dp)) {
                    Text("KLASÖRLER", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp), color = UI.TextFaint, modifier = Modifier.weight(1f))
                    IconButton(onClick = { newFolder = true }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Rounded.CreateNewFolder, "Yeni klasör", tint = UI.TextSoft, modifier = Modifier.size(20.dp))
                    }
                }
                LazyColumn(Modifier.weight(1f)) {
                    items(folders) { f ->
                        var menu by remember { mutableStateOf(false) }
                        Box {
                            NavItem(Icons.Rounded.Folder, f, notebooks.count { it.folder == f }, folder == f, onLong = { menu = true }) { onSelectFolder(f) }
                            DropdownMenu(menu, { menu = false }) {
                                DropdownMenuItem(text = { Text("Yeniden adlandır") }, onClick = { menu = false; folderRename = f })
                                DropdownMenuItem(text = { Text("Klasörü sil") }, onClick = { menu = false; folderDelete = f })
                            }
                        }
                    }
                    if (folders.isEmpty()) item {
                        Text(
                            "Defterlerinizi sınıf veya konuya göre gruplamak için klasör oluşturun.",
                            fontSize = 12.sp, color = UI.TextFaint, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                NavItem(Icons.Rounded.Settings, "Ayarlar", null, false) { onSettings() }
            }
            Box(Modifier.width(1.dp).fillMaxHeight().background(UI.Border))

            // ---------- defterler ----------
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 26.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (isFav) "Favoriler" else folder ?: "Tüm defterler", style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp), color = UI.Text)
                        val count = notebooks.count { matches(it, folder) }
                        Text("$count defter", color = UI.TextSoft, fontSize = 14.sp)
                    }
                    Box {
                        Pill("↕ " + sort.title, false) { sortMenu = true }
                        DropdownMenu(sortMenu, { sortMenu = false }) {
                            SortMode.entries.forEach { m -> DropdownMenuItem(text = { Text((if (m == sort) "✓ " else "") + m.title) }, onClick = { sortMenu = false; onSort(m) }) }
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    SearchField(query) { query = it }
                }
                val filtered = notebooks.filter { matches(it, folder) && (query.isBlank() || it.name.contains(query, ignoreCase = true)) }
                val sorted = when (sort) {
                    SortMode.UPDATED -> filtered.sortedByDescending { it.updated }
                    SortMode.NAME -> filtered.sortedBy { it.name.lowercase(Locale.forLanguageTag("tr")) }
                    SortMode.CREATED -> filtered.sortedByDescending { it.created }
                }
                // favoriler her zaman en üstte
                val list = sorted.sortedByDescending { it.favorite }
                if (list.isEmpty()) {
                    EmptyState(
                        if (notebooks.isEmpty()) "Henüz defter yok" else if (query.isNotBlank()) "Sonuç bulunamadı" else "Bu klasör boş",
                        if (notebooks.isEmpty()) "\"Yeni defter\" ile boş bir defter açın ya da \"PDF aç\" ile bir PDF'in üzerine yazın."
                        else if (query.isNotBlank()) "Farklı bir ad deneyin." else "Bir defterin menüsünden \"Klasöre taşı\"yı seçin veya burada yeni defter oluşturun.",
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(if (wide) 128.dp else 112.dp),
                        contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 12.dp, bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(list, key = { it.id }) { nb ->
                            NotebookCard(
                                nb, onOpen = { onOpen(nb.id) }, onRename = { renameTarget = nb }, onMove = { moveTarget = nb }, onDelete = { deleteTarget = nb },
                                onFavorite = { onFavorite(nb.id, !nb.favorite) }, onLock = { lockTarget = nb },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNew) NameDialog("Yeni defter", "Defter adı", "", "Oluştur",
        note = "Desen (kareli, izometrik...), kağıt rengi ve aralık defterin içindeki \"Sayfa\" panelinden değiştirilir.",
        onDismiss = { showNew = false }) { showNew = false; onCreate(it) }
    if (newFolder) NameDialog("Yeni klasör", "Klasör adı", "", "Oluştur", onDismiss = { newFolder = false }) { newFolder = false; onCreateFolder(it) }
    folderRename?.let { f -> NameDialog("Klasörü yeniden adlandır", "Klasör adı", f, "Kaydet", onDismiss = { folderRename = null }) { folderRename = null; onRenameFolder(f, it) } }
    folderDelete?.let { f ->
        ConfirmDialog("Klasörü sil", "\"$f\" klasörü silinecek. İçindeki defterler silinmez, \"Tüm defterler\"de kalır.", "Sil", { onDeleteFolder(f) }, { folderDelete = null })
    }
    renameTarget?.let { nb -> NameDialog("Yeniden adlandır", "Defter adı", nb.name, "Kaydet", onDismiss = { renameTarget = null }) { renameTarget = null; onRename(nb.id, it) } }
    deleteTarget?.let { nb ->
        ConfirmDialog("Defteri sil", "\"${nb.name}\" ve içindeki tüm yazılar kalıcı olarak silinecek.", "Sil", { onDelete(nb.id) }, { deleteTarget = null })
    }
    lockTarget?.let { nb ->
        if (nb.locked) {
            PinDialog("Kilidi kaldır", "Defterin PIN kodunu girin", onDismiss = { lockTarget = null }) { pin ->
                if (com.poyankee.note.data.Lock.check(nb.id, pin, nb.lockHash)) { onLock(nb.id, ""); lockTarget = null; true } else false
            }
        } else {
            PinDialog("Defteri kilitle", "4-8 haneli bir PIN belirleyin. PIN'i unutursanız defter açılamaz.", confirmTwice = true, onDismiss = { lockTarget = null }) { pin ->
                onLock(nb.id, com.poyankee.note.data.Lock.hash(nb.id, pin)); lockTarget = null; true
            }
        }
    }
    moveTarget?.let { nb ->
        MoveDialog(nb, folders, onDismiss = { moveTarget = null }, onCreateFolder = onCreateFolder) { f -> moveTarget = null; onMove(nb.id, f) }
    }
    if (busy != null) {
        AlertDialog(
            onDismissRequest = {}, confirmButton = {}, containerColor = UI.Surface,
            text = { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(28.dp)); Spacer(Modifier.width(16.dp)); Text(busy) } },
        )
    }
}

@Composable
private fun NavItem(icon: ImageVector, text: String, count: Int?, selected: Boolean, onLong: (() -> Unit)? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 1.dp).clip(RoundedCornerShape(10.dp))
            .background(if (selected) UI.PrimarySoft else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLong)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) UI.Primary else UI.TextSoft, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, color = if (selected) UI.PrimaryText else UI.Text, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (count != null) Text("$count", color = UI.TextFaint, fontSize = 13.sp)
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    Row(
        Modifier.width(280.dp).height(42.dp).clip(RoundedCornerShape(21.dp)).background(UI.Surface)
            .border(1.dp, UI.Border, RoundedCornerShape(21.dp)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = UI.TextFaint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Defter ara", color = UI.TextFaint, fontSize = 15.sp)
            BasicTextField(value, onChange, singleLine = true, textStyle = TextStyle(fontSize = 15.sp, color = UI.Text), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun EmptyState(title: String, text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(380.dp)) {
            Box(Modifier.size(84.dp).clip(CircleShape).background(UI.PrimarySoft), contentAlignment = Alignment.Center) {
                ToolGlyph(com.poyankee.note.editor.Tool.PEN, UI.Primary, Color(0xFFF59E0B), 40.dp)
            }
            Spacer(Modifier.height(18.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = UI.Text)
            Spacer(Modifier.height(6.dp))
            Text(text, color = UI.TextSoft, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

private val dateFmt = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("tr"))

@Composable
private fun NotebookCard(
    nb: NotebookInfo, onOpen: () -> Unit, onRename: () -> Unit, onMove: () -> Unit, onDelete: () -> Unit,
    onFavorite: () -> Unit, onLock: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val cover by produceState<ImageBitmap?>(null, nb.cover, nb.coverStamp) {
        value = nb.cover?.let { f -> withContext(Dispatchers.IO) { try { BitmapFactory.decodeFile(f.path)?.asImageBitmap() } catch (_: Throwable) { null } } }
    }
    Column {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.72f)
                .shadow(6.dp, RoundedCornerShape(10.dp), ambientColor = UI.Shadow, spotColor = UI.Shadow)
                .clip(RoundedCornerShape(10.dp)).background(UI.Surface)
                .combinedClickable(onClick = onOpen, onLongClick = { menu = true }),
        ) {
            val img = if (nb.locked) null else cover
            if (nb.locked) {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF0F172A)))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Lock, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(44.dp))
                }
            } else if (img != null) {
                Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                val hue = (nb.id.hashCode() and 0x7fffffff) % 5
                val colors = listOf(
                    listOf(Color(0xFF6366F1), Color(0xFF4338CA)), listOf(Color(0xFF14B8A6), Color(0xFF0F766E)),
                    listOf(Color(0xFFF59E0B), Color(0xFFD97706)), listOf(Color(0xFFEC4899), Color(0xFFBE185D)),
                    listOf(Color(0xFF64748B), Color(0xFF334155)),
                )[hue]
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(colors)), contentAlignment = Alignment.Center) {
                    if (nb.isPdf) Icon(Icons.Rounded.PictureAsPdf, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(44.dp))
                    else Text(nb.name.take(1).uppercase(), color = Color.White.copy(alpha = 0.92f), fontSize = 44.sp, fontWeight = FontWeight.Bold)
                }
            }
            // sırt şeridi
            Box(Modifier.width(7.dp).fillMaxHeight().background(Color(0x14000000)))
            if (nb.favorite) {
                Box(Modifier.align(Alignment.TopStart).padding(8.dp).size(28.dp).clip(CircleShape).background(Color(0xE6FFFFFF)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Star, "Favori", tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                }
            }
            if (nb.isPdf) {
                Surface(color = Color(0xFFD92D20), shape = RoundedCornerShape(6.dp), modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                    Text("PDF", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) {
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(Color(0xCCFFFFFF)).clickable { menu = true },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.MoreHoriz, "Menü", tint = UI.Text, modifier = Modifier.size(20.dp)) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Aç") }, onClick = { menu = false; onOpen() })
                    DropdownMenuItem(text = { Text("Yeniden adlandır") }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text("Klasöre taşı") }, onClick = { menu = false; onMove() })
                    DropdownMenuItem(text = { Text(if (nb.favorite) "Favorilerden çıkar" else "Favorilere ekle") }, onClick = { menu = false; onFavorite() })
                    DropdownMenuItem(text = { Text(if (nb.locked) "Kilidi kaldır" else "Kilitle") }, onClick = { menu = false; onLock() })
                    DropdownMenuItem(text = { Text("Sil", color = UI.Danger) }, onClick = { menu = false; onDelete() })
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(nb.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = UI.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${nb.pageCount} sayfa · ${dateFmt.format(Date(nb.updated))}", fontSize = 12.sp, color = UI.TextSoft, maxLines = 1)
    }
}

@Composable
fun NameDialog(
    title: String, label: String, initial: String, confirm: String,
    note: String? = null, onDismiss: () -> Unit, onDone: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(120); try { focus.requestFocus() } catch (_: Exception) {} }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UI.Surface,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    text, { text = it }, label = { Text(label) }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                if (note != null) Text(note, fontSize = 13.sp, color = UI.TextSoft, modifier = Modifier.padding(top = 10.dp))
            }
        },
        confirmButton = {
            Button(onClick = { onDone(text.trim()) }, shape = RoundedCornerShape(10.dp)) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç", color = UI.TextSoft) } },
    )
}

@Composable
private fun MoveDialog(nb: NotebookInfo, folders: List<String>, onDismiss: () -> Unit, onCreateFolder: (String) -> Unit, onPick: (String) -> Unit) {
    var creating by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UI.Surface,
        title = { Text("Klasöre taşı") },
        text = {
            Column {
                Text("\"${nb.name}\"", color = UI.TextSoft, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                FolderRow("Klasörsüz", nb.folder.isEmpty()) { onPick("") }
                folders.forEach { f -> FolderRow(f, nb.folder == f) { onPick(f) } }
                TextButton(onClick = { creating = true }) { Icon(Icons.Rounded.CreateNewFolder, null); Spacer(Modifier.width(6.dp)); Text("Yeni klasör") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat", color = UI.TextSoft) } },
    )
    if (creating) NameDialog("Yeni klasör", "Klasör adı", "", "Oluştur ve taşı", onDismiss = { creating = false }) {
        creating = false
        if (it.isNotBlank()) { onCreateFolder(it); onPick(it) }
    }
}

@Composable
private fun FolderRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (selected) UI.PrimarySoft else Color.Transparent)
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Folder, null, tint = if (selected) UI.Primary else UI.TextSoft)
        Spacer(Modifier.width(12.dp))
        Text(name, color = UI.Text, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

/** Kenar çubuğunda "Favoriler" seçimi için özel değer. */
const val FAVORITES = "\u0000favoriler"

private fun matches(nb: NotebookInfo, folder: String?) = when (folder) {
    null -> true
    FAVORITES -> nb.favorite
    else -> nb.folder == folder
}

/** PIN girişi. [onPin] true dönerse pencere kapanır, false ise "yanlış PIN" gösterilir. */
@Composable
fun PinDialog(title: String, text: String, confirmTwice: Boolean = false, onDismiss: () -> Unit, onPin: (String) -> Boolean) {
    var pin by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(120); try { focus.requestFocus() } catch (_: Exception) {} }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UI.Surface,
        title = { Text(title) },
        text = {
            Column {
                Text(text, fontSize = 14.sp, color = UI.TextSoft)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    pin, { pin = it.filter { c -> c.isDigit() }.take(8); error = null }, label = { Text("PIN") }, singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                if (confirmTwice) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        pin2, { pin2 = it.filter { c -> c.isDigit() }.take(8); error = null }, label = { Text("PIN (tekrar)") }, singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                error?.let { Text(it, color = UI.Danger, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    pin.length < 4 -> error = "En az 4 hane"
                    confirmTwice && pin != pin2 -> error = "PIN'ler aynı değil"
                    !onPin(pin) -> error = "PIN yanlış"
                }
            }, shape = RoundedCornerShape(10.dp)) { Text("Tamam") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç", color = UI.TextSoft) } },
    )
}
