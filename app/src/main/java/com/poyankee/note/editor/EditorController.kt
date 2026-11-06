package com.poyankee.note.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.poyankee.note.core.Background
import com.poyankee.note.core.Bookmark
import com.poyankee.note.core.Crop
import com.poyankee.note.core.Geo
import com.poyankee.note.core.Geom
import com.poyankee.note.core.Ids
import com.poyankee.note.core.ImageItem
import com.poyankee.note.core.Item
import com.poyankee.note.core.MathExpr
import com.poyankee.note.core.Notebook
import com.poyankee.note.core.Page
import com.poyankee.note.core.PageNumbers
import com.poyankee.note.core.PageState
import com.poyankee.note.core.PaperColors
import com.poyankee.note.core.PdfGeom
import com.poyankee.note.core.Plot
import com.poyankee.note.core.ShapeItem
import com.poyankee.note.core.Similarity
import com.poyankee.note.core.Solids
import com.poyankee.note.core.Template
import com.poyankee.note.core.Templates
import com.poyankee.note.core.TextItem
import com.poyankee.note.core.UndoStack
import com.poyankee.note.core.VertexLabels
import com.poyankee.note.data.PenAction
import com.poyankee.note.data.PenPreset
import com.poyankee.note.data.Settings
import com.poyankee.note.data.Store
import com.poyankee.note.render.Formula
import com.poyankee.note.render.ImageSource
import com.poyankee.note.render.PageRenderer
import com.poyankee.note.render.PdfPages
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

enum class Tool(val title: String) {
    PEN("Kalem"), HIGHLIGHTER("Fosforlu kalem"), ERASER("Silgi"), LASSO("Kement seçim"),
    TEXT("Metin"), LASER("Lazer işaretçi"), TEMP("Geçici kalem"), SPOT("Spot ışığı"),
    SHAPE("Şekil kalemi"), LINE("Doğru parçası"), ARROW("Ok"), POLYGON("Düzgün çokgen"),
    COMPASS("Pergel"), MARK("Geometri işareti"), SOLID("3B şekil");

    val isGeometry get() = this in setOf(SHAPE, LINE, ARROW, POLYGON, COMPASS, MARK, SOLID)
}

enum class MarkKind(val title: String) { ANGLE("Açı / dik açı"), TICK("Eşit uzunluk çentiği"), PARALLEL("Paralel oku") }

class TextRequest(val pageIndex: Int, val x: Float, val y: Float, val existing: TextItem?, val sticky: Boolean = false)
class FormulaRequest(val pageIndex: Int, val existing: ImageItem?)

/** Uygulama içi pano: kopyalanan öğeler (defterler arası da çalışır). */
object Clipboard {
    var items: List<Item> = emptyList()
    var sourceDir: File? = null
}

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
    private var previousTool = Tool.PEN
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
    var shapeFill by mutableStateOf(false)
    var solidKind by mutableStateOf(Solids.Kind.CUBE)

    var canUndo by mutableStateOf(false)
    var canRedo by mutableStateOf(false)
    var currentPage by mutableIntStateOf(settings.lastPage(nb.id).coerceIn(0, maxOf(0, nb.pages.size - 1)))
    var pageCount by mutableIntStateOf(nb.pages.size)
    var hasSelection by mutableStateOf(false)
    var selectionHasShapes by mutableStateOf(false)
    var presentation by mutableStateOf(false)
    var hint by mutableStateOf<String?>(null)
    var message by mutableStateOf<String?>(null)
    var textRequest by mutableStateOf<TextRequest?>(null)
    var formulaRequest by mutableStateOf<FormulaRequest?>(null)
    val history = mutableStateListOf<Int>()
    val instruments = mutableStateListOf<InstrumentKind>()
    var secondScreenActive by mutableStateOf(false)
    var pagesVersion by mutableIntStateOf(0)
    /** Parmakla kaydırıldıkça artar (sağ kenardaki sayfa çubuğunu göstermek için). */
    var scrollTick by mutableIntStateOf(0)
    var bookmarksVersion by mutableIntStateOf(0)
    var clipboardCount by mutableIntStateOf(Clipboard.items.size)

    // ders araçları
    var curtain by mutableStateOf(false)
    var timerEnd by mutableLongStateOf(0L)
    var timerPaused by mutableLongStateOf(0L)

    /** Ekran görüntüsünü izleyen (ikinci ekran, kayıt) dinleyiciler. */
    val frameListeners = ArrayList<() -> Unit>()

    fun selectTool(t: Tool) {
        if (t != Tool.LASSO) view?.clearSelection()
        view?.cancelToolState()
        if (t != tool) previousTool = tool
        tool = t
        if (t.isGeometry) lastGeometryTool = t
        settings.tool.value = t.name
        hint = when (t) {
            Tool.COMPASS -> "Pergel: kalemi merkeze koyup yarıçap kadar çekin, sonra kalemi döndürerek yayı çizin."
            Tool.MARK -> "İşaret: köşeye dokunun (açı/dik açı) veya kenara dokunun (çentik/paralel)."
            Tool.POLYGON -> "Çokgen: merkezden bir köşeye doğru çekin."
            Tool.SOLID -> "3B şekil: kalemle bir kutu çizer gibi çapraz çekin."
            else -> null
        }
        view?.invalidate()
    }

    val activeColor get() = if (tool == Tool.HIGHLIGHTER) hlColor else penColor

    fun setColor(c: Int) {
        if (tool == Tool.HIGHLIGHTER) {
            val hc = (c and 0x00FFFFFF) or 0x66000000
            hlColor = hc; settings.hlColor.value = hc
        } else {
            penColor = c; settings.penColor.value = c
            if (tool == Tool.ERASER || tool == Tool.LASSO || tool == Tool.LASER || tool == Tool.SPOT) selectTool(Tool.PEN)
        }
        if (hasSelection) view?.selectionRecolor(c)
    }

    fun setWidth(w: Float) {
        if (tool == Tool.HIGHLIGHTER) { hlWidth = w; settings.hlWidthX10.value = (w * 10).toInt() }
        else { penWidth = w; settings.penWidth.value = (w * 10).toInt() }
    }

    val activeWidth get() = if (tool == Tool.HIGHLIGHTER) hlWidth else penWidth

    /* ---------- kalem hazır ayarları ve kalem tuşu ---------- */

    fun applyPreset(p: PenPreset) {
        selectTool(if (p.highlighter) Tool.HIGHLIGHTER else Tool.PEN)
        setColor(p.color); setWidth(p.width)
    }

    fun saveCurrentAsPreset() {
        val hl = tool == Tool.HIGHLIGHTER
        settings.addPreset(PenPreset(hl, activeColor, activeWidth))
        message = "Kalem kaydedildi"
    }

    /** Kalemin çift dokunuşu / tuşu. */
    fun onPenKey() {
        when (settings.penKeyActionValue) {
            PenAction.ERASER_TOGGLE -> selectTool(if (tool == Tool.ERASER) (if (previousTool == Tool.ERASER) Tool.PEN else previousTool) else Tool.ERASER)
            PenAction.UNDO -> undo()
            PenAction.LASER_TOGGLE -> selectTool(if (tool == Tool.LASER) (if (previousTool == Tool.LASER) Tool.PEN else previousTool) else Tool.LASER)
            PenAction.PEN_HIGHLIGHTER -> selectTool(if (tool == Tool.HIGHLIGHTER) Tool.PEN else Tool.HIGHLIGHTER)
            PenAction.NONE -> {}
        }
    }

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
        val r = undoStack.undo(nb) ?: return
        afterUndo(r.pageId, r.pagesChanged)
    }

    fun redo() {
        view?.clearSelection()
        val r = undoStack.redo(nb) ?: return
        afterUndo(r.pageId, r.pagesChanged)
    }

    private fun afterUndo(pageId: Long?, structural: Boolean) {
        val page = pageId?.let { id -> nb.pages.firstOrNull { it.id == id } }
        if (structural) {
            page?.let { store.savePage(nb.id, it) }
            pagesChanged()
            canUndo = undoStack.canUndo; canRedo = undoStack.canRedo
            page?.let { view?.invalidatePage(it.id) }
        } else page?.let { pageChanged(it) }
    }

    fun clearPage() {
        val p = nb.pages.getOrNull(currentPage) ?: return
        view?.clearSelection()
        commit(p, p.items.toList(), emptyList())
    }

    /* ---------- şekil eklentileri: köşe harfleri, ölçüler ---------- */

    /** Yeni çizilen şekiller için otomatik köşe harfleri ve uzunluk etiketleri. */
    fun shapeExtras(page: Page, geoms: List<Geom>): List<Item> {
        val out = ArrayList<Item>()
        val size = 13f
        if (settings.autoLetters.value) {
            for (g in geoms) {
                val poly = g as? Geom.Polyline ?: continue
                if (!poly.closed || poly.n < 3 || poly.n > 12) continue
                val letters = VertexLabels.nextLetters(page.items + out, poly.n)
                VertexLabels.positions(poly.pts, size).forEachIndexed { i, (x, y) ->
                    out += TextItem(Ids.next(), x, y, letters[i], size, penColor)
                }
            }
        }
        if (settings.autoMeasures.value) {
            for (g in geoms) {
                val poly = g as? Geom.Polyline ?: continue
                if (poly.closed || poly.n != 2) continue
                val len = Geo.dist(poly.pts[0], poly.pts[1], poly.pts[2], poly.pts[3]) / (Templates.MM * 10f)
                val (x, y) = VertexLabels.midLabel(poly.pts[0], poly.pts[1], poly.pts[2], poly.pts[3], 11f)
                out += TextItem(Ids.next(), x, y, String.format("%.1f cm", len), 11f, penColor)
            }
        }
        return out
    }

    /** Açı işareti için derece etiketi (ölçüler açıksa). */
    fun angleLabel(g: Geom): Item? {
        if (!settings.autoMeasures.value) return null
        return when (g) {
            is Geom.Arc -> {
                val mid = g.start + g.sweep / 2f
                val r = g.r + 12f
                TextItem(Ids.next(), g.cx + r * kotlin.math.cos(mid) - 8f, g.cy + r * kotlin.math.sin(mid) - 6f,
                    String.format("%.0f°", abs(Geo.toDeg(g.sweep))), 10f, penColor)
            }
            else -> null
        }
    }

    val fillColor get() = (penColor and 0x00FFFFFF) or 0x40000000

    /* ---------- pano ---------- */

    fun copy(items: List<Item>) {
        Clipboard.items = items
        Clipboard.sourceDir = store.dir(nb.id)
        clipboardCount = items.size
        message = "${items.size} öğe kopyalandı"
    }

    /** Panodaki öğeleri ekrandaki sayfaya yapıştırır ve seçer. */
    fun paste() {
        val items = Clipboard.items
        if (items.isEmpty()) { message = "Pano boş"; return }
        val v = view ?: return
        val (pi, cx, cy) = v.visibleCenter()
        val page = nb.pages[pi]
        // görselleri başka defterden geliyorsa kopyala
        val src = Clipboard.sourceDir
        val dst = store.dir(nb.id)
        if (src != null && src != dst) {
            for (it in items) {
                if (it is ImageItem) {
                    val f = File(src, it.file)
                    if (f.exists() && !File(dst, it.file).exists()) try { f.copyTo(File(dst, it.file)) } catch (_: Exception) {}
                }
            }
        }
        val box = items.map { it.bounds() }.reduce { a, b -> a.union(b) }
        // aynı yere üst üste gelmesin: ekranın ortasına taşı
        val sim = Similarity.translate(cx - box.cx, cy - box.cy)
        val pasted = items.map { it.transformed(sim, Ids.next()) }
        commit(page, emptyList(), pasted)
        selectTool(Tool.LASSO)
        v.select(pi, pasted)
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

    /** Yeni sayfa: verilen stil, yoksa ayara göre düz beyaz ya da yanındaki sayfanın stili. */
    private fun newPaperLike(ref: Page?, style: Background.Paper?): Page {
        val bg = ref?.background
        val paper = style ?: if (settings.newPageSame.value) ((bg as? Background.Paper) ?: nb.defaultPaper) else Background.Paper(Template.BLANK, PaperColors.WHITE)
        val w = ref?.width ?: nb.defaultSize.w
        val h = ref?.height ?: nb.defaultSize.h
        return Page(Ids.next(), paper, w, h)
    }

    fun insertPageAfter(index: Int, style: Background.Paper? = null) {
        val ref = nb.pages.getOrNull(index)
        val p = newPaperLike(ref, style)
        val at = (index + 1).coerceIn(0, nb.pages.size)
        nb.pages.add(at, p)
        undoStack.pageAdded(p, at)
        pagesChanged()
        goToPage(at, remember = false)
    }

    /** Son sayfanın altına yazılınca: yeni düz sayfa ekler, görünümü kaydırmaz. Geri alınabilir. */
    fun appendPageQuietly() {
        val ref = nb.pages.lastOrNull()
        val p = newPaperLike(ref, null)
        nb.pages.add(p)
        undoStack.pageAdded(p, nb.pages.size - 1)
        store.savePage(nb.id, p)
        pagesChanged()
        message = "Yeni sayfa eklendi"
    }

    fun duplicatePage(index: Int) {
        val src = nb.pages.getOrNull(index) ?: return
        val p = Page(Ids.next(), src.background, src.width, src.height)
        p.items.addAll(src.items.map { it.transformed(Similarity(), Ids.next()) })
        nb.pages.add(index + 1, p)
        undoStack.pageAdded(p, index + 1)
        store.savePage(nb.id, p)
        pagesChanged()
    }

    fun deletePage(index: Int) {
        if (nb.pages.size <= 1) { message = "Son sayfa silinemez"; return }
        val p = nb.pages.removeAt(index)
        undoStack.pageRemoved(p, index)
        // sayfa dosyası silinmez: "Geri al" ile geri gelebilsin (defter açılırken artıklar temizlenir)
        history.clear()
        pagesChanged()
        message = "Sayfa silindi — geri almak için ↶"
    }

    fun movePage(index: Int, delta: Int) {
        val to = index + delta
        if (index !in nb.pages.indices || to !in nb.pages.indices) return
        val p = nb.pages.removeAt(index)
        nb.pages.add(to, p)
        undoStack.pageMoved(index, to)
        pagesChanged()
    }

    /** Sayfayı 90° saat yönünde döndürür (üzerindeki yazılar da döner). */
    fun rotatePage(index: Int) {
        val p = nb.pages.getOrNull(index) ?: return
        val before = PageState.of(p)
        when (val bg = p.background) {
            is Background.PdfPage -> {
                val fw = if (bg.fullW > 0f) bg.fullW else p.width
                val fh = if (bg.fullH > 0f) bg.fullH else p.height
                val nr = (bg.rotation + 90) % 360
                val nbg = bg.copy(rotation = nr, fullW = fw, fullH = fh)
                val tr = PdfGeom.itemTransform(fw, fh, bg.rotation, bg.crop, nr, bg.crop)
                val (dw, dh) = PdfGeom.displaySize(fw, fh, nr, bg.crop)
                transformPage(p, tr, nbg, dw, dh)
            }
            is Background.Paper -> {
                val tr = PdfGeom.itemTransform(p.width, p.height, 0, Crop.NONE, 90, Crop.NONE)
                transformPage(p, tr, bg, p.height, p.width)
            }
        }
        undoStack.pageChanged(p, before)
        store.savePage(nb.id, p)
        pagesChanged()
        canUndo = undoStack.canUndo
    }

    /** PDF sayfasını kırpar. Paylar ekranda görünen yöne göredir (üst, sağ, alt, sol; 0..0.45). */
    fun setCrop(index: Int, top: Float, right: Float, bottom: Float, left: Float) {
        val p = nb.pages.getOrNull(index) ?: return
        val bg = p.background as? Background.PdfPage ?: return
        val before = PageState.of(p)
        val fw = if (bg.fullW > 0f) bg.fullW else p.width
        val fh = if (bg.fullH > 0f) bg.fullH else p.height
        val crop = PdfGeom.cropFromDisplayMargins(bg.rotation, top, right, bottom, left)
        val tr = PdfGeom.itemTransform(fw, fh, bg.rotation, bg.crop, bg.rotation, crop)
        val (dw, dh) = PdfGeom.displaySize(fw, fh, bg.rotation, crop)
        transformPage(p, tr, bg.copy(crop = crop, fullW = fw, fullH = fh), dw, dh)
        undoStack.pageChanged(p, before)
        store.savePage(nb.id, p)
        pagesChanged()
        canUndo = undoStack.canUndo
    }

    private fun transformPage(p: Page, tr: Similarity, bg: Background, w: Float, h: Float) {
        val moved = p.items.map { it.transformed(tr) }
        p.items.clear(); p.items.addAll(moved)
        p.background = bg; p.width = w; p.height = h
        view?.invalidatePage(p.id)
    }

    /** Sayfanın kağıt stilini (şablon, renk, aralık, saydamlık) değiştirir. [all] = tüm kağıt sayfalarına uygula. */
    fun setPageStyle(index: Int, style: Background.Paper, all: Boolean = false) {
        val targets = if (all) nb.pages.filter { it.background is Background.Paper } else listOfNotNull(nb.pages.getOrNull(index)?.takeIf { it.background is Background.Paper })
        for (p in targets) { p.background = style; view?.invalidatePage(p.id) }
        nb.defaultPaper = style
        store.saveMeta(nb)
        pagesVersion++
    }

    /** Kağıt sayfalarının boyutunu değiştirir (yazılar yerinde kalır). Geri alınabilir. */
    fun setPageSize(index: Int, w: Float, h: Float, all: Boolean) {
        val targets = if (all) nb.pages.filter { it.background is Background.Paper }
        else listOfNotNull(nb.pages.getOrNull(index)?.takeIf { it.background is Background.Paper })
        var changed = false
        for (p in targets) {
            if (abs(p.width - w) < 0.5f && abs(p.height - h) < 0.5f) continue
            val before = PageState.of(p)
            p.width = w; p.height = h
            undoStack.pageChanged(p, before)
            store.savePage(nb.id, p)
            changed = true
        }
        if (changed) pagesChanged()
    }

    /** Ekranın en-boy oranında sayfa boyutu ("Tam ekran"). */
    fun fullScreenSize(): Pair<Float, Float> {
        val v = view
        val a = if (v != null && v.width > 0 && v.height > 0) v.width.toFloat() / v.height else 16f / 10f
        return if (a >= 1f) 842f to (842f / a).roundToInt().toFloat() else (842f * a).roundToInt().toFloat() to 842f
    }

    /** Kendi deseniniz: resmi (veya PDF'in ilk sayfasını) sayfa zemini yapar. */
    fun setPaperImage(uri: Uri, all: Boolean) {
        imageExec.execute {
            try {
                val mime = context.contentResolver.getType(uri) ?: ""
                val name: String = if (mime.contains("pdf")) {
                    val tmp = File(context.cacheDir, "desen.pdf")
                    context.contentResolver.openInputStream(uri)!!.use { inp -> FileOutputStream(tmp).use { inp.copyTo(it) } }
                    val bmp = android.os.ParcelFileDescriptor.open(tmp, android.os.ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                        android.graphics.pdf.PdfRenderer(fd).use { r ->
                            r.openPage(0).use { pg ->
                                val s = 2200f / maxOf(pg.width, pg.height)
                                val b = Bitmap.createBitmap((pg.width * s).toInt(), (pg.height * s).toInt(), Bitmap.Config.ARGB_8888)
                                b.eraseColor(0xFFFFFFFF.toInt())
                                pg.render(b, null, android.graphics.Matrix().apply { setScale(s, s) }, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                b
                            }
                        }
                    }
                    val n = "bg_${Ids.next()}.png"
                    FileOutputStream(File(store.dir(nb.id), n)).use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
                    bmp.recycle()
                    n
                } else store.importImage(context, nb.id, uri).first
                main.post {
                    val cur = nb.pages.getOrNull(currentPage)?.background as? Background.Paper ?: nb.defaultPaper
                    setPageStyle(currentPage, cur.copy(image = name, template = if (cur.template == Template.BLANK) Template.BLANK else cur.template), all)
                }
            } catch (e: Exception) {
                main.post { message = "Desen eklenemedi: ${e.message}" }
            }
        }
    }

    fun rename(name: String) {
        if (name.isBlank()) return
        nb.name = name.trim()
        store.saveMeta(nb)
        pagesVersion++
    }

    private fun pagesChanged() {
        pageCount = nb.pages.size
        store.saveMeta(nb)
        view?.relayoutPages()
        pagesVersion++
        canUndo = undoStack.canUndo; canRedo = undoStack.canRedo
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

    /* ---------- metin, not, damga ---------- */

    fun requestText(pageIndex: Int, x: Float, y: Float, existing: TextItem?) {
        textRequest = TextRequest(pageIndex, x, y, existing, sticky = existing?.bg?.let { it != 0 } ?: false)
    }

    fun requestStickyNote() {
        val v = view ?: return
        val (pi, cx, cy) = v.visibleCenter()
        textRequest = TextRequest(pi, cx - 60f, cy - 30f, null, sticky = true)
    }

    fun finishText(req: TextRequest, text: String, size: Float, noteColor: Int = STICKY_COLORS[0]) {
        textRequest = null
        val page = nb.pages.getOrNull(req.pageIndex) ?: return
        val old = req.existing
        if (text.isBlank()) {
            if (old != null) commit(page, listOf(old), emptyList())
            return
        }
        val bg = if (req.sticky) noteColor else 0
        val item = if (old != null) TextItem(old.id, old.x, old.y, text, size, old.color, old.rotation, if (req.sticky) bg else old.bg)
        else TextItem(Ids.next(), req.x, req.y, text, size, if (req.sticky) 0xFF1D2330.toInt() else penColor, 0f, bg)
        commit(page, listOfNotNull(old), listOf(item))
    }

    /** Damga: büyük işaret veya kısa yazı, ekranın ortasına eklenir ve seçilir. */
    fun insertStamp(text: String, color: Int, bg: Int) {
        val v = view ?: return
        val (pi, cx, cy) = v.visibleCenter()
        val page = nb.pages[pi]
        val size = if (text.length <= 2) 40f else 18f
        val w = text.length * size * 0.6f
        val item = TextItem(Ids.next(), cx - w / 2f, cy - size / 2f, text, size, color, 0f, bg)
        commit(page, emptyList(), listOf(item))
        selectTool(Tool.LASSO)
        v.select(pi, listOf(item))
    }

    /* ---------- formül ---------- */

    fun requestFormula(pageIndex: Int, existing: ImageItem?) { formulaRequest = FormulaRequest(pageIndex, existing) }

    fun finishFormula(req: FormulaRequest, src: String, size: Float) {
        formulaRequest = null
        if (src.isBlank()) return
        val page = nb.pages.getOrNull(req.pageIndex) ?: return
        val color = penColor
        imageExec.execute {
            try {
                val (bmp, w, h) = Formula.render(src, size, color)
                val name = "fx_${Ids.next()}.png"
                FileOutputStream(File(store.dir(nb.id), name)).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                main.post {
                    imageCache.put(name, bmp)
                    val old = req.existing
                    val item = if (old != null) ImageItem(old.id, old.x, old.y, w, h, name, old.rotation, src)
                    else {
                        val v = view
                        val (cx, cy) = if (v != null) v.visibleCenter().let { it.second to it.third } else (page.width / 2 to page.height / 2)
                        ImageItem(Ids.next(), cx - w / 2f, cy - h / 2f, w, h, name, 0f, src)
                    }
                    commit(page, listOfNotNull(old), listOf(item))
                    if (old == null) { selectTool(Tool.LASSO); view?.select(req.pageIndex, listOf(item)) }
                }
            } catch (e: Exception) {
                main.post { message = "Formül çizilemedi: ${e.message}" }
            }
        }
    }

    /* ---------- fonksiyon grafiği ---------- */

    /** Koordinat düzleminin orijini ve birimi (koordinat şablonu değilse sayfa ortası). */
    fun plotFrame(page: Page): Triple<Float, Float, Float> {
        val bg = page.background as? Background.Paper
        val s = bg?.spacing ?: Templates.GRID
        if (bg != null && (bg.template == Template.COORDINATE || bg.template == Template.GRID || bg.template == Template.DOTS || bg.template == Template.UNIT_CIRCLE)) {
            val (ox, oy) = Templates.gridOrigin(page.width, page.height, s)
            val ax = ox + floor((page.width / 2f - ox) / s) * s
            val ay = oy + floor((page.height / 2f - oy) / s) * s
            return Triple(ax, ay, s)
        }
        return Triple(page.width / 2f, page.height / 2f, s)
    }

    /** Grafikleri ekrandaki sayfaya çizer. Hata varsa açıklamasını döndürür. */
    fun plotFunctions(list: List<Pair<String, Int>>): String? {
        val pi = view?.visibleCenter()?.first ?: currentPage
        val page = nb.pages.getOrNull(pi) ?: return "Sayfa yok"
        val (ox, oy, unit) = plotFrame(page)
        val added = ArrayList<Item>()
        for ((src, color) in list) {
            if (src.isBlank()) continue
            val f = try { MathExpr.parse(src) } catch (e: IllegalArgumentException) { return "\"$src\": ${e.message}" }
            val lines = Plot.polylines(f, ox, oy, unit, page.width, page.height)
            if (lines.isEmpty()) return "\"$src\" bu sayfada görünmüyor"
            for (l in lines) added += ShapeItem(Ids.next(), Geom.Polyline(l, false), color, 1.6f)
            // etiket: sağ tarafa yakın, eğrinin görünen bir noktası
            val pts = lines.flatMap { arr -> (0 until arr.size / 2).map { arr[2 * it] to arr[2 * it + 1] } }
                .filter { it.second in 20f..(page.height - 20f) && it.first < page.width - 70f }
            pts.maxByOrNull { it.first }?.let { (x, y) ->
                val label = if (src.trim().startsWith("y") || src.contains("=")) src.trim() else "y = ${src.trim()}"
                added += TextItem(Ids.next(), x - 10f, y - 20f, label, 11f, color)
            }
        }
        if (added.isEmpty()) return "Fonksiyon yazın"
        commit(page, emptyList(), added)
        if ((page.background as? Background.Paper)?.template != Template.COORDINATE) {
            message = "İpucu: \"Sayfa\" panelinden koordinat düzlemi deseni seçerseniz eksenler görünür."
        }
        return null
    }

    /* ---------- ders araçları: sayaç ---------- */

    fun startTimer(seconds: Int) { timerEnd = SystemClock.elapsedRealtime() + seconds * 1000L; timerPaused = 0L; view?.invalidate() }
    fun stopTimer() { timerEnd = 0L; timerPaused = 0L; view?.invalidate() }
    fun timerLeftMs(): Long = if (timerEnd == 0L) 0L else (timerEnd - SystemClock.elapsedRealtime()).coerceAtLeast(0L)

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

    /** Ana sayfadaki defter kapağı için ilk sayfanın küçük resmini kaydeder. */
    private fun saveCover() {
        val page = nb.pages.firstOrNull() ?: return
        val snapshot = Page(page.id, page.background, page.width, page.height).also { it.items.addAll(page.items) }
        val scale = 360f / page.width
        val job: () -> Unit = {
            try {
                val bg = (snapshot.background as? Background.PdfPage)?.let { pdf?.renderBlocking(it, scale) }
                val bmp = thumbRenderer.renderPage(snapshot, scale, bg)
                store.writeCover(nb.id, bmp)
                bmp.recycle(); bg?.recycle()
            } catch (_: Throwable) {}
        }
        if (pdf != null) pdf.runOnPdfThread(job) else thumbExec.execute { job() }
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
        val key = page.items.size * 31 + page.items.lastOrNull()?.id.hashCode() + page.background.hashCode() + page.width.hashCode()
        val cached = thumbs.get(page.id)
        if (cached != null && thumbKeys[page.id] == key) return cached
        val snapshot = Page(page.id, page.background, page.width, page.height).also { it.items.addAll(page.items) }
        val scale = widthPx / page.width
        val job: () -> Unit = {
            val bg = (snapshot.background as? Background.PdfPage)?.let { pdf?.renderBlocking(it, scale) }
            val bmp = try { thumbRenderer.renderPage(snapshot, scale, bg) } catch (_: Throwable) { null }
            bg?.recycle()
            main.post { if (bmp != null) { thumbs.put(snapshot.id, bmp); thumbKeys[snapshot.id] = key; done() } }
        }
        if (pdf != null) pdf.runOnPdfThread(job) else thumbExec.execute { job() }
        return cached
    }

    /** Silgiden önceki araca dön (silgi otomatik dönüşü). */
    fun returnFromEraser() {
        selectTool(if (previousTool == Tool.ERASER) Tool.PEN else previousTool)
    }

    fun close() {
        settings.setLastPage(nb.id, currentPage)
        view?.clearSelection()
        saveCover()
        store.saveMeta(nb)
        store.flush()
        pdf?.close()
        imageExec.shutdown(); thumbExec.shutdown()
    }

    companion object {
        val STICKY_COLORS = listOf(0xFFFFF3A3, 0xFFFFD6E0, 0xFFD4F5DD, 0xFFD6E8FF, 0xFFFFE2C2).map { it.toInt() }
    }
}
