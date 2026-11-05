package com.poyankee.note.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.poyankee.note.core.Background
import com.poyankee.note.core.Geo
import com.poyankee.note.core.Geom
import com.poyankee.note.core.ImageItem
import com.poyankee.note.core.InkItem
import com.poyankee.note.core.InkTool
import com.poyankee.note.core.Item
import com.poyankee.note.core.Page
import com.poyankee.note.core.ShapeItem
import com.poyankee.note.core.StrokeOutline
import com.poyankee.note.core.Template
import com.poyankee.note.core.Templates
import com.poyankee.note.core.TextItem
import java.util.WeakHashMap
import kotlin.math.max

/** Görsel dosyalarını sağlayan arayüz (yüklenmemişse null döner ve yükleme başlatır). */
fun interface ImageSource {
    fun get(file: String): Bitmap?
}

/**
 * Sayfa arka planını (şablon) ve öğeleri bir Canvas'a çizer. Sayfa koordinatlarında çalışır.
 * Her öğenin Path'i bir kez hesaplanıp önbellekte tutulur.
 */
class PageRenderer(private val images: ImageSource?) {

    private val pathCache = WeakHashMap<Item, Path>()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.SANS_SERIF }
    private val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; textSize = 7f }

    private val templateCache = HashMap<String, Templates.Geometry>()

    companion object {
        const val PAPER = 0xFFFFFFFF.toInt()
        const val BOARD = com.poyankee.note.core.PaperColors.BOARD

        fun lineColor(kind: Templates.Kind, dark: Boolean): Int = if (!dark) when (kind) {
            Templates.Kind.MINOR -> 0xFFCFDCE8.toInt()
            Templates.Kind.MEDIUM -> 0xFFAFC3D6.toInt()
            Templates.Kind.MAJOR -> 0xFF86A3BF.toInt()
            Templates.Kind.AXIS -> 0xFF37474F.toInt()
            Templates.Kind.MARGIN -> 0xFFEF9A9A.toInt()
        } else when (kind) {
            Templates.Kind.MINOR -> 0xFF35574B.toInt()
            Templates.Kind.MEDIUM -> 0xFF4A6E61.toInt()
            Templates.Kind.MAJOR -> 0xFF628A7C.toInt()
            Templates.Kind.AXIS -> 0xFFE0E0E0.toInt()
            Templates.Kind.MARGIN -> 0xFF9C5A5A.toInt()
        }

        fun dotColor(dark: Boolean) = if (dark) 0xFF7FA597.toInt() else 0xFF8FA3B6.toInt()

        fun lineWidth(kind: Templates.Kind) = when (kind) {
            Templates.Kind.MINOR -> 0.45f
            Templates.Kind.MEDIUM -> 0.6f
            Templates.Kind.MAJOR -> 0.8f
            Templates.Kind.AXIS -> 1.1f
            Templates.Kind.MARGIN -> 0.8f
        }
    }

    /** Kağıt rengini ve şablon desenini çizer (PDF sayfası değilse). */
    fun drawPaper(c: Canvas, page: Page) {
        val bg = page.background
        if (bg !is Background.Paper) return
        c.drawColor(bg.color)
        if (bg.image.isNotEmpty()) drawPaperImage(c, bg, page.width, page.height)
        drawTemplate(c, bg.template, bg.dark, page.width, page.height, bg.spacing, bg.opacity)
    }

    /** Kendi deseniniz: resmi sayfaya sığdırarak ortalar (görünürlük ayarı resme de uygulanır). */
    private fun drawPaperImage(c: Canvas, bg: Background.Paper, w: Float, h: Float) {
        val bmp = images?.get(bg.image) ?: return
        if (bmp.isRecycled) return
        val s = minOf(w / bmp.width, h / bmp.height)
        val iw = bmp.width * s; val ih = bmp.height * s
        val l = (w - iw) / 2f; val t = (h - ih) / 2f
        bmpPaint.alpha = (255 * bg.opacity.coerceIn(0.1f, 1f)).toInt()
        c.drawBitmap(bmp, null, RectF(l, t, l + iw, t + ih), bmpPaint)
        bmpPaint.alpha = 255
    }

    fun drawTemplate(c: Canvas, t: Template, dark: Boolean, w: Float, h: Float, spacing: Float = Templates.defaultSpacing(t), opacity: Float = 1f) {
        if (t == Template.BLANK || opacity <= 0.01f) return
        val g = templateCache.getOrPut("$t/$w/$h/$spacing") { Templates.geometry(t, w, h, spacing) }
        for (l in g.lines) {
            lineP.color = mulAlpha(lineColor(l.kind, dark), opacity)
            lineP.strokeWidth = lineWidth(l.kind)
            c.drawLine(l.x1, l.y1, l.x2, l.y2, lineP)
        }
        for (ci in g.circles) {
            lineP.color = mulAlpha(lineColor(ci.kind, dark), opacity)
            lineP.strokeWidth = lineWidth(ci.kind)
            c.drawCircle(ci.cx, ci.cy, ci.r, lineP)
        }
        if (g.dots.isNotEmpty()) {
            fill.color = mulAlpha(dotColor(dark), opacity)
            var i = 0
            while (i + 1 < g.dots.size) { c.drawCircle(g.dots[i], g.dots[i + 1], g.dotRadius, fill); i += 2 }
        }
        if (g.labels.isNotEmpty()) {
            labelP.color = mulAlpha(if (dark) 0xFFE0E0E0.toInt() else 0xFF455A64.toInt(), max(opacity, 0.5f))
            for (lb in g.labels) c.drawText(lb.text, lb.x, lb.y, labelP)
        }
    }

    /** Öğeleri çizer. Fosforlu kalem her zaman yazının altında kalır. [skip] = çizilmeyecek öğe kimlikleri. */
    fun drawItems(c: Canvas, items: List<Item>, skip: Set<Long> = emptySet()) {
        for (it in items) if (it is InkItem && it.tool == InkTool.HIGHLIGHTER && it.id !in skip) drawItem(c, it)
        for (it in items) if (!(it is InkItem && it.tool == InkTool.HIGHLIGHTER) && it.id !in skip) drawItem(c, it)
    }

    fun drawItem(c: Canvas, item: Item, alphaMul: Float = 1f) {
        when (item) {
            is InkItem -> {
                val p = pathCache.getOrPut(item) { inkPath(item.pts, item.width, item.pressure) }
                fill.color = mulAlpha(item.color, alphaMul)
                c.drawPath(p, fill)
            }
            is ShapeItem -> {
                val p = pathCache.getOrPut(item) { geomPath(item.geom) }
                if (item.fill != 0) {
                    fill.color = mulAlpha(item.fill, alphaMul)
                    c.drawPath(p, fill)
                }
                stroke.color = mulAlpha(item.color, alphaMul)
                stroke.strokeWidth = item.width
                stroke.pathEffect = if (item.dashed) DashPathEffect(floatArrayOf(item.width * 3f + 3f, item.width * 2f + 3f), 0f) else null
                c.drawPath(p, stroke)
                stroke.pathEffect = null
            }
            is TextItem -> drawText(c, item, alphaMul)
            is ImageItem -> {
                val bmp = images?.get(item.file)
                c.save()
                c.rotate(Math.toDegrees(item.rotation.toDouble()).toFloat(), item.x, item.y)
                val r = RectF(item.x, item.y, item.x + item.w, item.y + item.h)
                if (bmp != null && !bmp.isRecycled) {
                    bmpPaint.alpha = (255 * alphaMul).toInt()
                    c.drawBitmap(bmp, null, r, bmpPaint)
                } else {
                    fill.color = 0x22000000
                    c.drawRect(r, fill)
                }
                c.restore()
            }
        }
    }

    fun drawText(c: Canvas, t: TextItem, alphaMul: Float = 1f) {
        text.color = mulAlpha(t.color, alphaMul)
        text.textSize = t.size
        c.save()
        c.rotate(Math.toDegrees(t.rotation.toDouble()).toFloat(), t.x, t.y)
        if (t.bg != 0) {
            // yapışkan not / damga zemini
            val lines = t.text.split('\n')
            val w = lines.maxOf { text.measureText(it) }
            val h = lines.size * t.size * 1.25f
            val pad = t.padding()
            val r = RectF(t.x - pad, t.y - pad, t.x + w + pad, t.y + h + pad * 0.6f)
            fill.color = mulAlpha(0x22000000, alphaMul)
            c.drawRoundRect(RectF(r.left + 1.2f, r.top + 1.8f, r.right + 1.2f, r.bottom + 1.8f), pad * 0.5f, pad * 0.5f, fill)
            fill.color = mulAlpha(t.bg, alphaMul)
            c.drawRoundRect(r, pad * 0.5f, pad * 0.5f, fill)
        }
        t.text.split('\n').forEachIndexed { i, line -> c.drawText(line, t.x, t.y + t.size * (0.95f + 1.25f * i), text) }
        c.restore()
    }

    fun mulAlpha(color: Int, m: Float): Int {
        if (m >= 1f) return color
        val a = ((color ushr 24) * m).toInt().coerceIn(0, 255)
        return (color and 0x00FFFFFF) or (a shl 24)
    }

    fun inkPath(pts: FloatArray, width: Float, pressure: Boolean): Path = polygonPath(StrokeOutline.outline(pts, width, pressure))

    fun geomPath(g: Geom): Path {
        val p = Path()
        when (g) {
            is Geom.Polyline -> {
                if (g.n == 0) return p
                p.moveTo(g.pts[0], g.pts[1])
                for (i in 1 until g.n) p.lineTo(g.pts[2 * i], g.pts[2 * i + 1])
                if (g.closed) p.close()
            }
            is Geom.Circle -> p.addCircle(g.cx, g.cy, g.r, Path.Direction.CW)
            is Geom.Ellipse -> {
                p.addOval(RectF(g.cx - g.rx, g.cy - g.ry, g.cx + g.rx, g.cy + g.ry), Path.Direction.CW)
                val m = Matrix(); m.setRotate(Geo.toDeg(g.rot), g.cx, g.cy); p.transform(m)
            }
            is Geom.Arc -> p.addArc(RectF(g.cx - g.r, g.cy - g.r, g.cx + g.r, g.cy + g.r), Geo.toDeg(g.start), Geo.toDeg(g.sweep))
        }
        return p
    }

    fun polygonPath(xy: FloatArray): Path {
        val p = Path()
        if (xy.size < 4) return p
        p.moveTo(xy[0], xy[1])
        var i = 2
        while (i + 1 < xy.size) { p.lineTo(xy[i], xy[i + 1]); i += 2 }
        p.close()
        return p
    }

    /** Sayfanın tamamını (arka plan + öğeler) bir bitmap'e çizer (küçük resim / dışa aktarma için). PDF zemini ayrıca verilir. */
    fun renderPage(page: Page, scale: Float, pdfBitmap: Bitmap?): Bitmap {
        val w = (page.width * scale).toInt().coerceAtLeast(1)
        val h = (page.height * scale).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(PAPER)
        if (pdfBitmap != null) c.drawBitmap(pdfBitmap, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), bmpPaint)
        c.scale(scale, scale)
        drawPaper(c, page)
        drawItems(c, page.items)
        return bmp
    }
}
