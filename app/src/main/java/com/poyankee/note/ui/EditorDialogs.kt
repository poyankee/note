@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.poyankee.note.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.poyankee.note.core.Background
import com.poyankee.note.core.Page
import com.poyankee.note.core.Template
import com.poyankee.note.editor.EditorController
import com.poyankee.note.editor.Exporter
import com.poyankee.note.editor.InkView
import com.poyankee.note.editor.InstrumentKind
import com.poyankee.note.editor.LessonRecorder
import com.poyankee.note.editor.MarkKind
import com.poyankee.note.editor.SecondScreen
import com.poyankee.note.editor.Tool
import com.poyankee.note.render.PageRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

@Composable
fun GotoDialog(ctl: EditorController, onDismiss: () -> Unit) {
    val nb = ctl.nb
    val hasPdf = nb.pages.any { it.background is Background.PdfPage }
    var number by remember { mutableStateOf("") }
    var bookMode by remember { mutableStateOf(hasPdf && nb.pageOffset != 0) }
    var error by remember { mutableStateOf<String?>(null) }
    var showOffset by remember { mutableStateOf(false) }
    var offsetInput by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(150); try { focus.requestFocus() } catch (_: Exception) {} }
    fun go() {
        val n = number.trim().toIntOrNull()
        if (n == null) { error = "Bir sayı yazın"; return }
        if (bookMode) {
            if (ctl.goToBookPage(n)) onDismiss() else error = "Kitapta $n. sayfa bulunamadı"
        } else {
            if (n in 1..nb.pages.size) { ctl.goToPage(n - 1); onDismiss() } else error = "1 ile ${nb.pages.size} arasında bir sayı yazın"
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sayfaya git") },
        text = {
            Column {
                OutlinedTextField(
                    number, { number = it.filter { ch -> ch.isDigit() }; error = null },
                    label = { Text(if (bookMode) "Kitaptaki sayfa numarası" else "Sayfa numarası (1 - ${nb.pages.size})") },
                    singleLine = true, isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { go() }, onGo = { go() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                error?.let { Text(it, color = Color(0xFFC62828), fontSize = 13.sp) }
                if (hasPdf) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(!bookMode, { bookMode = false }, { Text("Defter / PDF sayfası") })
                        FilterChip(bookMode, { bookMode = true }, { Text("Kitap sayfası") })
                    }
                    Text(
                        "Kitabın sayfa numarası PDF'in sayfa sırasından farklıysa (kapak, önsöz...), aşağıdan bir kez ayarlayın.",
                        fontSize = 12.sp, color = Color(0xFF607D8B), modifier = Modifier.padding(top = 6.dp),
                    )
                    TextButton(onClick = { showOffset = !showOffset }) { Text("Kitap sayfa numarasını ayarla") }
                    if (showOffset) {
                        val cur = nb.pages.getOrNull(ctl.currentPage)
                        if (cur?.background is Background.PdfPage) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    offsetInput, { offsetInput = it.filter { ch -> ch.isDigit() || ch == '-' } },
                                    label = { Text("Şu an açık sayfa kitapta kaçıncı sayfa?") }, singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = {
                                    val b = offsetInput.toIntOrNull()
                                    if (b != null && ctl.setBookNumberForCurrent(b)) { showOffset = false; bookMode = true; error = null }
                                }) { Text("Ayarla") }
                            }
                        } else Text("Önce bir PDF sayfasına gidin.", fontSize = 12.sp)
                    }
                }
                val bms = ctl.nb.bookmarks
                if (bms.isNotEmpty()) {
                    Text("Yer imleri", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        bms.forEach { b ->
                            val idx = nb.indexOfPage(b.pageId)
                            if (idx >= 0) FilterChip(false, { ctl.goToPage(idx); onDismiss() }, { Text(b.title) })
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { go() }) { Text("Git") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@Composable
fun BookmarksDialog(ctl: EditorController, onDismiss: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = ctl.bookmarksVersion
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var toc by remember { mutableStateOf<List<Triple<String, Int, Int>>?>(null) }
    val pdfFile = ctl.nb.pdfFile?.let { File(ctl.store.dir(ctl.nb.id), it) }
    LaunchedEffect(tab) {
        if (tab == 1 && toc == null && pdfFile != null) {
            toc = withContext(Dispatchers.IO) { try { Exporter.outline(context, pdfFile) } catch (_: Throwable) { emptyList() } }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yer imleri") },
        text = {
            Column {
                if (pdfFile != null) {
                    TabRow(selectedTabIndex = tab) {
                        Tab(tab == 0, { tab = 0 }, text = { Text("Yer imlerim") })
                        Tab(tab == 1, { tab = 1 }, text = { Text("PDF içindekiler") })
                    }
                }
                if (tab == 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(title, { title = it }, label = { Text("Bu sayfaya yer imi (ör. Türev, Test 3)") }, singleLine = true, modifier = Modifier.weight(1f))
                        TextButton(onClick = { ctl.addBookmark(title); title = "" }) { Text("Ekle") }
                    }
                    if (ctl.nb.bookmarks.isEmpty()) Text("Henüz yer imi yok.", color = Color(0xFF78909C), modifier = Modifier.padding(8.dp))
                    LazyColumn(Modifier.heightIn(max = 420.dp)) {
                        items(ctl.nb.bookmarks.toList()) { b ->
                            val idx = ctl.nb.indexOfPage(b.pageId)
                            Row(
                                Modifier.fillMaxWidth().combinedClickable(onClick = { if (idx >= 0) { ctl.goToPage(idx); onDismiss() } }).padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Bookmark, null, tint = Accent)
                                Spacer(Modifier.width(8.dp))
                                Text(b.title, Modifier.weight(1f))
                                Text("s. ${idx + 1}", color = Color(0xFF78909C))
                                IconButton(onClick = { ctl.removeBookmark(b) }) { Icon(Icons.Filled.Delete, "Sil") }
                            }
                        }
                    }
                } else {
                    val list = toc
                    when {
                        list == null -> CircularProgressIndicator(Modifier.padding(16.dp))
                        list.isEmpty() -> Text("Bu PDF'te içindekiler bilgisi yok.", color = Color(0xFF78909C), modifier = Modifier.padding(8.dp))
                        else -> LazyColumn(Modifier.heightIn(max = 460.dp)) {
                            items(list) { (t, pdfIdx, level) ->
                                val idx = ctl.nb.pages.indexOfFirst { (it.background as? Background.PdfPage)?.index == pdfIdx }
                                Row(
                                    Modifier.fillMaxWidth().combinedClickable(onClick = { if (idx >= 0) { ctl.goToPage(idx); onDismiss() } })
                                        .padding(start = (level * 16).dp, top = 7.dp, bottom = 7.dp),
                                ) {
                                    Text(t, Modifier.weight(1f), fontWeight = if (level == 0) FontWeight.SemiBold else FontWeight.Normal)
                                    Text("${pdfIdx + 1}", color = Color(0xFF78909C))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } },
    )
}

@Composable
fun ExportDialog(ctl: EditorController, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var what by remember { mutableIntStateOf(0) } // 0 tüm defter pdf, 1 bu sayfa pdf, 2 bu sayfa png
    var progress by remember { mutableStateOf<Float?>(null) }
    var pending by remember { mutableStateOf<File?>(null) }
    val baseName = ctl.nb.name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(60).ifBlank { "Note" }

    suspend fun build(): File? = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "export").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val cur = ctl.nb.pages.getOrNull(ctl.currentPage) ?: return@withContext null
        try {
            if (what == 2) {
                val f = File(dir, "$baseName - s${ctl.currentPage + 1}.png")
                val snap = Page(cur.id, cur.background, cur.width, cur.height).also { it.items.addAll(cur.items) }
                val scale = 3f
                val pdfBmp: Bitmap? = (snap.background as? Background.PdfPage)?.let { bg ->
                    val pdf = ctl.pdf ?: return@let null
                    suspendCancellableCoroutine { cont -> pdf.runOnPdfThread { cont.resume(pdf.renderBlocking(bg, scale)) } }
                }
                val bmp = PageRenderer { ctl.loadImageBlocking(it) }.renderPage(snap, scale, pdfBmp)
                FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bmp.recycle(); pdfBmp?.recycle()
                f
            } else {
                val f = File(dir, if (what == 1) "$baseName - s${ctl.currentPage + 1}.pdf" else "$baseName.pdf")
                val pages = if (what == 1) listOf(cur) else ctl.nb.pages.toList()
                val snaps = pages.map { p -> Page(p.id, p.background, p.width, p.height).also { it.items.addAll(p.items) } }
                Exporter.export(context, ctl.nb, ctl.store.dir(ctl.nb.id), snaps, f, { ctl.loadImageBlocking(it) }) { d, t ->
                    progress = d.toFloat() / t
                }
                f
            }
        } catch (e: Throwable) {
            withContext(Dispatchers.Main) { ctl.message = "Dışa aktarılamadı: ${e.message}" }
            null
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val f = pending
        if (uri != null && f != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out -> f.inputStream().use { it.copyTo(out) } }
                    withContext(Dispatchers.Main) { ctl.message = "Kaydedildi" }
                } catch (e: Exception) { withContext(Dispatchers.Main) { ctl.message = "Kaydedilemedi: ${e.message}" } }
            }
        }
        onDismiss()
    }

    fun run(share: Boolean) {
        progress = 0f
        scope.launch {
            val f = build()
            progress = null
            if (f == null) return@launch
            if (share) {
                val uri = FileProvider.getUriForFile(context, context.packageName + ".files", f)
                val i = Intent(Intent.ACTION_SEND).apply {
                    type = if (f.name.endsWith(".png")) "image/png" else "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(i, "Paylaş"))
                onDismiss()
            } else {
                pending = f
                saveLauncher.launch(f.name)
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (progress == null) onDismiss() },
        title = { Text("Dışa aktar") },
        text = {
            Column {
                listOf("Tüm defter (PDF)", "Bu sayfa (PDF)", "Bu sayfa (resim, PNG)").forEachIndexed { i, n ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (what == i) Color(0xFFD6E4F5) else Color.Transparent)
                            .combinedClickable(onClick = { what = i }).padding(10.dp),
                    ) { Text(n) }
                }
                Text(
                    "Yazılarınız PDF'e vektör olarak eklenir: yakınlaştırınca bulanıklaşmaz, orijinal PDF bozulmaz.",
                    fontSize = 12.sp, color = Color(0xFF607D8B), modifier = Modifier.padding(top = 8.dp),
                )
                progress?.let {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Row {
                OutlinedButton(onClick = { run(false) }, enabled = progress == null) { Text("Dosyaya kaydet") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { run(true) }, enabled = progress == null) { Text("Paylaş") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = progress == null) { Text("Kapat") } },
    )
}
