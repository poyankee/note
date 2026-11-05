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

@Suppress("DEPRECATION")
@Composable
fun EditorScreen(ctl: EditorController, onClose: () -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val s = ctl.settings
    val ink = remember { InkView(context, ctl).also { ctl.view = it; it.keepScreenOn = true } }
    val scope = rememberCoroutineScope()

    var secondActive by remember { mutableStateOf(false) }
    val second = remember { SecondScreen(activity, ink, { s.secondScreen.value }) { secondActive = it; ctl.secondScreenActive = it } }
    DisposableEffect(Unit) {
        second.start()
        onDispose { second.stop() }
    }
    LaunchedEffect(s.secondScreen.value) { second.update() }

    // tam ekran (sunum modu)
    LaunchedEffect(ctl.presentation) {
        val c = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (ctl.presentation) {
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.systemBars())
        } else c.show(WindowInsetsCompat.Type.systemBars())
    }

    // ders kaydı
    val recorder = remember { LessonRecorder(context, ink) }
    var recState by remember { mutableIntStateOf(0) } // 0 yok, 1 kayıt, 2 duraklatıldı
    var recTime by remember { mutableStateOf("00:00") }
    LaunchedEffect(recState) {
        while (recState != 0) {
            val sec = recorder.elapsed() / 1000
            recTime = "%02d:%02d".format(sec / 60, sec % 60)
            delay(500)
        }
    }
    fun startRecording(audio: Boolean) {
        try { recorder.start(audio); recState = 1 } catch (e: Exception) { ctl.message = "Kayıt başlatılamadı: ${e.message}" }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) ctl.message = "Mikrofon izni verilmedi; video sessiz kaydedilecek."
        startRecording(granted)
    }
    fun toggleRecord() {
        if (recState != 0) return
        if (s.recordAudio.value && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        } else startRecording(s.recordAudio.value)
    }
    fun stopRecording() {
        val name = recorder.stop()
        recState = 0
        ctl.message = if (name != null) "Video kaydedildi: Galeri › Movies/Note › $name" else "Video kaydedilemedi"
    }
    DisposableEffect(Unit) { onDispose { if (recorder.isRecording) recorder.stop() } }

    // görsel ekleme
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) ctl.insertImage(uri) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) cameraUri?.let { ctl.insertImage(it) } }
    fun openCamera() {
        val f = File(context.cacheDir, "kamera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", f)
        cameraUri = uri
        try { takePhoto.launch(uri) } catch (_: Exception) { ctl.message = "Kamera açılamadı" }
    }

    var showGoto by remember { mutableStateOf(false) }
    var showPages by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showTemplate by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }

    BackHandler {
        when {
            ctl.presentation -> ctl.presentation = false
            else -> onClose()
        }
    }

    val toolsLeft = !s.leftHanded.value
    Column(Modifier.fillMaxSize().background(Color(0xFFDADDE2))) {
        if (!ctl.presentation) {
            TopBar(
                ctl, secondActive, recState, recTime,
                onClose = onClose,
                onGoto = { showGoto = true }, onPages = { showPages = true }, onBookmarks = { showBookmarks = true },
                onExport = { showExport = true }, onTemplate = { showTemplate = true }, onClear = { showClear = true },
                onImage = { pickImage.launch("image/*") }, onCamera = { openCamera() }, onSettings = onSettings,
                onRecord = { toggleRecord() },
                onPauseResume = { if (recState == 1) { recorder.pause(); recState = 2 } else if (recState == 2) { recorder.resume(); recState = 1 } },
                onStopRecord = { stopRecording() },
            )
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
            if (!ctl.presentation && toolsLeft) ToolRail(ctl)
            Box(Modifier.weight(1f).fillMaxHeight()) {
                AndroidView(factory = { ink }, modifier = Modifier.fillMaxSize())
                ctl.hint?.let { h ->
                    if (ctl.tool.isGeometry) {
                        Surface(
                            color = Color(0xE6263238), shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.TopCenter).padding(10.dp),
                        ) { Text(h, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 13.sp) }
                    }
                }
                if (ctl.hasSelection) SelectionBar(ctl, Modifier.align(Alignment.BottomCenter).padding(16.dp))
                if (ctl.presentation) {
                    PresentationBar(
                        ctl, recState, recTime,
                        onStopRecord = { stopRecording() },
                        modifier = Modifier.align(if (toolsLeft) Alignment.BottomStart else Alignment.BottomEnd).padding(12.dp),
                    )
                }
                ctl.message?.let { m ->
                    LaunchedEffect(m) { delay(3500); if (ctl.message == m) ctl.message = null }
                    Surface(
                        color = Color(0xEE263238), shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
                    ) { Text(m, color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) }
                }
            }
            if (!ctl.presentation && !toolsLeft) ToolRail(ctl)
        }
    }

    if (showGoto) GotoDialog(ctl) { showGoto = false }
    if (showPages) PagesDialog(ctl) { showPages = false }
    if (showBookmarks) BookmarksDialog(ctl) { showBookmarks = false }
    if (showExport) ExportDialog(ctl) { showExport = false }
    if (showTemplate) TemplateDialog(ctl) { showTemplate = false }
    if (showClear) ConfirmDialog("Sayfayı temizle", "Bu sayfadaki tüm yazılar silinecek. (Geri al ile geri getirebilirsiniz.)", "Temizle", { ctl.clearPage() }, { showClear = false })
    ctl.textRequest?.let { TextDialog(ctl, it) }
}

/* ---------------- üst çubuk ---------------- */

@Suppress("DEPRECATION")
@Composable
private fun TopBar(
    ctl: EditorController, secondActive: Boolean, recState: Int, recTime: String,
    onClose: () -> Unit, onGoto: () -> Unit, onPages: () -> Unit, onBookmarks: () -> Unit, onExport: () -> Unit,
    onTemplate: () -> Unit, onClear: () -> Unit, onImage: () -> Unit, onCamera: () -> Unit, onSettings: () -> Unit,
    onRecord: () -> Unit, onPauseResume: () -> Unit, onStopRecord: () -> Unit,
) {
    val white = Color.White
    var more by remember { mutableStateOf(false) }
    var insert by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(52.dp).background(Ink).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) { Icon(Icons.Filled.ArrowBack, "Defterler", tint = white) }
        Text(
            ctl.nb.name, color = white, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 220.dp),
        )
        Spacer(Modifier.width(10.dp))
        // sayfa göstergesi: dokun → sayfa numarasına git
        val pagesVersion = ctl.pagesVersion
        Surface(
            color = Color(0x33FFFFFF), shape = RoundedCornerShape(16.dp),
            modifier = Modifier.clip(RoundedCornerShape(16.dp)).combinedClickable(onClick = onGoto),
        ) {
            Text(ctl.pageLabel(ctl.currentPage) + (if (pagesVersion < 0) "" else ""), color = white, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp), fontSize = 14.sp)
        }
        if (ctl.history.isNotEmpty()) {
            TextButton(onClick = { ctl.goBack() }) { Text("‹ Geri dön (${ctl.history.last() + 1})", color = Color(0xFF90CAF9)) }
        }
        Spacer(Modifier.weight(1f))
        if (secondActive) Icon(Icons.Filled.Cast, "Projeksiyon bağlı", tint = Color(0xFF80CBC4), modifier = Modifier.padding(horizontal = 6.dp))
        if (recState != 0) {
            Surface(color = Color(0xFFC62828), shape = RoundedCornerShape(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 10.dp)) {
                    Text(if (recState == 1) "● $recTime" else "❚❚ $recTime", color = white, fontSize = 14.sp)
                    IconButton(onClick = onPauseResume, modifier = Modifier.size(36.dp)) {
                        Icon(if (recState == 1) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Duraklat / devam", tint = white)
                    }
                    IconButton(onClick = onStopRecord, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Stop, "Kaydı bitir", tint = white) }
                }
            }
        } else {
            IconButton(onClick = onRecord) { Icon(Icons.Filled.FiberManualRecord, "Ders kaydı başlat", tint = Color(0xFFEF5350)) }
        }
        IconButton(onClick = { ctl.undo() }, enabled = ctl.canUndo) { Icon(Icons.Filled.Undo, "Geri al", tint = if (ctl.canUndo) white else Color(0x66FFFFFF)) }
        IconButton(onClick = { ctl.redo() }, enabled = ctl.canRedo) { Icon(Icons.Filled.Redo, "Yinele", tint = if (ctl.canRedo) white else Color(0x66FFFFFF)) }
        IconButton(onClick = { ctl.insertPageAfter(ctl.currentPage) }) { Icon(Icons.Filled.Add, "Sonrasına sayfa ekle", tint = white) }
        IconButton(onClick = onPages) { Icon(Icons.Filled.ViewModule, "Sayfalar", tint = white) }
        IconButton(onClick = onBookmarks) { Icon(Icons.Filled.Bookmark, "Yer imleri ve içindekiler", tint = white) }
        Box {
            IconButton(onClick = { insert = true }) { Icon(Icons.Filled.Image, "Görsel ekle", tint = white) }
            DropdownMenu(insert, { insert = false }) {
                DropdownMenuItem(text = { Text("Galeriden görsel") }, leadingIcon = { Icon(Icons.Filled.Image, null) }, onClick = { insert = false; onImage() })
                DropdownMenuItem(text = { Text("Kameradan fotoğraf") }, leadingIcon = { Icon(Icons.Filled.CameraAlt, null) }, onClick = { insert = false; onCamera() })
            }
        }
        IconButton(onClick = { ctl.view?.clearSelection(); ctl.presentation = true }) { Icon(Icons.Filled.Fullscreen, "Sunum modu", tint = white) }
        IconButton(onClick = onExport) { Icon(Icons.Filled.Share, "Dışa aktar / paylaş", tint = white) }
        Box {
            IconButton(onClick = { more = true }) { Icon(Icons.Filled.MoreVert, "Diğer", tint = white) }
            DropdownMenu(more, { more = false }) {
                DropdownMenuItem(text = { Text("Sayfa şablonu / yeni sayfa") }, onClick = { more = false; onTemplate() })
                DropdownMenuItem(text = { Text("Sayfayı temizle") }, onClick = { more = false; onClear() })
                DropdownMenuItem(text = { Text("Ayarlar") }, onClick = { more = false; onSettings() })
            }
        }
    }
}

/* ---------------- araç çubuğu ---------------- */

@Composable
private fun ToolRail(ctl: EditorController) {
    val s = ctl.settings
    var options by remember { mutableStateOf(false) }
    var instMenu by remember { mutableStateOf(false) }
    var pickIndex by remember { mutableStateOf(-1) }
    Column(
        Modifier.width(64.dp).fillMaxHeight().background(Color(0xFFF7F8FA)).verticalScroll(rememberScrollState()).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val main = listOf(Tool.PEN, Tool.HIGHLIGHTER, Tool.ERASER, Tool.LASSO)
        for (t in main) ToolButton(ctl, t, options && ctl.tool == t, { options = it })
        // geometri grubu
        val gt = if (ctl.tool.isGeometry) ctl.tool else ctl.lastGeometryTool
        ToolButton(ctl, gt, options && ctl.tool == gt, { options = it })
        for (t in listOf(Tool.TEXT, Tool.LASER, Tool.TEMP)) ToolButton(ctl, t, false, { })
        // geometri araçları (cetvel, iletki, gönye)
        Box {
            RailButton(selected = ctl.instruments.isNotEmpty(), label = "Araçlar", onClick = { instMenu = true }) {
                InstrumentGlyph(null, if (ctl.instruments.isNotEmpty()) Accent else Ink)
            }
            DropdownMenu(instMenu, { instMenu = false }) {
                InstrumentKind.entries.forEach { k ->
                    DropdownMenuItem(
                        text = { Text((if (k in ctl.instruments) "✓ " else "") + k.title) },
                        leadingIcon = { InstrumentGlyph(k, Ink) },
                        onClick = { instMenu = false; ctl.view?.toggleInstrument(k) },
                    )
                }
                Text(
                    "Parmakla taşıyın, iki parmakla döndürün.\nKalemi kenarına yaklaştırınca düz çizer.",
                    fontSize = 12.sp, color = Color(0xFF607D8B), modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 6.dp, horizontal = 10.dp))
        // renkler
        val palette = s.paletteColors()
        val active = ctl.activeColor or 0xFF000000.toInt()
        palette.forEachIndexed { i, c ->
            Box(
                Modifier.padding(3.dp).size(32.dp).clip(CircleShape)
                    .border(if ((c or 0xFF000000.toInt()) == active) 3.dp else 1.dp, if ((c or 0xFF000000.toInt()) == active) Accent else Color(0x33000000), CircleShape)
                    .padding(4.dp).background(Color(c or 0xFF000000.toInt()), CircleShape)
                    .combinedClickable(onClick = { ctl.setColor(c) }, onLongClick = { pickIndex = i }),
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 6.dp, horizontal = 10.dp))
        // kalınlıklar
        val widths = if (ctl.tool == Tool.HIGHLIGHTER) listOf(8f, 14f, 22f) else listOf(1.0f, 2.0f, 3.6f)
        widths.forEachIndexed { i, w ->
            val sel = kotlin.math.abs(ctl.activeWidth - w) < 0.05f
            Box(
                Modifier.padding(2.dp).size(40.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (sel) Color(0xFFDCE7F5) else Color.Transparent)
                    .combinedClickable(onClick = { ctl.setWidth(w) }),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size((4 + i * 5).dp).background(Color(ctl.activeColor or 0xFF000000.toInt()), CircleShape))
            }
        }
    }
    if (pickIndex >= 0) {
        ColorPickerDialog(ctl.settings.paletteColors().getOrElse(pickIndex) { 0 }, onPick = { c ->
            ctl.settings.setPaletteColor(pickIndex, c); ctl.setColor(c); pickIndex = -1
        }, onDismiss = { pickIndex = -1 })
    }
}

@Composable
private fun RailButton(selected: Boolean, label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.padding(vertical = 2.dp).size(52.dp).clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFFD6E4F5) else Color.Transparent)
            .combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
        Text(label, fontSize = 8.sp, color = Color(0xFF546E7A), maxLines = 1, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 1.dp))
    }
}

@Composable
private fun ToolButton(ctl: EditorController, t: Tool, showOptions: Boolean, setOptions: (Boolean) -> Unit) {
    val selected = ctl.tool == t
    Box {
        RailButton(selected, shortName(t), onClick = {
            if (selected && hasOptions(t)) setOptions(!showOptions) else { setOptions(false); ctl.selectTool(t) }
        }) {
            val tint = if (selected) Accent else Ink
            ToolGlyph(t, tint, if (t == Tool.PEN || t == Tool.HIGHLIGHTER) Color(if (t == Tool.PEN) ctl.penColor else ctl.hlColor or 0xFF000000.toInt()) else tint)
        }
        DropdownMenu(showOptions, { setOptions(false) }) { ToolOptions(ctl, t) { setOptions(false) } }
    }
}

private fun hasOptions(t: Tool) = t == Tool.ERASER || t.isGeometry || t == Tool.PEN

private fun shortName(t: Tool) = when (t) {
    Tool.PEN -> "Kalem"; Tool.HIGHLIGHTER -> "Fosforlu"; Tool.ERASER -> "Silgi"; Tool.LASSO -> "Seçim"
    Tool.TEXT -> "Metin"; Tool.LASER -> "Lazer"; Tool.TEMP -> "Geçici"; Tool.SHAPE -> "Şekil"
    Tool.LINE -> "Doğru"; Tool.ARROW -> "Ok"; Tool.POLYGON -> "Çokgen"; Tool.COMPASS -> "Pergel"; Tool.MARK -> "İşaret"
}

@Composable
private fun ToolOptions(ctl: EditorController, t: Tool, close: () -> Unit) {
    Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp).width(300.dp)) {
        when {
            t == Tool.ERASER -> {
                Text("Silgi türü", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!ctl.eraserPartial, { ctl.eraserPartial = false; ctl.settings.eraserPartial.value = false }, { Text("Bütün çizgiyi sil") })
                    FilterChip(ctl.eraserPartial, { ctl.eraserPartial = true; ctl.settings.eraserPartial.value = true }, { Text("Kısmi sil") })
                }
                Text("Boyut", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Küçük", "Orta", "Büyük").forEachIndexed { i, n ->
                        FilterChip(ctl.eraserSize == i, { ctl.eraserSize = i; ctl.settings.eraserSize.value = i }, { Text(n) })
                    }
                }
            }
            t == Tool.PEN -> {
                LabeledRow("Basınç duyarlılığı") { Switch(ctl.settings.pressure.value, { ctl.settings.pressure.value = it }) }
                LabeledRow("Çizip bekleyince şekle çevir") { Switch(ctl.settings.shapeHold.value, { ctl.settings.shapeHold.value = it }) }
            }
            t.isGeometry -> {
                Text("Geometri aracı", fontWeight = FontWeight.SemiBold)
                for (g in listOf(Tool.SHAPE, Tool.LINE, Tool.ARROW, Tool.POLYGON, Tool.COMPASS, Tool.MARK)) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(if (ctl.tool == g) Color(0xFFD6E4F5) else Color.Transparent)
                            .combinedClickable(onClick = { ctl.selectTool(g) }).padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ToolGlyph(g, Ink, size = 22.dp); Spacer(Modifier.width(10.dp))
                        Column {
                            Text(g.title)
                            Text(
                                when (g) {
                                    Tool.SHAPE -> "Her çizimi şekle çevirir"
                                    Tool.LINE -> "Uzunluk ve açı gösterir"
                                    Tool.ARROW -> "Vektör / ok"
                                    Tool.POLYGON -> "Merkezden köşeye çekin"
                                    Tool.COMPASS -> "Çember ve yay"
                                    else -> "Dik açı, açı yayı, eşit kenar, paralel"
                                }, fontSize = 11.sp, color = Color(0xFF78909C),
                            )
                        }
                    }
                }
                if (ctl.tool == Tool.POLYGON) {
                    Text("Kenar sayısı: ${ctl.polygonSides}", fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (n in 3..10) FilterChip(ctl.polygonSides == n, { ctl.polygonSides = n; ctl.settings.polygonSides.value = n }, { Text("$n") })
                    }
                }
                if (ctl.tool == Tool.MARK) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MarkKind.entries.forEach { k -> FilterChip(ctl.markKind == k, { ctl.markKind = k }, { Text(k.title) }) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (n in 1..3) FilterChip(ctl.markCount == n, { ctl.markCount = n }, { Text("$n adet") })
                    }
                }
                LabeledRow("Kesikli çizgi") { Switch(ctl.shapeDashed, { ctl.shapeDashed = it }) }
                TextButton(onClick = close) { Text("Tamam") }
            }
        }
    }
}

/* ---------------- seçim / sunum çubukları ---------------- */

@Composable
private fun SelectionBar(ctl: EditorController, modifier: Modifier) {
    Surface(color = Color.White, shape = RoundedCornerShape(24.dp), shadowElevation = 6.dp, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
            TextButton(onClick = { ctl.view?.selectionDelete() }) { Icon(Icons.Filled.Delete, null); Spacer(Modifier.width(4.dp)); Text("Sil") }
            TextButton(onClick = { ctl.view?.selectionDuplicate() }) { Icon(Icons.Filled.ContentCopy, null); Spacer(Modifier.width(4.dp)); Text("Çoğalt") }
            Text("Renk değiştirmek için bir renge dokunun", fontSize = 12.sp, color = Color(0xFF78909C), modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = { ctl.view?.clearSelection() }) { Icon(Icons.Filled.Close, "Seçimi kaldır") }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun PresentationBar(ctl: EditorController, recState: Int, recTime: String, onStopRecord: () -> Unit, modifier: Modifier) {
    var expanded by remember { mutableStateOf(true) }
    Surface(color = Color(0xE6FFFFFF), shape = RoundedCornerShape(28.dp), shadowElevation = 6.dp, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
            if (expanded) {
                for (t in listOf(Tool.PEN, Tool.HIGHLIGHTER, Tool.ERASER, Tool.LASER, Tool.TEMP)) {
                    IconButton(onClick = { ctl.selectTool(t) }) { ToolGlyph(t, if (ctl.tool == t) Accent else Ink, size = 24.dp) }
                }
                ctl.settings.paletteColors().take(4).forEach { c ->
                    Box(Modifier.padding(4.dp)) { ColorDot(c, (c or 0xFF000000.toInt()) == (ctl.activeColor or 0xFF000000.toInt()), 28) { ctl.setColor(c) } }
                }
                IconButton(onClick = { ctl.undo() }) { Icon(Icons.Filled.Undo, "Geri al") }
                IconButton(onClick = { ctl.insertPageAfter(ctl.currentPage) }) { Icon(Icons.Filled.Add, "Yeni sayfa") }
                if (recState != 0) {
                    Text("● $recTime", color = Color(0xFFC62828), modifier = Modifier.padding(horizontal = 6.dp))
                    IconButton(onClick = onStopRecord) { Icon(Icons.Filled.Stop, "Kaydı bitir", tint = Color(0xFFC62828)) }
                }
            }
            IconButton(onClick = { expanded = !expanded }) { Text(if (expanded) "‹" else "›", fontSize = 22.sp) }
            IconButton(onClick = { ctl.presentation = false }) { Icon(Icons.Filled.FullscreenExit, "Sunumdan çık") }
        }
    }
}

/* ---------------- diyaloglar ---------------- */

@Composable
private fun GotoDialog(ctl: EditorController, onDismiss: () -> Unit) {
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
private fun PagesDialog(ctl: EditorController, onDismiss: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = ctl.pagesVersion
    var tick by remember { mutableIntStateOf(0) }
    val state = rememberLazyGridState(initialFirstVisibleItemIndex = ctl.currentPage)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sayfalar (${ctl.nb.pages.size})") },
        text = {
            Column {
                Text("Dokun: sayfaya git · Uzun bas: ekle, çoğalt, taşı, sil", fontSize = 12.sp, color = Color(0xFF607D8B))
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(130.dp), state = state,
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 560.dp),
                ) {
                    items(ctl.nb.pages.size, key = { ctl.nb.pages[it].id }) { i ->
                        val page = ctl.nb.pages[i]
                        var menu by remember { mutableStateOf(false) }
                        val bmp = if (tick >= 0) ctl.thumbnail(i, 260) { tick++ } else null
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(page.width / page.height)
                                    .border(if (i == ctl.currentPage) 3.dp else 1.dp, if (i == ctl.currentPage) Accent else Color(0x33000000))
                                    .background(Color.White)
                                    .combinedClickable(onClick = { ctl.goToPage(i); onDismiss() }, onLongClick = { menu = true }),
                            ) {
                                if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize())
                                DropdownMenu(menu, { menu = false }) {
                                    DropdownMenuItem(text = { Text("Sonrasına boş sayfa ekle") }, onClick = { menu = false; ctl.insertPageAfter(i) })
                                    DropdownMenuItem(text = { Text("Çoğalt") }, onClick = { menu = false; ctl.duplicatePage(i) })
                                    DropdownMenuItem(text = { Text("Yukarı taşı") }, onClick = { menu = false; ctl.movePage(i, -1) })
                                    DropdownMenuItem(text = { Text("Aşağı taşı") }, onClick = { menu = false; ctl.movePage(i, 1) })
                                    DropdownMenuItem(text = { Text("Sil") }, onClick = { menu = false; ctl.deletePage(i) })
                                }
                            }
                            val book = com.poyankee.note.core.PageNumbers.bookNumberOf(ctl.nb, page)
                            Text(if (book != null && ctl.nb.pageOffset != 0) "${i + 1}  (s. $book)" else "${i + 1}", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } },
    )
}

@Composable
private fun BookmarksDialog(ctl: EditorController, onDismiss: () -> Unit) {
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
private fun TemplateDialog(ctl: EditorController, onDismiss: () -> Unit) {
    val page = ctl.nb.pages.getOrNull(ctl.currentPage)
    val paper = page?.background as? Background.Paper
    var t by remember { mutableStateOf(paper?.template ?: ctl.nb.defaultTemplate) }
    var dark by remember { mutableStateOf(paper?.dark ?: false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sayfa şablonu") },
        text = {
            Column {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Template.entries.forEach { x -> FilterChip(t == x, { t = x }, { Text(x.title) }) }
                }
                LabeledRow("Kara tahta (koyu)") { Switch(dark, { dark = it }) }
                if (paper == null) Text("Bu bir PDF sayfası; şablon sadece yeni eklenen sayfaya uygulanır.", fontSize = 12.sp, color = Color(0xFF607D8B))
            }
        },
        confirmButton = {
            Row {
                if (paper != null) TextButton(onClick = { ctl.setTemplate(ctl.currentPage, t, dark); onDismiss() }) { Text("Bu sayfaya uygula") }
                TextButton(onClick = {
                    ctl.insertPageAfter(ctl.currentPage, t)
                    if (dark) ctl.setTemplate(ctl.currentPage, t, true)
                    onDismiss()
                }) { Text("Yeni sayfa ekle") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@Composable
private fun TextDialog(ctl: EditorController, req: com.poyankee.note.editor.TextRequest) {
    var text by remember(req) { mutableStateOf(req.existing?.text ?: "") }
    var size by remember(req) { mutableStateOf(req.existing?.size ?: 16f) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(req) { delay(150); try { focus.requestFocus() } catch (_: Exception) {} }
    AlertDialog(
        onDismissRequest = { ctl.textRequest = null },
        title = { Text(if (req.existing != null) "Metni düzenle" else "Metin ekle") },
        text = {
            Column {
                OutlinedTextField(text, { text = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp).focusRequester(focus))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                    for (sz in listOf(10f, 14f, 18f, 24f, 36f)) FilterChip(size == sz, { size = sz }, { Text("${sz.toInt()}") })
                }
            }
        },
        confirmButton = { TextButton(onClick = { ctl.finishText(req, text, size) }) { Text("Tamam") } },
        dismissButton = {
            Row {
                if (req.existing != null) TextButton(onClick = { ctl.finishText(req, "", size) }) { Text("Sil") }
                TextButton(onClick = { ctl.textRequest = null }) { Text("Vazgeç") }
            }
        },
    )
}

@Composable
private fun ExportDialog(ctl: EditorController, onDismiss: () -> Unit) {
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
                    suspendCancellableCoroutine { cont -> pdf.runOnPdfThread { cont.resume(pdf.renderBlocking(bg.index, scale)) } }
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
