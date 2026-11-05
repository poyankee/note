package com.poyankee.note.core

import kotlin.math.max
import kotlin.math.min

/*
 * Bu paketteki her şey saf Kotlin'dir (Android bağımlılığı yok) ve bilgisayarda birim testiyle denenebilir.
 * Tüm koordinatlar "sayfa koordinatı"dır: birim = PDF noktası (1/72 inç), sol üst köşe (0,0), y aşağı doğru artar.
 */

enum class InkTool { PEN, HIGHLIGHTER }

/** Eksen hizalı dikdörtgen. */
data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
    val cx get() = (left + right) / 2f
    val cy get() = (top + bottom) / 2f
    fun contains(x: Float, y: Float) = x in left..right && y in top..bottom
    fun inflate(d: Float) = Box(left - d, top - d, right + d, bottom + d)
    fun intersects(o: Box) = left <= o.right && o.left <= right && top <= o.bottom && o.top <= bottom
    fun union(o: Box) = Box(min(left, o.left), min(top, o.top), max(right, o.right), max(bottom, o.bottom))

    companion object {
        fun ofPoints(xy: FloatArray, stride: Int = 2): Box {
            if (xy.isEmpty()) return Box(0f, 0f, 0f, 0f)
            var l = Float.MAX_VALUE; var t = Float.MAX_VALUE
            var r = -Float.MAX_VALUE; var b = -Float.MAX_VALUE
            var i = 0
            while (i + 1 < xy.size) {
                val x = xy[i]; val y = xy[i + 1]
                if (x < l) l = x; if (x > r) r = x
                if (y < t) t = y; if (y > b) b = y
                i += stride
            }
            return Box(l, t, r, b)
        }
    }
}

/** Sayfadaki her öğe değişmezdir (immutable). Değişiklik = eski öğeyi çıkar, yenisini ekle. */
sealed class Item {
    abstract val id: Long
    abstract fun bounds(): Box
    abstract fun transformed(t: Similarity, newId: Long = id): Item
}

/**
 * Kalemle çizilmiş serbest vuruş.
 * [pts] = x, y, basınç (0..1) üçlüleri.
 */
class InkItem(
    override val id: Long,
    val tool: InkTool,
    val color: Int,
    val width: Float,
    val pts: FloatArray,
    val pressure: Boolean = true,
) : Item() {
    val count get() = pts.size / 3
    private val box by lazy { Box.ofPoints(pts, 3).inflate(width) }
    override fun bounds() = box
    override fun transformed(t: Similarity, newId: Long): InkItem {
        val out = pts.copyOf()
        var i = 0
        while (i < out.size) {
            val x = out[i]; val y = out[i + 1]
            out[i] = t.mapX(x, y); out[i + 1] = t.mapY(x, y)
            i += 3
        }
        return InkItem(newId, tool, color, width * t.scale, out, pressure)
    }
    fun withColor(c: Int, newId: Long = id) = InkItem(newId, tool, c, width, pts, pressure)
}

/** Geometrik şekil (düz çizgi, çokgen, çember, elips, yay). */
sealed class Geom {
    abstract fun transformed(t: Similarity): Geom
    /** Şeklin üzerinden örneklenmiş noktalar (x,y çiftleri). */
    abstract fun sample(step: Float = 4f): FloatArray

    class Polyline(val pts: FloatArray, val closed: Boolean) : Geom() {
        val n get() = pts.size / 2
        override fun transformed(t: Similarity) = Polyline(t.mapXY(pts), closed)
        override fun sample(step: Float): FloatArray {
            val out = ArrayList<Float>()
            val m = if (closed) n else n - 1
            for (i in 0 until m) {
                val ax = pts[2 * i]; val ay = pts[2 * i + 1]
                val j = (i + 1) % n
                val bx = pts[2 * j]; val by = pts[2 * j + 1]
                val len = Geo.dist(ax, ay, bx, by)
                val k = max(1, (len / step).toInt())
                for (s in 0 until k) {
                    val f = s.toFloat() / k
                    out += ax + (bx - ax) * f; out += ay + (by - ay) * f
                }
            }
            if (!closed && n > 0) { out += pts[2 * (n - 1)]; out += pts[2 * (n - 1) + 1] }
            if (closed && n > 0) { out += pts[0]; out += pts[1] }
            return out.toFloatArray()
        }
    }

    class Circle(val cx: Float, val cy: Float, val r: Float) : Geom() {
        override fun transformed(t: Similarity) = Circle(t.mapX(cx, cy), t.mapY(cx, cy), r * t.scale)
        override fun sample(step: Float) = Geo.ellipsePoints(cx, cy, r, r, 0f, 0f, Geo.TAU, step, true)
    }

    class Ellipse(val cx: Float, val cy: Float, val rx: Float, val ry: Float, val rot: Float) : Geom() {
        override fun transformed(t: Similarity) =
            Ellipse(t.mapX(cx, cy), t.mapY(cx, cy), rx * t.scale, ry * t.scale, rot + t.angle)
        override fun sample(step: Float) = Geo.ellipsePoints(cx, cy, rx, ry, rot, 0f, Geo.TAU, step, true)
    }

    /** Yay: [start] ve [sweep] radyan cinsinden (ekranda saat yönü pozitif, çünkü y aşağı). */
    class Arc(val cx: Float, val cy: Float, val r: Float, val start: Float, val sweep: Float) : Geom() {
        override fun transformed(t: Similarity) =
            Arc(t.mapX(cx, cy), t.mapY(cx, cy), r * t.scale, start + t.angle, sweep)
        override fun sample(step: Float) = Geo.ellipsePoints(cx, cy, r, r, 0f, start, sweep, step, false)
    }
}

class ShapeItem(
    override val id: Long,
    val geom: Geom,
    val color: Int,
    val width: Float,
    val dashed: Boolean = false,
    /** İç dolgu rengi (0 = dolgu yok). */
    val fill: Int = 0,
) : Item() {
    private val box by lazy { Box.ofPoints(geom.sample(6f)).inflate(width) }
    override fun bounds() = box
    override fun transformed(t: Similarity, newId: Long) = ShapeItem(newId, geom.transformed(t), color, width * t.scale, dashed, fill)
    fun withColor(c: Int, newId: Long = id) = ShapeItem(newId, geom, c, width, dashed, fill)
    fun withFill(f: Int, newId: Long = id) = ShapeItem(newId, geom, color, width, dashed, f)
    fun withGeom(g: Geom, newId: Long = id) = ShapeItem(newId, g, color, width, dashed, fill)
}

/** Metin kutusu. (x, y) = metnin sol üst köşesi; [size] = yazı boyutu (nokta). */
class TextItem(
    override val id: Long,
    val x: Float,
    val y: Float,
    val text: String,
    val size: Float,
    val color: Int,
    val rotation: Float = 0f,
    /** Arka plan rengi (0 = yok). Yapışkan not ve damgalar için. */
    val bg: Int = 0,
) : Item() {
    override fun bounds(): Box {
        val lines = text.split('\n')
        val w = (lines.maxOfOrNull { it.length } ?: 1) * size * 0.6f
        val h = lines.size * size * 1.25f
        val pad = if (bg != 0) padding() else 0f
        return rotatedBox(x - pad, y - pad, w + 2 * pad, h + 2 * pad, rotation)
    }
    fun padding() = size * 0.6f
    override fun transformed(t: Similarity, newId: Long) =
        TextItem(newId, t.mapX(x, y), t.mapY(x, y), text, size * t.scale, color, rotation + t.angle, bg)
    fun withColor(c: Int, newId: Long = id) = TextItem(newId, x, y, text, size, c, rotation, bg)
}

/** Görsel. [file] = defter klasöründeki dosya adı. (x,y) sol üst köşe, döndürme bu köşe etrafında. */
class ImageItem(
    override val id: Long,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val file: String,
    val rotation: Float = 0f,
    /** Formül görseli ise kaynağı (düzenlemek için); normal görselde boş. */
    val source: String = "",
) : Item() {
    override fun bounds() = rotatedBox(x, y, w, h, rotation)
    override fun transformed(t: Similarity, newId: Long) =
        ImageItem(newId, t.mapX(x, y), t.mapY(x, y), w * t.scale, h * t.scale, file, rotation + t.angle, source)
}

internal fun rotatedBox(x: Float, y: Float, w: Float, h: Float, rot: Float): Box {
    if (rot == 0f) return Box(x, y, x + w, y + h)
    val c = kotlin.math.cos(rot); val s = kotlin.math.sin(rot)
    val xs = floatArrayOf(0f, w, w, 0f); val ys = floatArrayOf(0f, 0f, h, h)
    val pts = FloatArray(8)
    for (i in 0 until 4) {
        pts[2 * i] = x + xs[i] * c - ys[i] * s
        pts[2 * i + 1] = y + xs[i] * s + ys[i] * c
    }
    return Box.ofPoints(pts)
}

/* ---------------- Sayfa ve defter ---------------- */

enum class Template(val title: String) {
    BLANK("Boş"),
    LINED("Çizgili"),
    GRID("Kareli"),
    DOTS("Noktalı"),
    ISOMETRIC("İzometrik"),
    COORDINATE("Koordinat düzlemi"),
    MILLIMETER("Milimetrik"),
    NUMBER_LINE("Sayı doğrusu"),
    UNIT_CIRCLE("Birim çember"),
    POLAR("Kutupsal"),
    CORNELL("Cornell not"),
    TWO_COLUMN("Çift sütun"),
}

/** Hazır kağıt renkleri. */
object PaperColors {
    const val WHITE = 0xFFFFFFFF.toInt()
    const val CREAM = 0xFFFBF6E9.toInt()
    const val GRAY = 0xFFEEF1F4.toInt()
    const val BOARD = 0xFF1F3B32.toInt()   // kara tahta yeşili
    const val NIGHT = 0xFF1C2230.toInt()   // gece mavisi
    val all = listOf(WHITE to "Beyaz", CREAM to "Krem", GRAY to "Gri", BOARD to "Kara tahta", NIGHT to "Gece")

    fun isDark(c: Int): Boolean {
        val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
        return (0.299 * r + 0.587 * g + 0.114 * b) < 110
    }
}

sealed class Background {
    /**
     * Kağıt sayfası.
     * [spacing] = desen aralığı (nokta), [opacity] = desen (çizgi/nokta) görünürlüğü 0..1.
     */
    data class Paper(
        val template: Template,
        val color: Int = PaperColors.WHITE,
        val spacing: Float = Templates.defaultSpacing(template),
        val opacity: Float = 1f,
        /** Kendi deseniniz: defter klasöründeki resim dosyası (boş = yok). */
        val image: String = "",
    ) : Background() {
        constructor(template: Template, dark: Boolean) : this(template, if (dark) PaperColors.BOARD else PaperColors.WHITE)
        val dark get() = PaperColors.isDark(color)
    }
    /**
     * Kaynak PDF'in [index]. sayfası (0'dan başlar).
     * [rotation] = ek döndürme (0/90/180/270, saat yönünde), [crop] = kırpma payları,
     * [fullW]/[fullH] = PDF sayfasının döndürme/kırpma öncesi boyutu (0 = sayfa boyutuyla aynı).
     */
    data class PdfPage(
        val index: Int,
        val rotation: Int = 0,
        val crop: Crop = Crop.NONE,
        val fullW: Float = 0f,
        val fullH: Float = 0f,
    ) : Background()
}

/** Kırpma payları: sayfanın (döndürme öncesi) sol/üst/sağ/alt kenarından kesilen oran (0..1). */
data class Crop(val l: Float, val t: Float, val r: Float, val b: Float) {
    val isNone get() = l == 0f && t == 0f && r == 0f && b == 0f
    companion object { val NONE = Crop(0f, 0f, 0f, 0f) }
}

class Page(
    val id: Long,
    var background: Background,
    var width: Float,
    var height: Float,
) {
    val items: MutableList<Item> = ArrayList()
    /** Sayfanın koyu (kara tahta) olup olmadığı. */
    val dark get() = (background as? Background.Paper)?.dark == true
}

data class Bookmark(val pageId: Long, val title: String)

enum class PageSize(val title: String, val w: Float, val h: Float) {
    A4_PORTRAIT("A4 dikey", 595f, 842f),
    A4_LANDSCAPE("A4 yatay", 842f, 595f),
    WIDE("16:9 (projeksiyon / video)", 960f, 540f),
}

class Notebook(
    val id: String,
    var name: String,
    val created: Long,
    var updated: Long,
    /** Defter klasöründeki kaynak PDF adı (yoksa null). */
    var pdfFile: String?,
    /** Kitaptaki sayfa no = PDF sayfa no - ofset. Ör: kapak+önsöz 8 sayfa ise ofset 8. */
    var pageOffset: Int = 0,
    var defaultPaper: Background.Paper = Background.Paper(Template.GRID),
    var defaultSize: PageSize = PageSize.A4_PORTRAIT,
    var folder: String = "",
    var favorite: Boolean = false,
    /** Defter kilidi: PIN'in özeti (boş = kilitsiz). */
    var lockHash: String = "",
) {
    val defaultTemplate get() = defaultPaper.template
    val pages: MutableList<Page> = ArrayList()
    val bookmarks: MutableList<Bookmark> = ArrayList()

    fun indexOfPage(id: Long) = pages.indexOfFirst { it.id == id }
}
