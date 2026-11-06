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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Description
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import com.poyankee.note.core.Solids
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
fun EditorScreen(
    ctl: EditorController,
    onClose: () -> Unit,
    onSettings: () -> Unit,
    tabs: List<Pair<String, String>> = emptyList(),
    onTab: (String) -> Unit = {},
    onCloseTab: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as Activity
    val s = ctl.settings
    val ink = remember { InkView(context, ctl).also { ctl.view = it; it.keepScreenOn = true } }
    val dark = UI.dark
    LaunchedEffect(dark) { ink.deskColor = UI.Desk.toArgb(); ink.invalidate() }

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
    var showPlot by remember { mutableStateOf(false) }
    var showStamp by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }
    var cropIndex by remember { mutableIntStateOf(-1) }
    var paletteFor by remember { mutableStateOf<Boolean?>(null) }

    BackHandler {
        when {
            ctl.presentation -> ctl.presentation = false
            panel != Panel.NONE -> panel = Panel.NONE
            else -> onClose()
        }
    }

    val dockLeft = !s.leftHanded.value
    Column(Modifier.fillMaxSize().background(UI.Desk)) {
        if (!ctl.presentation && tabs.size > 1) TabsRow(tabs, ctl.nb.id, onTab, onCloseTab)
        if (!ctl.presentation) {
            EditorTopBar(
                ctl, secondActive, recState, recTime, panel,
                onClose = onClose, onRename = { showRename = true },
                onPagePanel = { panel = if (panel == Panel.PAGE) Panel.NONE else Panel.PAGE },
                onPages = { panel = if (panel == Panel.PAGES) Panel.NONE else Panel.PAGES },
                onBookmarks = { showBookmarks = true }, onExport = { showExport = true }, onClear = { showClear = true },
                onImage = { pickImage.launch("image/*") }, onCamera = { openCamera() }, onSettings = onSettings,
                onFormula = { ctl.requestFormula(ctl.view?.visibleCenter()?.first ?: ctl.currentPage, null) },
                onPlot = { showPlot = true }, onSticky = { ctl.requestStickyNote() }, onStamp = { showStamp = true },
                onTimer = { showTimer = true },
                onRecord = { toggleRecord() },
                onPauseResume = { if (recState == 1) { recorder.pause(); recState = 2 } else if (recState == 2) { recorder.resume(); recState = 1 } },
                onStopRecord = { stopRecording() },
            )
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            val areaW = maxWidth; val areaH = maxHeight
            AndroidView(factory = { ink }, modifier = Modifier.fillMaxSize().clipToBounds())

            if (!ctl.presentation) {
                // yüzen araç menüsü (tutamaktan sürüklenip taşınabilir, daraltılabilir)
                Draggable(s.dockX, s.dockY, areaW, areaH, Modifier.align(if (dockLeft) Alignment.CenterStart else Alignment.CenterEnd).padding(horizontal = 14.dp)) { handle, _ ->
                    ToolDock(ctl, handle, onTimer = { showTimer = true }, onClear = { showClear = true }, onEditPalette = { paletteFor = it })
                }
                // geometri araçlarında kısa ipucu
                Column(Modifier.align(Alignment.TopCenter).padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ctl.hint?.let { h ->
                        if (ctl.tool.isGeometry) {
                            Surface(color = Color(0xE61D2330), shape = RoundedCornerShape(10.dp)) {
                                Text(h, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                            }
                        }
                    }
                }
                // sayfa gezgini
                Draggable(s.navX, s.navY, areaW, areaH, Modifier.align(if (dockLeft) Alignment.BottomEnd else Alignment.BottomStart).padding(16.dp)) { handle, _ ->
                    PageNavigator(ctl, handle, onGoto = { showGoto = true }, onPages = { panel = Panel.PAGES })
                }
            } else {
                Draggable(s.presX, s.presY, areaW, areaH, Modifier.align(if (dockLeft) Alignment.BottomStart else Alignment.BottomEnd).padding(14.dp)) { handle, vertical ->
                    FocusBar(ctl, handle, vertical, recState, recTime, onStopRecord = { stopRecording() })
                }
            }

            if (ctl.hasSelection) SelectionBar(ctl, Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp))

            // sağdan açılan paneller
            SidePanel(panel == Panel.PAGE) { PageStylePanel(ctl) { panel = Panel.NONE } }
            SidePanel(panel == Panel.PAGES) { PagesPanel(ctl, onCrop = { cropIndex = it }) { panel = Panel.NONE } }

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
    ctl.formulaRequest?.let { FormulaDialog(ctl, it) }
    if (showPlot) PlotDialog(ctl) { showPlot = false }
    if (showStamp) StampDialog(ctl) { showStamp = false }
    if (showTimer) TimerDialog(ctl) { showTimer = false }
    if (cropIndex >= 0) CropDialog(ctl, cropIndex) { cropIndex = -1 }
    paletteFor?.let { hl -> PaletteDialog(ctl, hl) { paletteFor = null } }
}

/* ======================= sürüklenebilir panel ======================= */

/**
 * İçeriği tutamağından sürüklenerek taşınabilen panel. Konum ayarlarda saklanır (uygulama kapansa da kalır).
 * [content] tutamak için verilecek Modifier'ı alır.
 */
@Composable
private fun Draggable(
    px: com.poyankee.note.data.Settings.Pref<Float>, py: com.poyankee.note.data.Settings.Pref<Float>,
    areaW: androidx.compose.ui.unit.Dp, areaH: androidx.compose.ui.unit.Dp,
    modifier: Modifier, content: @Composable (handle: Modifier, nearSideEdge: Boolean) -> Unit,
) {
    var x by remember { mutableStateOf(px.value) }
    var y by remember { mutableStateOf(py.value) }
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    // Panelin ofsetsiz konumu ve boyutu (dp): ekran dışına çıkmasın diye sınır hesabında kullanılır
    var baseX by remember { mutableStateOf(Float.NaN) }
    var baseY by remember { mutableStateOf(0f) }
    var w by remember { mutableStateOf(0f) }
    var h by remember { mutableStateOf(0f) }
    fun clampX(v: Float) = if (baseX.isNaN()) v else v.coerceIn(-baseX, (areaW.value - w - baseX).coerceAtLeast(-baseX))
    fun clampY(v: Float) = if (baseX.isNaN()) v else v.coerceIn(-baseY, (areaH.value - h - baseY).coerceAtLeast(-baseY))
    // Dikey/yatay kararı yalnızca sürükleme bitince (ve ilk ölçümde) verilir; aksi halde boyut
    // değiştikçe karar da değişip sonsuz döngüye girebilir.
    var nearSide by remember { mutableStateOf(false) }
    fun decide() {
        if (baseX.isNaN() || areaW.value <= 0f) return
        val cx = baseX + x + w / 2
        nearSide = cx < areaW.value * 0.22f || cx > areaW.value * 0.78f
    }
    val handle = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragEnd = { px.value = x; py.value = y; decide() },
            onDragCancel = { px.value = x; py.value = y },
        ) { change, drag ->
            change.consume()
            x = clampX(x + drag.x / density)
            y = clampY(y + drag.y / density)
        }
    }
    // ekran döndüğünde veya eski kayıtlı konum ekran dışındaysa içeri çek
    LaunchedEffect(areaW, areaH, w, h, baseX) {
        val cx = clampX(x); val cy = clampY(y)
        if (cx != x || cy != y) { x = cx; y = cy; px.value = x; py.value = y }
    }
    val measured = !baseX.isNaN()
    LaunchedEffect(measured, areaW) { decide() }
    Box(
        modifier
            .offset { androidx.compose.ui.unit.IntOffset((x * density).roundToInt(), (y * density).roundToInt()) }
            // ölçüm kaydırmadan sonra yapılır: konum kaydırmayı içerir, kaydırma çıkarılınca asıl yer bulunur
            .onGloballyPositioned { c ->
                val pos = c.positionInParent()
                // kaydırma piksele yuvarlanarak uygulanır; aynısı çıkarılmazsa küçük farklar döngü yaratır
                val bx = ((pos.x - (x * density).roundToInt()) / density).roundToInt().toFloat()
                val by = ((pos.y - (y * density).roundToInt()) / density).roundToInt().toFloat()
                if (bx != baseX) baseX = bx
                if (by != baseY) baseY = by
                val nw = (c.size.width / density).roundToInt().toFloat(); val nh = (c.size.height / density).roundToInt().toFloat()
                if (nw != w) w = nw
                if (nh != h) h = nh
            },
    ) {
        content(handle, nearSide)
    }
}

/** Sürükleme tutamağı (altı nokta). */
@Composable
private fun Grip(modifier: Modifier, vertical: Boolean = false) {
    Box(modifier.size(if (vertical) 22.dp else 30.dp, if (vertical) 30.dp else 22.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.size(14.dp)) {
            val r = size.width / 14f
            for (i in 0..2) for (j in 0..1) {
                val (cx, cy) = if (vertical) (4f + j * 6f) to (2f + i * 5f) else (2f + i * 5f) to (4f + j * 6f)
                drawCircle(UI.TextFaint, 1.4f * r, androidx.compose.ui.geometry.Offset(cx * r, cy * r))
            }
        }
    }
}

/* ======================= sekmeler ======================= */

@Composable
private fun TabsRow(tabs: List<Pair<String, String>>, current: String, onTab: (String) -> Unit, onClose: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(UI.SurfaceAlt).padding(horizontal = 8.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for ((id, name) in tabs) {
            val sel = id == current
            Row(
                Modifier.clip(RoundedCornerShape(10.dp)).background(if (sel) UI.Surface else Color.Transparent)
                    .clickable(enabled = !sel) { onTab(id) }.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, fontSize = 13.sp, color = if (sel) UI.Text else UI.TextSoft, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp))
                Box(Modifier.size(26.dp).clip(CircleShape).clickable { onClose(id) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, "Sekmeyi kapat", tint = UI.TextFaint, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/* ======================= üst çubuk ======================= */

@Suppress("DEPRECATION")
@Composable
private fun EditorTopBar(
    ctl: EditorController, secondActive: Boolean, recState: Int, recTime: String, panel: Panel,
    onClose: () -> Unit, onRename: () -> Unit, onPagePanel: () -> Unit, onPages: () -> Unit, onBookmarks: () -> Unit,
    onExport: () -> Unit, onClear: () -> Unit, onImage: () -> Unit, onCamera: () -> Unit, onSettings: () -> Unit,
    onFormula: () -> Unit, onPlot: () -> Unit, onSticky: () -> Unit, onStamp: () -> Unit, onTimer: () -> Unit,
    onRecord: () -> Unit, onPauseResume: () -> Unit, onStopRecord: () -> Unit,
) {
    var more by remember { mutableStateOf(false) }
    var insert by remember { mutableStateOf(false) }
    val pv = ctl.pagesVersion
    Column(Modifier.fillMaxWidth().testTag("topbar").background(UI.Surface)) {
        Box(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp)) {
            Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                BarIcon(Icons.Rounded.ArrowBack, "Defterlere dön", onClick = onClose)
                Column(Modifier.widthIn(max = 240.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onRename).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(ctl.nb.name + (if (pv < 0) "" else ""), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = UI.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (ctl.nb.folder.isNotEmpty()) ctl.nb.folder else "Defter", fontSize = 12.sp, color = UI.TextSoft, maxLines = 1)
                }
            }
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                BarIcon(Icons.Rounded.Undo, "Geri al", enabled = ctl.canUndo) { ctl.undo() }
                BarIcon(Icons.Rounded.Redo, "Yinele", enabled = ctl.canRedo) { ctl.redo() }
            }
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
                    BarIcon(Icons.Rounded.AddCircleOutline, "Ekle") { insert = true }
                    DropdownMenu(insert, { insert = false }) {
                        DropdownMenuItem(text = { Text("Görsel (galeri)") }, leadingIcon = { Icon(Icons.Rounded.Image, null) }, onClick = { insert = false; onImage() })
                        DropdownMenuItem(text = { Text("Fotoğraf çek") }, leadingIcon = { Icon(Icons.Rounded.CameraAlt, null) }, onClick = { insert = false; onCamera() })
                        DropdownMenuItem(text = { Text("Formül") }, leadingIcon = { Text("∑", fontSize = 20.sp, color = UI.Text) }, onClick = { insert = false; onFormula() })
                        DropdownMenuItem(text = { Text("Fonksiyon grafiği") }, leadingIcon = { Text("ƒ", fontSize = 20.sp, color = UI.Text) }, onClick = { insert = false; onPlot() })
                        DropdownMenuItem(text = { Text("Yapışkan not") }, leadingIcon = { Icon(Icons.Rounded.Description, null) }, onClick = { insert = false; onSticky() })
                        DropdownMenuItem(text = { Text("Damga (✓ ✗ ★ Dikkat!)") }, leadingIcon = { Text("✓", fontSize = 20.sp, color = Color(0xFF16A34A)) }, onClick = { insert = false; onStamp() })
                    }
                }
                if (recState == 0) BarIcon(Icons.Rounded.FiberManualRecord, "Ders kaydı", tint = UI.Danger, onClick = onRecord)
                BarIcon(Icons.Rounded.Fullscreen, "Odak modu (menüleri gizle)") { ctl.view?.clearSelection(); ctl.presentation = true }
                BarIcon(Icons.Rounded.Share, "Dışa aktar / paylaş", onClick = onExport)
                Box {
                    BarIcon(Icons.Rounded.MoreVert, "Diğer") { more = true }
                    DropdownMenu(more, { more = false }) {
                        if (ctl.clipboardCount > 0) DropdownMenuItem(text = { Text("Yapıştır (${ctl.clipboardCount})") }, onClick = { more = false; ctl.paste() })
                        DropdownMenuItem(text = { Text(if (ctl.curtain) "Perdeyi kaldır" else "Perde") }, onClick = { more = false; ctl.curtain = !ctl.curtain; ctl.view?.invalidate() })
                        DropdownMenuItem(text = { Text("Sayaç") }, onClick = { more = false; onTimer() })
                        DropdownMenuItem(text = { Text("Sayfayı döndür") }, onClick = { more = false; ctl.rotatePage(ctl.currentPage) })
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
            .background(if (selected) UI.PrimarySoft else UI.SurfaceAlt)
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
private fun ToolDock(ctl: EditorController, handle: Modifier, onTimer: () -> Unit, onClear: () -> Unit, onEditPalette: (Boolean) -> Unit) {
    // seçili araca tekrar (çift) dokununca o aracın seçenekleri açılır
    var optionsFor by remember { mutableStateOf<Tool?>(null) }
    @Composable
    fun Btn(t: Tool, selectedOverride: Boolean? = null, label: String = shortName(t)) {
        Box(Modifier.testTag("tool-" + t.name)) {
            DockButton(ctl, t, selectedOverride, label) { optionsFor = if (optionsFor == t) null else t }
            DropdownMenu(optionsFor == t, { optionsFor = null }) {
                ToolOptions(ctl, onClear = { optionsFor = null; onClear() }, onEditPalette = { optionsFor = null; onEditPalette(it) })
            }
        }
    }
    var instMenu by remember { mutableStateOf(false) }
    var lessonMenu by remember { mutableStateOf(false) }
    val collapsed = ctl.settings.dockCollapsed.value
    FloatingCard(Modifier.testTag("dock"), radius = 22) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(vertical = 4.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // tutamak + daralt
            Grip(handle)
            if (collapsed) {
                // küçükken kalem ve silgi her zaman görünür; başka bir araç seçiliyse o da görünür
                Btn(Tool.PEN)
                Btn(Tool.ERASER)
                if (ctl.tool != Tool.PEN && ctl.tool != Tool.ERASER) Btn(ctl.tool, selectedOverride = true)
                Box(Modifier.size(40.dp).clip(CircleShape).clickable { ctl.settings.dockCollapsed.value = false }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "Menüyü aç", tint = UI.TextSoft)
                }
                return@Column
            }
            for (t in listOf(Tool.PEN, Tool.HIGHLIGHTER, Tool.ERASER, Tool.LASSO)) Btn(t)
            val gt = if (ctl.tool.isGeometry) ctl.tool else ctl.lastGeometryTool
            Btn(gt, selectedOverride = ctl.tool.isGeometry, label = "Geometri")
            for (t in listOf(Tool.TEXT, Tool.LASER, Tool.TEMP)) Btn(t)
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
            Box {
                val lessonOn = ctl.tool == Tool.SPOT || ctl.curtain || ctl.timerEnd > 0L
                DockItem(selected = lessonOn, label = "Ders", onClick = { lessonMenu = true }) {
                    ToolGlyph(Tool.SPOT, if (lessonOn) UI.Primary else UI.Text, size = 26.dp)
                }
                DropdownMenu(lessonMenu, { lessonMenu = false }) {
                    Text("Ders araçları", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    DropdownMenuItem(
                        text = { Text("Spot ışığı" + if (ctl.tool == Tool.SPOT) "  ✓" else "") },
                        onClick = { lessonMenu = false; ctl.selectTool(if (ctl.tool == Tool.SPOT) Tool.PEN else Tool.SPOT) },
                    )
                    DropdownMenuItem(
                        text = { Text(if (ctl.curtain) "Perdeyi kaldır" else "Perde (sayfanın altını kapat)") },
                        onClick = { lessonMenu = false; ctl.curtain = !ctl.curtain; ctl.view?.invalidate() },
                    )
                    DropdownMenuItem(text = { Text(if (ctl.timerEnd > 0L) "Sayaç (çalışıyor)" else "Sayaç") }, onClick = { lessonMenu = false; onTimer() })
                }
            }
            Box(Modifier.size(40.dp).clip(CircleShape).clickable { ctl.settings.dockCollapsed.value = true }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.KeyboardArrowUp, "Menüyü daralt", tint = UI.TextSoft)
            }
        }
    }
}

@Composable
private fun DockButton(ctl: EditorController, t: Tool, selectedOverride: Boolean? = null, label: String = shortName(t), onOptions: () -> Unit = {}) {
    val selected = selectedOverride ?: (ctl.tool == t)
    DockItem(selected, label, onClick = { if (ctl.tool == t) onOptions() else ctl.selectTool(t) }) {
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
    Tool.TEXT -> "Metin"; Tool.LASER -> "Lazer"; Tool.TEMP -> "Geçici"; Tool.SPOT -> "Spot"; Tool.SHAPE -> "Şekil"
    Tool.LINE -> "Doğru"; Tool.ARROW -> "Ok"; Tool.POLYGON -> "Çokgen"; Tool.COMPASS -> "Pergel"; Tool.MARK -> "İşaret"
    Tool.SOLID -> "3B"
}

/* ======================= bağlam çubuğu (renk, kalınlık, seçenekler) ======================= */

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ToolOptions(ctl: EditorController, onClear: () -> Unit, onEditPalette: (Boolean) -> Unit) {
    val t = ctl.tool
    Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp).widthIn(min = 240.dp, max = 420.dp)) {
        Text(t.title, fontWeight = FontWeight.SemiBold, color = UI.Text, modifier = Modifier.padding(bottom = 6.dp))
        when {
            t == Tool.ERASER -> {
                PillRow {
                    Pill("Bütün çizgi", !ctl.eraserPartial) { ctl.eraserPartial = false; ctl.settings.eraserPartial.value = false }
                    Pill("Kısmi", ctl.eraserPartial) { ctl.eraserPartial = true; ctl.settings.eraserPartial.value = true }
                }
                SectionTitle("Boyut")
                Row {
                    listOf(8, 14, 22).forEachIndexed { i, d ->
                        SizeDot(d, ctl.eraserSize == i, Color(0xFF98A2B3)) { ctl.eraserSize = i; ctl.settings.eraserSize.value = i }
                    }
                }
                LabeledRow("Sildikten sonra kaleme dön") { Switch(ctl.settings.eraserReturn.value, { ctl.settings.eraserReturn.value = it }) }
                Pill("Sayfayı temizle", false, onClick = onClear)
            }
            t == Tool.LASSO -> {
                Hint("Seçmek istediğiniz yazının etrafını çizin")
                if (ctl.clipboardCount > 0) Pill("Yapıştır (${ctl.clipboardCount})", false) { ctl.paste() }
            }
            t == Tool.LASER -> Hint("Kalemle gösterin; iz birkaç saniyede kaybolur")
            t == Tool.SPOT -> Hint("Kalemin gösterdiği yer aydınlanır")
            else -> {
                if (t.isGeometry) GeometryChooser(ctl)
                val hl = t == Tool.HIGHLIGHTER
                val colors = if (hl) ctl.settings.highlighterColors() else ctl.settings.paletteColors()
                val active = ctl.activeColor or 0xFF000000.toInt()
                SectionTitle("Renk")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    colors.forEach { c -> ColorDot(c, (c or 0xFF000000.toInt()) == active, 34, onLongClick = { onEditPalette(hl) }) { ctl.setColor(c) } }
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(UI.SurfaceAlt).clickable { onEditPalette(hl) },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Edit, "Renkleri düzenle", tint = UI.TextSoft, modifier = Modifier.size(16.dp)) }
                }
                if (t != Tool.TEXT) {
                    SectionTitle("Kalınlık")
                    Row {
                        val widths = if (hl) listOf(8f, 14f, 22f) else listOf(1.0f, 2.0f, 3.6f)
                        widths.forEachIndexed { i, w ->
                            SizeDot(6 + i * 5, abs(ctl.activeWidth - w) < 0.05f, Color(ctl.activeColor or 0xFF000000.toInt())) { ctl.setWidth(w) }
                        }
                    }
                }
                if (t == Tool.PEN || t == Tool.HIGHLIGHTER) {
                    SectionTitle("Kayıtlı kalemler")
                    PresetRow(ctl)
                }
            }
        }
    }
}

/** Kayıtlı kalemler: dokun = seç, uzun bas = sil, + = şu anki kalemi kaydet. */
@Composable
private fun PresetRow(ctl: EditorController) {
    val list = ctl.settings.presetList()
    var menuFor by remember { mutableStateOf<com.poyankee.note.data.PenPreset?>(null) }
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        list.forEach { p ->
            Box {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .combinedClickable(onClick = { ctl.applyPreset(p) }, onLongClick = { menuFor = p }),
                    contentAlignment = Alignment.Center,
                ) { ToolGlyph(if (p.highlighter) Tool.HIGHLIGHTER else Tool.PEN, Color(p.color or 0xFF000000.toInt()), Color(p.color or 0xFF000000.toInt()), 24.dp) }
                DropdownMenu(menuFor == p, { menuFor = null }) {
                    DropdownMenuItem(text = { Text("Kayıtlı kalemi sil") }, onClick = { ctl.settings.removePreset(p); menuFor = null })
                }
            }
        }
        Box(
            Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(UI.SurfaceAlt).clickable { ctl.saveCurrentAsPreset() }.padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) { Text(if (list.isEmpty()) "+ Kalemi kaydet" else "+", fontSize = 13.sp, color = UI.TextSoft) }
    }
}

@Composable
private fun GeometryChooser(ctl: EditorController) {
    var opts by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        for (g in listOf(Tool.SHAPE, Tool.LINE, Tool.ARROW, Tool.POLYGON, Tool.COMPASS, Tool.MARK, Tool.SOLID)) {
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
                    Tool.SOLID -> ctl.solidKind.title.substringBefore(" ")
                    else -> "Seçenekler"
                }, opts,
            ) { opts = !opts }
            DropdownMenu(opts, { opts = false }) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp).width(300.dp)) {
                    Text(ctl.tool.title, fontWeight = FontWeight.SemiBold)
                    Text(
                        when (ctl.tool) {
                            Tool.SHAPE -> "Her çizimi düzgün şekle çevirir."
                            Tool.LINE -> "Uzunluk ve açıyı gösterir; köşelere ve 15°'ye yapışır."
                            Tool.ARROW -> "Vektör / ok çizer."
                            Tool.POLYGON -> "Merkezden bir köşeye doğru çekin."
                            Tool.COMPASS -> "Merkezi seçip yarıçap kadar çekin, sonra yayı çizin."
                            Tool.SOLID -> "Kutu çizer gibi çapraz çekin; arkadaki kenarlar kesikli çizilir."
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
                    if (ctl.tool == Tool.SOLID) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                            Solids.Kind.entries.forEach { k -> Pill(k.title, ctl.solidKind == k) { ctl.solidKind = k } }
                        }
                    }
                    LabeledRow("Kesikli çizgi") { Switch(ctl.shapeDashed, { ctl.shapeDashed = it }) }
                    LabeledRow("İçini doldur", "Kalem renginin açık tonuyla") { Switch(ctl.shapeFill, { ctl.shapeFill = it }) }
                    LabeledRow("Köşelere harf (A, B, C)") { Switch(ctl.settings.autoLetters.value, { ctl.settings.autoLetters.value = it }) }
                    LabeledRow("Ölçüleri yaz (cm, °)") { Switch(ctl.settings.autoMeasures.value, { ctl.settings.autoMeasures.value = it }) }
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
private fun PageNavigator(ctl: EditorController, handle: Modifier, onGoto: () -> Unit, onPages: () -> Unit) {
    val pv = ctl.pagesVersion
    Row(verticalAlignment = Alignment.CenterVertically) {
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
                Grip(handle, vertical = true)
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
            SelAction(Icons.Rounded.ContentCopy, "Kopyala", UI.Text) { ctl.view?.selectionCopy() }
            SelAction(Icons.Rounded.ContentCut, "Kes", UI.Text) { ctl.view?.selectionCut() }
            SelAction(Icons.Rounded.AddCircleOutline, "Çoğalt", UI.Text) { ctl.view?.selectionDuplicate() }
            if (ctl.selectionHasShapes) SelAction(Icons.Rounded.FormatColorFill, "Doldur", UI.Text) { ctl.view?.selectionFill() }
            SelAction(Icons.Rounded.Delete, "Sil", UI.Danger) { ctl.view?.selectionDelete() }
            Text("Renk: üstten seçin", fontSize = 12.sp, color = UI.TextSoft, modifier = Modifier.padding(horizontal = 8.dp))
            SmallRound(Icons.Rounded.Close, "Seçimi kaldır", true) { ctl.view?.clearSelection() }
        }
    }
}

@Composable
private fun SelAction(icon: ImageVector, text: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(5.dp)); Text(text, color = tint, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

/**
 * Odak modu paneli: menüler gizliyken sadece temel araçlar. Ekranın sağ/sol kenarına
 * taşınınca dikey, diğer yerlerde yatay durur.
 */
@Suppress("DEPRECATION")
@Composable
private fun FocusBar(ctl: EditorController, handle: Modifier, vertical: Boolean, recState: Int, recTime: String, onStopRecord: () -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    @Composable
    fun Items() {
        Grip(handle, vertical = !vertical)
        if (expanded) {
            for (t in listOf(Tool.PEN, Tool.HIGHLIGHTER, Tool.ERASER, Tool.LASER)) {
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
                Text("● $recTime", color = UI.Danger, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.padding(4.dp))
                SmallRound(Icons.Rounded.Stop, "Kaydı bitir", true, onStopRecord)
            }
        }
        val collapseIcon = if (vertical) (if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown)
        else (if (expanded) Icons.Rounded.ChevronLeft else Icons.Rounded.ChevronRight)
        SmallRound(collapseIcon, "Daralt", true) { expanded = !expanded }
        SmallRound(Icons.Rounded.FullscreenExit, "Odak modundan çık", true) { ctl.presentation = false }
    }
    FloatingCard(radius = 28) {
        if (vertical) Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp)) { Items() }
        else Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) { Items() }
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
@OptIn(ExperimentalLayoutApi::class)
private fun PageStylePanel(ctl: EditorController, onClose: () -> Unit) {
    val pv = ctl.pagesVersion
    val page = ctl.nb.pages.getOrNull(ctl.currentPage)
    val isPdf = page?.background is Background.PdfPage
    val current = (page?.background as? Background.Paper) ?: ctl.nb.defaultPaper
    var style by remember(ctl.currentPage, isPdf, page?.background, pv) { mutableStateOf(current) }
    var all by remember { mutableStateOf(false) }
    var customColor by remember { mutableStateOf(false) }
    val pickPaper = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) ctl.setPaperImage(uri, all) }
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
                val custom = PaperColors.all.none { it.first == style.color }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { customColor = true }) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape)
                            .border(if (custom) 3.dp else 1.dp, if (custom) UI.Primary else UI.Border, CircleShape)
                            .padding(if (custom) 4.dp else 0.dp).clip(CircleShape)
                            .background(if (custom) Color(style.color) else UI.SurfaceAlt),
                        contentAlignment = Alignment.Center,
                    ) { if (!custom) Icon(Icons.Rounded.Add, "Başka renk", tint = UI.TextSoft, modifier = Modifier.size(20.dp)) }
                    Text("Diğer", fontSize = 10.sp, color = UI.TextSoft, modifier = Modifier.padding(top = 3.dp))
                }
            }
            if (customColor) {
                var picked by remember { mutableStateOf(style.color) }
                val start = remember { style.color }
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { customColor = false }, containerColor = UI.Surface,
                    title = { Text("Kağıt rengi") },
                    text = { Column { HsvPicker(start) { picked = it or 0xFF000000.toInt() } } },
                    confirmButton = { Button(onClick = { customColor = false; apply(style.copy(color = picked)) }, shape = RoundedCornerShape(10.dp)) { Text("Uygula") } },
                    dismissButton = { androidx.compose.material3.TextButton(onClick = { customColor = false }) { Text("Vazgeç", color = UI.TextSoft) } },
                )
            }

            if (!isPdf && page != null) {
                SectionTitle("Sayfa boyutu")
                val (fw, fh) = ctl.fullScreenSize()
                val sizes = listOf(
                    "A4 dikey" to (595f to 842f), "A4 yatay" to (842f to 595f),
                    "16:9" to (960f to 540f), "Tam ekran" to (fw to fh),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sizes.forEach { (label, wh) ->
                        val sel = abs(page.width - wh.first) < 1f && abs(page.height - wh.second) < 1f
                        Pill(label, sel) { ctl.setPageSize(ctl.currentPage, wh.first, wh.second, all) }
                    }
                }
                Text("\"Tam ekran\" sayfayı bu ekranın oranına getirir; kenar boşluğu kalmaz.", fontSize = 12.sp, color = UI.TextSoft, modifier = Modifier.padding(top = 4.dp))
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
                SectionTitle("Kendi deseniniz")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(if (style.image.isEmpty()) "Resim / PDF'ten desen" else "Deseni değiştir", false) { pickPaper.launch(arrayOf("image/*", "application/pdf")) }
                    if (style.image.isNotEmpty()) Pill("Kaldır", false) { apply(style.copy(image = "")) }
                }
                Text("Çalışma kağıdınızı ya da özel bir deseni sayfa zemini yapın.", fontSize = 12.sp, color = UI.TextSoft, modifier = Modifier.padding(top = 4.dp))
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
private fun PagesPanel(ctl: EditorController, onCrop: (Int) -> Unit, onClose: () -> Unit) {
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
                            DropdownMenuItem(text = { Text("90° döndür") }, onClick = { menu = false; ctl.rotatePage(i) })
                            if (page.background is Background.PdfPage) DropdownMenuItem(text = { Text("Kırp") }, onClick = { menu = false; onCrop(i) })
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
