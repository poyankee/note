@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.poyankee.note.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.poyankee.note.core.PageNumbers
import com.poyankee.note.core.PaperColors
import com.poyankee.note.core.Template
import com.poyankee.note.core.Templates
import com.poyankee.note.editor.EditorController
import com.poyankee.note.editor.InkView
import com.poyankee.note.editor.InstrumentKind
import com.poyankee.note.editor.LessonRecorder
import com.poyankee.note.editor.MarkKind
import com.poyankee.note.editor.SecondScreen
import com.poyankee.note.editor.Tool
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class Panel { NONE, PAGE, PAGES }

@Composable
fun EditorScreen(ctl: EditorController, onClose: () -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val s = ctl.settings
    val ink = remember { InkView(context, ctl).also { ctl.view = it; it.keepScreenOn = true } }

    // ikinci ekran
    var secondActive by remember { mutableStateOf(false) }
    val second = remember { SecondScreen(activity, ink, { s.secondScreen.value }) { secondActive = it; ctl.secondScreenActive = it } }
    DisposableEffect(Unit) {
        second.start()
        onDispose { second.stop() }
    }
    LaunchedEffect(s.secondScreen.value) { second.update() }

    // sunum modu: sistem çubuklarını gizle
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

    var panel by remember { mutableStateOf(Panel.NONE) }
    var showGoto by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }

    BackHandler {
        when {
            ctl.presentation -> ctl.presentation = false
            panel != Panel.NONE -> panel = Panel.NONE
            else -> onClose()
        }
    }

    val dockLeft = !s.leftHanded.value
    Column(Modifier.fillMaxSize().background(UI.Desk)) {
        if (!ctl.presentation) {
            EditorTopBar(
                ctl, secondActive, recState, recTime, panel,
                onClose = onClose, onRename = { showRename = true },
                onPagePanel = { panel = if (panel == Panel.PAGE) Panel.NONE else Panel.PAGE },
                onPages = { panel = if (panel == Panel.PAGES) Panel.NONE else Panel.PAGES },
                onBookmarks = { showBookmarks = true }, onExport = { showExport = true }, onClear = { showClear = true },
                onImage = { pickImage.launch("image/*") }, onCamera = { openCamera() }, onSettings = onSettings,
                onRecord = { toggleRecord() },
                onPauseResume = { if (recState == 1) { recorder.pause(); recState = 2 } else if (recState == 2) { recorder.resume(); recState = 1 } },
                onStopRecord = { stopRecording() },
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            AndroidView(factory = { ink }, modifier = Modifier.fillMaxSize().clipToBounds())

            if (!ctl.presentation) {
                // yüzen araç menüsü
                ToolDock(ctl, Modifier.align(if (dockLeft) Alignment.CenterStart else Alignment.CenterEnd).padding(horizontal = 14.dp))
                // seçili araca göre seçenekler
                Column(Modifier.align(Alignment.TopCenter).padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ContextBar(ctl, onClear = { showClear = true })
                    ctl.hint?.let { h ->
                        if (ctl.tool.isGeometry) {
                            Spacer(Modifier.height(8.dp))
                            Surface(color = Color(0xE61D2330), shape = RoundedCornerShape(10.dp)) {
                                Text(h, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                            }
                        }
                    }
                }
                // sayfa gezgini
                PageNavigator(ctl, onGoto = { showGoto = true }, onPages = { panel = Panel.PAGES },
                    modifier = Modifier.align(if (dockLeft) Alignment.BottomEnd else Alignment.BottomStart).padding(16.dp))
            } else {
                PresentationBar(ctl, recState, recTime, onStopRecord = { stopRecording() },
                    modifier = Modifier.align(if (dockLeft) Alignment.BottomStart else Alignment.BottomEnd).padding(14.dp))
            }

            if (ctl.hasSelection) SelectionBar(ctl, Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp))

            // sağdan açılan paneller
            SidePanel(panel == Panel.PAGE) { PageStylePanel(ctl) { panel = Panel.NONE } }
            SidePanel(panel == Panel.PAGES) { PagesPanel(ctl) { panel = Panel.NONE } }

            ctl.message?.let { m ->
                LaunchedEffect(m) { delay(3500); if (ctl.message == m) ctl.message = null }
                Surface(
                    color = Color(0xF01D2330), shape = RoundedCornerShape(12.dp), shadowElevation = 6.dp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp, start = 16.dp, end = 16.dp),
                ) { Text(m, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)) }
            }
        }
    }

    if (showGoto) GotoDialog(ctl) { showGoto = false }
    if (showBookmarks) BookmarksDialog(ctl) { showBookmarks = false }
    if (showExport) ExportDialog(ctl) { showExport = false }
    if (showRename) NameDialog("Defteri yeniden adlandır", "Defter adı", ctl.nb.name, "Kaydet", onDismiss = { showRename = false }) { showRename = false; ctl.rename(it) }
    if (showClear) ConfirmDialog("Sayfayı temizle", "Bu sayfadaki tüm yazılar silinecek. (Geri al ile geri getirebilirsiniz.)", "Temizle", { ctl.clearPage() }, { showClear = false })
    ctl.textRequest?.let { TextDialog(ctl, it) }
}

/* ======================= üst çubuk ======================= */

@Suppress("DEPRECATION")
@Composable
private fun EditorTopBar(
    ctl: EditorController, secondActive: Boolean, recState: Int, recTime: String, panel: Panel,
    onClose: () -> Unit, onRename: () -> Unit, onPagePanel: () -> Unit, onPages: () -> Unit, onBookmarks: () -> Unit,
    onExport: () -> Unit, onClear: () -> Unit, onImage: () -> Unit, onCamera: () -> Unit, onSettings: () -> Unit,
    onRecord: () -> Unit, onPauseResume: () -> Unit, onStopRecord: () -> Unit,
) {
    var more by remember { mutableStateOf(false) }
    var insert by remember { mutableStateOf(false) }
    val pv = ctl.pagesVersion
    Column(Modifier.fillMaxWidth().testTag("topbar").background(UI.Surface)) {
        Box(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp)) {
            // sol: geri + başlık
            Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                BarIcon(Icons.Rounded.ArrowBack, "Defterlere dön", onClick = onClose)
                Column(Modifier.widthIn(max = 260.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onRename).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(ctl.nb.name + (if (pv < 0) "" else ""), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = UI.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (ctl.nb.folder.isNotEmpty()) ctl.nb.folder else "Defter", fontSize = 12.sp, color = UI.TextSoft, maxLines = 1)
                }
            }
            // orta: geri al / yinele
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                BarIcon(Icons.Rounded.Undo, "Geri al", enabled = ctl.canUndo) { ctl.undo() }
                BarIcon(Icons.Rounded.Redo, "Yinele", enabled = ctl.canRedo) { ctl.redo() }
            }
            // sağ
            Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
                if (secondActive) Icon(Icons.Rounded.Cast, "Projeksiyon bağlı", tint = Color(0xFF0E9384), modifier = Modifier.padding(horizontal = 8.dp))
                if (recState != 0) {
                    Surface(color = UI.Danger, shape = RoundedCornerShape(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
                            Text(if (recState == 1) "● $recTime" else "❚❚ $recTime", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = onPauseResume, modifier = Modifier.size(38.dp)) {
                                Icon(if (recState == 1) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Duraklat / devam", tint = Color.White)
                            }
                            IconButton(onClick = onStopRecord, modifier = Modifier.size(38.dp)) { Icon(Icons.Rounded.Stop, "Kaydı bitir", tint = Color.White) }
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                }
                LabeledBarButton("Sayfa", panel == Panel.PAGE, onPagePanel) { PaperGlyph(if (panel == Panel.PAGE) UI.Primary else UI.Text, 22.dp) }
                BarIcon(Icons.Rounded.ViewModule, "Sayfalar", selected = panel == Panel.PAGES, onClick = onPages)
                BarIcon(Icons.Rounded.Bookmark, "Yer imleri", onClick = onBookmarks)
                Box {
                    BarIcon(Icons.Rounded.Image, "Görsel ekle") { insert = true }
                    DropdownMenu(insert, { insert = false }) {
                        DropdownMenuItem(text = { Text("Galeriden görsel") }, leadingIcon = { Icon(Icons.Rounded.Image, null) }, onClick = { insert = false; onImage() })
                        DropdownMenuItem(text = { Text("Kameradan fotoğraf") }, leadingIcon = { Icon(Icons.Rounded.CameraAlt, null) }, onClick = { insert = false; onCamera() })
                    }
                }
                if (recState == 0) BarIcon(Icons.Rounded.FiberManualRecord, "Ders kaydı", tint = UI.Danger, onClick = onRecord)
                BarIcon(Icons.Rounded.Fullscreen, "Sunum modu") { ctl.view?.clearSelection(); ctl.presentation = true }
                BarIcon(Icons.Rounded.Share, "Dışa aktar / paylaş", onClick = onExport)
                Box {
                    BarIcon(Icons.Rounded.MoreVert, "Diğer") { more = true }
                    DropdownMenu(more, { more = false }) {
                        DropdownMenuItem(text = { Text("Sayfayı temizle") }, onClick = { more = false; onClear() })
                        DropdownMenuItem(text = { Text("Ayarlar") }, onClick = { more = false; onSettings() })
                    }
                }
            }
        }
        HorizontalDivider(color = UI.Border)
    }
}

@Composable
private fun BarIcon(icon: ImageVector, desc: String, enabled: Boolean = true, selected: Boolean = false, tint: Color = UI.Text, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
            .background(if (selected) UI.PrimarySoft else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = if (!enabled) UI.TextFaint.copy(alpha = 0.6f) else if (selected) UI.Primary else tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun LabeledBarButton(text: String, selected: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Row(
        Modifier.height(40.dp).clip(RoundedCornerShape(12.dp))
            .background(if (selected) UI.PrimarySoft else Color(0xFFF2F4F7))
            .clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (selected) UI.PrimaryText else UI.Text)
    }
    Spacer(Modifier.width(4.dp))
}

/* ======================= araç menüsü ======================= */

@Composable
private fun ToolDock(ctl: EditorController, modifier: Modifier) {
    var instMenu by remember { mutableStateOf(false) }
    FloatingCard(modifier.testTag("dock"), radius = 22) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            for (t in listOf(Tool.PEN, Tool.HIGHLIGHTER, Tool.ERASER, Tool.LASSO)) DockButton(ctl, t)
            val gt = if (ctl.tool.isGeometry) ctl.tool else ctl.lastGeometryTool
            DockButton(ctl, gt, selectedOverride = ctl.tool.isGeometry, label = "Geometri")
            for (t in listOf(Tool.TEXT, Tool.LASER, Tool.TEMP)) DockButton(ctl, t)
            Box(Modifier.padding(vertical = 4.dp).width(30.dp).height(1.dp).background(UI.Border))
            Box {
                DockItem(selected = ctl.instruments.isNotEmpty(), label = "Cetvel", onClick = { instMenu = true }) {
                    InstrumentGlyph(null, if (ctl.instruments.isNotEmpty()) UI.Primary else UI.Text)
                }
                DropdownMenu(instMenu, { instMenu = false }) {
                    Text("Geometri araçları", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    InstrumentKind.entries.forEach { k ->
                        DropdownMenuItem(
                            text = { Text(k.title, fontWeight = if (k in ctl.instruments) FontWeight.SemiBold else FontWeight.Normal) },
                            leadingIcon = { InstrumentGlyph(k, if (k in ctl.instruments) UI.Primary else UI.Text) },
                            trailingIcon = { if (k in ctl.instruments) Text("Açık", color = UI.Primary, fontSize = 12.sp) },
                            onClick = { instMenu = false; ctl.view?.toggleInstrument(k) },
                        )
                    }
                    Text(
                        "Parmakla taşıyın, iki parmakla döndürün.\nKalemi kenarına yaklaştırınca düz çizer.",
                        fontSize = 12.sp, color = UI.TextSoft, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DockButton(ctl: EditorController, t: Tool, selectedOverride: Boolean? = null, label: String = shortName(t)) {
    val selected = selectedOverride ?: (ctl.tool == t)
    DockItem(selected, label, onClick = { ctl.selectTool(t) }) {
        val tint = if (selected) UI.Primary else UI.Text
        val accent = when (t) {
            Tool.PEN -> Color(ctl.penColor or 0xFF000000.toInt())
            Tool.HIGHLIGHTER -> Color(ctl.hlColor or 0xFF000000.toInt())
            else -> tint
        }
        ToolGlyph(t, tint, accent, 26.dp)
    }
}

@Composable
private fun DockItem(selected: Boolean, label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Column(
        Modifier.width(60.dp).clip(RoundedCornerShape(14.dp))
            .background(if (selected) UI.PrimarySoft else Color.Transparent)
            .clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
        Text(label, fontSize = 10.sp, color = if (selected) UI.PrimaryText else UI.TextSoft, maxLines = 1,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

private fun shortName(t: Tool) = when (t) {
    Tool.PEN -> "Kalem"; Tool.HIGHLIGHTER -> "Fosforlu"; Tool.ERASER -> "Silgi"; Tool.LASSO -> "Seçim"
    Tool.TEXT -> "Metin"; Tool.LASER -> "Lazer"; Tool.TEMP -> "Geçici"; Tool.SHAPE -> "Şekil"
    Tool.LINE -> "Doğru"; Tool.ARROW -> "Ok"; Tool.POLYGON -> "Çokgen"; Tool.COMPASS -> "Pergel"; Tool.MARK -> "İşaret"
}

/* ======================= bağlam çubuğu (renk, kalınlık, seçenekler) ======================= */

private val HighlighterColors = listOf(0xFFFFEB3B, 0xFF7CF29A, 0xFFFF9EC7, 0xFF8EC5FF, 0xFFFFB74D).map { it.toInt() }

@Composable
private fun ContextBar(ctl: EditorController, onClear: () -> Unit) {
    val t = ctl.tool
    var pickIndex by remember { mutableIntStateOf(-1) }
    FloatingCard(radius = 26) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                t == Tool.ERASER -> {
                    PillRow {
                        Pill("Bütün çizgi", !ctl.eraserPartial) { ctl.eraserPartial = false; ctl.settings.eraserPartial.value = false }
                        Pill("Kısmi", ctl.eraserPartial) { ctl.eraserPartial = true; ctl.settings.eraserPartial.value = true }
                    }
                    BarDivider()
                    listOf(8, 14, 22).forEachIndexed { i, d ->
                        SizeDot(d, ctl.eraserSize == i, Color(0xFF98A2B3)) { ctl.eraserSize = i; ctl.settings.eraserSize.value = i }
                    }
                    BarDivider()
                    Pill("Sayfayı temizle", false, onClick = onClear)
                }
                t == Tool.LASSO -> Hint("Seçmek istediğiniz yazının etrafını çizin")
                t == Tool.LASER -> Hint("Kalemle gösterin; iz birkaç saniyede kaybolur")
                else -> {
                    if (t.isGeometry) {
                        GeometryChooser(ctl)
                        BarDivider()
                    }
                    val colors = if (t == Tool.HIGHLIGHTER) HighlighterColors else ctl.settings.paletteColors()
                    val active = ctl.activeColor or 0xFF000000.toInt()
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        colors.forEachIndexed { i, c ->
                            ColorDot(
                                c, (c or 0xFF000000.toInt()) == active, 32,
                                onLongClick = if (t != Tool.HIGHLIGHTER) ({ pickIndex = i }) else null,
                            ) { ctl.setColor(c) }
                        }
                    }
                    if (t != Tool.TEXT) {
                        BarDivider()
                        val widths = if (t == Tool.HIGHLIGHTER) listOf(8f, 14f, 22f) else listOf(1.0f, 2.0f, 3.6f)
                        widths.forEachIndexed { i, w ->
                            SizeDot(6 + i * 5, abs(ctl.activeWidth - w) < 0.05f, Color(ctl.activeColor or 0xFF000000.toInt())) { ctl.setWidth(w) }
                        }
                    }
                }
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
private fun GeometryChooser(ctl: EditorController) {
    var opts by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        for (g in listOf(Tool.SHAPE, Tool.LINE, Tool.ARROW, Tool.POLYGON, Tool.COMPASS, Tool.MARK)) {
            val sel = ctl.tool == g
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(if (sel) UI.PrimarySoft else Color.Transparent)
                    .clickable { if (sel) opts = !opts else ctl.selectTool(g) },
                contentAlignment = Alignment.Center,
            ) { ToolGlyph(g, if (sel) UI.Primary else UI.Text, size = 22.dp) }
        }
        Spacer(Modifier.width(4.dp))
        Box {
            Pill(
                when (ctl.tool) {
                    Tool.POLYGON -> "${ctl.polygonSides} kenar"
                    Tool.MARK -> ctl.markKind.title.substringBefore(" ")
                    else -> if (ctl.shapeDashed) "Kesikli" else "Düz"
                }, opts,
            ) { opts = !opts }
            DropdownMenu(opts, { opts = false }) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp).width(280.dp)) {
                    Text(ctl.tool.title, fontWeight = FontWeight.SemiBold)
                    Text(
                        when (ctl.tool) {
                            Tool.SHAPE -> "Her çizimi düzgün şekle çevirir."
                            Tool.LINE -> "Uzunluk ve açıyı gösterir; köşelere ve 15°'ye yapışır."
                            Tool.ARROW -> "Vektör / ok çizer."
                            Tool.POLYGON -> "Merkezden bir köşeye doğru çekin."
                            Tool.COMPASS -> "Merkezi seçip yarıçap kadar çekin, sonra yayı çizin."
                            else -> "Köşeye dokunun: açı / dik açı. Kenara dokunun: çentik / paralel."
                        }, fontSize = 12.sp, color = UI.TextSoft, modifier = Modifier.padding(bottom = 8.dp),
                    )
                    if (ctl.tool == Tool.POLYGON) {
                        Text("Kenar sayısı", fontSize = 13.sp, color = UI.TextSoft)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                            for (n in 3..10) Pill("$n", ctl.polygonSides == n) { ctl.polygonSides = n; ctl.settings.polygonSides.value = n }
                        }
                    }
                    if (ctl.tool == Tool.MARK) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                            MarkKind.entries.forEach { k -> Pill(k.title, ctl.markKind == k) { ctl.markKind = k } }
                        }
                        PillRow { for (n in 1..3) Pill("$n adet", ctl.markCount == n) { ctl.markCount = n } }
                    }
                    LabeledRow("Kesikli çizgi") { Switch(ctl.shapeDashed, { ctl.shapeDashed = it }) }
                }
            }
        }
    }
}

@Composable
private fun SizeDot(d: Int, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(if (selected) UI.PrimarySoft else Color.Transparent).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Box(Modifier.size(d.dp).clip(CircleShape).background(color)) }
}

@Composable
private fun BarDivider() {
    Box(Modifier.padding(horizontal = 10.dp).width(1.dp).height(26.dp).background(UI.Border))
}

@Composable
private fun Hint(text: String) {
    Text(text, color = UI.TextSoft, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
}

/* ======================= sayfa gezgini ======================= */

@Composable
private fun PageNavigator(ctl: EditorController, onGoto: () -> Unit, onPages: () -> Unit, modifier: Modifier) {
    val pv = ctl.pagesVersion
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (ctl.history.isNotEmpty()) {
            FloatingCard(radius = 26) {
                Text(
                    "‹ ${ctl.history.last() + 1}. sayfaya dön", color = UI.Primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    modifier = Modifier.clickable { ctl.goBack() }.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        FloatingCard(radius = 26) {
            Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                SmallRound(Icons.Rounded.ChevronLeft, "Önceki sayfa", ctl.currentPage > 0) { ctl.goToPage(ctl.currentPage - 1, false) }
                Column(
                    Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onGoto).padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("${ctl.currentPage + 1} / ${ctl.pageCount}" + (if (pv < 0) "" else ""), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = UI.Text)
                    val book = ctl.nb.pages.getOrNull(ctl.currentPage)?.let { PageNumbers.bookNumberOf(ctl.nb, it) }
                    Text(if (book != null && ctl.nb.pageOffset != 0) "kitap s. $book" else "sayfaya git", fontSize = 10.sp, color = UI.TextSoft)
                }
                SmallRound(Icons.Rounded.ChevronRight, "Sonraki sayfa", ctl.currentPage < ctl.pageCount - 1) { ctl.goToPage(ctl.currentPage + 1, false) }
                SmallRound(Icons.Rounded.ViewModule, "Tüm sayfalar", true, onPages)
            }
        }
        Spacer(Modifier.width(10.dp))
        Button(
            onClick = { ctl.insertPageAfter(ctl.currentPage) },
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.height(52.dp),
        ) {
            Icon(Icons.Rounded.Add, null)
            Spacer(Modifier.width(6.dp))
            Text("Sayfa", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun SmallRound(icon: ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = if (enabled) UI.Text else UI.TextFaint.copy(alpha = 0.5f)) }
}

/* ======================= seçim / sunum çubukları ======================= */

@Composable
private fun SelectionBar(ctl: EditorController, modifier: Modifier) {
    FloatingCard(modifier, radius = 26) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            SelAction(Icons.Rounded.Delete, "Sil", UI.Danger) { ctl.view?.selectionDelete() }
            SelAction(Icons.Rounded.ContentCopy, "Çoğalt", UI.Text) { ctl.view?.selectionDuplicate() }
            Text("Renk için üstteki renge dokunun", fontSize = 12.sp, color = UI.TextSoft, modifier = Modifier.padding(horizontal = 10.dp))
            SmallRound(Icons.Rounded.Close, "Seçimi kaldır", true) { ctl.view?.clearSelection() }
        }
    }
}

@Composable
private fun SelAction(icon: ImageVector, text: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text(text, color = tint, fontWeight = FontWeight.SemiBold)
    }
}

@Suppress("DEPRECATION")
@Composable
private fun PresentationBar(ctl: EditorController, recState: Int, recTime: String, onStopRecord: () -> Unit, modifier: Modifier) {
    var expanded by remember { mutableStateOf(true) }
    FloatingCard(modifier, radius = 28) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
            if (expanded) {
                for (t in listOf(Tool.PEN, Tool.HIGHLIGHTER, Tool.ERASER, Tool.LASER, Tool.TEMP)) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(if (ctl.tool == t) UI.PrimarySoft else Color.Transparent).clickable { ctl.selectTool(t) },
                        contentAlignment = Alignment.Center,
                    ) { ToolGlyph(t, if (ctl.tool == t) UI.Primary else UI.Text, size = 24.dp) }
                }
                ctl.settings.paletteColors().take(4).forEach { c ->
                    Box(Modifier.padding(3.dp)) { ColorDot(c, (c or 0xFF000000.toInt()) == (ctl.activeColor or 0xFF000000.toInt()), 30) { ctl.setColor(c) } }
                }
                SmallRound(Icons.Rounded.Undo, "Geri al", true) { ctl.undo() }
                SmallRound(Icons.Rounded.Add, "Yeni sayfa", true) { ctl.insertPageAfter(ctl.currentPage) }
                if (recState != 0) {
                    Text("● $recTime", color = UI.Danger, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp))
                    SmallRound(Icons.Rounded.Stop, "Kaydı bitir", true, onStopRecord)
                }
            }
            SmallRound(if (expanded) Icons.Rounded.ChevronLeft else Icons.Rounded.ChevronRight, "Daralt", true) { expanded = !expanded }
            SmallRound(Icons.Rounded.FullscreenExit, "Sunumdan çık", true) { ctl.presentation = false }
        }
    }
}

/* ======================= sağ paneller ======================= */

@Composable
private fun BoxScope.SidePanel(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible, modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        enter = slideInHorizontally { it } + fadeIn(), exit = slideOutHorizontally { it } + fadeOut(),
    ) {
        Surface(
            modifier = Modifier.width(360.dp).fillMaxHeight().padding(10.dp),
            shape = RoundedCornerShape(22.dp), color = UI.Surface, shadowElevation = 16.dp,
        ) { content() }
    }
}

@Composable
private fun PanelHeader(title: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = UI.Text, modifier = Modifier.weight(1f))
        SmallRound(Icons.Rounded.Close, "Kapat", true, onClose)
    }
}

/** Kağıt deseni, rengi, desen görünürlüğü ve aralığı. Değişiklikler anında sayfaya uygulanır. */
@Composable
private fun PageStylePanel(ctl: EditorController, onClose: () -> Unit) {
    val pv = ctl.pagesVersion
    val page = ctl.nb.pages.getOrNull(ctl.currentPage)
    val isPdf = page?.background is Background.PdfPage
    val current = (page?.background as? Background.Paper) ?: ctl.nb.defaultPaper
    var style by remember(ctl.currentPage, isPdf) { mutableStateOf(current) }
    var all by remember { mutableStateOf(false) }
    fun apply(p: Background.Paper) {
        style = p
        if (!isPdf) ctl.setPageStyle(ctl.currentPage, p, all) else ctl.nb.defaultPaper = p
    }
    Column(Modifier.fillMaxSize()) {
        PanelHeader("Sayfa" + (if (pv < 0) "" else ""), onClose)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (isPdf) {
                Surface(color = Color(0xFFFFF7ED), shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        "Bu bir PDF sayfası. Seçtiğiniz desen, \"+ Sayfa\" ile eklenecek yeni sayfalara uygulanır.",
                        fontSize = 13.sp, color = Color(0xFF9A3412), modifier = Modifier.padding(12.dp),
                    )
                }
            }
            SectionTitle("Desen")
            for (row in Template.entries.chunked(3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    for (t in row) {
                        val sel = style.template == t
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable {
                                apply(style.copy(template = t, spacing = Templates.defaultSpacing(t)))
                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(10.dp))
                                    .border(if (sel) 2.5.dp else 1.dp, if (sel) UI.Primary else UI.Border, RoundedCornerShape(10.dp)),
                            ) {
                                PaperPreview(style.copy(template = t, spacing = if (sel) style.spacing else Templates.defaultSpacing(t)), Modifier.fillMaxSize())
                            }
                            Text(
                                t.title, fontSize = 12.sp, color = if (sel) UI.PrimaryText else UI.Text,
                                fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                            )
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            SectionTitle("Kağıt rengi")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PaperColors.all.forEach { (c, name) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { apply(style.copy(color = c)) }) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape)
                                .border(if (style.color == c) 3.dp else 1.dp, if (style.color == c) UI.Primary else UI.Border, CircleShape)
                                .padding(if (style.color == c) 4.dp else 0.dp).clip(CircleShape).background(Color(c)),
                        )
                        Text(name, fontSize = 10.sp, color = UI.TextSoft, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }

            if (style.template != Template.BLANK) {
                SectionTitle("Desen görünürlüğü  ·  %${(style.opacity * 100).roundToInt()}")
                Slider(
                    value = style.opacity, onValueChange = { apply(style.copy(opacity = (it * 20).roundToInt() / 20f)) },
                    valueRange = 0.1f..1f, colors = SliderDefaults.colors(thumbColor = UI.Primary, activeTrackColor = UI.Primary),
                )
                val range = Templates.spacingRange(style.template)
                val mm = style.spacing / Templates.MM
                SectionTitle("Aralık  ·  ${"%.1f".format(mm)} mm")
                Slider(
                    value = style.spacing.coerceIn(range.start, range.endInclusive),
                    onValueChange = { apply(style.copy(spacing = (it * 2).roundToInt() / 2f)) },
                    valueRange = range, colors = SliderDefaults.colors(thumbColor = UI.Primary, activeTrackColor = UI.Primary),
                )
            }

            if (!isPdf) {
                LabeledRow("Tüm sayfalara uygula", "Kapalıyken yalnızca bu sayfa değişir") {
                    Switch(all, { all = it; if (it) ctl.setPageStyle(ctl.currentPage, style, true) })
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        HorizontalDivider(color = UI.Border)
        Button(
            onClick = { ctl.insertPageAfter(ctl.currentPage, style) }, shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
        ) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("Bu desenle yeni sayfa ekle") }
    }
}

/** Sayfa küçük resimleri: dokun = git, uzun bas = sayfa işlemleri. */
@Composable
private fun PagesPanel(ctl: EditorController, onClose: () -> Unit) {
    val pv = ctl.pagesVersion
    var tick by remember { mutableIntStateOf(0) }
    val state = rememberLazyGridState(initialFirstVisibleItemIndex = ctl.currentPage)
    Column(Modifier.fillMaxSize()) {
        PanelHeader("Sayfalar (${ctl.nb.pages.size})" + (if (pv < 0) "" else ""), onClose)
        Text("Dokun: git · Uzun bas: ekle, çoğalt, taşı, sil", fontSize = 12.sp, color = UI.TextSoft, modifier = Modifier.padding(horizontal = 20.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), state = state,
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(20.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(ctl.nb.pages.size, key = { ctl.nb.pages[it].id }) { i ->
                val page = ctl.nb.pages[i]
                var menu by remember { mutableStateOf(false) }
                val bmp = if (tick >= 0) ctl.thumbnail(i, 300) { tick++ } else null
                val cur = i == ctl.currentPage
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(page.width / page.height)
                            .shadow(if (cur) 0.dp else 3.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .border(if (cur) 3.dp else 0.dp, if (cur) UI.Primary else Color.Transparent, RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .combinedClickable(onClick = { ctl.goToPage(i) }, onLongClick = { menu = true }),
                    ) {
                        if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize())
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text("Sonrasına boş sayfa ekle") }, onClick = { menu = false; ctl.insertPageAfter(i) })
                            DropdownMenuItem(text = { Text("Çoğalt") }, onClick = { menu = false; ctl.duplicatePage(i) })
                            DropdownMenuItem(text = { Text("Yukarı taşı") }, onClick = { menu = false; ctl.movePage(i, -1) })
                            DropdownMenuItem(text = { Text("Aşağı taşı") }, onClick = { menu = false; ctl.movePage(i, 1) })
                            DropdownMenuItem(text = { Text("Sil", color = UI.Danger) }, onClick = { menu = false; ctl.deletePage(i) })
                        }
                    }
                    val book = PageNumbers.bookNumberOf(ctl.nb, page)
                    Text(
                        if (book != null && ctl.nb.pageOffset != 0) "${i + 1}  ·  s. $book" else "${i + 1}",
                        fontSize = 12.sp, color = if (cur) UI.Primary else UI.TextSoft, fontWeight = if (cur) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }
        }
        HorizontalDivider(color = UI.Border)
        OutlinedButton(
            onClick = { ctl.insertPageAfter(ctl.nb.pages.size - 1) }, shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(46.dp),
        ) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("Sona sayfa ekle") }
    }
}
