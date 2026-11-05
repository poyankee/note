package com.poyankee.note.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.util.Matrix
import java.io.File
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
                        pd = out.importPage(np)
                    } else {
                        pd = PDPage(PDRectangle(page.width, page.height))
                        out.addPage(pd)
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
        val box = pd.cropBox
        val llx = box.lowerLeftX; val lly = box.lowerLeftY
        val cw = box.width; val ch = box.height
        val rot = ((pd.rotation % 360) + 360) % 360
        val dispW = if (rot == 90 || rot == 270) ch else cw
        val k = if (page.width > 0f) dispW / page.width else 1f
        return when (rot) {
            90 -> Matrix(0f, k, k, 0f, llx, lly)
            180 -> Matrix(-k, 0f, 0f, k, llx + cw, lly)
            270 -> Matrix(0f, -k, -k, 0f, llx + cw, lly + ch)
            else -> Matrix(k, 0f, 0f, -k, llx, lly + ch)
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
        if (bg.opacity <= 0.01f) return
        val g = Templates.geometry(bg.template, page.width, page.height, bg.spacing)
        cs.saveGraphicsState()
        if (bg.opacity < 0.99f) setAlpha(cs, bg.opacity)
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
                drawImage(cs, img, item.x, item.y, w, h, item.rotation)
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

    private fun textBitmap(t: TextItem): Bitmap? {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = t.size * TEXT_SCALE; color = t.color }
        val lines = t.text.split('\n')
        val w = (lines.maxOf { p.measureText(it) } + 4).toInt().coerceAtLeast(1)
        val h = (t.size * TEXT_SCALE * (1.25f * lines.size)).toInt().coerceAtLeast(1)
        if (w * h > 20_000_000) return null
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        lines.forEachIndexed { i, s -> c.drawText(s, 0f, t.size * TEXT_SCALE * (0.95f + 1.25f * i), p) }
        return b
    }

    /** Görseli (x,y) sol üst köşesinden, [rot] kadar dönmüş olarak çizer (sayfa koordinatı y aşağı). */
    private fun drawImage(cs: PDPageContentStream, img: PDImageXObject, x: Float, y: Float, w: Float, h: Float, rot: Float) {
        val c = cos(rot); val s = sin(rot)
        cs.drawImage(img, Matrix(c * w, s * w, s * h, -c * h, x - s * h, y + c * h))
    }
}
