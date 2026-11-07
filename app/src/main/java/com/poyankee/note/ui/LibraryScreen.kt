@file:OptIn(ExperimentalFoundationApi::class)

package com.poyankee.note.ui

import com.poyankee.note.core.L

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.AssignmentLate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material.icons.outlined.StickyNote2 as OStickyNote
import androidx.compose.material.icons.outlined.Bolt as OBolt
import androidx.compose.material.icons.outlined.DisabledByDefault as OWrong
import androidx.compose.material.icons.outlined.Delete as ODelete
import androidx.compose.material.icons.outlined.Settings as OSettings
import androidx.compose.material.icons.outlined.Article as OArticle
import androidx.compose.material.icons.outlined.Description as ODescription
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.StarBorder
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
    trash: List<Pair<NotebookInfo, Long>> = emptyList(),
    onRestoreTrash: (String) -> Unit = {},
    onPurge: (String) -> Unit = {},
    onEmptyTrash: () -> Unit = {},
    onOpenTodo: (String, Int) -> Unit = { _, _ -> },
    onMerge: (List<String>) -> Unit = {},
    onFromImages: () -> Unit = {},
    onWrongBook: () -> Unit = {},
    onForgotPin: (NotebookInfo) -> Unit = {},
    onSaveTemplate: (String) -> Unit = {},
    onFromTemplate: (String) -> Unit = {},
    /** Defter yanda açıkken (bölünmüş görünüm): dar sütun, iki sütunlu ızgara. */
    compact: Boolean = false,
    /** Yanda açık olan defter (vurgulanır). */
    currentId: String? = null,
) {
    var showTemplates by remember { mutableStateOf(false) }
    var kind by remember { mutableStateOf(0) }
    var showTodos by remember { mutableStateOf(false) }
    val todos = com.poyankee.note.data.TodoList.get(androidx.compose.ui.platform.LocalContext.current)
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
    var viewMenu by remember { mutableStateOf(false) }
    var folderColorFor by remember { mutableStateOf<String?>(null) }
    // toplu seçim: uzun basınca başlar
    var selection by remember { mutableStateOf<Set<String>?>(null) }
    var bulkDelete by remember { mutableStateOf(false) }
    var bulkMove by remember { mutableStateOf(false) }
    val settings = com.poyankee.note.data.Settings.get(androidx.compose.ui.platform.LocalContext.current)
    val view = settings.libraryViewValue
    val isFav = folder == FAVORITES
    val isTrash = folder == TRASH
    var confirmEmpty by remember { mutableStateOf(false) }
    var purgeTarget by remember { mutableStateOf<NotebookInfo?>(null) }
    LaunchedEffect(folder) { selection = null }
    androidx.activity.compose.BackHandler(selection != null) { selection = null }

    BoxWithConstraints(Modifier.fillMaxSize().background(UI.Bg)) {
        val wide = maxWidth > 720.dp
        // telefonda (dar ekran) kenar çubuğu gizli; üstteki ☰ ile açılır
        val narrow = maxWidth < 600.dp
        var sideOpen by remember { mutableStateOf(false) }
        val sideW = if (compact || maxWidth > 1000.dp) 290.dp else if (wide) 260.dp else 236.dp
        Row(Modifier.fillMaxSize()) {
            // ---------- sol menü: manzara başlığı, bölümler, klasörler, ayarlar ----------
            val quick = com.poyankee.note.MainActivity.QUICK_FOLDER
            if (!narrow || sideOpen) Box(Modifier.width(sideW).fillMaxHeight().background(UI.SideBg)) {
                val art = SideArt.of(settings.sideArt.value)
                val headH = if (art == SideArt.NONE) 110.dp else 200.dp
                SideArtView(art, Modifier.fillMaxWidth().height(headH))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 24.dp, top = 36.dp)) {
                    Box(
                        Modifier.size(46.dp).shadow(6.dp, RoundedCornerShape(13.dp)).clip(RoundedCornerShape(13.dp)).background(UI.LogoBg),
                        contentAlignment = Alignment.Center,
                    ) { Icon(AppIcon.of(settings.appIcon.value).icon, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(settings.appTitle.value.ifBlank { "Note" }, style = TextStyle(fontSize = 25.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
                            color = if (art == SideArt.NIGHT || UI.dark) Color.White else Color(0xFF1B2A3A), maxLines = 1)
                        if (settings.appSubtitle.value.isNotBlank()) Text(settings.appSubtitle.value, fontSize = 13.sp,
                            color = if (art == SideArt.NIGHT || UI.dark) Color.White.copy(alpha = 0.8f) else Color(0xFF3B4D60), maxLines = 1)
                    }
                }
                Column(
                    Modifier.padding(top = headH - 30.dp).fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(UI.SideBg)
                        .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 16.dp),
                ) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        for (it in SideItem.parse(settings.sideItems.value)) when (it) {
                            SideItem.ALL -> NavItem(Icons.Outlined.OStickyNote, L.t("Tüm Notlar"), notebooks.size, folder == null && !showTodos) { showTodos = false; onSelectFolder(null) }
                            SideItem.FAVORITES -> NavItem(Icons.Rounded.StarOutline, L.t("Favoriler"), notebooks.count { it.favorite }, isFav) { showTodos = false; onSelectFolder(FAVORITES) }
                            SideItem.QUICK -> NavItem(Icons.Outlined.OBolt, L.t("Hızlı Notlar"), notebooks.count { it.folder == quick }, folder == quick) { showTodos = false; onSelectFolder(quick) }
                            SideItem.WRONG -> NavItem(Icons.Outlined.OWrong, L.t("Yanlış Defteri"), null, false) { onWrongBook() }
                            SideItem.TODO -> NavItem(Icons.Rounded.Checklist, L.t("Yapılacaklar"), todos.items.count { !it.done }.takeIf { it > 0 }, showTodos) { showTodos = !showTodos }
                            SideItem.TRASH -> NavItem(Icons.Outlined.ODelete, L.t("Çöp Kutusu"), trash.size.takeIf { it > 0 }, isTrash) { showTodos = false; onSelectFolder(TRASH) }
                        }
                        Box(Modifier.padding(horizontal = 12.dp, vertical = 12.dp).fillMaxWidth().height(1.dp).background(UI.Border))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 14.dp, end = 4.dp, bottom = 6.dp)) {
                            Text(settings.folderTitle.value.ifBlank { L.t("Klasörler") }, fontSize = 17.sp, color = UI.TextSoft, modifier = Modifier.weight(1f))
                            Box(
                                Modifier.size(34.dp).clip(CircleShape).background(UI.SurfaceAlt).clickable { newFolder = true },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Rounded.Add, L.t("Yeni klasör"), tint = UI.Text, modifier = Modifier.size(20.dp)) }
                        }
                        folders.filter { it != quick }.forEach { f ->
                            Box {
                                var menu by remember { mutableStateOf(false) }
                                NavItem(Icons.Rounded.Folder, f, notebooks.count { it.folder == f }, folder == f, onLong = { menu = true },
                                    iconTint = Color(settings.folderColor(f) ?: folderAutoColor(f))) { showTodos = false; onSelectFolder(f) }
                                DropdownMenu(menu, { menu = false }) {
                                    DropdownMenuItem(text = { Text(L.t("Yeniden adlandır")) }, onClick = { menu = false; folderRename = f })
                                    DropdownMenuItem(text = { Text(L.t("Renk")) }, onClick = { menu = false; folderColorFor = f })
                                    DropdownMenuItem(text = { Text(L.t("Klasörü sil")) }, onClick = { menu = false; folderDelete = f })
                                }
                            }
                        }
                    }
                    // ayarlar kartı
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp).shadow(3.dp, RoundedCornerShape(18.dp), ambientColor = UI.Shadow, spotColor = UI.Shadow)
                            .clip(RoundedCornerShape(18.dp)).background(UI.Surface).clickable { onSettings() }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.OSettings, null, tint = UI.Text, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(settings.profileName.value.ifBlank { L.t("Ayarlar") }, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = UI.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(settings.profileSubtitle.value.ifBlank { L.t("Profil, tema, yedekleme") }, fontSize = 13.sp, color = UI.TextSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Rounded.ChevronRight, L.t("Ayarlar"), tint = UI.Text)
                    }
                }
            }

            // ---------- notlar ----------
            Box(Modifier.weight(1f).fillMaxHeight()) {
            Column(Modifier.fillMaxSize()) {
                if (isTrash) {
                    TrashView(trash, onRestore = onRestoreTrash, onPurge = { purgeTarget = it }, onEmpty = { confirmEmpty = true })
                } else {
                val sel = selection
                val pad = if (compact) 20.dp else 30.dp
                if (sel != null) Row(
                    Modifier.fillMaxWidth().padding(start = pad, end = pad, top = 22.dp, bottom = 6.dp).clip(RoundedCornerShape(16.dp))
                        .background(UI.PrimarySoft).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { selection = null }) { Icon(Icons.Rounded.Close, L.t("Seçimi bitir"), tint = UI.PrimaryText) }
                    Text("${sel.size} " + L.t("seçili"), color = UI.PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                    val visible = notebooks.filter { matches(it, folder) }.map { it.id }.toSet()
                    TextButton(onClick = { selection = if (sel.containsAll(visible)) emptySet() else visible }) {
                        Text(if (sel.containsAll(visible) && visible.isNotEmpty()) L.t("Hiçbiri") else L.t("Tümünü seç"))
                    }
                    val enabled = sel.isNotEmpty()
                    TextButton(enabled = enabled, onClick = {
                        val chosen = notebooks.filter { it.id in sel }
                        val fav = !chosen.all { it.favorite }
                        chosen.forEach { if (it.favorite != fav) onFavorite(it.id, fav) }
                    }) { Icon(Icons.Rounded.Star, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(L.t("Favori")) }
                    if (sel.size >= 2) TextButton(onClick = {
                        val order = notebooks.filter { it.id in sel }.sortedByDescending { it.updated }.map { it.id }
                        selection = null; onMerge(order)
                    }) { Text(L.t("Birleştir")) }
                    TextButton(enabled = enabled, onClick = { bulkMove = true }) {
                        Icon(Icons.Rounded.Folder, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(L.t("Klasöre taşı"))
                    }
                    TextButton(enabled = enabled, onClick = { bulkDelete = true }) {
                        Icon(Icons.Rounded.Delete, null, tint = if (enabled) UI.Danger else UI.TextFaint, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp))
                        Text(L.t("Sil"), color = if (enabled) UI.Danger else UI.TextFaint)
                    }
                } else {
                    Row(Modifier.fillMaxWidth().padding(start = pad, end = pad, top = 28.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (narrow) IconButton(onClick = { sideOpen = !sideOpen }) { Icon(Icons.Rounded.Menu, L.t("Menü"), tint = UI.Text) }
                        Column(Modifier.weight(1f)) {
                            Text(if (isFav) L.t("Favoriler") else if (folder == quick) L.t("Hızlı Notlar") else folder ?: L.t("Tüm Notlar"),
                                style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp), color = UI.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val count = notebooks.count { matches(it, folder) }
                            Text("$count " + L.t("not"), color = UI.TextSoft, fontSize = 16.sp)
                        }
                        val isList = view == com.poyankee.note.data.LibraryView.LIST
                        SquareButton(if (isList) Icons.Rounded.GridView else Icons.Rounded.ViewList, if (isList) L.t("Izgara görünümü") else L.t("Liste görünümü")) {
                            settings.libraryView.value = if (isList) com.poyankee.note.data.LibraryView.MEDIUM.name else com.poyankee.note.data.LibraryView.LIST.name
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(start = pad, end = pad, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        SearchField(query, Modifier.weight(1f)) { query = it }
                        Spacer(Modifier.width(10.dp))
                        Box {
                            SquareButton(Icons.Rounded.Tune, L.t("Sırala ve seç")) { sortMenu = true }
                            DropdownMenu(sortMenu, { sortMenu = false }, shape = RoundedCornerShape(16.dp), containerColor = UI.Surface) {
                                Text(L.t("Sırala"), fontSize = 12.sp, color = UI.TextSoft, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                                SortMode.entries.forEach { m -> DropdownMenuItem(text = { Text((if (m == sort) "✓ " else "    ") + m.title) }, onClick = { sortMenu = false; onSort(m) }) }
                                HorizontalDivider(color = UI.Border)
                                Text(L.t("Kapak boyutu"), fontSize = 12.sp, color = UI.TextSoft, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                                com.poyankee.note.data.LibraryView.entries.forEach { v ->
                                    DropdownMenuItem(text = { Text((if (v == view) "✓ " else "    ") + v.title) }, onClick = { sortMenu = false; settings.libraryView.value = v.name })
                                }
                                HorizontalDivider(color = UI.Border)
                                DropdownMenuItem(text = { Text(L.t("Seç")) }, leadingIcon = { Icon(Icons.Rounded.Check, null) }, onClick = { sortMenu = false; selection = emptySet() })
                            }
                        }
                    }
                }
                // filtreler
                if (sel == null) Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = pad, end = pad, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    FolderTab(null, L.t("Tümü"), notebooks.count { matches(it, folder) }, kind == 0) { kind = 0 }
                    FolderTab(Icons.Outlined.OArticle, L.t("Defterler"), notebooks.count { matches(it, folder) && !it.isPdf }, kind == 1) { kind = 1 }
                    FolderTab(Icons.Outlined.ODescription, "PDF", notebooks.count { matches(it, folder) && it.isPdf }, kind == 2) { kind = 2 }
                    if (!isFav) FolderTab(Icons.Rounded.StarOutline, L.t("Favoriler"), notebooks.count { matches(it, folder) && it.favorite }, kind == 3) { kind = 3 }
                }
                val filtered = notebooks.filter {
                    matches(it, folder) && (query.isBlank() || it.name.contains(query, ignoreCase = true)) &&
                        when (kind) { 1 -> !it.isPdf; 2 -> it.isPdf; 3 -> it.favorite; else -> true }
                }
                val sorted = when (sort) {
                    SortMode.UPDATED -> filtered.sortedByDescending { it.updated }
                    SortMode.NAME -> filtered.sortedBy { it.name.lowercase(Locale.forLanguageTag("tr")) }
                    SortMode.CREATED -> filtered.sortedByDescending { it.created }
                }
                // favoriler her zaman en üstte
                val list = sorted.sortedByDescending { it.favorite }
                if (list.isEmpty()) {
                    EmptyState(
                        if (notebooks.isEmpty()) L.t("Henüz defter yok") else if (query.isNotBlank()) L.t("Sonuç bulunamadı") else L.t("Bu klasör boş"),
                        if (notebooks.isEmpty()) "\"Yeni defter\" ile boş bir defter açın ya da \"PDF aç\" ile bir PDF'in üzerine yazın."
                        else if (query.isNotBlank()) L.t("Farklı bir ad deneyin.") else "Bir defterin menüsünden \"Klasöre taşı\"yı seçin veya burada yeni defter oluşturun.",
                    )
                } else {
                    val actions: (NotebookInfo) -> CardActions = { nb ->
                        CardActions(
                            onOpen = { val s0 = selection; if (s0 != null) selection = if (nb.id in s0) s0 - nb.id else s0 + nb.id else onOpen(nb.id) },
                            onLongPress = { val s0 = selection; selection = (s0 ?: emptySet()) + nb.id },
                            onRename = { renameTarget = nb }, onMove = { moveTarget = nb }, onDelete = { deleteTarget = nb },
                            onFavorite = { onFavorite(nb.id, !nb.favorite) }, onLock = { lockTarget = nb },
                            onTemplate = { onSaveTemplate(nb.id) },
                        )
                    }
                    if (view == com.poyankee.note.data.LibraryView.LIST) {
                        LazyColumn(contentPadding = PaddingValues(start = pad - 6.dp, end = pad - 6.dp, top = 6.dp, bottom = 110.dp), modifier = Modifier.fillMaxSize()) {
                            items(list, key = { it.id }) { nb ->
                                NotebookRow(nb, selection?.contains(nb.id), settings.folderColor(nb.folder)?.let { Color(it) }, actions(nb), current = nb.id == currentId)
                            }
                        }
                    } else LazyVerticalGrid(
                        columns = if (compact) GridCells.Fixed(2) else GridCells.Adaptive(
                            when (view) {
                                com.poyankee.note.data.LibraryView.SMALL -> 130.dp
                                com.poyankee.note.data.LibraryView.LARGE -> 210.dp
                                else -> 165.dp
                            },
                        ),
                        contentPadding = PaddingValues(start = pad, end = pad, top = 6.dp, bottom = 110.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(list, key = { it.id }) { nb ->
                            NotebookCard(nb, selection?.contains(nb.id), actions(nb), current = nb.id == currentId)
                        }
                    }
                }
                }
            }
            // yeni: sağ altta "+" (defter, hızlı not, PDF, fotoğraf, şablon, klasör)
            if (!isTrash && selection == null) Box(Modifier.align(Alignment.BottomEnd).padding(end = 26.dp, bottom = 26.dp)) {
                var addMenu by remember { mutableStateOf(false) }
                Box(
                    Modifier.size(68.dp).shadow(12.dp, CircleShape, ambientColor = UI.Primary, spotColor = UI.Primary).clip(CircleShape).background(UI.Primary).clickable { addMenu = true }.testTag("add"),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Add, L.t("Yeni"), tint = Color.White, modifier = Modifier.size(34.dp)) }
                DropdownMenu(addMenu, { addMenu = false }, shape = RoundedCornerShape(18.dp), containerColor = UI.Surface) {
                    DropdownMenuItem(text = { Text(L.t("Yeni defter")) }, leadingIcon = { Icon(Icons.Rounded.LibraryBooks, null, tint = UI.Primary) }, onClick = { addMenu = false; onCreate("") })
                    DropdownMenuItem(text = { Text(L.t("Hızlı not")) }, leadingIcon = { Icon(Icons.Rounded.FlashOn, null, tint = Color(0xFFF59E0B)) }, onClick = { addMenu = false; onQuickNote() })
                    DropdownMenuItem(text = { Text(L.t("PDF aç")) }, leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, null, tint = Color(0xFFD92D20)) }, onClick = { addMenu = false; onImportPdf() })
                    DropdownMenuItem(text = { Text(L.t("Fotoğraflardan defter")) }, leadingIcon = { Icon(Icons.Rounded.Image, null) }, onClick = { addMenu = false; onFromImages() })
                    DropdownMenuItem(text = { Text(L.t("Şablondan yeni defter")) }, leadingIcon = { Icon(Icons.Rounded.Description, null) }, onClick = { addMenu = false; showTemplates = true })
                    HorizontalDivider(color = UI.Border)
                    DropdownMenuItem(text = { Text(L.t("Yeni klasör")) }, leadingIcon = { Icon(Icons.Rounded.CreateNewFolder, null) }, onClick = { addMenu = false; newFolder = true })
                }
            }
            }
        }
        if (showTodos) Box(Modifier.padding(start = sideW + 1.dp)) {
            TodoPanel(todos, null, onOpen = { id, page -> showTodos = false; onOpenTodo(id, page) }, onClose = { showTodos = false })
        }
    }

    if (showNew) NameDialog(L.t("Yeni defter"), L.t("Defter adı"), "", L.t("Oluştur"),
        note = "Desen (kareli, izometrik...), kağıt rengi ve aralık defterin içindeki \"Sayfa\" panelinden değiştirilir.",
        onDismiss = { showNew = false }) { showNew = false; onCreate(it) }
    if (newFolder) NameDialog(L.t("Yeni klasör"), L.t("Klasör adı"), "", L.t("Oluştur"), onDismiss = { newFolder = false }) { newFolder = false; onCreateFolder(it) }
    folderRename?.let { f -> NameDialog(L.t("Klasörü yeniden adlandır"), L.t("Klasör adı"), f, L.t("Kaydet"), onDismiss = { folderRename = null }) { folderRename = null; onRenameFolder(f, it) } }
    folderDelete?.let { f ->
        ConfirmDialog(L.t("Klasörü sil"), "\"$f\" klasörü silinecek. İçindeki defterler silinmez, \"Tüm defterler\"de kalır.", L.t("Sil"), { onDeleteFolder(f) }, { folderDelete = null })
    }
    renameTarget?.let { nb -> NameDialog(L.t("Yeniden adlandır"), L.t("Defter adı"), nb.name, L.t("Kaydet"), onDismiss = { renameTarget = null }) { renameTarget = null; onRename(nb.id, it) } }
    deleteTarget?.let { nb ->
        ConfirmDialog(L.t("Defteri sil"), "\"${nb.name}\" çöp kutusuna taşınacak. 30 gün içinde geri getirebilirsiniz.", L.t("Sil"), { onDelete(nb.id) }, { deleteTarget = null })
    }
    lockTarget?.let { nb ->
        if (nb.locked) {
            PinDialog(L.t("Kilidi kaldır"), L.t("Defterin PIN kodunu girin"), onForgot = { lockTarget = null; onForgotPin(nb) }, onDismiss = { lockTarget = null }) { pin ->
                if (com.poyankee.note.data.Lock.check(nb.id, pin, nb.lockHash)) { onLock(nb.id, ""); lockTarget = null; true } else false
            }
        } else {
            PinDialog(L.t("Defteri kilitle"), L.t("4-8 haneli bir PIN belirleyin. Unutursanız cihazınızın ekran kilidiyle (desen, PIN, parmak izi) kilidi kaldırabilirsiniz."), confirmTwice = true, onDismiss = { lockTarget = null }) { pin ->
                onLock(nb.id, com.poyankee.note.data.Lock.hash(nb.id, pin)); lockTarget = null; true
            }
        }
    }
    if (bulkDelete) {
        val ids = selection ?: emptySet()
        ConfirmDialog(L.t("Defterleri sil"), "${ids.size} defter çöp kutusuna taşınacak. 30 gün içinde geri getirebilirsiniz.", L.t("Sil"),
            { ids.forEach(onDelete); selection = null }, { bulkDelete = false })
    }
    if (bulkMove) {
        val ids = selection ?: emptySet()
        MoveDialog("${ids.size} defter", null, folders, onDismiss = { bulkMove = false }, onCreateFolder = onCreateFolder) { f ->
            bulkMove = false; ids.forEach { onMove(it, f) }; selection = null
        }
    }
    if (showTemplates) {
        val templates = notebooks.filter { it.folder == TEMPLATE_FOLDER }
        AlertDialog(
            onDismissRequest = { showTemplates = false }, containerColor = UI.Surface,
            title = { Text(L.t("Şablondan yeni defter")) },
            text = {
                Column {
                    if (templates.isEmpty()) Text(
                        "Henüz şablon yok. Bir defterin ⋯ menüsünden \"Şablon olarak kaydet\"i seçin; sayfa düzeni ve içindekiler şablon olur (\"$TEMPLATE_FOLDER\" klasöründe durur).",
                        color = UI.TextSoft, fontSize = 14.sp,
                    )
                    templates.forEach { t -> FolderRow(t.name.removePrefix(L.t("Şablon: ")).removePrefix("Şablon: "), false) { showTemplates = false; onFromTemplate(t.id) } }
                }
            },
            confirmButton = { TextButton(onClick = { showTemplates = false }) { Text(L.t("Kapat")) } },
        )
    }
    if (confirmEmpty) ConfirmDialog(L.t("Çöp kutusunu boşalt"), "Çöp kutusundaki ${trash.size} defter kalıcı olarak silinecek. Bu geri alınamaz.", L.t("Sil"), onEmptyTrash) { confirmEmpty = false }
    purgeTarget?.let { nb ->
        ConfirmDialog(L.t("Kalıcı olarak sil"), "\"${nb.name}\" kalıcı olarak silinecek. Bu geri alınamaz.", L.t("Sil"), { onPurge(nb.id) }) { purgeTarget = null }
    }
    folderColorFor?.let { f ->
        FolderColorDialog(f, settings.folderColor(f), onDismiss = { folderColorFor = null }) { c -> settings.setFolderColor(f, c); folderColorFor = null }
    }
    moveTarget?.let { nb ->
        MoveDialog("\"${nb.name}\"", nb.folder, folders, onDismiss = { moveTarget = null }, onCreateFolder = onCreateFolder) { f -> moveTarget = null; onMove(nb.id, f) }
    }
    if (busy != null) {
        AlertDialog(
            onDismissRequest = {}, confirmButton = {}, containerColor = UI.Surface,
            text = { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(28.dp)); Spacer(Modifier.width(16.dp)); Text(busy) } },
        )
    }
}

/** Sol şeritte simge + kısa yazı. */
@Composable
private fun RailItem(icon: ImageVector, text: String, selected: Boolean, badge: Int = 0, onClick: () -> Unit) {
    Column(
        Modifier.width(84.dp).padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp))
            .background(if (selected) UI.ToolSelBg else Color.Transparent).clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Icon(icon, null, tint = if (selected) UI.ToolSelFg else UI.BarFgSoft, modifier = Modifier.size(24.dp))
            if (badge > 0) Box(
                Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-6).dp).clip(CircleShape).background(UI.Primary).padding(horizontal = 5.dp, vertical = 1.dp),
            ) { Text("$badge", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(4.dp))
        Text(text, fontSize = 12.sp, color = if (selected) UI.ToolSelFg else UI.BarFgSoft, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/** Klasöre renk verilmemişse adından sabit, hoş bir renk. */
fun folderAutoColor(name: String): Int {
    val c = listOf(0xFF8B5CF6, 0xFF3B82F6, 0xFFEF4444, 0xFF22C55E, 0xFFF59E0B, 0xFF14B8A6, 0xFFEC4899)
    return c[(name.hashCode() and 0x7fffffff) % c.size].toInt()
}

/** Filtre çipi (defterlerin üstünde). */
@Composable
private fun FolderTab(icon: ImageVector?, text: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.height(44.dp).clip(RoundedCornerShape(22.dp))
            .background(if (selected) UI.Primary else UI.Surface)
            .border(1.dp, if (selected) UI.Primary else UI.Border, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = if (selected) Color.White else UI.Text, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = if (selected) Color.White else UI.Text, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
        Spacer(Modifier.width(10.dp))
        Text("$count", color = if (selected) Color.White else UI.TextSoft, fontSize = 15.sp)
    }
}

/** Beyaz, köşeleri yuvarlak kare düğme (ızgara/liste, filtre). */
@Composable
private fun SquareButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(50.dp).shadow(2.dp, RoundedCornerShape(15.dp), ambientColor = UI.Shadow, spotColor = UI.Shadow)
            .clip(RoundedCornerShape(15.dp)).background(UI.Surface).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = UI.Text, modifier = Modifier.size(24.dp)) }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun NavItem(icon: ImageVector, text: String, count: Int?, selected: Boolean, onLong: (() -> Unit)? = null, iconTint: Color? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(16.dp))
            .background(if (selected) UI.PrimarySoft else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLong)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = iconTint ?: if (selected) UI.Primary else UI.Text, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(18.dp))
        Text(text, color = UI.Text, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (count != null) Text("$count", color = if (selected) UI.Text else UI.TextSoft, fontSize = 16.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun SearchField(value: String, modifier: Modifier, onChange: (String) -> Unit) {
    Row(
        modifier.height(50.dp).shadow(2.dp, RoundedCornerShape(25.dp), ambientColor = UI.Shadow, spotColor = UI.Shadow)
            .clip(RoundedCornerShape(25.dp)).background(UI.Surface).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = UI.TextSoft, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(L.t("Notlarda ara..."), color = UI.TextFaint, fontSize = 16.sp, maxLines = 1)
            BasicTextField(value, onChange, singleLine = true, textStyle = TextStyle(fontSize = 16.sp, color = UI.Text), modifier = Modifier.fillMaxWidth())
        }
        if (value.isNotEmpty()) Icon(Icons.Rounded.Close, L.t("Temizle"), tint = UI.TextSoft, modifier = Modifier.size(20.dp).clip(CircleShape).clickable { onChange("") })
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

private class CardActions(
    val onOpen: () -> Unit, val onLongPress: () -> Unit, val onRename: () -> Unit, val onMove: () -> Unit, val onDelete: () -> Unit,
    val onFavorite: () -> Unit, val onLock: () -> Unit, val onTemplate: () -> Unit = {},
)

@Composable
private fun rememberCover(nb: NotebookInfo): ImageBitmap? {
    val cover by produceState<ImageBitmap?>(null, nb.cover, nb.coverStamp) {
        value = nb.cover?.let { f -> withContext(Dispatchers.IO) { try { BitmapFactory.decodeFile(f.path)?.asImageBitmap() } catch (_: Throwable) { null } } }
    }
    return if (nb.locked) null else cover
}

@Composable
private fun NotebookMenu(nb: NotebookInfo, open: Boolean, onClose: () -> Unit, a: CardActions) {
    DropdownMenu(open, onClose) {
        DropdownMenuItem(text = { Text(L.t("Aç")) }, onClick = { onClose(); a.onOpen() })
        DropdownMenuItem(text = { Text(L.t("Seç")) }, onClick = { onClose(); a.onLongPress() })
        DropdownMenuItem(text = { Text(L.t("Yeniden adlandır")) }, onClick = { onClose(); a.onRename() })
        DropdownMenuItem(text = { Text(L.t("Klasöre taşı")) }, onClick = { onClose(); a.onMove() })
        DropdownMenuItem(text = { Text(if (nb.favorite) L.t("Favorilerden çıkar") else L.t("Favorilere ekle")) }, onClick = { onClose(); a.onFavorite() })
        DropdownMenuItem(text = { Text(if (nb.locked) L.t("Kilidi kaldır") else L.t("Kilitle")) }, onClick = { onClose(); a.onLock() })
        if (!nb.locked) DropdownMenuItem(text = { Text(L.t("Şablon olarak kaydet")) }, onClick = { onClose(); a.onTemplate() })
        DropdownMenuItem(text = { Text(L.t("Sil"), color = UI.Danger) }, onClick = { onClose(); a.onDelete() })
    }
}

@Composable
private fun SelectMark(selected: Boolean, modifier: Modifier) {
    Box(
        modifier.size(28.dp).clip(CircleShape).background(if (selected) UI.Primary else Color(0xCCFFFFFF)).border(2.dp, if (selected) UI.Primary else Color(0xFF98A2B3), CircleShape),
        contentAlignment = Alignment.Center,
    ) { if (selected) Icon(Icons.Rounded.Check, L.t("Seçili"), tint = Color.White, modifier = Modifier.size(18.dp)) }
}

/** Liste görünümünde bir satır. [selected] null = seçim modu kapalı. */
@Composable
private fun NotebookRow(nb: NotebookInfo, selected: Boolean?, folderColor: Color?, a: CardActions, current: Boolean = false) {
    var menu by remember { mutableStateOf(false) }
    val img = rememberCover(nb)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp)
            .shadow(3.dp, RoundedCornerShape(16.dp), ambientColor = UI.Shadow, spotColor = UI.Shadow)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected == true) UI.PrimarySoft else UI.Surface)
            .border(if (current) 2.dp else 1.dp, if (selected == true || current) UI.Primary else UI.Border.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .combinedClickable(onClick = a.onOpen, onLongClick = a.onLongPress).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected != null) { SelectMark(selected, Modifier); Spacer(Modifier.width(12.dp)) }
        Box(Modifier.width(56.dp).height(72.dp).clip(RoundedCornerShape(10.dp)).background(UI.SurfaceAlt).border(1.dp, UI.Border.copy(alpha = 0.5f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            when {
                nb.locked -> Icon(Icons.Rounded.Lock, null, tint = UI.TextSoft, modifier = Modifier.size(22.dp))
                img != null -> Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                nb.isPdf -> Icon(Icons.Rounded.PictureAsPdf, null, tint = Color(0xFFD92D20), modifier = Modifier.size(24.dp))
                else -> Text(nb.name.take(1).uppercase(), color = UI.TextSoft, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(nb.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = UI.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (nb.folder.isNotEmpty()) {
                    Icon(Icons.Rounded.Folder, null, tint = folderColor ?: Color(folderAutoColor(nb.folder)), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(nb.folder + " · ", fontSize = 13.sp, color = UI.TextSoft, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp))
                }
                Text("${nb.pageCount} " + L.t("sayfa") + (if (nb.isPdf) " · PDF" else ""), fontSize = 13.sp, color = UI.TextSoft, maxLines = 1)
            }
        }
        Text(dateFmt.format(Date(nb.updated)), fontSize = 12.sp, color = UI.TextFaint, maxLines = 1)
        if (selected == null) {
            IconButton(onClick = a.onFavorite) {
                Icon(if (nb.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, L.t("Favori"), tint = if (nb.favorite) Color(0xFFF59E0B) else UI.TextFaint)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreHoriz, L.t("Menü"), tint = UI.TextSoft) }
                NotebookMenu(nb, menu, { menu = false }, a)
            }
        }
    }
}

@Composable
private fun NotebookCard(nb: NotebookInfo, selected: Boolean?, a: CardActions, current: Boolean = false) {
    var menu by remember { mutableStateOf(false) }
    val img = rememberCover(nb)
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().shadow(4.dp, shape, ambientColor = UI.Shadow, spotColor = UI.Shadow).clip(shape).background(UI.Surface)
            .border(if (current || selected == true) 2.dp else 0.dp, if (current || selected == true) UI.Primary else Color.Transparent, shape)
            .combinedClickable(onClick = a.onOpen, onLongClick = a.onLongPress).padding(10.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.12f).clip(RoundedCornerShape(14.dp)).background(UI.CardThumb)) {
            when {
                nb.locked -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Lock, L.t("Kilitli"), tint = UI.TextSoft, modifier = Modifier.size(34.dp))
                }
                img != null -> Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
                nb.isPdf -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PictureAsPdf, null, tint = Color(0xFFD92D20).copy(alpha = 0.7f), modifier = Modifier.size(40.dp))
                }
                else -> {}
            }
            if (nb.isPdf) {
                Surface(color = Color(0xFFD92D20), shape = RoundedCornerShape(8.dp), modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                    Text("PDF", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                }
            }
            if (selected != null) SelectMark(selected, Modifier.align(Alignment.TopEnd).padding(8.dp))
            else {
                Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).clickable { menu = true },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.MoreHoriz, L.t("Menü"), tint = UI.Text, modifier = Modifier.size(24.dp)) }
                    NotebookMenu(nb, menu, { menu = false }, a)
                }
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(6.dp).size(32.dp).clip(CircleShape).background(UI.Surface.copy(alpha = 0.85f)).clickable { a.onFavorite() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (nb.favorite) Icons.Rounded.Star else Icons.Rounded.StarOutline, L.t("Favori"),
                        tint = if (nb.favorite) Color(0xFFF59E0B) else UI.TextSoft, modifier = Modifier.size(22.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(nb.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = UI.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
        Text("${nb.pageCount} " + L.t("sayfa") + "  ·  " + shortDate.format(Date(nb.updated)), fontSize = 14.sp, color = UI.TextSoft, maxLines = 1,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 4.dp))
    }
}

private val shortDate = SimpleDateFormat("d MMM", Locale.forLanguageTag("tr"))

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
        dismissButton = { TextButton(onClick = onDismiss) { Text(L.t("Vazgeç"), color = UI.TextSoft) } },
    )
}

@Composable
private fun MoveDialog(what: String, current: String?, folders: List<String>, onDismiss: () -> Unit, onCreateFolder: (String) -> Unit, onPick: (String) -> Unit) {
    var creating by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UI.Surface,
        title = { Text(L.t("Klasöre taşı")) },
        text = {
            Column {
                Text(what, color = UI.TextSoft, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                FolderRow(L.t("Klasörsüz"), current == "") { onPick("") }
                folders.forEach { f -> FolderRow(f, current == f) { onPick(f) } }
                TextButton(onClick = { creating = true }) { Icon(Icons.Rounded.CreateNewFolder, null); Spacer(Modifier.width(6.dp)); Text(L.t("Yeni klasör")) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(L.t("Kapat"), color = UI.TextSoft) } },
    )
    if (creating) NameDialog(L.t("Yeni klasör"), L.t("Klasör adı"), "", L.t("Oluştur ve taşı"), onDismiss = { creating = false }) {
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

/** Klasör rengi seçimi. */
@Composable
private fun FolderColorDialog(name: String, current: Int?, onDismiss: () -> Unit, onPick: (Int?) -> Unit) {
    val colors = listOf(0xFF6366F1, 0xFF1565C0, 0xFF14B8A6, 0xFF2E7D32, 0xFFF59E0B, 0xFFEA580C, 0xFFD32F2F, 0xFFEC4899, 0xFF6A1B9A, 0xFF64748B).map { it.toInt() }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = UI.Surface,
        title = { Text("\"$name\" rengi") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { colors.take(5).forEach { c -> ColorDot(c, current == c, 40) { onPick(c) } } }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { colors.drop(5).forEach { c -> ColorDot(c, current == c, 40) { onPick(c) } } }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(null) }) { Text(L.t("Renksiz")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(L.t("Vazgeç"), color = UI.TextSoft) } },
    )
}

/** Çöp kutusu: silinen defterler 30 gün sonra kendiliğinden silinir. */
@Composable
private fun TrashView(items: List<Pair<NotebookInfo, Long>>, onRestore: (String) -> Unit, onPurge: (NotebookInfo) -> Unit, onEmpty: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 26.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(L.t("Çöp kutusu"), style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp), color = UI.Text)
                Text(L.t("Silinen defterler 30 gün sonra kendiliğinden silinir"), color = UI.TextSoft, fontSize = 14.sp)
            }
            if (items.isNotEmpty()) TextButton(onClick = onEmpty) { Text(L.t("Çöp kutusunu boşalt"), color = UI.Danger) }
        }
        if (items.isEmpty()) {
            EmptyState(L.t("Çöp kutusu boş"), L.t("Sildiğiniz defterler burada 30 gün bekler; bu sürede geri getirebilirsiniz."))
            return
        }
        LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp), modifier = Modifier.fillMaxSize()) {
            items(items, key = { it.first.id }) { (nb, at) ->
                val daysLeft = (30 - ((System.currentTimeMillis() - at) / (24L * 60 * 60 * 1000))).coerceAtLeast(0)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(UI.Surface).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (nb.isPdf) Icons.Rounded.PictureAsPdf else Icons.Rounded.LibraryBooks, null, tint = UI.TextSoft, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(nb.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = UI.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${nb.pageCount} sayfa · silindi: ${dateFmt.format(Date(at))} · $daysLeft gün kaldı", fontSize = 13.sp, color = UI.TextSoft, maxLines = 1)
                    }
                    TextButton(onClick = { onRestore(nb.id) }) { Text(L.t("Geri getir")) }
                    TextButton(onClick = { onPurge(nb) }) { Text(L.t("Kalıcı sil"), color = UI.Danger) }
                }
            }
        }
    }
}

/** Şablon defterlerin durduğu klasör. */
const val TEMPLATE_FOLDER = "Şablonlar"

/** Kenar çubuğunda "Çöp kutusu" seçimi için özel değer. */
const val TRASH = "\u0000cop"

/** Kenar çubuğunda "Favoriler" seçimi için özel değer. */
const val FAVORITES = "\u0000favoriler"

private fun matches(nb: NotebookInfo, folder: String?) = when (folder) {
    null -> true
    FAVORITES -> nb.favorite
    else -> nb.folder == folder
}

/** Yanlış PIN denemelerini sınırlar: 5 yanlıştan sonra giderek artan bekleme. */
object PinGuard {
    private var failures = 0
    private var lockedUntil = 0L
    fun waitSeconds(): Int = ((lockedUntil - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
    fun fail() {
        failures++
        if (failures >= 5) lockedUntil = System.currentTimeMillis() + 30_000L * (1L shl (failures - 5).coerceAtMost(6))
    }
    fun success() { failures = 0; lockedUntil = 0L }
}

/** PIN girişi. [onPin] true dönerse pencere kapanır, false ise "yanlış PIN" gösterilir. */
@Composable
fun PinDialog(title: String, text: String, confirmTwice: Boolean = false, onForgot: (() -> Unit)? = null, onDismiss: () -> Unit, onPin: (String) -> Boolean) {
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
                        pin2, { pin2 = it.filter { c -> c.isDigit() }.take(8); error = null }, label = { Text(L.t("PIN (tekrar)")) }, singleLine = true,
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
                val wait = PinGuard.waitSeconds()
                when {
                    !confirmTwice && wait > 0 -> error = "Çok fazla yanlış deneme. $wait saniye sonra tekrar deneyin."
                    pin.length < 4 -> error = L.t("En az 4 hane")
                    confirmTwice && pin != pin2 -> error = L.t("PIN'ler aynı değil")
                    !onPin(pin) -> { if (!confirmTwice) PinGuard.fail(); error = L.t("PIN yanlış") }
                    else -> if (!confirmTwice) PinGuard.success()
                }
            }, shape = RoundedCornerShape(10.dp)) { Text(L.t("Tamam")) }
        },
        dismissButton = {
            Row {
                if (onForgot != null) TextButton(onClick = onForgot) { Text(L.t("PIN'i unuttum"), color = UI.TextSoft) }
                TextButton(onClick = onDismiss) { Text(L.t("Vazgeç"), color = UI.TextSoft) }
            }
        },
    )
}
