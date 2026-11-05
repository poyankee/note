package com.poyankee.note.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.poyankee.note.core.Affine
import com.poyankee.note.core.Background
import com.poyankee.note.core.Geo
import com.poyankee.note.core.Geom
import com.poyankee.note.core.ImageItem
import com.poyankee.note.core.InkItem
import com.poyankee.note.core.InkTool
import com.poyankee.note.core.Item
import com.poyankee.note.core.Notebook
import com.poyankee.note.core.Page
import com.poyankee.note.core.ShapeItem
import com.poyankee.note.core.StrokeOutline
import com.poyankee.note.core.Templates
import com.poyankee.note.core.TextItem
import com.poyankee.note.render.PageRenderer
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.cos.COSArray
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.util.Matrix
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Defteri PDF'e aktarır. Yazılar ve şekiller vektör olarak (her yakınlaştırmada net) orijinal PDF sayfalarının üzerine eklenir.
 * Metin ve görseller resim olarak gömülür (Türkçe karakter sorunu olmasın diye).
 */
object Exporter {

    fun init(context: Context) {
        try { PDFBoxResourceLoader.init(context.applicationContext) } catch (_: Throwable) {}
    }

    /** PDF'in içindekiler listesi: (başlık, PDF sayfa indeksi, seviye). */
    fun outline(context: Context, pdf: File): List<Triple<String, Int, Int>> {
        init(context)
        val out = ArrayList<Triple<String, Int, Int>>()
        PDDocument.load(pdf).use { doc ->
            val root = doc.documentCatalog.documentOutline ?: return out
            fun walk(item: PDOutlineItem?, level: Int) {
                var cur = item
                var guard = 0
                while (cur != null && guard++ < 5000) {
                    val page = try { cur.findDestinationPage(doc) } catch (_: Exception) { null }
                    val idx = if (page != null) doc.pages.indexOf(page) else -1
                    if (idx >= 0) out += Triple(cur.title ?: "", idx, level)
                    if (level < 4) walk(cur.firstChild, level + 1)
                    cur = cur.nextSibling
                }
            }
            walk(root.firstChild, 0)
        }
        return out
    }

    /**
     * [pages] sayfalarını [dest] dosyasına yazar. Arka planda çağrılmalı.
     * [progress] (bitti, toplam) ile ilerleme bildirir.
     */
    fun export(
        context: Context,
        nb: Notebook,
        dir: File,
        pages: List<Page>,
        dest: File,
        loadImage: (String) -> Bitmap?,
        progress: (Int, Int) -> Unit,
    ) {
        init(context)
        val srcFile = nb.pdfFile?.let { File(dir, it) }
        val src = if (srcFile != null && srcFile.exists() && pages.any { it.background is Background.PdfPage }) PDDocument.load(srcFile) else null
        try {
            PDDocument().use { out ->
                pages.forEachIndexed { n, page ->
                    val bg = page.background
                    val pd: PDPage
                    if (bg is Background.PdfPage && src != null && bg.index < src.numberOfPages) {
                        // Sayfa sözlüğünün kopyası: aynı PDF sayfası iki kez kullanılsa da (çoğaltılmış sayfa) yazılar karışmaz.
                        val sp = src.getPage(bg.index)
                        val d = COSDictionary(sp.cosObject)
                        val contents = d.getDictionaryObject(COSName.CONTENTS)
                        if (contents is COSArray) {
                            val copy = COSArray()
                            for (k in 0 until contents.size()) copy.add(contents.get(k))
                            d.setItem(COSName.CONTENTS, copy)
                        }
                        val np = PDPage(d)
                        // kalıtılan özellikleri (kaynaklar, boyut, döndürme) açıkça yaz
                        np.resources = sp.resources
                        np.mediaBox = sp.mediaBox
                        np.cropBox = sp.cropBox
                        np.rotation = sp.rotation
                        // kırpma: görünen kırpma dikdörtgenini PDF kullanıcı koordinatına çevirip kutuyu daralt
                        if (!bg.crop.isNone) {
                            val fw = if (bg.fullW > 0f) bg.fullW else page.width
                            val fh = if (bg.fullH > 0f) bg.fullH else page.height
                            val m = displayToUser(sp.cropBox, sp.rotation, fw)
                            val x0 = bg.crop.l * fw; val y0 = bg.crop.t * fh
                            val x1 = fw - bg.crop.r * fw; val y1 = fh - bg.crop.b * fh
                            val ux0 = m.mapX(x0, y0); val uy0 = m.mapY(x0, y0)
                            val ux1 = m.mapX(x1, y1); val uy1 = m.mapY(x1, y1)
                            val rect = PDRectangle(minOf(ux0, ux1), minOf(uy0, uy1), abs(ux1 - ux0), abs(uy1 - uy0))
                            np.cropBox = rect
                            np.mediaBox = rect
                        }
                        np.rotation = (sp.rotation + bg.rotation) % 360
                        pd = out.importPage(np)
                    } else {
                        pd = PDPage(PDRectangle(page.width, page.height))
                        out.addPage(pd)
                    }
                    paperImage = null; paperImageX = null
                    if (bg is Background.Paper && bg.image.isNotEmpty()) {
                        paperImage = loadImage(bg.image)
                        paperImageX = paperImage?.let { LosslessFactory.createFromImage(out, it) }
                    }
                    PDPageContentStream(out, pd, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                        cs.saveGraphicsState()
                        cs.transform(pageMatrix(pd, page))
                        if (bg is Background.Paper) drawPaper(cs, page, bg)
                        val items = page.items
                        for (it in items) if (it is InkItem && it.tool == InkTool.HIGHLIGHTER) drawItem(out, cs, it, loadImage)
                        for (it in items) if (!(it is InkItem && it.tool == InkTool.HIGHLIGHTER)) drawItem(out, cs, it, loadImage)
                        cs.restoreGraphicsState()
                    }
                    progress(n + 1, pages.size)
                }
                out.save(dest)
            }
        } finally {
            src?.close()
        }
    }

    /** Sayfa koordinatlarını (y aşağı) PDF koordinatlarına (y yukarı, döndürme dahil) çeviren matris. */
    private fun pageMatrix(pd: PDPage, page: Page): Matrix {
        val a = displayToUser(pd.cropBox, pd.rotation, page.width)
        return Matrix(a.a, a.b, a.c, a.d, a.e, a.f)
    }

    /** Görünen sayfa (genişliği [displayWidth]) → PDF kullanıcı koordinatı. */
    private fun displayToUser(box: PDRectangle, rotation: Int, displayWidth: Float): Affine {
        val llx = box.lowerLeftX; val lly = box.lowerLeftY
        val cw = box.width; val ch = box.height
        val rot = ((rotation % 360) + 360) % 360
        val dispW = if (rot == 90 || rot == 270) ch else cw
        val k = if (displayWidth > 0f) dispW / displayWidth else 1f
        return when (rot) {
            90 -> Affine(0f, k, k, 0f, llx, lly)
            180 -> Affine(-k, 0f, 0f, k, llx + cw, lly)
            270 -> Affine(0f, -k, -k, 0f, llx + cw, lly + ch)
            else -> Affine(k, 0f, 0f, -k, llx, lly + ch)
        }
    }

    private fun rgb(c: Int) = floatArrayOf(((c shr 16) and 0xFF) / 255f, ((c shr 8) and 0xFF) / 255f, (c and 0xFF) / 255f)
    private fun alpha(c: Int) = ((c ushr 24) and 0xFF) / 255f

    private fun setAlpha(cs: PDPageContentStream, a: Float) {
        val gs = PDExtendedGraphicsState()
        gs.nonStrokingAlphaConstant = a
        gs.strokingAlphaConstant = a
        cs.setGraphicsStateParameters(gs)
    }

    private fun drawPaper(cs: PDPageContentStream, page: Page, bg: Background.Paper) {
        if (bg.color != PageRenderer.PAPER) {
            val c = rgb(bg.color)
            cs.setNonStrokingColor(c[0], c[1], c[2])
            cs.addRect(0f, 0f, page.width, page.height); cs.fill()
        }
        if (bg.image.isNotEmpty()) paperImage?.let { b ->
            val s = minOf(page.width / b.width, page.height / b.height)
            val iw = b.width * s; val ih = b.height * s
            cs.saveGraphicsState()
            if (bg.opacity < 0.99f) setAlpha(cs, bg.opacity.coerceAtLeast(0.1f))
            paperImageX?.let { drawImage(cs, it, (page.width - iw) / 2f, (page.height - ih) / 2f, iw, ih, 0f) }
            cs.restoreGraphicsState()
        }
        if (bg.opacity <= 0.01f) return
        val g = Templates.geometry(bg.template, page.width, page.height, bg.spacing)
        cs.saveGraphicsState()
        if (bg.opacity < 0.99f) setAlpha(cs, bg.opacity)
        for (ci in g.circles) {
            val c = rgb(PageRenderer.lineColor(ci.kind, bg.dark))
            cs.setStrokingColor(c[0], c[1], c[2])
            cs.setLineWidth(PageRenderer.lineWidth(ci.kind))
            polygon(cs, Geo.ellipsePoints(ci.cx, ci.cy, ci.r, ci.r, 0f, 0f, Geo.TAU, 2f, true)); cs.stroke()
        }
        if (g.labels.isNotEmpty()) {
            val c = rgb(if (bg.dark) 0xFFE0E0E0.toInt() else 0xFF455A64.toInt())
            cs.setNonStrokingColor(c[0], c[1], c[2])
            val font = PDType1Font.HELVETICA
            for (lb in g.labels) {
                try {
                    val w = font.getStringWidth(lb.text) / 1000f * 7f
                    cs.beginText()
                    cs.setFont(font, 7f)
                    cs.setTextMatrix(Matrix(1f, 0f, 0f, -1f, lb.x - w / 2f, lb.y))
                    cs.showText(lb.text)
                    cs.endText()
                } catch (_: Exception) {}
            }
        }
        for (l in g.lines) {
            val c = rgb(PageRenderer.lineColor(l.kind, bg.dark))
            cs.setStrokingColor(c[0], c[1], c[2])
            cs.setLineWidth(PageRenderer.lineWidth(l.kind))
            cs.moveTo(l.x1, l.y1); cs.lineTo(l.x2, l.y2); cs.stroke()
        }
        if (g.dots.isNotEmpty()) {
            val c = rgb(PageRenderer.dotColor(bg.dark))
            cs.setNonStrokingColor(c[0], c[1], c[2])
            var i = 0
            while (i + 1 < g.dots.size) {
                val pts = Geo.ellipsePoints(g.dots[i], g.dots[i + 1], g.dotRadius, g.dotRadius, 0f, 0f, Geo.TAU, 0.5f, true)
                polygon(cs, pts); cs.fill()
                i += 2
            }
        }
        cs.restoreGraphicsState()
    }

    private fun polygon(cs: PDPageContentStream, xy: FloatArray) {
        if (xy.size < 4) return
        cs.moveTo(xy[0], xy[1])
        var i = 2
        while (i + 1 < xy.size) { cs.lineTo(xy[i], xy[i + 1]); i += 2 }
        cs.closePath()
    }

    private fun drawItem(doc: PDDocument, cs: PDPageContentStream, item: Item, loadImage: (String) -> Bitmap?) {
        when (item) {
            is InkItem -> {
                cs.saveGraphicsState()
                val c = rgb(item.color)
                cs.setNonStrokingColor(c[0], c[1], c[2])
                val a = alpha(item.color)
                if (a < 0.99f) setAlpha(cs, a)
                polygon(cs, StrokeOutline.outline(item.pts, item.width, item.pressure))
                cs.fill()
                cs.restoreGraphicsState()
            }
            is ShapeItem -> {
                cs.saveGraphicsState()
                val c = rgb(item.color)
                cs.setStrokingColor(c[0], c[1], c[2])
                val a = alpha(item.color)
                if (a < 0.99f) setAlpha(cs, a)
                cs.setLineWidth(item.width)
                cs.setLineCapStyle(1); cs.setLineJoinStyle(1)
                if (item.dashed) cs.setLineDashPattern(floatArrayOf(item.width * 3f + 3f, item.width * 2f + 3f), 0f)
                val g = item.geom
                if (item.fill != 0) {
                    cs.saveGraphicsState()
                    val fc = rgb(item.fill)
                    cs.setNonStrokingColor(fc[0], fc[1], fc[2])
                    setAlpha(cs, alpha(item.fill))
                    polygon(cs, if (g is Geom.Polyline) g.pts else g.sample(1.5f))
                    cs.fill()
                    cs.restoreGraphicsState()
                }
                if (g is Geom.Polyline) {
                    if (g.n >= 2) {
                        cs.moveTo(g.pts[0], g.pts[1])
                        for (i in 1 until g.n) cs.lineTo(g.pts[2 * i], g.pts[2 * i + 1])
                        if (g.closed) cs.closePath()
                    }
                } else {
                    val s = g.sample(1.5f)
                    if (s.size >= 4) {
                        cs.moveTo(s[0], s[1])
                        var i = 2
                        while (i + 1 < s.size) { cs.lineTo(s[i], s[i + 1]); i += 2 }
                        if (g is Geom.Circle || g is Geom.Ellipse) cs.closePath()
                    }
                }
                cs.stroke()
                cs.restoreGraphicsState()
            }
            is TextItem -> {
                val b = textBitmap(item) ?: return
                val img = LosslessFactory.createFromImage(doc, b)
                val w = b.width / TEXT_SCALE; val h = b.height / TEXT_SCALE
                val pad = if (item.bg != 0) item.padding() else 0f
                val c0 = cos(item.rotation); val s0 = sin(item.rotation)
                // zeminli metinde resim, dolgu payı kadar sol üstten başlar (döndürme metnin köşesi etrafında)
                drawImage(cs, img, item.x - pad * c0 + pad * s0, item.y - pad * s0 - pad * c0, w, h, item.rotation)
                b.recycle()
            }
            is ImageItem -> {
                val b = loadImage(item.file) ?: return
                val img: PDImageXObject = JPEGFactory.createFromImage(doc, b, 0.9f)
                drawImage(cs, img, item.x, item.y, item.w, item.h, item.rotation)
            }
        }
    }

    private const val TEXT_SCALE = 4f
    private var paperImage: Bitmap? = null
    private var paperImageX: PDImageXObject? = null

    private fun textBitmap(t: TextItem): Bitmap? {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = t.size * TEXT_SCALE; color = t.color }
        val lines = t.text.split('\n')
        val pad = if (t.bg != 0) t.padding() * TEXT_SCALE else 0f
        val tw = lines.maxOf { p.measureText(it) }
        val w = (tw + 4 + 2 * pad).toInt().coerceAtLeast(1)
        val h = (t.size * TEXT_SCALE * (1.25f * lines.size) + pad * 1.6f).toInt().coerceAtLeast(1)
        if (w * h > 20_000_000) return null
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        if (t.bg != 0) {
            val bgp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = t.bg }
            c.drawRoundRect(android.graphics.RectF(0f, 0f, w.toFloat(), h.toFloat()), pad * 0.5f, pad * 0.5f, bgp)
        }
        lines.forEachIndexed { i, s -> c.drawText(s, pad, pad + t.size * TEXT_SCALE * (0.95f + 1.25f * i), p) }
        return b
    }

    /** Görseli (x,y) sol üst köşesinden, [rot] kadar dönmüş olarak çizer (sayfa koordinatı y aşağı). */
    private fun drawImage(cs: PDPageContentStream, img: PDImageXObject, x: Float, y: Float, w: Float, h: Float, rot: Float) {
        val c = cos(rot); val s = sin(rot)
        cs.drawImage(img, Matrix(c * w, s * w, s * h, -c * h, x - s * h, y + c * h))
    }
}
