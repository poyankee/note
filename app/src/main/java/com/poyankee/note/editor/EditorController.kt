package com.poyankee.note.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.poyankee.note.core.Background
import com.poyankee.note.core.Bookmark
import com.poyankee.note.core.Ids
import com.poyankee.note.core.ImageItem
import com.poyankee.note.core.Item
import com.poyankee.note.core.Notebook
import com.poyankee.note.core.Page
import com.poyankee.note.core.PageNumbers
import com.poyankee.note.core.Template
import com.poyankee.note.core.TextItem
import com.poyankee.note.core.UndoStack
import com.poyankee.note.data.Settings
import com.poyankee.note.data.Store
import com.poyankee.note.render.ImageSource
import com.poyankee.note.render.PageRenderer
import com.poyankee.note.render.PdfPages
import java.io.File
import java.util.concurrent.Executors

enum class Tool(val title: String) {
    PEN("Kalem"), HIGHLIGHTER("Fosforlu kalem"), ERASER("Silgi"), LASSO("Kement seçim"),
    TEXT("Metin"), LASER("Lazer işaretçi"), TEMP("Geçici kalem"),
    SHAPE("Şekil kalemi"), LINE("Doğru parçası"), ARROW("Ok"), POLYGON("Düzgün çokgen"),
    COMPASS("Pergel"), MARK("Geometri işareti");

    val isGeometry get() = this in setOf(SHAPE, LINE, ARROW, POLYGON, COMPASS, MARK)
}

enum class MarkKind(val title: String) { ANGLE("Açı / dik açı"), TICK("Eşit uzunluk çentiği"), PARALLEL("Paralel oku") }

class TextRequest(val pageIndex: Int, val x: Float, val y: Float, val existing: TextItem?)

/**
 * Düzenleyicinin durumu. Compose arayüzü buradaki durumları okur; InkView çizim ve girişi yönetir.
 */
class EditorController(
    val context: Context,
    val store: Store,
    val settings: Settings,
    val nb: Notebook,
) {
    val undoStack = UndoStack()
    val main = Handler(Looper.getMainLooper())
    var view: InkView? = null

    val pdf: PdfPages? = nb.pdfFile?.let { f ->
        val file = File(store.dir(nb.id), f)
        if (file.exists()) try { PdfPages(file) { view?.onPdfUpdated() } } catch (_: Exception) { null } else null
    }

    /* ---------- Compose tarafından izlenen durum ---------- */
    var tool by mutableStateOf(try { Tool.valueOf(settings.tool.value) } catch (_: Exception) { Tool.PEN })
        private set
    var lastGeometryTool by mutableStateOf(Tool.SHAPE)
    var penColor by mutableIntStateOf(settings.penColor.value)
    var penWidth by mutableStateOf(settings.penWidth.value / 10f)
    var hlColor by mutableIntStateOf(settings.hlColor.value)
    var hlWidth by mutableStateOf(settings.hlWidthX10.value / 10f)
    var eraserPartial by mutableStateOf(settings.eraserPartial.value)
    var eraserSize by mutableIntStateOf(settings.eraserSize.value)
    var polygonSides by mutableIntStateOf(settings.polygonSides.value)
    var markKind by mutableStateOf(MarkKind.ANGLE)
    var markCount by mutableIntStateOf(1)
    var shapeDashed by mutableStateOf(false)

    var canUndo by mutableStateOf(false)
    var canRedo by mutableStateOf(false)
    var currentPage by mutableIntStateOf(0)
    var pageCount by mutableIntStateOf(nb.pages.size)
    var hasSelection by mutableStateOf(false)
    var presentation by mutableStateOf(false)
    var hint by mutableStateOf<String?>(null)
    var message by mutableStateOf<String?>(null)
    var textRequest by mutableStateOf<TextRequest?>(null)
    val history = mutableStateListOf<Int>()
    val instruments = mutableStateListOf<InstrumentKind>()
    var secondScreenActive by mutableStateOf(false)
    var pagesVersion by mutableIntStateOf(0)
    var bookmarksVersion by mutableIntStateOf(0)

    /** Ekran görüntüsünü izleyen (ikinci ekran, kayıt) dinleyiciler. */
    val frameListeners = ArrayList<() -> Unit>()

    fun selectTool(t: Tool) {
        if (t != Tool.LASSO) view?.clearSelection()
        view?.cancelToolState()
        tool = t
        if (t.isGeometry) lastGeometryTool = t
        settings.tool.value = t.name
        hint = when (t) {
            Tool.COMPASS -> "Pergel: kalemi merkeze koyup yarıçap kadar çekin, sonra kalemi döndürerek yayı çizin."
            Tool.MARK -> "İşaret: köşeye dokunun (açı/dik açı) veya kenara dokunun (çentik/paralel)."
            Tool.POLYGON -> "Çokgen: merkezden bir köşeye doğru çekin."
            else -> null
        }
    }

    val activeColor get() = if (tool == Tool.HIGHLIGHTER) hlColor else penColor

    fun setColor(c: Int) {
        if (tool == Tool.HIGHLIGHTER) {
            val hc = (c and 0x00FFFFFF) or 0x66000000
            hlColor = hc; settings.hlColor.value = hc
        } else {
            penColor = c; settings.penColor.value = c
            if (tool == Tool.ERASER || tool == Tool.LASSO || tool == Tool.LASER) selectTool(Tool.PEN)
        }
        if (hasSelection) view?.selectionRecolor(c)
    }

    fun setWidth(w: Float) {
        if (tool == Tool.HIGHLIGHTER) { hlWidth = w; settings.hlWidthX10.value = (w * 10).toInt() }
        else { penWidth = w; settings.penWidth.value = (w * 10).toInt() }
    }

    val activeWidth get() = if (tool == Tool.HIGHLIGHTER) hlWidth else penWidth

    /* ---------- düzenleme ---------- */

    /** Değişikliği uygular, geri alma listesine ekler ve kaydeder. */
    fun commit(page: Page, remove: Collection<Item>, add: List<Item>) {
        undoStack.apply(page, remove, add) ?: return
        pageChanged(page)
    }

    fun pageChanged(page: Page) {
        canUndo = undoStack.canUndo; canRedo = undoStack.canRedo
        store.savePage(nb.id, page)
        view?.invalidatePage(page.id)
    }

    fun undo() {
        view?.clearSelection()
        val id = undoStack.undo(nb) ?: return
        nb.pages.firstOrNull { it.id == id }?.let { pageChanged(it) }
    }

    fun redo() {
        view?.clearSelection()
        val id = undoStack.redo(nb) ?: return
        nb.pages.firstOrNull { it.id == id }?.let { pageChanged(it) }
    }

    fun clearPage() {
        val p = nb.pages.getOrNull(currentPage) ?: return
        view?.clearSelection()
        commit(p, p.items.toList(), emptyList())
    }

    /* ---------- sayfalar ---------- */

    fun pageLabel(i: Int): String {
        val p = nb.pages.getOrNull(i) ?: return ""
        val b = PageNumbers.bookNumberOf(nb, p)
        return if (b != null && nb.pageOffset != 0) "${i + 1} / ${nb.pages.size}  (kitap s. $b)" else "${i + 1} / ${nb.pages.size}"
    }

    fun goToPage(i: Int, remember: Boolean = true) {
        val idx = i.coerceIn(0, nb.pages.size - 1)
        if (remember && idx != currentPage) {
            history.add(currentPage)
            if (history.size > 30) history.removeAt(0)
        }
        view?.goToPage(idx)
    }

    fun goBack() {
        if (history.isEmpty()) return
        val i = history.removeAt(history.size - 1)
        view?.goToPage(i.coerceIn(0, nb.pages.size - 1))
    }

    /** Kitap sayfa numarasına git. Bulunamazsa false. */
    fun goToBookPage(book: Int): Boolean {
        val i = PageNumbers.indexOfBookPage(nb, book) ?: return false
        goToPage(i); return true
    }

    /** "Bu sayfa kitapta kaçıncı sayfa?" sorusunun cevabıyla ofseti ayarla. */
    fun setBookNumberForCurrent(book: Int): Boolean {
        val bg = nb.pages.getOrNull(currentPage)?.background as? Background.PdfPage ?: return false
        nb.pageOffset = bg.index + 1 - book
        store.saveMeta(nb)
        pagesVersion++
        return true
    }

    private fun newPaperLike(ref: Page?, template: Template?): Page {
        val bg = ref?.background
        // PDF sayfasının arasına eklenen sayfa PDF ile aynı boyutta olsun
        if (ref != null && bg is Background.PdfPage) {
            return Page(Ids.next(), Background.Paper(template ?: nb.defaultTemplate, false), ref.width, ref.height)
        }
        val paper = bg as? Background.Paper
        val t = template ?: paper?.template ?: nb.defaultTemplate
        val dark = paper?.dark ?: nb.defaultDark
        val w = if (ref != null && paper != null) ref.width else nb.defaultSize.w
        val h = if (ref != null && paper != null) ref.height else nb.defaultSize.h
        return Page(Ids.next(), Background.Paper(t, dark), w, h)
    }

    fun insertPageAfter(index: Int, template: Template? = null) {
        val ref = nb.pages.getOrNull(index)
        val p = newPaperLike(ref, template)
        nb.pages.add((index + 1).coerceIn(0, nb.pages.size), p)
        pagesChanged()
        goToPage(index + 1, remember = false)
    }

    fun duplicatePage(index: Int) {
        val src = nb.pages.getOrNull(index) ?: return
        val p = Page(Ids.next(), src.background, src.width, src.height)
        p.items.addAll(src.items.map { it.transformed(com.poyankee.note.core.Similarity(), Ids.next()) })
        nb.pages.add(index + 1, p)
        store.savePage(nb.id, p)
        pagesChanged()
    }

    fun deletePage(index: Int) {
        if (nb.pages.size <= 1) { message = "Son sayfa silinemez"; return }
        val p = nb.pages.removeAt(index)
        nb.bookmarks.removeAll { it.pageId == p.id }
        store.deletePageFile(nb.id, p.id)
        history.clear()
        pagesChanged()
    }

    fun movePage(index: Int, delta: Int) {
        val to = index + delta
        if (index !in nb.pages.indices || to !in nb.pages.indices) return
        val p = nb.pages.removeAt(index)
        nb.pages.add(to, p)
        pagesChanged()
    }

    fun setTemplate(index: Int, t: Template, dark: Boolean) {
        val p = nb.pages.getOrNull(index) ?: return
        if (p.background is Background.PdfPage) return
        p.background = Background.Paper(t, dark)
        store.saveMeta(nb)
        view?.invalidatePage(p.id)
        pagesVersion++
    }

    private fun pagesChanged() {
        pageCount = nb.pages.size
        store.saveMeta(nb)
        view?.relayoutPages()
        pagesVersion++
    }

    /* ---------- yer imleri ---------- */

    fun addBookmark(title: String) {
        val p = nb.pages.getOrNull(currentPage) ?: return
        nb.bookmarks.removeAll { it.pageId == p.id }
        nb.bookmarks += Bookmark(p.id, title.ifBlank { "Sayfa ${currentPage + 1}" })
        store.saveMeta(nb); bookmarksVersion++
    }

    fun removeBookmark(b: Bookmark) {
        nb.bookmarks.remove(b); store.saveMeta(nb); bookmarksVersion++
    }

    /* ---------- metin ---------- */

    fun requestText(pageIndex: Int, x: Float, y: Float, existing: TextItem?) {
        textRequest = TextRequest(pageIndex, x, y, existing)
    }

    fun finishText(req: TextRequest, text: String, size: Float) {
        textRequest = null
        val page = nb.pages.getOrNull(req.pageIndex) ?: return
        val old = req.existing
        if (text.isBlank()) {
            if (old != null) commit(page, listOf(old), emptyList())
            return
        }
        val item = if (old != null) TextItem(old.id, old.x, old.y, text, size, old.color, old.rotation)
        else TextItem(Ids.next(), req.x, req.y, text, size, penColor)
        commit(page, listOfNotNull(old), listOf(item))
    }

    /* ---------- görseller ---------- */

    private val imageExec = Executors.newSingleThreadExecutor()
    private val imageCache = object : LruCache<String, Bitmap>(64 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val loading = HashSet<String>()

    val images = ImageSource { file ->
        imageCache.get(file) ?: run {
            if (loading.add(file)) imageExec.execute {
                val bmp = try { BitmapFactory.decodeFile(File(store.dir(nb.id), file).path) } catch (_: Throwable) { null }
                main.post {
                    loading.remove(file)
                    if (bmp != null) { imageCache.put(file, bmp); view?.invalidateAllPages() }
                }
            }
            null
        }
    }

    /** Senkron görsel kaynağı (arka plan işleri için). */
    fun loadImageBlocking(file: String): Bitmap? = imageCache.get(file)
        ?: try { BitmapFactory.decodeFile(File(store.dir(nb.id), file).path) } catch (_: Throwable) { null }

    fun insertImage(uri: Uri) {
        imageExec.execute {
            try {
                val (name, w, h) = store.importImage(context, nb.id, uri)
                main.post {
                    val v = view ?: return@post
                    val (pi, cx, cy) = v.visibleCenter()
                    val page = nb.pages[pi]
                    val maxW = page.width * 0.5f; val maxH = page.height * 0.4f
                    val s = minOf(maxW / w, maxH / h)
                    val iw = w * s; val ih = h * s
                    val x = (cx - iw / 2).coerceIn(0f, page.width - iw); val y = (cy - ih / 2).coerceIn(0f, page.height - ih)
                    val item = ImageItem(Ids.next(), x, y, iw, ih, name)
                    commit(page, emptyList(), listOf(item))
                    selectTool(Tool.LASSO)
                    v.select(pi, listOf(item))
                }
            } catch (e: Exception) {
                main.post { message = "Görsel eklenemedi: ${e.message}" }
            }
        }
    }

    /* ---------- küçük resimler ---------- */

    private val thumbExec = Executors.newSingleThreadExecutor()
    private val thumbRenderer by lazy { PageRenderer { loadImageBlocking(it) } }
    private val thumbs = object : LruCache<Long, Bitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: Long, value: Bitmap) = value.byteCount
    }
    private val thumbKeys = HashMap<Long, Int>()

    /** Sayfa küçük resmi; hazır değilse null döner, hazır olunca [done] çağrılır. */
    fun thumbnail(index: Int, widthPx: Int, done: () -> Unit): Bitmap? {
        val page = nb.pages.getOrNull(index) ?: return null
        val key = page.items.size * 31 + page.items.lastOrNull()?.id.hashCode() + page.background.hashCode()
        val cached = thumbs.get(page.id)
        if (cached != null && thumbKeys[page.id] == key) return cached
        val snapshot = Page(page.id, page.background, page.width, page.height).also { it.items.addAll(page.items) }
        val scale = widthPx / page.width
        val job: () -> Unit = {
            val bg = (snapshot.background as? Background.PdfPage)?.let { pdf?.renderBlocking(it.index, scale) }
            val bmp = try { thumbRenderer.renderPage(snapshot, scale, bg) } catch (_: Throwable) { null }
            bg?.recycle()
            main.post { if (bmp != null) { thumbs.put(snapshot.id, bmp); thumbKeys[snapshot.id] = key; done() } }
        }
        if (pdf != null) pdf.runOnPdfThread(job) else thumbExec.execute { job() }
        return cached
    }

    fun close() {
        view?.clearSelection()
        store.saveMeta(nb)
        store.flush()
        pdf?.close()
        imageExec.shutdown(); thumbExec.shutdown()
    }
}
