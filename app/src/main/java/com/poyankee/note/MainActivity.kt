package com.poyankee.note

import com.poyankee.note.core.L

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import com.poyankee.note.core.Background
import com.poyankee.note.core.PageSize
import com.poyankee.note.core.Template
import com.poyankee.note.data.AutoBackup
import com.poyankee.note.data.Lock
import com.poyankee.note.data.NotebookInfo
import com.poyankee.note.data.PenKeys
import com.poyankee.note.data.Settings
import com.poyankee.note.data.SortMode
import com.poyankee.note.data.Store
import com.poyankee.note.data.ThemeMode
import com.poyankee.note.editor.EditorController
import com.poyankee.note.editor.Exporter
import com.poyankee.note.ui.EditorScreen
import com.poyankee.note.ui.LibraryScreen
import com.poyankee.note.ui.NoteTheme
import com.poyankee.note.ui.PinDialog
import com.poyankee.note.ui.SettingsScreen
import com.poyankee.note.ui.UI
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var store: Store
    private lateinit var settings: Settings
    private val bg = Executors.newSingleThreadExecutor()
    /** Defter açma ayrı iş parçacığında: yedekleme vb. uzun işler açılışı bekletmesin. */
    private val openExec = Executors.newSingleThreadExecutor()

    private var notebooks by mutableStateOf<List<NotebookInfo>>(emptyList())
    private var folders by mutableStateOf<List<String>>(emptyList())
    private var trash by mutableStateOf<List<Pair<NotebookInfo, Long>>>(emptyList())
    private var folder by mutableStateOf<String?>(null)
    private var busy by mutableStateOf<String?>(null)
    private var editor by mutableStateOf<EditorController?>(null)
    private var showSettings by mutableStateOf(false)
    /** Açık olan yanlış defteri galerisi (defter kimliği). */
    private var wrongGallery by mutableStateOf<String?>(null)
    private var loading by mutableStateOf(false)
    /** Açık defter sekmeleri (kimlik, ad). */
    private var tabs by mutableStateOf<List<Pair<String, String>>>(emptyList())
    /** Kilitli defter açılırken PIN sorulacak defter. */
    private var pinFor by mutableStateOf<NotebookInfo?>(null)
    /** Testlerde tanıtım gösterilmez. */
    private val introSuppressed by lazy { try { Class.forName("androidx.test.espresso.Espresso"); true } catch (_: Throwable) { false } }

    private val openPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) importPdf(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.WHITE, android.graphics.Color.BLACK),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.WHITE, android.graphics.Color.BLACK),
        )
        super.onCreate(savedInstanceState)
        com.poyankee.note.data.CrashLog.install(applicationContext)
        applyLanguage(this)
        store = Store(applicationContext)
        settings = Settings.get(this)
        settings.migrateDesign()
        bg.execute { Exporter.init(applicationContext) }
        folders = store.folders()
        store.purgeOldTrash()
        refresh()
        handleIntent(intent)
        // eski "Hızlı not" kısayolu kaldırıldı
        try { androidx.core.content.pm.ShortcutManagerCompat.removeAllDynamicShortcuts(this) } catch (_: Exception) {}
        // otomatik yedek (günde bir, ayarlıysa)
        bg.execute { try { AutoBackup.runIfDue(applicationContext, store, settings) } catch (_: Exception) {} }

        setContent {
            val systemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            // tek tasarım: renkleri ayarlardaki uygulama rengi belirler
            val st = com.poyankee.note.ui.UiStyle.CLEAN
            if (UI.style != st) UI.style = st
            val wantDark = settings.eyeComfort.value || st == com.poyankee.note.ui.UiStyle.DARK || when (settings.themeValue) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; ThemeMode.SYSTEM -> systemDark }
            val sc = listOf(0.88f, 1f, 1.15f, 1.32f)[settings.uiSize.value.coerceIn(0, 3)]
            if (UI.scale != sc) UI.scale = sc
            if (UI.warm != settings.eyeComfort.value) UI.warm = settings.eyeComfort.value
            if (UI.dark != wantDark) UI.dark = wantDark
            val pal = com.poyankee.note.ui.Palette.CUSTOM
            if (UI.palette != pal) UI.palette = pal
            if (UI.customAccent != settings.customAccent.value) UI.customAccent = settings.customAccent.value
            LaunchedEffect(UI.dark) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !UI.dark
                    isAppearanceLightNavigationBars = !UI.dark
                }
            }
            val libraryUi: @androidx.compose.runtime.Composable (Boolean, String?) -> Unit = { compact, cur ->
                LibraryScreen(
                            notebooks = notebooks,
                            folders = folders,
                            folder = folder,
                            busy = busy,
                            onSelectFolder = { folder = it },
                            onOpen = { openById(it) },
                            onCreate = { create(it) },
                            onImportPdf = { openPdf.launch(arrayOf("application/pdf")) },
                            onRename = { id, name -> if (name.isNotBlank()) { store.rename(id, name); notebooks = notebooks.map { if (it.id == id) it.copy(name = name) else it } } },
                            onDelete = { id -> store.delete(id); notebooks = notebooks.filter { it.id != id }; tabs = tabs.filter { it.first != id }; refresh() },
                            trash = trash,
                            onRestoreTrash = { id -> store.restoreFromTrash(id); refresh() },
                            onPurge = { id -> store.purge(id); trash = trash.filter { it.first.id != id } },
                            onEmptyTrash = { store.emptyTrash(); trash = emptyList() },
                            onOpenTodo = { id, page -> if (page >= 0) settings.setLastPage(id, page); openById(id) },
                            onSaveTemplate = { id -> saveTemplate(id) },
                            onFromTemplate = { id -> fromTemplate(id) },
                            onMerge = { ids -> mergeNotebooks(ids) },
                            onFromImages = { pickImages.launch("image/*") },
                            onForgotPin = { nb -> forgotPin(nb) },
                            onWrongBook = {
                                bg.execute {
                                    val id = com.poyankee.note.data.WrongBook.ensure(store, settings)
                                    runOnUiThread { refresh(); if (editor != null) closeEditor(keepTab = true); wrongGallery = id }
                                }
                            },
                            onMove = { id, f -> store.setFolder(id, f); notebooks = notebooks.map { if (it.id == id) it.copy(folder = f) else it } },
                            onCreateFolder = { name -> addFolder(name) },
                            onRenameFolder = { old, new -> renameFolder(old, new) },
                            onDeleteFolder = { name -> deleteFolder(name) },
                            onSettings = { showSettings = true },
                            sort = settings.sortValue,
                            onSort = { settings.sortMode.value = it.name },
                            onFavorite = { id, fav -> store.setFavorite(id, fav); notebooks = notebooks.map { if (it.id == id) it.copy(favorite = fav) else it } },
                            onLock = { id, hash -> store.setLock(id, hash); notebooks = notebooks.map { if (it.id == id) it.copy(lockHash = hash) else it } },
                            compact = compact, currentId = cur,
                        )
            }
            NoteTheme {
                val ed = editor
                val presenting = ed?.presentation == true
                Box(Modifier.fillMaxSize().background(UI.Surface).let { if (presenting) it else it.safeDrawingPadding() }) {
                    when {
                        showSettings -> {
                            BackHandler { showSettings = false }
                            SettingsScreen(
                                settings, onBack = { showSettings = false },
                                onBackupNow = { uri -> backupTo(uri) },
                                onRestore = { uri -> restoreFrom(uri) },
                                onBackupFolder = { uri -> setBackupFolder(uri) },
                                editorOpen = ed != null,
                            )
                        }
                        wrongGallery != null && ed == null -> {
                            val wid = wrongGallery!!
                            BackHandler { wrongGallery = null }
                            com.poyankee.note.ui.WrongGallery(store, settings, wid,
                                onOpenPage = { p -> if (p >= 0) settings.setLastPage(wid, p); wrongGallery = null; open(wid) },
                                onBack = { wrongGallery = null })
                        }
                        // her defter kendi ekran durumuyla açılsın (sekme geçişinde eski çizim alanı kullanılmasın)
                        ed != null -> androidx.compose.runtime.key(ed) { EditorScreen(
                            ed, onClose = { closeEditor() }, onSettings = { showSettings = true },
                            tabs = tabs, onTab = { id -> if (id != ed.nb.id) openById(id) },
                            onCloseTab = { id -> closeTab(id) },
                            onOpenTodo = { id, page -> settings.setLastPage(id, page); if (id != ed.nb.id) openById(id) },
                            onInsertPdf = { uri, after -> insertPdfInto(ed.nb.id, uri, after) },
                            onExtract = { idx, name -> extractPages(ed.nb.id, idx, name) },
                            onNew = { create("") },
                        ) }
                        else -> libraryUi(false, null)
                    }
                    if (!settings.introShown.value && !introSuppressed) com.poyankee.note.ui.IntroDialog { settings.introShown.value = true }
                    // açılıyor / kaydediliyor: ekranı bekletmeden sağ üstte küçük gösterge
                    if (loading) Box(Modifier.fillMaxSize().padding(top = 10.dp, end = 14.dp), contentAlignment = Alignment.TopEnd) {
                        androidx.compose.material3.Surface(shape = androidx.compose.foundation.shape.CircleShape, color = UI.Surface, shadowElevation = 6.dp) {
                            CircularProgressIndicator(Modifier.padding(8.dp).size(22.dp), strokeWidth = 2.5.dp)
                        }
                    }
                    pinFor?.let { nb ->
                        PinDialog(L.t("Kilitli defter"), "\"${nb.name}\" defterini açmak için PIN girin", onForgot = { pinFor = null; forgotPin(nb) }, onDismiss = { pinFor = null }) { pin ->
                            if (Lock.check(nb.id, pin, nb.lockHash)) { pinFor = null; open(nb.id); true } else false
                        }
                    }
                }
            }
        }
    }

    /* ---------- kalem tuşu (çift dokunuş) ---------- */

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        PenKeys.monitor?.let { m ->
            m(event)
            if (code != KeyEvent.KEYCODE_BACK) return true
        }
        val learner = PenKeys.learner
        if (learner != null && PenKeys.canLearn(code)) {
            if (event.action == KeyEvent.ACTION_UP) learner(code)
            return true
        }
        val ed = editor
        // klavye kısayolları (klavye bağlıyken): Ctrl+Z geri al, Ctrl+Y yinele, Ctrl+C/X/V, Ctrl+D çoğalt, Sayfa yukarı/aşağı, Del, Esc
        if (ed != null && event.action == KeyEvent.ACTION_DOWN && handleShortcut(ed, event)) return true
        // Focus Pen Pro'da kalemi yukarı / aşağı kaydırma: önceki / sonraki sayfa
        if (ed != null && (PenKeys.slideUp(event) || PenKeys.slideDown(event))) {
            if (event.action == KeyEvent.ACTION_DOWN) ed.goToPage(ed.currentPage + (if (PenKeys.slideDown(event)) 1 else -1), false)
            return true
        }
        if (ed != null && PenKeys.isPenKey(event, settings.penKeyCodeSet())) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) ed.onPenKey()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun handleShortcut(ed: EditorController, e: KeyEvent): Boolean {
        val v = ed.view ?: return false
        if (e.isCtrlPressed) {
            when (e.keyCode) {
                KeyEvent.KEYCODE_Z -> { if (e.isShiftPressed) ed.redo() else ed.undo(); return true }
                KeyEvent.KEYCODE_Y -> { ed.redo(); return true }
                KeyEvent.KEYCODE_C -> if (ed.hasSelection) { v.selectionCopy(); return true }
                KeyEvent.KEYCODE_X -> if (ed.hasSelection) { v.selectionCut(); return true }
                KeyEvent.KEYCODE_V -> if (ed.clipboardCount > 0) { ed.paste(); return true }
                KeyEvent.KEYCODE_D -> { if (ed.hasSelection) v.selectionDuplicate(); return true }
            }
            return false
        }
        when (e.keyCode) {
            KeyEvent.KEYCODE_PAGE_DOWN -> { ed.goToPage(ed.currentPage + 1, false); return true }
            KeyEvent.KEYCODE_PAGE_UP -> { ed.goToPage(ed.currentPage - 1, false); return true }
            KeyEvent.KEYCODE_FORWARD_DEL -> if (ed.hasSelection) { v.selectionDelete(); return true }
            KeyEvent.KEYCODE_ESCAPE -> {
                if (ed.hasSelection) { v.clearSelection(); return true }
                if (ed.presentation) { ed.presentation = false; return true }
            }
        }
        return false
    }

    /* ---------- klasörler ---------- */

    private fun addFolder(name: String) {
        val n = name.trim()
        if (n.isEmpty() || n in folders) return
        folders = folders + n
        store.saveFolders(folders)
    }

    private fun renameFolder(old: String, new: String) {
        val n = new.trim()
        if (n.isEmpty() || n == old) return
        folders = folders.map { if (it == old) n else it }.distinct()
        settings.folderColor(old)?.let { settings.setFolderColor(n, it); settings.setFolderColor(old, null) }
        store.saveFolders(folders)
        notebooks.filter { it.folder == old }.forEach { store.setFolder(it.id, n) }
        notebooks = notebooks.map { if (it.folder == old) it.copy(folder = n) else it }
        if (folder == old) folder = n
    }

    private fun deleteFolder(name: String) {
        folders = folders - name
        settings.setFolderColor(name, null)
        store.saveFolders(folders)
        notebooks.filter { it.folder == name }.forEach { store.setFolder(it.id, "") }
        notebooks = notebooks.map { if (it.folder == name) it.copy(folder = "") else it }
        if (folder == name) folder = null
    }

    /* ---------- yedekleme ---------- */

    private fun toast(msg: String) = runOnUiThread { android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show() }

    private fun backupTo(uri: Uri) {
        busy = L.t("Yedekleniyor...")
        bg.execute {
            try {
                contentResolver.openOutputStream(uri)?.use { store.backupTo(it) }
                toast(L.t("Yedek kaydedildi"))
            } catch (e: Exception) { toast("Yedeklenemedi: ${e.message}") }
            runOnUiThread { busy = null }
        }
    }

    private fun restoreFrom(uri: Uri) {
        closeEditor()
        busy = L.t("Geri yükleniyor...")
        bg.execute {
            try {
                val n = contentResolver.openInputStream(uri)?.use { store.restoreFrom(it) } ?: 0
                toast("$n defter geri yüklendi")
            } catch (e: Exception) { toast("Geri yüklenemedi: ${e.message}") }
            runOnUiThread { busy = null; folders = store.folders(); refresh() }
        }
    }

    private fun setBackupFolder(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        } catch (_: Exception) {}
        settings.backupTree.value = uri.toString()
        settings.autoBackup.value = true
        bg.execute {
            val ok = try { AutoBackup.runIfDue(applicationContext, store, settings, force = true) } catch (_: Exception) { false }
            toast(if (ok) L.t("Yedek klasörü ayarlandı, ilk yedek alındı") else L.t("Bu klasöre yazılamadı; başka bir klasör seçin"))
        }
    }

    /* ---------- defterler ---------- */

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        if (intent.action == ACTION_QUICK_NOTE) { intent.action = null; return }
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        }
        if (uri != null) {
            intent.action = null
            importPdf(uri)
        }
    }

    private fun refresh() {
        bg.execute {
            val l = store.list()
            val known = store.folders()
            val t = store.listTrash()
            runOnUiThread {
                notebooks = l
                trash = t
                val extra = l.map { it.folder }.filter { it.isNotEmpty() && it !in known }.distinct()
                folders = known + extra
            }
        }
    }

    /** Arka planda uzun iş: ana sayfada "çalışıyor" penceresi, bitince mesaj ve liste yenileme. */
    private fun runOp(label: String, op: () -> String?, after: (() -> Unit)? = null) {
        busy = label
        bg.execute {
            val msg = try { op() } catch (e: Throwable) { "Olmadı: ${e.message}" }
            runOnUiThread {
                busy = null
                refresh()
                msg?.let { android.widget.Toast.makeText(this, it, android.widget.Toast.LENGTH_LONG).show() }
                after?.invoke()
            }
        }
    }

    /* ---------- kilit kurtarma: cihazın ekran kilidiyle ---------- */
    private var forgotFor: NotebookInfo? = null
    private val confirmCredential = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val nb = forgotFor ?: return@registerForActivityResult
        forgotFor = null
        if (r.resultCode == RESULT_OK) {
            store.setLock(nb.id, "")
            notebooks = notebooks.map { if (it.id == nb.id) it.copy(lockHash = "") else it }
            com.poyankee.note.ui.PinGuard.success()
            android.widget.Toast.makeText(this, L.t("Kilit kaldırıldı"), android.widget.Toast.LENGTH_SHORT).show()
            open(nb.id)
        }
    }

    private fun forgotPin(nb: NotebookInfo) {
        val km = getSystemService(android.app.KeyguardManager::class.java)
        if (km == null || !km.isDeviceSecure) {
            android.widget.Toast.makeText(this, L.t("Cihazınızda ekran kilidi olmadığı için PIN kurtarılamıyor. Ekran kilidi ekleyip tekrar deneyin."), android.widget.Toast.LENGTH_LONG).show()
            return
        }
        @Suppress("DEPRECATION")
        val i = km.createConfirmDeviceCredentialIntent(L.t("Defter kilidini kaldır"), "\"${nb.name}\" için cihazınızın ekran kilidini girin") ?: return
        forgotFor = nb
        confirmCredential.launch(i)
    }

    private fun insertPdfInto(id: String, uri: Uri, after: Int) {
        closeEditor(keepTab = true)
        var ok = false
        runOp(L.t("PDF sayfaları ekleniyor..."), {
            store.flush()
            val n = com.poyankee.note.data.PdfOps.insertPdf(applicationContext, store, id, uri, after)
            settings.setLastPage(id, after + 1)
            ok = true
            "$n sayfa eklendi"
        }) { if (ok) open(id) }
    }

    private fun extractPages(id: String, indices: List<Int>, name: String) {
        runOp(L.t("Yeni defter hazırlanıyor..."), {
            store.flush()
            val nb = com.poyankee.note.data.PdfOps.extract(applicationContext, store, id, indices, name)
            "\"${nb.name}\" oluşturuldu (${nb.pages.size} sayfa)"
        })
    }

    private fun saveTemplate(id: String) {
        addFolder(com.poyankee.note.ui.TEMPLATE_FOLDER)
        runOp(L.t("Şablon kaydediliyor..."), {
            store.flush()
            val src = store.load(id)
            val nb = com.poyankee.note.data.PdfOps.extract(applicationContext, store, id, src.pages.indices.toList(), L.t("Şablon: ") + src.name.removePrefix(L.t("Şablon: ")).removePrefix("Şablon: "))
            store.setFolder(nb.id, com.poyankee.note.ui.TEMPLATE_FOLDER)
            store.flush()
            "\"${src.name}\" şablon olarak kaydedildi (Şablonlar klasörü)"
        })
    }

    private fun fromTemplate(id: String) {
        val f = folder?.takeIf { it != com.poyankee.note.ui.FAVORITES && it != com.poyankee.note.ui.TRASH && it != com.poyankee.note.ui.TEMPLATE_FOLDER } ?: ""
        var created: String? = null
        runOp(L.t("Defter hazırlanıyor..."), {
            store.flush()
            val src = store.load(id)
            val nb = com.poyankee.note.data.PdfOps.extract(applicationContext, store, id, src.pages.indices.toList(), src.name.removePrefix(L.t("Şablon: ")).removePrefix("Şablon: "))
            store.setFolder(nb.id, f)
            store.flush()
            created = nb.id
            null
        }) { created?.let { open(it) } }
    }

    private fun mergeNotebooks(ids: List<String>) {
        runOp(L.t("Defterler birleştiriliyor..."), {
            store.flush()
            val nb = com.poyankee.note.data.PdfOps.merge(applicationContext, store, ids, L.t("Birleştirilmiş defter"))
            "\"${nb.name}\" oluşturuldu (${nb.pages.size} sayfa)"
        })
    }

    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNullOrEmpty()) return@registerForActivityResult
        val f = folder?.takeIf { it != com.poyankee.note.ui.FAVORITES && it != com.poyankee.note.ui.TRASH } ?: ""
        var created: String? = null
        runOp(L.t("Fotoğraflar defter yapılıyor..."), {
            val nb = com.poyankee.note.data.PdfOps.fromImages(applicationContext, store, uris, L.t("Fotoğraflardan defter"), f)
            created = nb.id
            null
        }) { created?.let { open(it) } }
    }

    private fun importPdf(uri: Uri) {
        closeEditor()
        busy = L.t("PDF açılıyor...")
        val f = folder?.takeIf { it != com.poyankee.note.ui.FAVORITES && it != com.poyankee.note.ui.TRASH } ?: ""
        bg.execute {
            try {
                val nb = store.importPdf(applicationContext, uri, f)
                runOnUiThread { busy = null; showEditor(EditorController(this, store, settings, nb)); refresh() }
            } catch (e: Exception) {
                runOnUiThread { busy = null; android.widget.Toast.makeText(this, e.message ?: L.t("PDF açılamadı"), android.widget.Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun create(name: String, inFolder: String? = null) {
        val title = name.ifBlank { L.t("Adsız defter") }
        // yeni defter ve hızlı not her zaman düz beyaz açılır (desen defter içinden değiştirilir)
        val size = runCatching { PageSize.valueOf(settings.newSize.value) }.getOrDefault(PageSize.A4_PORTRAIT)
        // "Tam sayfa": sayfa ekranın en-boy oranında (üst/alt çubuklar için biraz pay bırakılır)
        val dm = resources.displayMetrics
        val landscape = dm.widthPixels > dm.heightPixels
        val pw = if (size == PageSize.SCREEN) (if (landscape) 842f else 595f) else size.w
        val ph = if (size == PageSize.SCREEN) pw * ((dm.heightPixels - 90 * dm.density) / dm.widthPixels).coerceIn(0.45f, 2.2f) else size.h
        val paper = Background.Paper(Template.BLANK, com.poyankee.note.core.PaperColors.WHITE)
        val f = inFolder ?: folder?.takeIf { it != com.poyankee.note.ui.FAVORITES && it != com.poyankee.note.ui.TRASH } ?: ""
        bg.execute {
            val nb = store.create(title, paper, size, f, pw, ph)
            runOnUiThread { showEditor(EditorController(this, store, settings, nb)); refresh() }
        }
    }

    /** Hızlı not: "Hızlı notlar" klasöründe tarihli boş bir defter açar. */
    private fun quickNote() {
        closeEditor()
        addFolder(QUICK_FOLDER)
        val name = L.t("Not ") + java.text.SimpleDateFormat("d MMM HH:mm", java.util.Locale.forLanguageTag("tr")).format(java.util.Date())
        create(name, QUICK_FOLDER)
    }

    private fun openById(id: String) {
        val info = notebooks.firstOrNull { it.id == id }
        if (info != null && info.locked) { pinFor = info; return }
        open(id)
    }

    private fun open(id: String) {
        loading = true
        openExec.execute {
            try {
                store.flush() // önceki defterin kaydı bitsin (arka planda)
                val nb = store.load(id)
                runOnUiThread { loading = false; showEditor(EditorController(this, store, settings, nb)) }
            } catch (e: Exception) {
                runOnUiThread {
                    loading = false
                    android.widget.Toast.makeText(this, "Defter açılamadı: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showEditor(c: EditorController) {
        // yanda başka bir defter açıksa önce onu kaydedip kapat
        val old = editor
        if (old != null && old !== c) closeEditor(keepTab = true, quiet = true)
        editor = c
        if (tabs.none { it.first == c.nb.id }) tabs = (tabs + (c.nb.id to c.nb.name)).takeLast(6)
    }

    private fun closeTab(id: String) {
        val ed = editor
        val rest = tabs.filter { it.first != id }
        if (ed != null && ed.nb.id == id) {
            closeEditor()
            tabs = rest
            rest.lastOrNull()?.let { openById(it.first) }
        } else tabs = rest
    }

    private fun closeEditor(keepTab: Boolean = true, quiet: Boolean = false) {
        val ed = editor ?: return
        editor = null
        if (!keepTab) tabs = tabs.filter { it.first != ed.nb.id }
        tabs = tabs.map { if (it.first == ed.nb.id) it.first to ed.nb.name else it }
        ed.close()
        if (!quiet) android.widget.Toast.makeText(this, L.t("Kaydedildi"), android.widget.Toast.LENGTH_SHORT).show()
        refresh()
        bg.execute { Thread.sleep(800); runOnUiThread { refresh() } }
    }

    override fun onPause() {
        super.onPause()
        editor?.let { store.saveMeta(it.nb) }
        store.flush()
    }

    override fun onDestroy() {
        if (isFinishing) editor?.close()
        super.onDestroy()
    }

    companion object {
        const val ACTION_QUICK_NOTE = "com.poyankee.note.QUICK_NOTE"
        const val QUICK_FOLDER = "Hızlı notlar"

        /** Seçili dili uygular (İngilizce ise sözlüğü yükler). */
        fun applyLanguage(ctx: android.content.Context) {
            val en = ctx.getSharedPreferences("settings", MODE_PRIVATE).getString("language", "tr") == "en"
            if (en && L.dict.isEmpty()) L.dict = try { ctx.assets.open("i18n/en.tsv").bufferedReader().use { L.parse(it.readText()) } } catch (_: Exception) { emptyMap() }
            L.english = en
        }

        /** Dil değişince uygulamayı yeniden başlatır (bütün yazılar yeni dille açılır). */
        fun restart(activity: android.app.Activity) {
            // ayarlar diske yazılmadan süreç kapanırsa dil değişikliği kaybolur: önce eşzamanlı yaz
            try { activity.getSharedPreferences("settings", MODE_PRIVATE).edit().commit() } catch (_: Exception) {}
            val i = android.content.Intent(activity, MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
            activity.startActivity(i)
            Runtime.getRuntime().exit(0)
        }
    }
}
