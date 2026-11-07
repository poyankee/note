@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.poyankee.note.ui

import com.poyankee.note.core.L

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
import androidx.compose.material.icons.outlined.Image as OImage
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.MenuOpen
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.AssignmentLate
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.CropSquare
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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

private enum class Panel { NONE, PAGE, PAGES, LIBRARY }

@Composable
fun EditorScreen(
    ctl: EditorController,
    onClose: () -> Unit,
    onSettings: () -> Unit,
    tabs: List<Pair<String, String>> = emptyList(),
    onTab: (String) -> Unit = {},
    onCloseTab: (String) -> Unit = {},
    onOpenTodo: (String, Int) -> Unit = { _, _ -> },
    onInsertPdf: (Uri, Int) -> Unit = { _, _ -> },
    onExtract: (List<Int>, String) -> Unit = { _, _ -> },
    /** Sekme çubuğundaki "+" (yeni defter). */
    onNew: () -> Unit = {},
    /** Bölünmüş görünüm: null = desteklenmiyor (dar ekran), true = notlar yanda açık. */
    split: Boolean? = null,
    onSplit: () -> Unit = {},
) {
    val context = LocalContext.current
    var showTodos by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showValueTable by remember { mutableStateOf(false) }
    var showExtract by remember { mutableStateOf(false) }
    var extractRange by remember { mutableStateOf<String?>(null) }
    var tagEdit by remember { mutableStateOf<String?>(null) }
    var showWrongFilter by remember { mutableStateOf(false) }
    var saveToLib by remember { mutableStateOf(false) }
    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) onInsertPdf(uri, ctl.currentPage) }
    ctl.openRequest?.let { req -> LaunchedEffect(req) { ctl.openRequest = null; onOpenTodo(req.first, req.second) } }
    val activity = context as Activity
    val s = ctl.settings
    val ink = remember { InkView(context, ctl).also { ctl.view = it; it.keepScreenOn = true } }
    val dark = UI.dark
    val desk = s.deskColor.value
    LaunchedEffect(dark, desk) { ink.deskColor = if (desk != 0) desk else UI.Desk.toArgb(); ink.invalidate() }

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
        if (!granted) ctl.message = L.t("Mikrofon izni verilmedi; video sessiz kaydedilecek.")
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
        ctl.message = if (name != null) "Video kaydedildi: Galeri › Movies/Note › $name" else L.t("Video kaydedilemedi")
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
        try { takePhoto.launch(uri) } catch (_: Exception) { ctl.message = L.t("Kamera açılamadı") }
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
    // yeni tasarım: üst çubuk sabit bir kart; Gelişmiş ayarlardan "sayfayla birlikte kaysın" seçilebilir
    // başlık sabit değil: ilk sayfanın üstünde, sayfayla birlikte kayar
    val fixedHeader = false
    // birden çok defter açıksa üstte sekmeler: aşağı kaydırınca gizlenir, yukarı kaydırınca görünür
    val showTabs = tabs.size > 1 && !ctl.presentation
    var tabsShown by remember { mutableStateOf(true) }
    LaunchedEffect(ink) {
        var last = ink.sy
        androidx.compose.runtime.snapshotFlow { ctl.viewTick }.collect {
            val y = ink.sy
            if (y > last + 0.5f) tabsShown = false else if (y < last - 0.5f) tabsShown = true
            last = y
        }
    }
    val tabsReserve = if (showTabs) 64.dp else 0.dp
    val topBar: @Composable () -> Unit = {
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
                    onTodos = { showTodos = !showTodos },
                    toolsInBar = false,
                    headerMode = !fixedHeader,
                    onTimer2 = { showTimer = true },
                    onExtra = { key ->
                        when (key) {
                            "library" -> panel = if (panel == Panel.LIBRARY) Panel.NONE else Panel.LIBRARY
                            "wrong" -> { ctl.view?.clearSelection(); ctl.captureMode = true }
                            "valuetable" -> showValueTable = true
                            "search" -> showSearch = true
                            "print" -> printNotebook(context, ctl)
                            "extract" -> showExtract = true
                            "insertpdf" -> pickPdf.launch(arrayOf("application/pdf"))
                        }
                    },
                )
    }
    Column(Modifier.fillMaxSize().background(UI.Desk)) {
        if (!ctl.presentation && fixedHeader) Box(
            Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 10.dp).fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = UI.Shadow, spotColor = UI.Shadow).clip(RoundedCornerShape(20.dp)),
        ) { topBar() }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            val areaW = maxWidth; val areaH = maxHeight
            AndroidView(factory = { ink }, modifier = Modifier.fillMaxSize().clipToBounds())

            // defter menüsü: ilk sayfanın üstünde, sayfayla birlikte kayar (isteğe bağlı)
            if (!ctl.presentation && !fixedHeader) {
                @Suppress("UNUSED_VARIABLE") val vt = ctl.viewTick
                val dens = androidx.compose.ui.platform.LocalDensity.current.density
                val r = ink.pageScreenRect(0)
                val viewW = areaW.value * dens
                val minW = minOf(viewW - 24 * dens, 760 * dens)
                val wPx = r.width().coerceIn(minW, viewW - 24 * dens)
                val xPx = (r.centerX() - wPx / 2).coerceIn(12 * dens, viewW - wPx - 12 * dens)
                val reservePx = tabsReserve.value * dens
                val yPx = r.top - (ink.headerPx - reservePx) - 10 * dens
                if (yPx + ink.headerPx > -20) Box(
                    Modifier.offset { androidx.compose.ui.unit.IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
                        .width((wPx / dens).dp)
                        .onGloballyPositioned { c -> val h = c.size.height.toFloat() + reservePx; if (h != ink.headerPx) ink.setHeaderHeight(h) }
                        .shadow(8.dp, RoundedCornerShape(UI.radius.dp)).clip(RoundedCornerShape(UI.radius.dp)),
                ) {
                    topBar()
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                showTabs && tabsShown,
                enter = androidx.compose.animation.slideInVertically { -it } + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically { -it } + androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
            ) {
                TabsRow(tabs, ctl.nb.id, onTab, onCloseTab, onNew, null, {})
            }

            if (!ctl.presentation) {
                // yüzen araç menüsü (tutamaktan sürüklenip taşınabilir, daraltılabilir)
                Draggable(s.dockX, s.dockY, areaW, areaH, Modifier.align(if (dockLeft) Alignment.CenterStart else Alignment.CenterEnd).padding(horizontal = 14.dp)) { handle, nearSide ->
                    ToolDock(
                        ctl, handle, vertical = nearSide, onClear = { showClear = true }, onEditPalette = { paletteFor = it },
                        onBack = onClose,
                        onPages = { panel = if (panel == Panel.PAGES) Panel.NONE else Panel.PAGES },
                        onLibrary = { panel = if (panel == Panel.LIBRARY) Panel.NONE else Panel.LIBRARY },
                        onTodos = { showTodos = !showTodos },
                        onWrong = { ctl.view?.clearSelection(); ctl.captureMode = true },
                        onImage = { pickImage.launch("image/*") }, showBack = false,
                        insertMenu = { close ->
                            InsertMenuItems(
                                close, onImage = { pickImage.launch("image/*") }, onCamera = { openCamera() },
                                onFormula = { ctl.requestFormula(ctl.view?.visibleCenter()?.first ?: ctl.currentPage, null) },
                                onPlot = { showPlot = true }, onSticky = { ctl.requestStickyNote() }, onStamp = { showStamp = true },
                                onExtra = { key ->
                                    when (key) {
                                        "library" -> panel = if (panel == Panel.LIBRARY) Panel.NONE else Panel.LIBRARY
                                        "wrong" -> { ctl.view?.clearSelection(); ctl.captureMode = true }
                                        "valuetable" -> showValueTable = true
                                        "insertpdf" -> pickPdf.launch(arrayOf("application/pdf"))
                                    }
                                },
                            )
                        },
                    )
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
            } else {
                Draggable(s.presX, s.presY, areaW, areaH, Modifier.align(if (dockLeft) Alignment.BottomStart else Alignment.BottomEnd).padding(14.dp)) { handle, vertical ->
                    FocusBar(ctl, handle, vertical, recState, recTime, onStopRecord = { stopRecording() })
                }
            }

            if (ctl.pageCount > 1) PageScrollbar(ctl, Modifier.align(Alignment.CenterEnd))

            if (!ctl.presentation && !ctl.hasSelection && !ctl.hasRegion && !ctl.captureMode) PagePill(ctl, ink, Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp))

            if (ctl.hasRegion && !ctl.hasSelection) FloatingCard(Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp), radius = 26) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    if (!ctl.isWrongBook) SelAction(Icons.Rounded.AssignmentLate, L.t("Bu alanı yanlış defterine ekle"), UI.Primary) { ctl.view?.regionToWrong() }
                    else Text(L.t("Seçilecek çizim yok"), color = UI.TextSoft, modifier = Modifier.padding(10.dp))
                    SmallRound(Icons.Rounded.Close, L.t("Vazgeç"), true) { ctl.view?.clearRegion() }
                }
            }
            if (ctl.hasSelection) SelectionBar(ctl, onSaveToLib = { saveToLib = true }, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp))

            // sağdan açılan paneller
            if (showTodos && !ctl.presentation) Box(Modifier.align(Alignment.CenterStart)) {
                TodoPanel(
                    com.poyankee.note.data.TodoList.get(context),
                    TodoLink(ctl.nb.id, ctl.nb.name, ctl.currentPage),
                    onOpen = { id, page -> if (id == ctl.nb.id) ctl.goToPage(page) else { showTodos = false; onOpenTodo(id, page) } },
                    onClose = { showTodos = false },
                )
            }
            SidePanel(panel == Panel.PAGE) { PageStylePanel(ctl) { panel = Panel.NONE } }
            SidePanel(panel == Panel.LIBRARY) { LibraryPanel(onInsert = { ctl.insertLibrary(it) }) { panel = Panel.NONE } }
            if (ctl.captureMode) {
                Surface(
                    color = Color(0xF01565C0), shape = RoundedCornerShape(14.dp), shadowElevation = 6.dp,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 4.dp)) {
                        Text(L.t("Soruyu kalemle dikdörtgen içine alın"), color = Color.White, fontSize = 14.sp)
                        androidx.compose.material3.TextButton(onClick = { ctl.captureMode = false }) { Text(L.t("Vazgeç"), color = Color.White, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
            val wrongPage = ctl.nb.pages.getOrNull(ctl.currentPage)
            val wrongSrc = remember(ctl.currentPage, ctl.pagesVersion) { wrongPage?.let { s.wrongSource(it.id) } }
            if (ctl.isWrongBook && wrongPage != null && !ctl.presentation && !ctl.captureMode) {
                @Suppress("UNUSED_VARIABLE") val wv = ctl.wrongVersion
                val tag = s.wrongTag(wrongPage.id); val done = s.wrongDone(wrongPage.id)
                Surface(
                    color = UI.Surface, shape = RoundedCornerShape(22.dp), shadowElevation = 6.dp,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                        if (wrongSrc != null) androidx.compose.material3.TextButton(onClick = { onOpenTodo(wrongSrc.first, wrongSrc.second) }) { Text(L.t("Kaynağa git ›"), fontWeight = FontWeight.SemiBold) }
                        androidx.compose.material3.TextButton(onClick = { tagEdit = tag }) { Text(if (tag.isEmpty()) L.t("Konu ekle") else "Konu: $tag") }
                        androidx.compose.material3.TextButton(onClick = { s.setWrongDone(wrongPage.id, !done); ctl.wrongVersion++ }) {
                            Text(if (done) L.t("✓ Çözüldü") else L.t("☐ Çözüldü"), color = if (done) Color(0xFF16A34A) else UI.Text)
                        }
                        androidx.compose.material3.TextButton(onClick = { ctl.reviewMode = !ctl.reviewMode; ctl.view?.refreshAll() }) {
                            Text(if (ctl.reviewMode) L.t("Tekrar modu: açık") else L.t("Tekrar modu"), color = if (ctl.reviewMode) UI.Primary else UI.Text)
                        }
                        androidx.compose.material3.TextButton(onClick = { showWrongFilter = true }) { Text(L.t("Sorular ▾")) }
                    }
                }
            } else if (wrongSrc != null && !ctl.presentation && !ctl.captureMode) {
                Surface(
                    color = UI.Surface, shape = RoundedCornerShape(20.dp), shadowElevation = 6.dp,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp).clip(RoundedCornerShape(20.dp)).clickable { onOpenTodo(wrongSrc.first, wrongSrc.second) },
                ) { Text(L.t("Kaynağa git ›"), color = UI.Primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            }
            SidePanel(panel == Panel.PAGES) { PagesPanel(ctl, onCrop = { cropIndex = it }, onExtract = { r -> extractRange = r; showExtract = true }) { panel = Panel.NONE } }

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
    if (showSearch) SearchDialog(ctl) { showSearch = false }
    tagEdit?.let { t ->
        val page = ctl.nb.pages.getOrNull(ctl.currentPage)
        NameDialog(L.t("Konu etiketi"), L.t("Konu (ör. Türev, Olasılık)"), t, L.t("Kaydet"), onDismiss = { tagEdit = null }) { v ->
            tagEdit = null
            if (page != null) { s.setWrongTag(page.id, v); ctl.wrongVersion++ }
        }
    }
    if (showWrongFilter) WrongFilterDialog(ctl) { showWrongFilter = false }
    if (showValueTable) ValueTableDialog(ctl) { showValueTable = false }
    if (showExtract) ExtractDialog(ctl, extractRange, onDismiss = { showExtract = false; extractRange = null }) { idx, name -> showExtract = false; extractRange = null; onExtract(idx, name) }
    if (saveToLib) NameDialog(L.t("Kütüphaneye kaydet"), L.t("Çizimin adı"), "", L.t("Kaydet"), onDismiss = { saveToLib = false }) { name ->
        saveToLib = false
        val n = com.poyankee.note.data.UserLibrary.get(context).save(name, ctl.view?.selectedItems() ?: emptyList())
        ctl.message = if (n > 0) L.t("Kütüphaneye kaydedildi (Kütüphane › Benim çizimlerim)") else L.t("Görseller kütüphaneye kaydedilemez")
    }
    if (showExport) ExportDialog(ctl) { showExport = false }
    if (showRename) NameDialog(L.t("Defteri yeniden adlandır"), L.t("Defter adı"), ctl.nb.name, L.t("Kaydet"), onDismiss = { showRename = false }) { showRename = false; ctl.rename(it) }
    if (showClear) ConfirmDialog(L.t("Sayfayı temizle"), L.t("Bu sayfadaki tüm yazılar silinecek. (Geri al ile geri getirebilirsiniz.)"), L.t("Temizle"), { ctl.clearPage() }, { showClear = false })
    ctl.textRequest?.let { TextDialog(ctl, it) }
    ctl.formulaRequest?.let { FormulaDialog(ctl, it) }
    if (showPlot) PlotDialog(ctl) { showPlot = false }
    if (showStamp) StampDialog(ctl) { showStamp = false }
    if (cropIndex >= 0) CropDialog(ctl, cropIndex) { cropIndex = -1 }
    paletteFor?.let { hl -> PaletteDialog(ctl, hl) { paletteFor = null } }
}

/* ======================= sağ kenar sayfa çubuğu ======================= */

/**
 * Normalde gizli; sayfa kaydırılınca sağ kenarda belirir. Tutamaktan sürüklenerek hızlı gezinilir,
 * sürüklerken sayfa numarası gösterilir. Bırakınca birkaç saniyede kaybolur.
 */
@Composable
private fun PageScrollbar(ctl: EditorController, modifier: Modifier) {
    val tick = ctl.scrollTick
    var dragging by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(tick, dragging) {
        if (tick == 0 && !dragging) return@LaunchedEffect
        visible = true
        if (!dragging) { delay(1800); visible = false }
    }
    val view = ctl.view ?: return
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val frac = view.scrollFraction() + 0f * tick
    val visibleFrac = view.visibleFraction()
    AnimatedVisibility(visible, modifier, enter = fadeIn(), exit = fadeOut()) {
        BoxWithConstraints(Modifier.fillMaxHeight().width(170.dp).padding(vertical = 70.dp)) {
            val trackH = maxHeight.value
            val thumbH = (trackH * visibleFrac).coerceIn(44f, trackH)
            val top = (trackH - thumbH) * frac
            Box(Modifier.align(Alignment.TopEnd).padding(end = 4.dp).width(4.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(Color(0x22000000)))
            Box(
                Modifier.align(Alignment.TopEnd).offset { androidx.compose.ui.unit.IntOffset(0, (top * density).roundToInt()) }
                    .width(28.dp).height(thumbH.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { dragging = true },
                            onDragEnd = { dragging = false },
                            onDragCancel = { dragging = false },
                        ) { change, drag ->
                            change.consume()
                            val range = (trackH - thumbH).coerceAtLeast(1f)
                            view.scrollToFraction(view.scrollFraction() + drag.y / density / range)
                            ctl.scrollTick++
                        }
                    },
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(Modifier.width(10.dp).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(if (dragging) UI.Primary else Color(0x99475467)))
            }
            if (dragging) {
                Surface(
                    color = Color(0xE61D2330), shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.align(Alignment.TopEnd).offset { androidx.compose.ui.unit.IntOffset((-40 * density).roundToInt(), ((top + thumbH / 2 - 16) * density).roundToInt()) },
                ) {
                    Text("${ctl.currentPage + 1} / ${ctl.pageCount}", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }
    }
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
    var decided by remember { mutableStateOf(false) }
    // dikey olunca hangi kenara yapışık kalacak (null = yapışma yok)
    var anchorRight by remember { mutableStateOf<Boolean?>(null) }
    fun decide() {
        if (baseX.isNaN() || areaW.value <= 0f) return
        // sol/sağ kenara değiyorsa (köşelerde değil) dikey
        val left = baseX + x; val top = baseY + y
        val touchesSide = left < 48f || left + w > areaW.value - 48f
        val cy = top + h / 2
        val inCorner = cy < areaH.value * 0.12f || cy > areaH.value * 0.88f
        nearSide = touchesSide && !inCorner
        anchorRight = if (nearSide) left + w / 2 > areaW.value / 2 else null
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
        if (!baseX.isNaN() && !decided) { decided = true; decide() }
        // boyut değişince (yatay → dikey) kenara yapışık kal
        anchorRight?.let { right ->
            if (!baseX.isNaN()) {
                val ax = if (right) clampX(Float.MAX_VALUE / 4) else clampX(-Float.MAX_VALUE / 4)
                if (ax != x) { x = ax; px.value = x }
            }
        }
    }
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
private fun TabsRow(tabs: List<Pair<String, String>>, current: String, onTab: (String) -> Unit, onClose: (String) -> Unit, onNew: () -> Unit, split: Boolean?, onSplit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(UI.Desk.copy(alpha = 0.97f)).padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (split != null) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onSplit),
                contentAlignment = Alignment.Center,
            ) { Icon(if (split) Icons.Rounded.MenuOpen else Icons.Rounded.Menu, L.t(if (split) "Notları gizle" else "Notları göster"), tint = UI.Text) }
            Spacer(Modifier.width(6.dp))
        }
        Row(
            Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for ((id, name) in tabs) {
                val sel = id == current
                Column(
                    Modifier.height(48.dp).shadow(if (sel) 3.dp else 0.dp, RoundedCornerShape(14.dp), ambientColor = UI.Shadow, spotColor = UI.Shadow)
                        .clip(RoundedCornerShape(14.dp)).background(if (sel) UI.Surface else UI.SurfaceAlt)
                        .clickable(enabled = !sel) { onTab(id) },
                ) {
                    Row(Modifier.weight(1f).padding(start = 18.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(name, fontSize = 17.sp, color = if (sel) UI.Primary else UI.Text, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 200.dp))
                        Box(Modifier.padding(start = 6.dp).size(32.dp).clip(CircleShape).clickable { onClose(id) }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, L.t("Sekmeyi kapat"), tint = if (sel) UI.Primary else UI.TextSoft, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (sel) Box(Modifier.padding(horizontal = 14.dp).fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(UI.Primary))
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(UI.SurfaceAlt).clickable(onClick = onNew),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, L.t("Yeni defter"), tint = UI.Text) }
    }
}

/** Alttaki sayfa ve yakınlaştırma çubuğu: ‹ 1/3 ›  −  %100  + */
@Composable
private fun PagePill(ctl: EditorController, ink: InkView, modifier: Modifier) {
    @Suppress("UNUSED_VARIABLE") val vt = ctl.viewTick
    FloatingCard(modifier.testTag("pagepill"), radius = 24) {
        Row(Modifier.height(48.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallRound(Icons.Rounded.ChevronLeft, L.t("Önceki sayfa"), ctl.currentPage > 0) { ctl.goToPage(ctl.currentPage - 1) }
            Text("${ctl.currentPage + 1} / ${ctl.pageCount}", fontSize = 16.sp, color = UI.Text, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 6.dp))
            SmallRound(Icons.Rounded.ChevronRight, L.t("Sonraki sayfa"), ctl.currentPage < ctl.pageCount - 1) { ctl.goToPage(ctl.currentPage + 1) }
            Box(Modifier.padding(horizontal = 8.dp).width(1.dp).height(24.dp).background(UI.Border))
            SmallRound(Icons.Rounded.Remove, L.t("Uzaklaştır"), true) { ink.zoomStep(1f / 1.25f) }
            Text("%${ink.zoomPercent()}", fontSize = 16.sp, color = UI.Text, fontWeight = FontWeight.Medium,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { ink.fitToWidth() }.padding(horizontal = 6.dp, vertical = 4.dp))
            SmallRound(Icons.Rounded.Add, L.t("Yakınlaştır"), true) { ink.zoomStep(1.25f) }
        }
    }
}

/** "Ekle" menüsünün içeriği (sol araç çubuğundaki ⊕). */
@Composable
private fun InsertMenuItems(
    close: () -> Unit, onImage: () -> Unit, onCamera: () -> Unit, onFormula: () -> Unit, onPlot: () -> Unit,
    onSticky: () -> Unit, onStamp: () -> Unit, onExtra: (String) -> Unit,
) {
                DropdownMenuItem(text = { Text(L.t("Görsel (galeri)")) }, leadingIcon = { Icon(Icons.Rounded.Image, null) }, onClick = { close(); onImage() })
                DropdownMenuItem(text = { Text(L.t("Fotoğraf çek")) }, leadingIcon = { Icon(Icons.Rounded.CameraAlt, null) }, onClick = { close(); onCamera() })
                DropdownMenuItem(text = { Text(L.t("Formül")) }, leadingIcon = { Text("∑", fontSize = 20.sp, color = UI.Text) }, onClick = { close(); onFormula() })
                DropdownMenuItem(text = { Text(L.t("Fonksiyon grafiği")) }, leadingIcon = { Text("ƒ", fontSize = 20.sp, color = UI.Text) }, onClick = { close(); onPlot() })
                DropdownMenuItem(text = { Text(L.t("Yapışkan not")) }, leadingIcon = { Icon(Icons.Rounded.Description, null) }, onClick = { close(); onSticky() })
                DropdownMenuItem(text = { Text(L.t("Damga (✓ ✗ ★ Dikkat!)")) }, leadingIcon = { Text("✓", fontSize = 20.sp, color = Color(0xFF16A34A)) }, onClick = { close(); onStamp() })
                DropdownMenuItem(text = { Text(L.t("Değer tablosundan grafik")) }, leadingIcon = { Text("⋰", fontSize = 20.sp, color = UI.Text) }, onClick = { close(); onExtra("valuetable") })
                DropdownMenuItem(text = { Text(L.t("Hazır kütüphane")) }, leadingIcon = { Icon(Icons.Rounded.Category, null) }, onClick = { close(); onExtra("library") })
                HorizontalDivider(color = UI.Border)
                DropdownMenuItem(text = { Text(L.t("Soruyu yanlış defterine ekle")) }, leadingIcon = { Text("✗", fontSize = 20.sp, color = UI.Danger) }, onClick = { close(); onExtra("wrong") })
                DropdownMenuItem(text = { Text(L.t("Başka PDF'ten sayfa ekle")) }, leadingIcon = { Icon(Icons.Rounded.Description, null) }, onClick = { close(); onExtra("insertpdf") })
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
    onTodos: () -> Unit = {},
    onExtra: (String) -> Unit = {},
    toolsInBar: Boolean = false,
    onTimer2: () -> Unit = {},
    headerMode: Boolean = false,
) {
    var more by remember { mutableStateOf(false) }
    var titleMenu by remember { mutableStateOf(false) }
    @Suppress("UNUSED_VARIABLE") val pv = ctl.pagesVersion
    Row(
        Modifier.fillMaxWidth().testTag("topbar").background(UI.Surface).height(68.dp).padding(start = 8.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarIcon(Icons.Rounded.ArrowBack, L.t("Defterlere dön"), onClick = onClose)
        Box(Modifier.padding(horizontal = 6.dp).width(1.dp).height(34.dp).background(UI.Border))
        // başlık ▾: defterle ilgili işlemler
        Box(Modifier.weight(1f)) {
            Column(Modifier.clip(RoundedCornerShape(10.dp)).clickable { titleMenu = true }.testTag("title").padding(horizontal = 10.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ctl.nb.name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = UI.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Icon(Icons.Rounded.KeyboardArrowDown, null, tint = UI.Text, modifier = Modifier.padding(start = 4.dp).size(22.dp))
                }
                Text(
                    (if (ctl.nb.folder.isNotEmpty()) ctl.nb.folder + " · " else "") + (if (ctl.pdf != null) "PDF" else L.t("Defter")) + " · ${ctl.pageCount} " + L.t("sayfa"),
                    fontSize = 14.sp, color = UI.TextSoft, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            DropdownMenu(titleMenu, { titleMenu = false }, shape = RoundedCornerShape(18.dp), containerColor = UI.Surface, shadowElevation = 14.dp) {
                MenuRow(Icons.Rounded.Edit, L.t("Yeniden adlandır")) { titleMenu = false; onRename() }
                MenuRow(Icons.Rounded.ViewModule, L.t("Sayfalar")) { titleMenu = false; onPages() }
                MenuRow(Icons.Rounded.GridOn, L.t("Sayfa düzeni (desen, renk)")) { titleMenu = false; onPagePanel() }
                MenuRow(Icons.Rounded.Category, L.t("Kütüphane")) { titleMenu = false; onExtra("library") }
                MenuRow(Icons.Rounded.Checklist, L.t("Yapılacaklar")) { titleMenu = false; onTodos() }
                if (recState == 0) MenuRow(Icons.Rounded.FiberManualRecord, L.t("Ders kaydı")) { titleMenu = false; onRecord() }
                MenuRow(Icons.Rounded.Fullscreen, L.t("Odak modu (menüleri gizle)")) { titleMenu = false; ctl.view?.clearSelection(); ctl.presentation = true }
                Spacer(Modifier.height(4.dp))
            }
        }
        if (secondActive) Icon(Icons.Rounded.Cast, L.t("Projeksiyon bağlı"), tint = Color(0xFF0E9384), modifier = Modifier.padding(horizontal = 8.dp))
        if (recState != 0) {
            Surface(color = UI.Danger, shape = RoundedCornerShape(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
                    Text(if (recState == 1) "● $recTime" else "❚❚ $recTime", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = onPauseResume, modifier = Modifier.size(38.dp)) {
                        Icon(if (recState == 1) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, L.t("Duraklat / devam"), tint = Color.White)
                    }
                    IconButton(onClick = onStopRecord, modifier = Modifier.size(38.dp)) { Icon(Icons.Rounded.Stop, L.t("Kaydı bitir"), tint = Color.White) }
                }
            }
            Spacer(Modifier.width(6.dp))
        }
        BarIcon(Icons.Rounded.BookmarkBorder, L.t("Yer imleri"), onClick = onBookmarks)
        BarIcon(Icons.Rounded.Share, L.t("Dışa aktar / paylaş"), onClick = onExport)
        Box {
            BarIcon(Icons.Rounded.MoreVert, L.t("Diğer")) { more = true }
            DropdownMenu(
                more, { more = false },
                shape = RoundedCornerShape(20.dp), containerColor = UI.Surface, shadowElevation = 14.dp,
            ) {
                // üstte sık işlemler: büyük kutucuklar
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MenuTile(Icons.Rounded.Search, L.t("Ara")) { more = false; onExtra("search") }
                    MenuTile(Icons.Rounded.Print, L.t("Yazdır")) { more = false; onExtra("print") }
                    MenuTile(Icons.Rounded.Summarize, L.t("Konu özeti")) { more = false; ctl.buildSummary() }
                    MenuTile(Icons.Rounded.Settings, L.t("Ayarlar")) { more = false; onSettings() }
                }
                MenuSection(L.t("Sayfa"))
                MenuRow(Icons.Rounded.FitScreen, L.t("Sayfaya sığdır")) { more = false; ctl.view?.fitToWidth() }
                MenuRow(Icons.Rounded.Lock, L.t("Yakınlaştırma kilidi"), trailing = {
                    Switch(ctl.settings.zoomLock.value, { ctl.settings.zoomLock.value = it }, modifier = Modifier.height(24.dp))
                }) { ctl.settings.zoomLock.value = !ctl.settings.zoomLock.value }
                MenuRow(Icons.Rounded.RotateRight, L.t("Sayfayı döndür")) { more = false; ctl.rotatePage(ctl.currentPage) }
                MenuRow(Icons.Rounded.DeleteSweep, L.t("Sayfayı temizle"), danger = true) { more = false; onClear() }
                MenuSection(L.t("Defter"))
                if (ctl.clipboardCount > 0) MenuRow(Icons.Rounded.ContentPaste, L.t("Yapıştır") + " (${ctl.clipboardCount})") { more = false; ctl.paste() }
                MenuRow(Icons.Rounded.CallSplit, L.t("Sayfaları ayrı deftere çıkar")) { more = false; onExtra("extract") }
                if (ctl.settings.solutionColor.value != 0) MenuRow(Icons.Rounded.VisibilityOff, L.t("Çözümleri gizle"), trailing = {
                    Switch(ctl.settings.hideSolutions.value, { ctl.settings.hideSolutions.value = it; ctl.view?.refreshAll() }, modifier = Modifier.height(24.dp))
                }) { ctl.settings.hideSolutions.value = !ctl.settings.hideSolutions.value; ctl.view?.refreshAll() }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

/** ⋮ menüsünde büyük kutucuk: simge ve kısa yazı. */
@Composable
private fun MenuTile(icon: ImageVector, text: String, onClick: () -> Unit) {
    Column(
        Modifier.width(84.dp).clip(RoundedCornerShape(14.dp)).background(UI.SurfaceAlt).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(UI.PrimarySoft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = UI.Primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(text, fontSize = 12.sp, color = UI.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MenuSection(text: String) {
    Text(text.uppercase(java.util.Locale.forLanguageTag("tr")), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = UI.TextFaint,
        modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 2.dp), letterSpacing = 0.8.sp)
}

@Composable
private fun MenuRow(icon: ImageVector, text: String, danger: Boolean = false, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    Row(
        Modifier.widthIn(min = 300.dp).padding(horizontal = 8.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (danger) UI.Danger else UI.TextSoft, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, fontSize = 15.sp, color = if (danger) UI.Danger else UI.Text, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
private fun BarIcon(icon: ImageVector, desc: String, enabled: Boolean = true, selected: Boolean = false, tint: Color = UI.BarFg, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
            .background(if (selected) UI.ToolSelBg else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = if (!enabled) UI.BarFgSoft.copy(alpha = 0.45f) else if (selected) UI.ToolSelFg else tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun LabeledBarButton(text: String, selected: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Row(
        Modifier.height(40.dp).clip(RoundedCornerShape(12.dp))
            .background(if (selected) UI.ToolSelBg else when (UI.style) { UiStyle.CLEAN -> UI.SurfaceAlt; else -> Color(0x22FFFFFF) })
            .clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (selected) UI.ToolSelFg else UI.BarFg)
    }
    Spacer(Modifier.width(4.dp))
}

/* ======================= araç menüsü ======================= */

@Composable
private fun ToolDock(
    ctl: EditorController, handle: Modifier, vertical: Boolean = true, onClear: () -> Unit, onEditPalette: (Boolean) -> Unit,
    onBack: () -> Unit, onPages: () -> Unit, onLibrary: () -> Unit, onTodos: () -> Unit, onWrong: () -> Unit,
    onImage: () -> Unit = {}, showBack: Boolean = false,
    insertMenu: @Composable (close: () -> Unit) -> Unit = {},
) {
    // seçili araca tekrar dokununca o aracın seçenekleri açılır
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
    @Composable
    fun Act(icon: ImageVector, label: String, enabled: Boolean = true, selected: Boolean = false, onClick: () -> Unit) {
        DockItem(selected, label, onClick = { if (enabled) onClick() }) {
            Icon(icon, label, tint = if (!enabled) UI.TextFaint.copy(alpha = 0.5f) else if (selected) UI.Primary else UI.Text, modifier = Modifier.size(24.dp))
        }
    }
    var instMenu by remember { mutableStateOf(false) }
    val items = RailItem.parse(ctl.settings.railItems.value)
    val collapsed = ctl.settings.dockCollapsed.value
    // tamamen gizli (ör. video çekerken): yalnızca küçük bir düğme kalır
    if (ctl.settings.dockHidden.value) {
        FloatingCard(Modifier.testTag("dock"), radius = 22) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                Grip(handle, vertical = true)
                Box(Modifier.size(40.dp).clip(CircleShape).clickable { ctl.settings.dockHidden.value = false }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Visibility, L.t("Araçları göster"), tint = UI.Primary)
                }
            }
        }
        return
    }
    @Composable
    fun Sep() {
        if (vertical) Box(Modifier.padding(vertical = 3.dp).width(30.dp).height(1.dp).background(UI.Border))
        else Box(Modifier.padding(horizontal = 3.dp).height(30.dp).width(1.dp).background(UI.Border))
    }
    @Composable
    fun Items() {
        Grip(handle, vertical = !vertical)
        if (showBack) { Act(Icons.Rounded.ArrowBack, L.t("Defterler"), onClick = onBack); Sep() }
        val shownItems = if (collapsed) RailItem.parse(ctl.settings.railCollapsed.value, RailItem.COLLAPSED_DEFAULT) else items
        // daraltılmışken seçili araç listede yoksa o da görünsün
        if (collapsed) {
            val curItem = when {
                ctl.tool == Tool.PEN -> RailItem.PEN; ctl.tool == Tool.HIGHLIGHTER -> RailItem.HIGHLIGHTER; ctl.tool == Tool.ERASER -> RailItem.ERASER
                ctl.tool == Tool.LASSO -> RailItem.LASSO; ctl.tool == Tool.TEXT -> RailItem.TEXT; ctl.tool == Tool.LASER -> RailItem.LASER
                ctl.tool == Tool.TEMP -> RailItem.TEMP; ctl.tool == Tool.SHAPE -> RailItem.SHAPES; ctl.tool.isGeometry -> RailItem.GEOMETRY
                else -> null
            }
            if (curItem != null && curItem !in shownItems) Btn(ctl.tool, selectedOverride = true)
        }
        for (it in shownItems) when (it) {
            RailItem.PEN -> Btn(Tool.PEN)
            RailItem.HIGHLIGHTER -> Btn(Tool.HIGHLIGHTER)
            RailItem.ERASER -> Btn(Tool.ERASER)
            RailItem.LASSO -> Btn(Tool.LASSO)
            RailItem.GEOMETRY -> { val gt = if (ctl.tool.isGeometry) ctl.tool else ctl.lastGeometryTool; Btn(gt, selectedOverride = ctl.tool.isGeometry, label = L.t("Geometri")) }
            RailItem.TEXT -> Btn(Tool.TEXT)
            RailItem.SHAPES -> Btn(Tool.SHAPE, label = L.t("Şekiller"))
            RailItem.IMAGE -> Act(Icons.Outlined.OImage, L.t("Resim"), onClick = onImage)
            RailItem.INSERT -> Box {
                var open by remember { mutableStateOf(false) }
                Act(Icons.Rounded.AddCircleOutline, L.t("Ekle")) { open = true }
                DropdownMenu(open, { open = false }, shape = RoundedCornerShape(18.dp), containerColor = UI.Surface, shadowElevation = 14.dp) {
                    insertMenu { open = false }
                }
            }
            RailItem.LASER -> Btn(Tool.LASER)
            RailItem.TEMP -> Btn(Tool.TEMP)
            RailItem.RULER -> Box {
                DockItem(selected = ctl.instruments.isNotEmpty(), label = L.t("Cetvel"), onClick = { instMenu = true }) {
                    InstrumentGlyph(null, if (ctl.instruments.isNotEmpty()) UI.Primary else UI.Text)
                }
                DropdownMenu(instMenu, { instMenu = false }) {
                    InstrumentKind.entries.forEach { k ->
                        DropdownMenuItem(
                            text = { Text(k.title, fontWeight = if (k in ctl.instruments) FontWeight.SemiBold else FontWeight.Normal) },
                            leadingIcon = { InstrumentGlyph(k, if (k in ctl.instruments) UI.Primary else UI.Text) },
                            trailingIcon = { if (k in ctl.instruments) Text(L.t("Açık"), color = UI.Primary, fontSize = 12.sp) },
                            onClick = { instMenu = false; ctl.view?.toggleInstrument(k) },
                        )
                    }
                }
            }
            RailItem.UNDO -> Act(Icons.Rounded.Undo, L.t("Geri al"), enabled = ctl.canUndo) { ctl.undo() }
            RailItem.REDO -> Act(Icons.Rounded.Redo, L.t("Yinele"), enabled = ctl.canRedo) { ctl.redo() }
            RailItem.PAGES -> DockItem(false, "${ctl.currentPage + 1}/${ctl.pageCount}", onClick = onPages) {
                Icon(Icons.Rounded.ViewModule, L.t("Sayfalar"), tint = UI.Text, modifier = Modifier.size(24.dp))
            }
            RailItem.NEW_PAGE -> Act(Icons.Rounded.Add, L.t("Yeni sayfa")) { ctl.insertPageAfter(ctl.currentPage) }
            RailItem.LIBRARY -> Act(Icons.Rounded.Category, L.t("Kütüphane"), onClick = onLibrary)
            RailItem.TODO -> Act(Icons.Rounded.Checklist, L.t("Yapılacaklar"), onClick = onTodos)
            RailItem.WRONG -> Act(Icons.Rounded.AssignmentLate, L.t("Yanlış defteri"), onClick = onWrong)
            RailItem.COLORS -> {
                Sep()
                val hl = ctl.tool == Tool.HIGHLIGHTER
                val colors = (if (hl) ctl.settings.highlighterColors() else ctl.settings.paletteColors()).take(5)
                val active = ctl.activeColor or 0xFF000000.toInt()
                colors.forEach { c -> Box(Modifier.padding(3.dp)) { ColorDot(c, (c or 0xFF000000.toInt()) == active, 30, onLongClick = { onEditPalette(hl) }) { ctl.setColor(c) } } }
                Box {
                    var wheel by remember { mutableStateOf(false) }
                    Box(
                        Modifier.padding(3.dp).size(30.dp).clip(CircleShape)
                            .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                            .clickable { wheel = true }.testTag("color-wheel"),
                    )
                    DropdownMenu(wheel, { wheel = false }, shape = RoundedCornerShape(22.dp), containerColor = UI.Surface, shadowElevation = 14.dp) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).width(260.dp)) {
                            Text(L.t("Parmağınızla / kalemle renk topunun üzerinde kaydırın"), fontSize = 13.sp, color = UI.TextSoft)
                            Spacer(Modifier.height(10.dp))
                            ColorWheel(ctl.activeColor, Modifier.fillMaxWidth()) { c -> ctl.setColor(c) }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.TextButton(onClick = { wheel = false; onEditPalette(hl) }) { Text(L.t("Paleti düzenle")) }
                                Spacer(Modifier.weight(1f))
                                androidx.compose.material3.TextButton(onClick = { wheel = false }) { Text(L.t("Tamam")) }
                            }
                        }
                    }
                }
            }
        }
        Sep()
        // daralt / genişlet (daraltınca görünecekler Ayarlar › Menüler'den seçilir) ve tamamen gizle
        Box(Modifier.size(44.dp).clip(CircleShape).clickable { ctl.settings.dockCollapsed.value = !collapsed }.testTag("dock-collapse"), contentAlignment = Alignment.Center) {
            Icon(
                if (collapsed) (if (vertical) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.ChevronRight) else (if (vertical) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.ChevronLeft),
                L.t(if (collapsed) "Menüyü aç" else "Menüyü daralt"), tint = UI.Text, modifier = Modifier.size(26.dp),
            )
        }
        Box(Modifier.size(40.dp).clip(CircleShape).clickable { ctl.settings.dockHidden.value = true }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.VisibilityOff, L.t("Araçları gizle"), tint = UI.TextSoft, modifier = Modifier.size(20.dp))
        }
        // cetvel menüsü listede yoksa da açılabilsin
        if (RailItem.RULER !in items) DropdownMenu(instMenu, { instMenu = false }) {
            InstrumentKind.entries.forEach { k ->
                DropdownMenuItem(
                    text = { Text(k.title, fontWeight = if (k in ctl.instruments) FontWeight.SemiBold else FontWeight.Normal) },
                    leadingIcon = { InstrumentGlyph(k, if (k in ctl.instruments) UI.Primary else UI.Text) },
                    onClick = { instMenu = false; ctl.view?.toggleInstrument(k) },
                )
            }
        }
    }
    FloatingCard(Modifier.testTag("dock"), radius = 20) {
        if (vertical) Column(
            Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
        ) { Items() }
        else Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) { Items() }
    }
}

/** Soldaki araç listesinde gösterilebilecek öğeler (Ayarlar › Araç listesi). */
enum class RailItem(val title: String) {
    PEN("Kalem"), HIGHLIGHTER("Fosforlu"), ERASER("Silgi"), LASSO("Seçim"), GEOMETRY("Geometri"), TEXT("Metin"),
    LASER("Lazer"), TEMP("Geçici kalem"), RULER("Cetvel"), UNDO("Geri al"), REDO("Yinele"), PAGES("Sayfalar"),
    NEW_PAGE("Yeni sayfa"), LIBRARY("Kütüphane"), TODO("Yapılacaklar"), WRONG("Yanlış defteri"), COLORS("Renkler"),
    SHAPES("Şekiller"), IMAGE("Resim"), INSERT("Ekle");

    companion object {
        const val DEFAULT = "UNDO,REDO,PEN,ERASER,HIGHLIGHTER,SHAPES,LASSO,IMAGE,TEXT,GEOMETRY,INSERT,COLORS"
        const val OLD_DEFAULT = "UNDO,REDO,PEN,HIGHLIGHTER,ERASER,LASSO,GEOMETRY,TEXT,RULER,PAGES,COLORS"
        /** Menü daraltılınca görünenler (Ayarlar › Menüler). */
        const val COLLAPSED_DEFAULT = "PEN,ERASER,COLORS"
        fun parse(s: String, def: String = DEFAULT): List<RailItem> = s.split(',').mapNotNull { n -> entries.firstOrNull { it.name == n.trim() } }.ifEmpty { parse(def, DEFAULT) }
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
        Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
            .background(if (selected) UI.PrimarySoft else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}

private fun shortName(t: Tool) = when (t) {
    Tool.PEN -> L.t("Kalem"); Tool.HIGHLIGHTER -> L.t("Fosforlu"); Tool.ERASER -> L.t("Silgi"); Tool.LASSO -> L.t("Seçim")
    Tool.TEXT -> L.t("Metin"); Tool.LASER -> L.t("Lazer"); Tool.TEMP -> L.t("Geçici"); Tool.SPOT -> L.t("Spot"); Tool.SHAPE -> L.t("Şekil")
    Tool.LINE -> L.t("Doğru"); Tool.ARROW -> L.t("Ok"); Tool.POLYGON -> L.t("Çokgen"); Tool.COMPASS -> L.t("Pergel"); Tool.MARK -> L.t("İşaret")
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
                    Pill(L.t("Bütün çizgi"), !ctl.eraserPartial) { ctl.eraserPartial = false; ctl.settings.eraserPartial.value = false }
                    Pill(L.t("Kısmi"), ctl.eraserPartial) { ctl.eraserPartial = true; ctl.settings.eraserPartial.value = true }
                }
                SectionTitle(L.t("Boyut"))
                Row {
                    listOf(8, 14, 22).forEachIndexed { i, d ->
                        SizeDot(d, ctl.eraserSize == i, Color(0xFF98A2B3)) { ctl.eraserSize = i; ctl.settings.eraserSize.value = i }
                    }
                }
                LabeledRow(L.t("Sildikten sonra kaleme dön")) { Switch(ctl.settings.eraserReturn.value, { ctl.settings.eraserReturn.value = it }) }
                Pill(L.t("Sayfayı temizle"), false, onClick = onClear)
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
                SectionTitle(L.t("Renk"))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    colors.forEach { c -> ColorDot(c, (c or 0xFF000000.toInt()) == active, 34, onLongClick = { onEditPalette(hl) }) { ctl.setColor(c) } }
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(UI.SurfaceAlt).clickable { onEditPalette(hl) },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Edit, L.t("Renkleri düzenle"), tint = UI.TextSoft, modifier = Modifier.size(16.dp)) }
                }
                if (t != Tool.TEXT) {
                    SectionTitle(L.t("Kalınlık"))
                    Row {
                        val widths = if (hl) listOf(8f, 14f, 22f) else listOf(1.0f, 2.0f, 3.6f)
                        widths.forEachIndexed { i, w ->
                            SizeDot(6 + i * 5, abs(ctl.activeWidth - w) < 0.05f, Color(ctl.activeColor or 0xFF000000.toInt())) { ctl.setWidth(w) }
                        }
                    }
                }
                if (t == Tool.PEN || t == Tool.HIGHLIGHTER) {
                    SectionTitle(L.t("Kayıtlı kalemler"))
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
                    DropdownMenuItem(text = { Text(L.t("Kayıtlı kalemi sil")) }, onClick = { ctl.settings.removePreset(p); menuFor = null })
                }
            }
        }
        Box(
            Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(UI.SurfaceAlt).clickable { ctl.saveCurrentAsPreset() }.padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) { Text(if (list.isEmpty()) L.t("+ Kalemi kaydet") else "+", fontSize = 13.sp, color = UI.TextSoft) }
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
                    else -> L.t("Seçenekler")
                }, opts,
            ) { opts = !opts }
            DropdownMenu(opts, { opts = false }) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp).width(300.dp)) {
                    Text(ctl.tool.title, fontWeight = FontWeight.SemiBold)
                    Text(
                        when (ctl.tool) {
                            Tool.SHAPE -> L.t("Her çizimi düzgün şekle çevirir.")
                            Tool.LINE -> L.t("Uzunluk ve açıyı gösterir; köşelere ve 15°'ye yapışır.")
                            Tool.ARROW -> L.t("Vektör / ok çizer.")
                            Tool.POLYGON -> L.t("Merkezden bir köşeye doğru çekin.")
                            Tool.COMPASS -> L.t("Merkezi seçip yarıçap kadar çekin, sonra yayı çizin.")
                            Tool.SOLID -> L.t("Kutu çizer gibi çapraz çekin; arkadaki kenarlar kesikli çizilir.")
                            else -> L.t("Köşeye dokunun: açı / dik açı. Kenara dokunun: çentik / paralel.")
                        }, fontSize = 12.sp, color = UI.TextSoft, modifier = Modifier.padding(bottom = 8.dp),
                    )
                    if (ctl.tool == Tool.POLYGON) {
                        Text(L.t("Kenar sayısı"), fontSize = 13.sp, color = UI.TextSoft)
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
                    LabeledRow(L.t("Kesikli çizgi")) { Switch(ctl.shapeDashed, { ctl.shapeDashed = it }) }
                    LabeledRow(L.t("İçini doldur"), L.t("Kalem renginin açık tonuyla")) { Switch(ctl.shapeFill, { ctl.shapeFill = it }) }
                    LabeledRow(L.t("Köşelere harf (A, B, C)")) { Switch(ctl.settings.autoLetters.value, { ctl.settings.autoLetters.value = it }) }
                    LabeledRow(L.t("Ölçüleri yaz (cm, °)")) { Switch(ctl.settings.autoMeasures.value, { ctl.settings.autoMeasures.value = it }) }
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
                SmallRound(Icons.Rounded.ChevronLeft, L.t("Önceki sayfa"), ctl.currentPage > 0) { ctl.goToPage(ctl.currentPage - 1, false) }
                Column(
                    Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onGoto).padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("${ctl.currentPage + 1} / ${ctl.pageCount}" + (if (pv < 0) "" else ""), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = UI.Text)
                    val book = ctl.nb.pages.getOrNull(ctl.currentPage)?.let { PageNumbers.bookNumberOf(ctl.nb, it) }
                    Text(if (book != null && ctl.nb.pageOffset != 0) "kitap s. $book" else L.t("sayfaya git"), fontSize = 10.sp, color = UI.TextSoft)
                }
                SmallRound(Icons.Rounded.ChevronRight, L.t("Sonraki sayfa"), ctl.currentPage < ctl.pageCount - 1) { ctl.goToPage(ctl.currentPage + 1, false) }
                SmallRound(Icons.Rounded.ViewModule, L.t("Tüm sayfalar"), true, onPages)
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
            Text(L.t("Sayfa"), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
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
private fun SelectionBar(ctl: EditorController, onSaveToLib: () -> Unit, modifier: Modifier) {
    FloatingCard(modifier, radius = 26) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            SelAction(Icons.Rounded.ContentCopy, L.t("Kopyala"), UI.Text) { ctl.view?.selectionCopy() }
            SelAction(Icons.Rounded.ContentCut, L.t("Kes"), UI.Text) { ctl.view?.selectionCut() }
            SelAction(Icons.Rounded.AddCircleOutline, L.t("Çoğalt"), UI.Text) { ctl.view?.selectionDuplicate() }
            if (ctl.selectionHasShapes) SelAction(Icons.Rounded.FormatColorFill, L.t("Doldur"), UI.Text) { ctl.view?.selectionFill() }
            if (ctl.selectionHasShapes) SelAction(Icons.Rounded.Straighten, if (ctl.liveMeasure) L.t("Ölçü: açık") else L.t("Ölçü: kapalı"), if (ctl.liveMeasure) UI.Primary else UI.Text) {
                ctl.liveMeasure = !ctl.liveMeasure; ctl.view?.invalidate()
            }
            Box {
                var boxMenu by remember { mutableStateOf(false) }
                SelAction(Icons.Rounded.CropSquare, L.t("Kutu"), UI.Text) { boxMenu = true }
                DropdownMenu(boxMenu, { boxMenu = false }) {
                    EditorController.InfoBox.entries.forEach { k ->
                        DropdownMenuItem(text = { Text(k.title, color = Color(k.color), fontWeight = FontWeight.SemiBold) }, onClick = { boxMenu = false; ctl.makeInfoBox(k) })
                    }
                }
            }
            SelAction(Icons.Rounded.Category, L.t("Kütüphaneye"), UI.Text, onSaveToLib)
            if (!ctl.isWrongBook) SelAction(Icons.Rounded.AssignmentLate, L.t("Yanlış defterine"), UI.Text) { ctl.view?.selectionToWrong() }
            SelAction(Icons.Rounded.Delete, L.t("Sil"), UI.Danger) { ctl.view?.selectionDelete() }
            BarDivider()
            ctl.settings.paletteColors().take(6).forEach { c ->
                Box(Modifier.padding(2.dp)) { ColorDot(c, false, 26) { ctl.view?.selectionRecolor(c) } }
            }
            SmallRound(Icons.Rounded.Close, L.t("Seçimi kaldır"), true) { ctl.view?.clearSelection() }
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
            SmallRound(Icons.Rounded.Undo, L.t("Geri al"), true) { ctl.undo() }
            SmallRound(Icons.Rounded.Add, L.t("Yeni sayfa"), true) { ctl.insertPageAfter(ctl.currentPage) }
            if (recState != 0) {
                Text("● $recTime", color = UI.Danger, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.padding(4.dp))
                SmallRound(Icons.Rounded.Stop, L.t("Kaydı bitir"), true, onStopRecord)
            }
        }
        val collapseIcon = if (vertical) (if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown)
        else (if (expanded) Icons.Rounded.ChevronLeft else Icons.Rounded.ChevronRight)
        SmallRound(collapseIcon, L.t("Daralt"), true) { expanded = !expanded }
        SmallRound(Icons.Rounded.FullscreenExit, L.t("Odak modundan çık"), true) { ctl.presentation = false }
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
        SmallRound(Icons.Rounded.Close, L.t("Kapat"), true, onClose)
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
        PanelHeader(L.t("Sayfa") + (if (pv < 0) "" else ""), onClose)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (isPdf) {
                Surface(color = Color(0xFFFFF7ED), shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        "Bu bir PDF sayfası. Seçtiğiniz desen, \"+ Sayfa\" ile eklenecek yeni sayfalara uygulanır.",
                        fontSize = 13.sp, color = Color(0xFF9A3412), modifier = Modifier.padding(12.dp),
                    )
                }
            }
            SectionTitle(L.t("Desen"))
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

            SectionTitle(L.t("Kağıt rengi"))
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
                    ) { if (!custom) Icon(Icons.Rounded.Add, L.t("Başka renk"), tint = UI.TextSoft, modifier = Modifier.size(20.dp)) }
                    Text(L.t("Diğer"), fontSize = 10.sp, color = UI.TextSoft, modifier = Modifier.padding(top = 3.dp))
                }
            }
            if (customColor) {
                var picked by remember { mutableStateOf(style.color) }
                val start = remember { style.color }
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { customColor = false }, containerColor = UI.Surface,
                    title = { Text(L.t("Kağıt rengi")) },
                    text = { Column { HsvPicker(start) { picked = it or 0xFF000000.toInt() } } },
                    confirmButton = { Button(onClick = { customColor = false; apply(style.copy(color = picked)) }, shape = RoundedCornerShape(10.dp)) { Text(L.t("Uygula")) } },
                    dismissButton = { androidx.compose.material3.TextButton(onClick = { customColor = false }) { Text(L.t("Vazgeç"), color = UI.TextSoft) } },
                )
            }

            SectionTitle(L.t("Masa rengi (sayfanın dışı)"))
            Pill(L.t("Temaya göre"), ctl.settings.deskColor.value == 0) { ctl.settings.deskColor.value = 0 }
            Spacer(Modifier.height(8.dp))
            ColorChoiceRow(
                presets = DESK_PRESETS, saved = ctl.settings.savedDeskList(), selected = ctl.settings.deskColor.value, size = 32,
                onPick = { c -> ctl.settings.deskColor.value = c },
                onAdd = { c -> ctl.settings.addColor(ctl.settings.savedDesks, c) },
                onRemove = { c -> ctl.settings.removeColor(ctl.settings.savedDesks, c) },
            )

            if (!isPdf && page != null) {
                SectionTitle(L.t("Sayfa boyutu"))
                val (fw, fh) = ctl.fullScreenSize()
                val sizes = listOf(
                    L.t("A4 dikey") to (595f to 842f), L.t("A4 yatay") to (842f to 595f),
                    "16:9" to (960f to 540f), L.t("Tam ekran") to (fw to fh),
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
                SectionTitle(L.t("Kendi deseniniz"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(if (style.image.isEmpty()) L.t("Resim / PDF'ten desen") else L.t("Deseni değiştir"), false) { pickPaper.launch(arrayOf("image/*", "application/pdf")) }
                    if (style.image.isNotEmpty()) Pill(L.t("Kaldır"), false) { apply(style.copy(image = "")) }
                }
                Text(L.t("Çalışma kağıdınızı ya da özel bir deseni sayfa zemini yapın."), fontSize = 12.sp, color = UI.TextSoft, modifier = Modifier.padding(top = 4.dp))
                LabeledRow(L.t("Tüm sayfalara uygula"), L.t("Kapalıyken yalnızca bu sayfa değişir")) {
                    Switch(all, { all = it; if (it) ctl.setPageStyle(ctl.currentPage, style, true) })
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        HorizontalDivider(color = UI.Border)
        Button(
            onClick = { ctl.insertPageAfter(ctl.currentPage, style) }, shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
        ) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text(L.t("Bu desenle yeni sayfa ekle")) }
    }
}

/** Sayfa küçük resimleri: dokun = git, uzun bas = sayfa işlemleri. */
@Composable
private fun PagesPanel(ctl: EditorController, onCrop: (Int) -> Unit, onExtract: (String) -> Unit = {}, onClose: () -> Unit) {
    val pv = ctl.pagesVersion
    var tick by remember { mutableIntStateOf(0) }
    val state = rememberLazyGridState(initialFirstVisibleItemIndex = ctl.currentPage)
    // çoklu seçim (sayfa kimlikleri) ve "taşı: yeni yerine dokun" modu
    var picked by remember { mutableStateOf<Set<Long>?>(null) }
    var moving by remember { mutableStateOf<Long?>(null) }
    Column(Modifier.fillMaxSize()) {
        PanelHeader("Sayfalar (${ctl.nb.pages.size})" + (if (pv < 0) "" else ""), onClose)
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                when {
                    moving != null -> L.t("Sayfanın gideceği yere dokunun")
                    picked != null -> L.t("Sayfalara dokunarak seçin")
                    else -> L.t("Dokun: git · Uzun bas: ekle, çoğalt, taşı, sil")
                },
                fontSize = 12.sp, color = if (moving != null) UI.Primary else UI.TextSoft, modifier = Modifier.weight(1f),
            )
            Pill(if (picked != null || moving != null) L.t("Bitti") else L.t("Seç"), picked != null) {
                if (picked != null || moving != null) { picked = null; moving = null } else picked = emptySet()
            }
        }
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
                val isPicked = picked?.contains(page.id) == true
                val isMoving = moving == page.id
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(page.width / page.height)
                            .shadow(if (cur) 0.dp else 3.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                if (cur || isPicked || isMoving) 3.dp else 0.dp,
                                when { isMoving -> Color(0xFFF59E0B); isPicked -> UI.Primary; cur -> UI.Primary; else -> Color.Transparent }, RoundedCornerShape(8.dp),
                            )
                            .background(Color.White)
                            .combinedClickable(
                                onClick = {
                                    val mv = moving; val pk = picked
                                    when {
                                        mv != null -> { val from = ctl.nb.indexOfPage(mv); if (from >= 0 && from != i) ctl.movePage(from, i - from); moving = null }
                                        pk != null -> picked = if (page.id in pk) pk - page.id else pk + page.id
                                        else -> ctl.goToPage(i)
                                    }
                                },
                                onLongClick = { if (picked == null && moving == null) menu = true },
                            ),
                    ) {
                        if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize())
                        if (isPicked) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape).background(UI.Primary), contentAlignment = Alignment.Center) {
                            Text("✓", color = Color.White, fontSize = 14.sp)
                        }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text(L.t("Sonrasına boş sayfa ekle")) }, onClick = { menu = false; ctl.insertPageAfter(i) })
                            DropdownMenuItem(text = { Text(L.t("Çoğalt")) }, onClick = { menu = false; ctl.duplicatePage(i) })
                            DropdownMenuItem(text = { Text(L.t("Taşı (yeni yerine dokun)")) }, onClick = { menu = false; moving = page.id })
                            DropdownMenuItem(text = { Text(L.t("Yukarı taşı")) }, onClick = { menu = false; ctl.movePage(i, -1) })
                            DropdownMenuItem(text = { Text(L.t("Aşağı taşı")) }, onClick = { menu = false; ctl.movePage(i, 1) })
                            DropdownMenuItem(text = { Text(L.t("90° döndür")) }, onClick = { menu = false; ctl.rotatePage(i) })
                            if (page.background is Background.PdfPage) DropdownMenuItem(text = { Text(L.t("Kırp")) }, onClick = { menu = false; onCrop(i) })
                            DropdownMenuItem(text = { Text(L.t("Sil"), color = UI.Danger) }, onClick = { menu = false; ctl.deletePage(i) })
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
        val pk = picked
        if (pk != null) {
            val idx = ctl.nb.pages.indices.filter { ctl.nb.pages[it].id in pk }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text("${idx.size} sayfa seçili", fontWeight = FontWeight.SemiBold, color = UI.Text)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(L.t("Döndür"), false) { idx.forEach { ctl.rotatePage(it) } }
                    Pill(L.t("Çoğalt"), false) { idx.sortedDescending().forEach { ctl.duplicatePage(it) }; picked = emptySet() }
                    Pill(L.t("Sil"), false) {
                        if (idx.size >= ctl.nb.pages.size) ctl.message = L.t("Bütün sayfalar silinemez")
                        else { idx.sortedDescending().forEach { ctl.deletePage(it) }; picked = emptySet() }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Pill(L.t("Ayrı deftere çıkar"), false) { if (idx.isNotEmpty()) onExtract(idx.joinToString(", ") { "${it + 1}" }) }
            }
        } else OutlinedButton(
            onClick = { ctl.insertPageAfter(ctl.nb.pages.size - 1) }, shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(46.dp),
        ) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text(L.t("Sona sayfa ekle")) }
    }
}
