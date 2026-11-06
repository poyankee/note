package com.poyankee.note.editor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.RenderNode
import android.os.Build
import android.os.SystemClock
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import com.poyankee.note.core.Background
import com.poyankee.note.core.Box
import com.poyankee.note.core.Geo
import com.poyankee.note.core.Geom
import com.poyankee.note.core.HitTest
import com.poyankee.note.core.Ids
import com.poyankee.note.core.ImageItem
import com.poyankee.note.core.InkItem
import com.poyankee.note.core.InkTool
import com.poyankee.note.core.Item
import com.poyankee.note.core.Marks
import com.poyankee.note.core.Page
import com.poyankee.note.core.ShapeItem
import com.poyankee.note.core.ShapeRecognizer
import com.poyankee.note.core.Shapes
import com.poyankee.note.core.Similarity
import com.poyankee.note.core.Snapper
import com.poyankee.note.core.StrokeOutline
import com.poyankee.note.core.Templates
import com.poyankee.note.core.TextItem
import com.poyankee.note.data.ButtonAction
import com.poyankee.note.render.PageRenderer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Sayfaların çizildiği ve kalem/parmak girişinin işlendiği görünüm.
 *
 * Koordinatlar:
 *  - Belge: sayfalar alt alta dizilir (birim = PDF noktası).
 *  - Sayfa: her sayfanın sol üst köşesi (0,0).
 *  - Ekran: ekran = (belge - kaydırma) * yakınlaştırma.
 *
 * Kalem / el ayrımı: Android her dokunuşun türünü bildirir (kalem, silgi ucu, parmak).
 * "Sadece kalem" modunda parmaklar asla çizmez; yalnızca kaydırır / yakınlaştırır.
 * Kalem ekrana değerken ya da ekranın üzerinde dururken (hover) parmak/avuç dokunuşları tamamen yok sayılır.
 */
@SuppressLint("ViewConstructor")
class InkView(context: Context, private val ctl: EditorController) : View(context) {

    private val nb = ctl.nb
    private val settings = ctl.settings
    private val renderer = PageRenderer(ctl.images)
    private val outRenderer = PageRenderer(ctl.images)
    private val density = resources.displayMetrics.density

    /* ---------------- yerleşim ---------------- */

    private val gap = 18f
    private val margin = 16f
    private var tops = FloatArray(0)
    private var docW = 0f
    private var docH = 0f
    var zoom = 1f; private set
    var sx = 0f; private set
    var sy = 0f; private set
    private var fitted = false

    fun relayoutPages() {
        val n = nb.pages.size
        tops = FloatArray(n)
        var y = 0f
        docW = 0f
        for (i in 0 until n) {
            tops[i] = y
            y += nb.pages[i].height + gap
            docW = max(docW, nb.pages[i].width)
        }
        docH = y - gap
        // silinmiş sayfaların görüntü kayıtlarını bırak
        val ids = nb.pages.map { it.id }.toHashSet()
        nodes.keys.filter { it !in ids }.forEach { nodes.remove(it)?.discardDisplayList() }
        clampScroll()
        invalidate()
        updateCurrentPage()
    }

    private fun pageLeft(i: Int) = (docW - nb.pages[i].width) / 2f
    private fun fitZoom() = if (docW <= 0f || width == 0) 1f else width / (docW + 2 * margin)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!fitted || oldw == 0) {
            zoom = fitZoom(); sx = -margin; sy = -margin; fitted = true
            goToPage(ctl.currentPage)
        } else if (oldw > 0 && oldw != w) {
            // döndürme: aynı yerde kal, genişliğe göre ölçekle
            val f = w.toFloat() / oldw
            val cy = sy + oldh / 2f / zoom
            zoom *= f
            sy = cy - h / 2f / zoom
        }
        clampScroll(); invalidate(); scheduleSharp()
    }

    private fun clampScroll() {
        if (width == 0) return
        zoom = zoom.coerceIn(fitZoom() * 0.35f, fitZoom() * 12f)
        val vw = width / zoom; val vh = height / zoom
        sx = if (docW + 2 * margin <= vw) -(vw - docW) / 2f else sx.coerceIn(-margin, docW + margin - vw)
        sy = if (docH + 2 * margin <= vh) -margin else sy.coerceIn(-margin, docH + margin - vh)
    }

    fun pageIndexAt(docY: Float): Int {
        if (tops.isEmpty()) return 0
        var lo = 0; var hi = tops.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (tops[mid] - gap / 2 <= docY) lo = mid else hi = mid - 1
        }
        return lo
    }

    private fun toDocX(px: Float) = px / zoom + sx
    private fun toDocY(py: Float) = py / zoom + sy

    fun goToPage(i: Int) {
        if (i !in tops.indices) return
        sy = tops[i] - 8f / zoom
        scroller.forceFinished(true)
        clampScroll(); invalidate(); scheduleSharp()
        if (ctl.currentPage != i) ctl.currentPage = i
    }

    /** Kaydırma çubuğu için: belgenin neresindeyiz (0..1) ve ekranda görünen oran. */
    fun scrollFraction(): Float {
        if (height == 0) return 0f
        val range = docH + 2 * margin - height / zoom
        return if (range <= 0f) 0f else ((sy + margin) / range).coerceIn(0f, 1f)
    }
    fun visibleFraction(): Float = if (height == 0) 1f else ((height / zoom) / (docH + 2 * margin)).coerceIn(0.04f, 1f)
    fun scrollToFraction(f: Float) {
        if (height == 0) return
        val range = docH + 2 * margin - height / zoom
        if (range <= 0f) return
        sy = -margin + f.coerceIn(0f, 1f) * range
        scroller.forceFinished(true)
        clampScroll(); invalidate(); updateCurrentPage(); scheduleSharp()
    }

    /** Sayfanın ekrandaki yeri (piksel). */
    fun pageScreenRect(i: Int): RectF {
        val p = nb.pages[i]
        val l = (pageLeft(i) - sx) * zoom; val t = (tops[i] - sy) * zoom
        return RectF(l, t, l + p.width * zoom, t + p.height * zoom)
    }

    /** Sayfa genişliğine sığdır (ekranın ortasındaki yer korunur). */
    fun fitToWidth() {
        if (width == 0) return
        val cy = toDocY(height / 2f)
        zoom = fitZoom()
        sx = -margin
        sy = cy - height / 2f / zoom
        clampScroll(); invalidate(); scheduleSharp(); updateCurrentPage()
    }

    /** Ekranın ortasındaki sayfa ve o sayfadaki nokta. */
    fun visibleCenter(): Triple<Int, Float, Float> {
        val dx = toDocX(width / 2f); val dy = toDocY(height / 2f)
        val i = pageIndexAt(dy)
        val p = nb.pages[i]
        return Triple(i, (dx - pageLeft(i)).coerceIn(0f, p.width), (dy - tops[i]).coerceIn(0f, p.height))
    }

    private fun updateCurrentPage() {
        if (tops.isEmpty() || height == 0) return
        val i = pageIndexAt(toDocY(height * 0.4f))
        if (ctl.currentPage != i) ctl.currentPage = i
    }

    /* ---------------- çizim ---------------- */

    private val nodes = HashMap<Long, RenderNode>()
    private val dirty = HashSet<Long>()

    fun invalidatePage(id: Long) { dirty += id; invalidate() }
    fun invalidateAllPages() { dirty.addAll(nodes.keys); invalidate() }
    fun onPdfUpdated() { invalidate(); ctl.frameListeners.forEach { it() } }

    var deskColor = 0xFFE9EBEF.toInt()
    private val shadow = Paint().apply { color = 0x22000000 }
    private val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val overlayStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val overlayFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val overlayText = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    override fun onDraw(canvas: Canvas) {
        drawScene(canvas, zoom, sx, sy, width.toFloat(), height.toFloat(), forOutput = false)
        ctl.frameListeners.forEach { it() }
        if (laser.isNotEmpty() || temps.isNotEmpty()) postInvalidateOnAnimation()
        if (ctl.timerEnd > 0L) postInvalidateDelayed(250)
    }

    /**
     * Sahneyi verilen dönüşümle çizer. Ekran, ikinci ekran ve video kaydı aynı fonksiyonu kullanır.
     * [forOutput] = true iken seçim tutamaçları ve imleçler çizilmez.
     */
    fun drawScene(c: Canvas, z: Float, ox: Float, oy: Float, vw: Float, vh: Float, forOutput: Boolean) {
        // Önemli: yalnızca kendi alanımıza çiz (Compose içinde kırpma yapılmazsa üst çubuk ve menüler üzerine boyanır)
        c.save()
        c.clipRect(0f, 0f, vw, vh)
        fillPaint.color = deskColor
        c.drawRect(0f, 0f, vw, vh, fillPaint)
        try { drawSceneInner(c, z, ox, oy, vw, vh, forOutput) } finally { c.restore() }
    }

    private fun drawSceneInner(c: Canvas, z: Float, ox: Float, oy: Float, vw: Float, vh: Float, forOutput: Boolean) {
        if (tops.isEmpty()) return
        val top = oy; val bottom = oy + vh / z
        val first = pageIndexAt(top)
        val now = SystemClock.uptimeMillis()
        var i = first
        val useNodes = !forOutput && c.isHardwareAccelerated
        while (i < tops.size && tops[i] <= bottom) {
            val page = nb.pages[i]
            val px = pageLeft(i); val py = tops[i]
            c.save()
            c.translate((px - ox) * z, (py - oy) * z)
            c.scale(z, z)
            c.drawRect(-0.4f / z * 2f, -0.2f, page.width + 0.8f / z * 2f, page.height + 1.6f / z * 2f, shadow)
            // PDF zemini
            val bg = page.background
            if (bg is Background.PdfPage) {
                fillPaint.color = 0xFFFFFFFF.toInt()
                c.drawRect(0f, 0f, page.width, page.height, fillPaint)
                ctl.pdf?.base(bg)?.let { c.drawBitmap(it, null, RectF(0f, 0f, page.width, page.height), bmpPaint) }
                ctl.pdf?.sharp(bg)?.let { s -> if (!s.bitmap.isRecycled) c.drawBitmap(s.bitmap, null, s.rect, bmpPaint) }
            }
            // içerik
            val skip = if (sel != null && sel!!.page == i) sel!!.ids else emptySet()
            if (useNodes) {
                var node = nodes[page.id]
                if (node == null || page.id in dirty || !node.hasDisplayList()) {
                    if (node == null) { node = RenderNode("page"); nodes[page.id] = node }
                    node.setPosition(0, 0, ceil(page.width).toInt(), ceil(page.height).toInt())
                    val rc = node.beginRecording()
                    try {
                        renderer.drawPaper(rc, page)
                        renderer.drawItems(rc, page.items, skip)
                    } finally { node.endRecording() }
                    dirty -= page.id
                }
                c.drawRenderNode(node)
            } else {
                val r = if (forOutput) outRenderer else renderer
                c.save()
                c.clipRect(0f, 0f, page.width, page.height)
                r.drawPaper(c, page)
                r.drawItems(c, page.items, skip)
                c.restore()
            }
            drawPageOverlays(c, i, z, now, forOutput)
            c.restore()
            i++
        }
        // belge düzeyi kaplamalar: geometri araçları, lazer
        c.save()
        c.scale(z, z)
        c.translate(-ox, -oy)
        for (ins in instruments) ins.draw(c)
        drawLaser(c, z, now)
        if (!forOutput) drawCursor(c, z)
        drawProtractorReading(c, z)
        c.restore()
        drawLessonOverlays(c, z, ox, oy, vw, vh)
        if (useNodes) trimNodes(first, i)
    }

    /* ---------------- perde, spot ışığı, sayaç ---------------- */

    /** Perdenin üst kenarı (belge koordinatı); NaN = henüz yerleştirilmedi. */
    private var curtainY = Float.NaN
    private var curtainDragId = -1
    private var spotPos: Pair<Float, Float>? = null
    private val timerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }

    private fun drawLessonOverlays(c: Canvas, z: Float, ox: Float, oy: Float, vw: Float, vh: Float) {
        // spot ışığı: her yer kararır, kalemin olduğu daire aydınlık kalır
        val sp = spotPos
        if (ctl.tool == Tool.SPOT && sp != null) {
            val r = settings.spotRadiusDp.value * density * (vw / max(width.toFloat(), 1f))
            val p = Path()
            p.fillType = Path.FillType.EVEN_ODD
            p.addRect(0f, 0f, vw, vh, Path.Direction.CW)
            p.addCircle((sp.first - ox) * z, (sp.second - oy) * z, r, Path.Direction.CW)
            fillPaint.color = 0xC8000000.toInt()
            c.drawPath(p, fillPaint)
        }
        // perde: çizginin altını kapatır
        if (ctl.curtain) {
            if (curtainY.isNaN()) curtainY = sy + height / zoom * 0.4f
            val y = ((curtainY - oy) * z).coerceIn(0f, vh)
            fillPaint.color = 0xFF2A2F37.toInt()
            c.drawRect(0f, y, vw, vh, fillPaint)
            fillPaint.color = 0xFF4F46E5.toInt()
            c.drawRect(0f, y, vw, y + 4f * density, fillPaint)
            val hw = 46f * density * (vw / max(width.toFloat(), 1f))
            c.drawRoundRect(RectF(vw / 2 - hw, y - 2f * density, vw / 2 + hw, y + 14f * density), 8f * density, 8f * density, fillPaint)
            timerPaint.color = 0xFFFFFFFF.toInt(); timerPaint.textSize = 11f * density
            c.drawText("perdeyi çekin", vw / 2, y + 9.5f * density, timerPaint)
        }
        // sayaç (sağ üst)
        if (ctl.timerEnd > 0L) {
            val left = ctl.timerLeftMs()
            val sec = ((left + 999) / 1000).toInt()
            val text = if (left <= 0L) "Süre doldu" else "%02d:%02d".format(sec / 60, sec % 60)
            val scale = vw / max(width.toFloat(), 1f)
            timerPaint.textSize = 28f * density * scale
            val tw = timerPaint.measureText(text) + 28f * density * scale
            val th = 46f * density * scale
            val rx = vw - tw - 16f * density * scale; val ry = 14f * density * scale
            fillPaint.color = if (left <= 0L) 0xF0E5484D.toInt() else if (left < 10_000L) 0xF0F59E0B.toInt() else 0xE61D2330.toInt()
            c.drawRoundRect(RectF(rx, ry, rx + tw, ry + th), th / 2, th / 2, fillPaint)
            timerPaint.color = 0xFFFFFFFF.toInt()
            c.drawText(text, rx + tw / 2, ry + th * 0.68f, timerPaint)
        }
    }

    /** Perde kenarına dokunuldu mu (ekran koordinatı)? */
    private fun nearCurtain(y: Float): Boolean {
        if (!ctl.curtain || curtainY.isNaN()) return false
        return abs(y - (curtainY - sy) * zoom) < 30f * density
    }

    private fun trimNodes(first: Int, last: Int) {
        if (nodes.size < 12) return
        val keep = HashSet<Long>()
        for (k in max(0, first - 3) until min(nb.pages.size, last + 3)) keep += nb.pages[k].id
        val it = nodes.entries.iterator()
        while (it.hasNext()) { val e = it.next(); if (e.key !in keep) { e.value.discardDisplayList(); it.remove() } }
    }

    private fun drawPageOverlays(c: Canvas, i: Int, z: Float, now: Long, forOutput: Boolean) {
        // geçici kalem
        for (t in temps) {
            if (t.page != i) continue
            val left = t.until - now
            val a = if (left < 600) (left / 600f).coerceIn(0f, 1f) else 1f
            renderer.drawItem(c, t.item, a)
        }
        // seçili öğeler (dönüşüm önizlemesiyle)
        val s = sel
        if (s != null && s.page == i) {
            val tr = s.live
            val eg = s.editGeom
            val one = s.single
            if (eg != null && one != null) renderer.drawItem(c, one.withGeom(eg))
            else for (it in s.items) renderer.drawItem(c, if (tr.isIdentity) it else it.transformed(tr))
            if (!forOutput) drawSelectionFrame(c, s, z)
        }
        val st = stroke ?: return
        if (st.page != i) return
        when {
            st.shape != null -> {
                overlayStroke.color = st.color; overlayStroke.strokeWidth = st.width
                overlayStroke.pathEffect = if (st.dashed) DashPathEffect(floatArrayOf(st.width * 3 + 3, st.width * 2 + 3), 0f) else null
                c.drawPath(renderer.geomPath(st.shape!!), overlayStroke)
                overlayStroke.pathEffect = null
                for (g in st.extra) c.drawPath(renderer.geomPath(g), overlayStroke)
                st.label?.let { drawLabel(c, it, st.labelX, st.labelY, z) }
            }
            st.lasso -> {
                overlayStroke.color = 0xFF1E88E5.toInt(); overlayStroke.strokeWidth = 1.5f / z
                overlayStroke.pathEffect = DashPathEffect(floatArrayOf(6f / z, 5f / z), 0f)
                val xy = st.xy()
                if (xy.size >= 4) {
                    val p = Path(); p.moveTo(xy[0], xy[1])
                    var k = 2
                    while (k + 1 < xy.size) { p.lineTo(xy[k], xy[k + 1]); k += 2 }
                    c.drawPath(p, overlayStroke)
                }
                overlayStroke.pathEffect = null
            }
            st.solidParts.isNotEmpty() -> {
                overlayStroke.color = st.color; overlayStroke.strokeWidth = max(st.width, 0.8f)
                for (part in st.solidParts) {
                    overlayStroke.pathEffect = if (part.hidden) DashPathEffect(floatArrayOf(st.width * 3 + 3, st.width * 2 + 3), 0f) else null
                    c.drawPath(renderer.geomPath(part.geom), overlayStroke)
                }
                overlayStroke.pathEffect = null
            }
            st.ink -> {
                var pts = st.inkPoints(settings.smoothing.value)
                // tahmin: kalemin bir sonraki konumunu öngörüp çizgiyi biraz ileri uzat (gecikme hissini azaltır)
                if (settings.prediction.value && st.guide == null && st.n >= 3) {
                    val n = st.n
                    val lx = st.pts[3 * (n - 1)]; val ly = st.pts[3 * (n - 1) + 1]
                    val qx = st.pts[3 * (n - 3)]; val qy = st.pts[3 * (n - 3) + 1]
                    val vx = (lx - qx) * 0.5f; val vy = (ly - qy) * 0.5f
                    val lim = 6f / zoom
                    val l = Geo.len(vx, vy)
                    val k = if (l > lim) lim / l else 1f
                    pts = pts + floatArrayOf(lx + vx * k, ly + vy * k, st.pts[3 * (n - 1) + 2])
                }
                if (pts.size >= 3) {
                    fillPaint.color = st.color
                    c.drawPath(renderer.inkPath(pts, st.width, st.pressure), fillPaint)
                }
                st.label?.let { drawLabel(c, it, st.labelX, st.labelY, z) }
            }
        }
    }

    private fun drawLabel(c: Canvas, text: String, x: Float, y: Float, z: Float) {
        overlayText.textSize = 13f * density / z
        val w = overlayText.measureText(text) + 10f * density / z
        val h = 18f * density / z
        fillPaint.color = 0xE6263238.toInt()
        c.drawRoundRect(RectF(x - w / 2, y - h - 6f / z, x + w / 2, y - 6f / z), 4f / z, 4f / z, fillPaint)
        overlayText.color = 0xFFFFFFFF.toInt()
        c.drawText(text, x, y - 6f / z - h * 0.28f, overlayText)
    }

    private fun drawSelectionFrame(c: Canvas, s: Selection, z: Float) {
        val b = s.liveBox()
        overlayStroke.color = 0xFF1E88E5.toInt(); overlayStroke.strokeWidth = 1.2f / z
        overlayStroke.pathEffect = DashPathEffect(floatArrayOf(6f / z, 4f / z), 0f)
        if (s.live.angle == 0f) c.drawRect(b.left, b.top, b.right, b.bottom, overlayStroke)
        else {
            val base = s.box()
            val tr = s.live
            val xs = floatArrayOf(base.left, base.right, base.right, base.left)
            val ys = floatArrayOf(base.top, base.top, base.bottom, base.bottom)
            val p = Path()
            for (k in 0 until 4) {
                val x = tr.mapX(xs[k], ys[k]); val y = tr.mapY(xs[k], ys[k])
                if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            p.close(); c.drawPath(p, overlayStroke)
        }
        overlayStroke.pathEffect = null
        if (s.live.isIdentity) {
            val r = handleR() / z
            fillPaint.color = 0xFF1E88E5.toInt()
            c.drawCircle(b.right, b.bottom, r * 0.55f, fillPaint)
            c.drawCircle(b.cx, b.top - rotOffset() / z, r * 0.55f, fillPaint)
            overlayStroke.strokeWidth = 1f / z
            c.drawLine(b.cx, b.top, b.cx, b.top - rotOffset() / z, overlayStroke)
            val hs = 6f * density / z
            for ((hx, hy) in s.vertexHandles()) {
                fillPaint.color = 0xFFFFFFFF.toInt(); c.drawRect(hx - hs, hy - hs, hx + hs, hy + hs, fillPaint)
                overlayStroke.color = 0xFF4F46E5.toInt(); overlayStroke.strokeWidth = 2f / z
                c.drawRect(hx - hs, hy - hs, hx + hs, hy + hs, overlayStroke)
            }
        }
    }

    private fun handleR() = 22f * density
    private fun rotOffset() = 34f * density

    /* ---------------- lazer, imleç ---------------- */

    private class LaserPt(val x: Float, val y: Float, val t: Long, val start: Boolean)
    private val laser = ArrayDeque<LaserPt>()
    private var laserDot: Pair<Float, Float>? = null
    private var cursor: Triple<Float, Float, Float>? = null // eraser cursor: x,y,r (belge)
    private var protractorPen: Pair<Float, Float>? = null

    private fun drawLaser(c: Canvas, z: Float, now: Long) {
        val life = settings.laserMs.value.toLong()
        while (laser.isNotEmpty() && now - laser.first().t > life) laser.removeFirst()
        if (laser.size >= 2) {
            var prev: LaserPt? = null
            for (p in laser) {
                val q = prev
                if (q != null && !p.start) {
                    val a = 1f - (now - p.t).toFloat() / life
                    overlayStroke.strokeWidth = 9f * density / z
                    overlayStroke.color = renderer.mulAlpha(0x55FF1744, a)
                    c.drawLine(q.x, q.y, p.x, p.y, overlayStroke)
                    overlayStroke.strokeWidth = 3.2f * density / z
                    overlayStroke.color = renderer.mulAlpha(0xFFFF1744.toInt(), a)
                    c.drawLine(q.x, q.y, p.x, p.y, overlayStroke)
                }
                prev = p
            }
        }
        laserDot?.let { (x, y) ->
            fillPaint.color = 0x55FF1744; c.drawCircle(x, y, 11f * density / z, fillPaint)
            fillPaint.color = 0xFFFF1744.toInt(); c.drawCircle(x, y, 5f * density / z, fillPaint)
        }
    }

    private fun drawCursor(c: Canvas, z: Float) {
        val (x, y, r) = cursor ?: return
        overlayStroke.color = 0xAA455A64.toInt(); overlayStroke.strokeWidth = 1.2f / z
        c.drawCircle(x, y, r, overlayStroke)
    }

    private fun drawProtractorReading(c: Canvas, z: Float) {
        val (x, y) = protractorPen ?: return
        val ins = instruments.firstOrNull { it.kind == InstrumentKind.PROTRACTOR } ?: return
        val a = ins.protractorAngle(x, y) ?: return
        overlayStroke.color = 0xFF0D47A1.toInt(); overlayStroke.strokeWidth = 1f / z
        c.drawLine(ins.cx, ins.cy, x, y, overlayStroke)
        drawLabel(c, String.format("%.0f°", a), x, y - 10f / z, z)
    }

    /* ---------------- geometri araçları ---------------- */

    val instruments = ArrayList<Instrument>()

    fun toggleInstrument(kind: InstrumentKind) {
        val ex = instruments.firstOrNull { it.kind == kind }
        if (ex != null) { instruments.remove(ex); ctl.instruments.remove(kind) }
        else {
            val i = Instrument(kind, toDocX(width / 2f), toDocY(height / 2f))
            instruments += i; ctl.instruments.add(kind)
        }
        invalidate()
    }

    /* ---------------- seçim ---------------- */

    private inner class Selection(val page: Int, var items: List<Item>) {
        val ids get() = items.map { it.id }.toHashSet()
        var live = Similarity()
        /** Tek şekil seçiliyken köşe sürükleme önizlemesi. */
        var editGeom: Geom? = null
        val single: ShapeItem? get() = items.singleOrNull() as? ShapeItem
        /** Sürüklenebilir köşe tutamaçları (tek şekil seçiliyken). */
        fun vertexHandles(): List<Pair<Float, Float>> {
            val g = editGeom ?: single?.geom ?: return emptyList()
            return when (g) {
                is Geom.Polyline -> if (g.n <= 16) (0 until g.n).map { g.pts[2 * it] to g.pts[2 * it + 1] } else emptyList()
                is Geom.Circle -> listOf((g.cx + g.r) to g.cy)
                else -> emptyList()
            }
        }
        fun box(): Box = items.map { it.bounds() }.reduce { a, b -> a.union(b) }
        fun liveBox(): Box {
            if (live.isIdentity) return box()
            return items.map { it.transformed(live).bounds() }.reduce { a, b -> a.union(b) }
        }
    }

    private var sel: Selection? = null

    fun select(pageIndex: Int, items: List<Item>) {
        if (items.isEmpty()) { clearSelection(); return }
        sel = Selection(pageIndex, items)
        nb.pages.getOrNull(pageIndex)?.let { dirty += it.id }
        ctl.hasSelection = true
        ctl.selectionHasShapes = items.any { it is ShapeItem }
        invalidate()
    }

    fun clearSelection() {
        val s = sel ?: return
        sel = null
        nb.pages.getOrNull(s.page)?.let { dirty += it.id }
        ctl.hasSelection = false
        invalidate()
    }

    fun selectionDelete() {
        val s = sel ?: return
        val page = nb.pages[s.page]
        clearSelection()
        ctl.commit(page, s.items, emptyList())
    }

    fun selectionDuplicate() {
        val s = sel ?: return
        val page = nb.pages[s.page]
        val copies = s.items.map { it.transformed(Similarity.translate(14f, 14f), Ids.next()) }
        ctl.commit(page, emptyList(), copies)
        select(s.page, copies)
    }

    fun selectionCopy() { sel?.let { ctl.copy(it.items) } }

    fun selectionCut() {
        val s = sel ?: return
        ctl.copy(s.items)
        selectionDelete()
    }

    /** Seçili şekillerin içini kalem rengiyle (yarı saydam) doldurur; doluysa boşaltır. */
    fun selectionFill() {
        val s = sel ?: return
        val page = nb.pages[s.page]
        val shapes = s.items.filterIsInstance<ShapeItem>()
        if (shapes.isEmpty()) return
        val clear = shapes.all { it.fill != 0 }
        val re = s.items.map { if (it is ShapeItem) it.withFill(if (clear) 0 else ctl.fillColor) else it }
        ctl.commit(page, s.items, re)
        sel = Selection(s.page, re)
        invalidate()
    }

    fun selectionRecolor(color: Int) {
        val s = sel ?: return
        val page = nb.pages[s.page]
        val re = s.items.map {
            when (it) {
                is InkItem -> it.withColor(if (it.tool == InkTool.HIGHLIGHTER) (color and 0x00FFFFFF) or 0x66000000 else color)
                is ShapeItem -> it.withColor(color)
                is TextItem -> it.withColor(color)
                else -> it
            }
        }
        ctl.commit(page, s.items, re)
        sel = Selection(s.page, re)
        invalidate()
    }

    /* ---------------- giriş ---------------- */

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val scroller = OverScroller(context)
    private var velocity: VelocityTracker? = null

    private var penId = -1
    private var penHovering = false
    private var hoverExit = 0L
    private var penUpTime = 0L
    private var penTool = Tool.PEN

    private fun isPen(e: MotionEvent, idx: Int): Boolean {
        val t = e.getToolType(idx)
        return t == MotionEvent.TOOL_TYPE_STYLUS || t == MotionEvent.TOOL_TYPE_ERASER
    }

    /** Kalem yakındayken ya da yeni kalktıysa parmak/avuç dokunuşlarını yok say. */
    private fun palmBlocked(): Boolean {
        val now = SystemClock.uptimeMillis()
        val hold = settings.palmHoldMs.value.toLong()
        return penId != -1 || penHovering || now - hoverExit < hold || now - penUpTime < minOf(250L, hold)
    }

    override fun onHoverEvent(e: MotionEvent): Boolean {
        if (!isPen(e, 0)) return super.onHoverEvent(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> {
                penHovering = true
                val dx = toDocX(e.x); val dy = toDocY(e.y)
                laserDot = if (ctl.tool == Tool.LASER) dx to dy else null
                if (ctl.tool == Tool.SPOT) spotPos = dx to dy
                cursor = if (ctl.tool == Tool.ERASER) Triple(dx, dy, eraserRadius()) else null
                protractorPen = if (instruments.any { it.kind == InstrumentKind.PROTRACTOR }) dx to dy else null
                invalidate()
            }
            MotionEvent.ACTION_HOVER_EXIT -> {
                penHovering = false; hoverExit = SystemClock.uptimeMillis()
                laserDot = null; cursor = null; protractorPen = null
                invalidate()
            }
        }
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val action = e.actionMasked
        val idx = e.actionIndex
        // perde kenarı: kalem ya da parmakla sürüklenir
        if (curtainDragId != -1 || (ctl.curtain && (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) && penId == -1 && nearCurtain(e.getY(idx)))) {
            when (action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> if (curtainDragId == -1) curtainDragId = e.getPointerId(idx)
                MotionEvent.ACTION_MOVE -> {
                    val pi = e.findPointerIndex(curtainDragId)
                    if (pi >= 0) { curtainY = toDocY(e.getY(pi)); invalidate() }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL ->
                    if (e.getPointerId(idx) == curtainDragId || action != MotionEvent.ACTION_POINTER_UP) curtainDragId = -1
            }
            return true
        }
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (isPen(e, idx)) {
                    if (penId == -1) {
                        cancelGesture()
                        penId = e.getPointerId(idx)
                        requestUnbufferedDispatch(e)
                        // Android 15+: kalem yazarken ekran yenileme hızını yüksek tut (144 Hz)
                        if (Build.VERSION.SDK_INT >= 35) requestedFrameRate = View.REQUESTED_FRAME_RATE_CATEGORY_HIGH
                        parent?.requestDisallowInterceptTouchEvent(true)
                        penDown(e, idx)
                    }
                } else fingerDown(e, idx)
            }
            MotionEvent.ACTION_MOVE -> {
                if (penId != -1) {
                    val pi = e.findPointerIndex(penId)
                    if (pi >= 0) penMove(e, pi)
                }
                fingerMove(e)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val pid = e.getPointerId(idx)
                val canceled = action == MotionEvent.ACTION_CANCEL ||
                    (Build.VERSION.SDK_INT >= 33 && (e.flags and MotionEvent.FLAG_CANCELED) != 0)
                if (pid == penId) {
                    if (canceled) penCancel() else penUp(e, idx)
                    penId = -1
                    penUpTime = SystemClock.uptimeMillis()
                    if (Build.VERSION.SDK_INT >= 35) requestedFrameRate = View.REQUESTED_FRAME_RATE_CATEGORY_DEFAULT
                    if (action == MotionEvent.ACTION_CANCEL) cancelGesture()
                } else {
                    fingerUp(e, idx, canceled)
                }
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    penId = -1
                    fingers.clear()
                }
            }
        }
        return true
    }

    /* ---------- parmak hareketleri ---------- */

    private class Finger(var x: Float, var y: Float, val sx: Float, val sy: Float)
    private val fingers = LinkedHashMap<Int, Finger>()
    private val ignored = HashSet<Int>()
    private var gestureActive = false
    private var gestureStart = 0L
    private var gestureMaxFingers = 0
    private var gestureMoved = false
    private var grabbed: Instrument? = null
    private var grabRawAngle = 0f
    private var lastSpan = 0f
    private var lastAngle = 0f
    private var lastCx = 0f
    private var lastCy = 0f
    private var fingerDrawing = false

    private fun fingerDown(e: MotionEvent, idx: Int) {
        val id = e.getPointerId(idx)
        val palmPx = settings.palmSizeDp.value * density
        val blocked = if (fingerDrawing) false else palmBlocked()
        if (blocked || e.getTouchMajor(idx) > palmPx || !settings.fingerGestures.value && settings.penOnly.value) {
            ignored += id; return
        }
        if (!settings.penOnly.value && fingers.isEmpty() && !gestureActive && penId == -1) {
            // parmakla çizim açık: tek parmak kalem gibi davranır
            fingerDrawing = true
            penId = id
            penDown(e, idx)
            return
        }
        if (fingerDrawing && penId != -1) {
            // ikinci parmak geldi: çizimi iptal et, harekete geç
            penCancel(); penId = -1; fingerDrawing = false
            val first = e.getPointerId(0)
            if (first != id) fingers[first] = Finger(e.getX(0), e.getY(0), e.getX(0), e.getY(0))
        }
        if (!gestureActive) {
            gestureActive = true
            gestureStart = SystemClock.uptimeMillis()
            gestureMaxFingers = 0
            gestureMoved = false
            scroller.forceFinished(true)
            velocity?.recycle(); velocity = VelocityTracker.obtain()
            val dx = toDocX(e.getX(idx)); val dy = toDocY(e.getY(idx))
            grabbed = instruments.lastOrNull { it.contains(dx, dy) }
            grabbed?.let { grabRawAngle = it.angle }
        }
        fingers[id] = Finger(e.getX(idx), e.getY(idx), e.getX(idx), e.getY(idx))
        gestureMaxFingers = max(gestureMaxFingers, fingers.size)
        resetGestureBaseline()
        velocity?.addMovement(e)
    }

    private fun resetGestureBaseline() {
        val f = fingers.values.toList()
        if (f.size >= 2) {
            lastSpan = Geo.dist(f[0].x, f[0].y, f[1].x, f[1].y)
            lastAngle = Geo.angleOf(f[0].x, f[0].y, f[1].x, f[1].y)
            lastCx = (f[0].x + f[1].x) / 2; lastCy = (f[0].y + f[1].y) / 2
        } else if (f.size == 1) { lastCx = f[0].x; lastCy = f[0].y }
    }

    private fun fingerMove(e: MotionEvent) {
        if (fingerDrawing) {
            val pi = e.findPointerIndex(penId)
            if (pi >= 0) penMove(e, pi)
            return
        }
        if (!gestureActive || fingers.isEmpty()) return
        velocity?.addMovement(e)
        for ((id, f) in fingers) {
            val pi = e.findPointerIndex(id)
            if (pi < 0) continue
            f.x = e.getX(pi); f.y = e.getY(pi)
            if (Geo.dist(f.x, f.y, f.sx, f.sy) > touchSlop) gestureMoved = true
        }
        if (!gestureMoved) return
        val f = fingers.values.toList()
        val g = grabbed
        if (f.size >= 2) {
            val span = Geo.dist(f[0].x, f[0].y, f[1].x, f[1].y)
            val ang = Geo.angleOf(f[0].x, f[0].y, f[1].x, f[1].y)
            val cx = (f[0].x + f[1].x) / 2; val cy = (f[0].y + f[1].y) / 2
            if (g != null) {
                g.cx += (cx - lastCx) / zoom; g.cy += (cy - lastCy) / zoom
                grabRawAngle += Geo.normAngle(ang - lastAngle)
                g.angle = Snapper.snapAngle(grabRawAngle, Geo.toRad(15f), Geo.toRad(2f))
            } else if (lastSpan > 0f && span > 0f) {
                // odak noktası etrafında yakınlaştır
                val docFx = toDocX(lastCx); val docFy = toDocY(lastCy)
                if (!settings.zoomLock.value) zoom *= span / lastSpan
                zoom = zoom.coerceIn(fitZoom() * 0.35f, fitZoom() * 12f)
                sx = docFx - cx / zoom; sy = docFy - cy / zoom
            }
            lastSpan = span; lastAngle = ang; lastCx = cx; lastCy = cy
        } else if (f.size == 1) {
            val dx = f[0].x - lastCx; val dy = f[0].y - lastCy
            if (g != null) { g.cx += dx / zoom; g.cy += dy / zoom }
            else { sx -= dx / zoom; sy -= dy / zoom }
            lastCx = f[0].x; lastCy = f[0].y
        }
        clampScroll(); invalidate(); updateCurrentPage(); scheduleSharp()
        if (g == null) ctl.scrollTick++
    }

    private fun fingerUp(e: MotionEvent, idx: Int, canceled: Boolean) {
        val id = e.getPointerId(idx)
        if (ignored.remove(id)) return
        if (fingerDrawing && id == penId) {
            if (canceled) penCancel() else penUp(e, idx)
            penId = -1; fingerDrawing = false
            return
        }
        if (!gestureActive) return
        velocity?.addMovement(e)
        val wasSingle = fingers.size == 1
        fingers.remove(id)
        if (fingers.isEmpty()) {
            val dur = SystemClock.uptimeMillis() - gestureStart
            if (!gestureMoved && !canceled && dur < 320 && settings.twoFingerUndo.value) {
                if (gestureMaxFingers == 2) ctl.undo()
                else if (gestureMaxFingers == 3) ctl.redo()
            } else if (wasSingle && gestureMoved && grabbed == null && gestureMaxFingers == 1) {
                velocity?.let { v ->
                    v.computeCurrentVelocity(1000)
                    val vx = -v.xVelocity; val vy = -v.yVelocity
                    if (abs(vy) > 400 || abs(vx) > 400) {
                        scroller.fling(
                            (sx * zoom).toInt(), (sy * zoom).toInt(), vx.toInt(), vy.toInt(),
                            Int.MIN_VALUE / 4, Int.MAX_VALUE / 4, Int.MIN_VALUE / 4, Int.MAX_VALUE / 4,
                        )
                        postInvalidateOnAnimation()
                    }
                }
            }
            gestureActive = false
            grabbed = null
            velocity?.recycle(); velocity = null
            scheduleSharp()
        } else resetGestureBaseline()
    }

    private fun cancelGesture() {
        fingers.clear(); gestureActive = false; grabbed = null
        velocity?.recycle(); velocity = null
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            sx = scroller.currX / zoom; sy = scroller.currY / zoom
            val bsx = sx; val bsy = sy
            clampScroll()
            if (bsx != sx && bsy != sy) scroller.forceFinished(true)
            updateCurrentPage()
            ctl.scrollTick++
            postInvalidateOnAnimation()
            if (scroller.isFinished) scheduleSharp()
        }
    }

    /* ---------- keskin PDF ---------- */

    private val sharpRunnable = Runnable { requestSharp() }
    private fun scheduleSharp() {
        if (ctl.pdf == null) return
        removeCallbacks(sharpRunnable); postDelayed(sharpRunnable, 140)
    }

    private fun requestSharp() {
        val pdf = ctl.pdf ?: return
        if (tops.isEmpty() || width == 0) return
        val top = sy; val bottom = sy + height / zoom
        val keep = HashSet<Int>()
        var i = pageIndexAt(top)
        while (i < tops.size && tops[i] <= bottom) {
            val page = nb.pages[i]
            val bg = page.background
            if (bg is Background.PdfPage) {
                val baseScale = min(1.6f, 1800f / max(page.width, page.height))
                if (zoom > baseScale * 1.05f) {
                    keep += bg.index
                    val l = sx - pageLeft(i); val t = sy - tops[i]
                    val r = RectF(l, t, l + width / zoom, t + height / zoom)
                    val padX = r.width() * 0.1f; val padY = r.height() * 0.1f
                    r.inset(-padX, -padY)
                    if (r.intersect(0f, 0f, page.width, page.height)) pdf.requestSharp(bg, r, zoom)
                }
            }
            i++
        }
        pdf.dropSharpExcept(keep)
    }

    /* ---------- kalem ---------- */

    /** Çizim sürerken tutulan durum. Noktalar sayfa koordinatındadır. */
    private class Stroke(val page: Int, val tool: Tool) {
        var pts = FloatArray(256)
        var n = 0
        var color = 0
        var width = 2f
        var pressure = true
        var ink = false
        var lasso = false
        var dashed = false
        var guide: Guide? = null
        var shape: Geom? = null
        var extra: List<Geom> = emptyList()
        var lineFollow = false
        var label: String? = null
        var labelX = 0f
        var labelY = 0f
        var ax = 0f; var ay = 0f // başlangıç noktası (araçlar için)
        var before: List<Item>? = null
        var lastX = 0f; var lastY = 0f
        var holdX = 0f; var holdY = 0f
        var moved = false
        var sweep = 0f; var prevAngle = 0f
        var editVertex = -1
        var solidParts: List<com.poyankee.note.core.Solids.Part> = emptyList()

        fun add(x: Float, y: Float, p: Float) {
            if (3 * n + 3 > pts.size) pts = pts.copyOf(pts.size * 2)
            pts[3 * n] = x; pts[3 * n + 1] = y; pts[3 * n + 2] = p; n++
        }
        fun raw() = pts.copyOf(3 * n)
        fun xy(): FloatArray { val o = FloatArray(n * 2); for (i in 0 until n) { o[2 * i] = pts[3 * i]; o[2 * i + 1] = pts[3 * i + 1] }; return o }
        fun inkPoints(smooth: Int): FloatArray = if (guide != null) raw() else StrokeOutline.smooth(raw(), smooth.coerceAtMost(2))
    }

    private var stroke: Stroke? = null

    /** Pergelin ikinci aşaması (merkez ve yarıçap seçildi). */
    private class CompassState(val page: Int, val cx: Float, val cy: Float, val r: Float)
    private var compass: CompassState? = null

    fun cancelToolState() {
        compass = null; stroke = null
        removeCallbacks(holdRunnable)
        invalidate()
    }

    private fun eraserRadius(): Float {
        val px = when (ctl.eraserSize) { 0 -> 7f; 1 -> 14f; else -> 28f } * density
        return px / zoom
    }

    private fun pressureOf(e: MotionEvent, idx: Int, h: Int = -1): Float {
        if (e.getToolType(idx) == MotionEvent.TOOL_TYPE_FINGER) return 0.6f
        val p = if (h >= 0) e.getHistoricalPressure(idx, h) else e.getPressure(idx)
        return p.coerceIn(0.05f, 1f)
    }

    private fun effectiveTool(e: MotionEvent, idx: Int): Tool {
        if (e.getToolType(idx) == MotionEvent.TOOL_TYPE_ERASER) return Tool.ERASER
        val btn = e.buttonState and (MotionEvent.BUTTON_STYLUS_PRIMARY or MotionEvent.BUTTON_STYLUS_SECONDARY or MotionEvent.BUTTON_SECONDARY)
        if (btn != 0) {
            when (settings.buttonActionValue) {
                ButtonAction.ERASER -> return Tool.ERASER
                ButtonAction.LASSO -> return Tool.LASSO
                ButtonAction.LASER -> return Tool.LASER
                ButtonAction.NONE -> {}
            }
        }
        return ctl.tool
    }

    private fun snapperFor(i: Int, exclude: Long = -1L): Snapper {
        val page = nb.pages[i]
        val bg = page.background
        val lattice = if (settings.gridSnap.value && bg is Background.Paper) Templates.snapLattice(bg.template, page.width, page.height, bg.spacing) else null
        return Snapper(lattice, Snapper.anchorsOf(if (exclude == -1L) page.items else page.items.filter { it.id != exclude }), max(3f, 13f * density / zoom), settings.angleSnap.value)
    }

    private fun penDown(e: MotionEvent, idx: Int) {
        scroller.forceFinished(true)
        laserDot = null; cursor = null
        val dx = toDocX(e.getX(idx)); val dy = toDocY(e.getY(idx))
        val tool = effectiveTool(e, idx)
        penTool = tool

        // seçim varsa: içine dokunmak = taşı/ölçekle/döndür
        val s = sel
        if (s != null) {
            val px = dx - pageLeft(s.page); val py = dy - tops[s.page]
            val b = s.box()
            val hr = handleR() / zoom
            val vh = s.vertexHandles()
            val vi = vh.indexOfFirst { (hx, hy) -> Geo.dist(px, py, hx, hy) < hr * 0.8f }
            val mode = when {
                vi >= 0 -> 5
                Geo.dist(px, py, b.right, b.bottom) < hr -> 2
                Geo.dist(px, py, b.cx, b.top - rotOffset() / zoom) < hr -> 3
                b.inflate(6f / zoom).contains(px, py) -> 1
                else -> 0
            }
            if (mode != 0) {
                val st = Stroke(s.page, Tool.LASSO)
                st.ax = px; st.ay = py
                st.sweep = mode.toFloat()
                st.editVertex = vi
                stroke = st
                return
            }
            clearSelection()
        }

        val i = pageIndexAt(dy)
        val page = nb.pages[i]
        val px = dx - pageLeft(i); val py = dy - tops[i]
        val st = Stroke(i, tool)
        st.ax = px; st.ay = py; st.lastX = px; st.lastY = py; st.holdX = px; st.holdY = py
        when (tool) {
            Tool.PEN, Tool.TEMP, Tool.SHAPE -> {
                st.ink = true; st.color = ctl.penColor; st.width = ctl.penWidth; st.pressure = settings.pressure.value
                if (tool == Tool.TEMP) st.color = ctl.penColor
            }
            Tool.HIGHLIGHTER -> { st.ink = true; st.color = ctl.hlColor; st.width = ctl.hlWidth; st.pressure = false }
            Tool.ERASER -> {
                st.before = page.items.toList()
                cursor = Triple(dx, dy, eraserRadius())
                eraseSegment(st, px, py, px, py)
            }
            Tool.LASSO -> st.lasso = true
            Tool.LASER -> { laser.addLast(LaserPt(dx, dy, SystemClock.uptimeMillis(), true)); laserDot = dx to dy }
            Tool.TEXT, Tool.MARK -> {}
            Tool.SPOT -> spotPos = dx to dy
            Tool.LINE, Tool.ARROW, Tool.POLYGON, Tool.SOLID -> {
                val (x, y) = snapperFor(i).point(px, py)
                st.ax = x; st.ay = y; st.color = ctl.penColor; st.width = ctl.penWidth; st.dashed = ctl.shapeDashed
            }
            Tool.COMPASS -> {
                st.color = ctl.penColor; st.width = ctl.penWidth; st.dashed = ctl.shapeDashed
                val cs = compass
                if (cs != null) {
                    // yay çizimi aşaması
                    val cpx = dx - pageLeft(cs.page); val cpy = dy - tops[cs.page]
                    val a = Geo.angleOf(cs.cx, cs.cy, cpx, cpy)
                    val s2 = Stroke(cs.page, tool)
                    s2.color = st.color; s2.width = st.width; s2.dashed = st.dashed
                    s2.ax = a; s2.prevAngle = a; s2.sweep = 0f
                    s2.shape = Geom.Arc(cs.cx, cs.cy, cs.r, a, 0f)
                    stroke = s2
                    invalidate()
                    return
                }
                val (x, y) = snapperFor(i).point(px, py)
                st.ax = x; st.ay = y
            }
        }
        if (st.ink) {
            st.guide = instruments.asReversed().firstNotNullOfOrNull { it.guideNear(dx, dy, max(4f, 18f * density / zoom)) }
            addInkPoint(st, dx, dy, pressureOf(e, idx))
            if (settings.shapeHold.value && st.guide == null && tool != Tool.TEMP) postDelayed(holdRunnable, settings.holdMs.value.toLong())
        }
        stroke = st
        protractorPen = if (instruments.any { it.kind == InstrumentKind.PROTRACTOR }) dx to dy else null
        invalidate()
    }

    private fun addInkPoint(st: Stroke, dx: Float, dy: Float, p: Float) {
        val g = st.guide
        val (x, y) = if (g != null) g.project(dx, dy) else dx to dy
        st.add(x - pageLeft(st.page), y - tops[st.page], p)
        if (g != null && st.n >= 2) {
            // kenar boyunca çizilen çizginin uzunluğu (cm)
            val ax = st.pts[0]; val ay = st.pts[1]
            val bx = st.pts[3 * (st.n - 1)]; val by = st.pts[3 * (st.n - 1) + 1]
            if (g is Guide.Line) {
                val cm = Geo.dist(ax, ay, bx, by) / (Templates.MM * 10f)
                st.label = String.format("%.1f cm", cm); st.labelX = bx; st.labelY = by - 8f / zoom
            }
        }
    }

    private fun penMove(e: MotionEvent, idx: Int) {
        val st = stroke ?: return
        val hs = e.historySize
        for (h in 0..hs) {
            val ex = if (h < hs) e.getHistoricalX(idx, h) else e.getX(idx)
            val ey = if (h < hs) e.getHistoricalY(idx, h) else e.getY(idx)
            val p = if (h < hs) pressureOf(e, idx, h) else pressureOf(e, idx)
            moveTo(st, toDocX(ex), toDocY(ey), p)
        }
        protractorPen = if (instruments.any { it.kind == InstrumentKind.PROTRACTOR }) toDocX(e.getX(idx)) to toDocY(e.getY(idx)) else null
        invalidate()
    }

    private fun moveTo(st: Stroke, dx: Float, dy: Float, p: Float) {
        val s = sel
        if (st.tool == Tool.LASSO && s != null && st.sweep > 0f) {
            // seçimi dönüştür
            val px = dx - pageLeft(s.page); val py = dy - tops[s.page]
            val b = s.box()
            if (st.sweep.toInt() == 5) {
                val one = s.single ?: return
                val sn = snapperFor(s.page, exclude = one.id)
                val (x, y) = sn.point(px, py)
                s.editGeom = when (val g = one.geom) {
                    is Geom.Polyline -> {
                        val pts = g.pts.copyOf()
                        pts[2 * st.editVertex] = x; pts[2 * st.editVertex + 1] = y
                        Geom.Polyline(pts, g.closed)
                    }
                    is Geom.Circle -> Geom.Circle(g.cx, g.cy, max(2f, Geo.dist(g.cx, g.cy, x, y)))
                    else -> g
                }
                st.moved = true
                return
            }
            s.live = when (st.sweep.toInt()) {
                1 -> Similarity.translate(px - st.ax, py - st.ay)
                2 -> {
                    val d0 = Geo.dist(b.left, b.top, st.ax, st.ay); val d1 = Geo.dist(b.left, b.top, px, py)
                    Similarity(scale = (d1 / max(d0, 1f)).coerceIn(0.05f, 20f), ox = b.left, oy = b.top)
                }
                else -> {
                    val a = Geo.angleOf(b.cx, b.cy, px, py) - Geo.angleOf(b.cx, b.cy, st.ax, st.ay)
                    Similarity(angle = Snapper.snapAngle(Geo.normAngle(a), Geo.toRad(15f), Geo.toRad(3f)), ox = b.cx, oy = b.cy)
                }
            }
            st.moved = true
            return
        }
        val px = dx - pageLeft(st.page); val py = dy - tops[st.page]
        if (Geo.dist(px, py, st.ax, st.ay) > touchSlop / zoom) st.moved = true
        when (st.tool) {
            Tool.PEN, Tool.HIGHLIGHTER, Tool.TEMP, Tool.SHAPE -> {
                if (st.shape != null) {
                    if (st.lineFollow) {
                        val g = st.shape as Geom.Polyline
                        val sn = snapperFor(st.page)
                        val (x, y, hit) = sn.point(px, py)
                        var line = Geom.Polyline(floatArrayOf(g.pts[0], g.pts[1], x, y), false)
                        if (!hit) line = sn.snap(Geom.Polyline(floatArrayOf(g.pts[0], g.pts[1], px, py), false)) as Geom.Polyline
                        st.shape = line
                        lineLabel(st, line)
                    }
                    return
                }
                addInkPoint(st, dx, dy, p)
                val hd = 6f * density / zoom
                if (Geo.dist(px, py, st.holdX, st.holdY) > hd) {
                    st.holdX = px; st.holdY = py
                    removeCallbacks(holdRunnable)
                    if (settings.shapeHold.value && st.guide == null && st.tool != Tool.TEMP) postDelayed(holdRunnable, settings.holdMs.value.toLong())
                }
            }
            Tool.ERASER -> {
                eraseSegment(st, st.lastX, st.lastY, px, py)
                cursor = Triple(dx, dy, eraserRadius())
            }
            Tool.LASSO -> st.add(px, py, 1f)
            Tool.LASER -> { laser.addLast(LaserPt(dx, dy, SystemClock.uptimeMillis(), false)); laserDot = dx to dy }
            Tool.SPOT -> spotPos = dx to dy
            Tool.SOLID -> {
                val (x, y) = snapperFor(st.page).point(px, py)
                st.solidParts = com.poyankee.note.core.Solids.create(ctl.solidKind, st.ax, st.ay, x, y)
            }
            Tool.LINE, Tool.ARROW -> {
                val sn = snapperFor(st.page)
                val (x, y, hit) = sn.point(px, py)
                var line = Geom.Polyline(floatArrayOf(st.ax, st.ay, x, y), false)
                if (!hit) line = sn.snap(Geom.Polyline(floatArrayOf(st.ax, st.ay, px, py), false)) as Geom.Polyline
                if (st.tool == Tool.ARROW) {
                    val parts = Shapes.arrow(line.pts[0], line.pts[1], line.pts[2], line.pts[3], 6f + st.width * 2.5f)
                    st.shape = parts[0]; st.extra = parts.drop(1)
                } else st.shape = line
                lineLabel(st, line)
            }
            Tool.POLYGON -> {
                val sn = snapperFor(st.page)
                var (x, y, hit) = sn.point(px, py)
                if (!hit && settings.angleSnap.value) {
                    val r = Geo.dist(st.ax, st.ay, px, py)
                    val a = Snapper.snapAngle(Geo.angleOf(st.ax, st.ay, px, py), Geo.toRad(15f), Geo.toRad(4f))
                    x = st.ax + r * kotlin.math.cos(a); y = st.ay + r * kotlin.math.sin(a)
                }
                if (Geo.dist(st.ax, st.ay, x, y) > 2f) {
                    val poly = Shapes.regularPolygon(st.ax, st.ay, x, y, ctl.polygonSides)
                    st.shape = poly
                    val side = Geo.dist(poly.pts[0], poly.pts[1], poly.pts[2], poly.pts[3]) / (Templates.MM * 10f)
                    st.label = String.format("kenar %.1f cm", side); st.labelX = st.ax; st.labelY = st.ay - 6f / zoom
                }
            }
            Tool.COMPASS -> {
                val cs = compass
                if (cs != null) {
                    val a = Geo.angleOf(cs.cx, cs.cy, px + pageLeft(st.page) - pageLeft(cs.page), py + tops[st.page] - tops[cs.page])
                    st.sweep += Geo.normAngle(a - st.prevAngle)
                    st.prevAngle = a
                    st.sweep = st.sweep.coerceIn(-Geo.TAU, Geo.TAU)
                    st.shape = if (abs(st.sweep) >= Geo.TAU - 0.02f) Geom.Circle(cs.cx, cs.cy, cs.r) else Geom.Arc(cs.cx, cs.cy, cs.r, st.ax, st.sweep)
                    st.label = String.format("%.0f°", abs(Geo.toDeg(st.sweep))); st.labelX = px; st.labelY = py - 8f / zoom
                } else {
                    val (x, y) = snapperFor(st.page).point(px, py)
                    val r = Geo.dist(st.ax, st.ay, x, y)
                    st.shape = Geom.Polyline(floatArrayOf(st.ax, st.ay, x, y), false)
                    st.extra = listOf(Geom.Circle(st.ax, st.ay, r))
                    st.label = String.format("r = %.1f cm", r / (Templates.MM * 10f)); st.labelX = x; st.labelY = y - 8f / zoom
                    st.lastX = x; st.lastY = y
                    return
                }
            }
            Tool.TEXT, Tool.MARK -> {}
        }
        st.lastX = px; st.lastY = py
    }

    private fun lineLabel(st: Stroke, line: Geom.Polyline) {
        val len = Geo.dist(line.pts[0], line.pts[1], line.pts[2], line.pts[3]) / (Templates.MM * 10f)
        var deg = -Geo.toDeg(Geo.angleOf(line.pts[0], line.pts[1], line.pts[2], line.pts[3]))
        if (deg < 0) deg += 360f
        st.label = String.format("%.1f cm  %.0f°", len, deg)
        st.labelX = line.pts[2]; st.labelY = line.pts[3] - 10f / zoom
    }

    /** Kalem bir süre hareketsiz kalınca çizimi şekle çevir. */
    private val holdRunnable = Runnable {
        val st = stroke ?: return@Runnable
        if (!st.ink || st.shape != null || st.n < 4) return@Runnable
        val g = ShapeRecognizer.recognize(st.raw()) ?: return@Runnable
        val snapped = snapperFor(st.page).snap(g)
        st.shape = snapped
        st.lineFollow = snapped is Geom.Polyline && snapped.n == 2 && !snapped.closed
        if (st.lineFollow) lineLabel(st, snapped as Geom.Polyline) else st.label = null
        if (st.tool == Tool.HIGHLIGHTER) st.width = ctl.hlWidth else st.width = max(ctl.penWidth, 0.8f)
        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        invalidate()
    }

    private fun eraseSegment(st: Stroke, ax: Float, ay: Float, bx: Float, by: Float) {
        val page = nb.pages[st.page]
        val r = eraserRadius()
        var changed = false
        if (ctl.eraserPartial) {
            val list = ArrayList(page.items)
            for (it in list) {
                val parts = HitTest.partialErase(it, ax, ay, bx, by, r) { Ids.next() } ?: continue
                page.items.remove(it)
                page.items.addAll(parts)
                changed = true
            }
        } else {
            changed = page.items.removeAll { it !is ImageItem && HitTest.touches(it, ax, ay, bx, by, r) }
        }
        if (changed) invalidatePage(page.id)
    }

    private fun penUp(e: MotionEvent, idx: Int) {
        removeCallbacks(holdRunnable)
        val st = stroke ?: return
        stroke = null
        protractorPen = null
        val page = nb.pages[st.page]
        val s = sel
        if (st.tool == Tool.LASSO && s != null && st.sweep.toInt() == 5) {
            val one = s.single
            val eg = s.editGeom
            s.editGeom = null
            if (one != null && eg != null) {
                val changed = one.withGeom(eg)
                ctl.commit(nb.pages[s.page], listOf(one), listOf(changed))
                sel = Selection(s.page, listOf(changed))
                dirty += nb.pages[s.page].id
            }
            invalidate(); return
        }
        if (st.tool == Tool.LASSO && s != null && st.sweep > 0f) {
            val tr = s.live
            s.live = Similarity()
            if (!tr.isIdentity) {
                val moved = s.items.map { it.transformed(tr) }
                ctl.commit(nb.pages[s.page], s.items, moved)
                sel = Selection(s.page, moved)
                dirty += nb.pages[s.page].id
            }
            invalidate(); return
        }
        when (st.tool) {
            Tool.PEN, Tool.HIGHLIGHTER, Tool.SHAPE, Tool.TEMP -> {
                var geom = st.shape
                if (geom == null && st.tool == Tool.SHAPE && st.n >= 4) geom = ShapeRecognizer.recognize(st.raw())?.let { snapperFor(st.page).snap(it) }
                val item: Item = if (geom != null) {
                    if (st.tool == Tool.HIGHLIGHTER && geom is Geom.Polyline && geom.n == 2) {
                        InkItem(Ids.next(), InkTool.HIGHLIGHTER, st.color, st.width,
                            floatArrayOf(geom.pts[0], geom.pts[1], 1f, geom.pts[2], geom.pts[3], 1f), false)
                    } else ShapeItem(
                        Ids.next(), geom, st.color, if (st.tool == Tool.HIGHLIGHTER) st.width else max(st.width, 0.8f),
                        ctl.shapeDashed && st.tool != Tool.HIGHLIGHTER, fillFor(geom, st.tool != Tool.HIGHLIGHTER),
                    )
                } else {
                    var pts = st.inkPoints(settings.smoothing.value)
                    if (st.guide == null) pts = StrokeOutline.smooth(StrokeOutline.dedupe(st.raw(), 0.35f / zoom), settings.smoothing.value)
                    else pts = StrokeOutline.dedupe(pts, 0.35f / zoom)
                    InkItem(Ids.next(), if (st.tool == Tool.HIGHLIGHTER) InkTool.HIGHLIGHTER else InkTool.PEN, st.color, st.width, pts, st.pressure)
                }
                if (st.tool == Tool.TEMP) {
                    temps += Temp(st.page, item, SystemClock.uptimeMillis() + settings.tempMs.value)
                    postInvalidateOnAnimation()
                } else {
                    val extras = if (item is ShapeItem) ctl.shapeExtras(page, listOf(item.geom)) else emptyList()
                    ctl.commit(page, emptyList(), listOf(item) + extras)
                    // son sayfanın alt kısmına yazıldıysa yeni sayfa hazırla
                    if (settings.autoNewPage.value && st.page == nb.pages.lastIndex && page.background is Background.Paper &&
                        item.bounds().bottom > page.height * 0.85f) ctl.appendPageQuietly()
                }
            }
            Tool.ERASER -> {
                cursor = null
                val before = st.before ?: return
                ctl.undoStack.record(page, before)
                ctl.pageChanged(page)
                // silgi menüden seçildiyse sildikten sonra kaleme dön (ayarlardan kapatılabilir)
                if (settings.eraserReturn.value && ctl.tool == Tool.ERASER && penTool == Tool.ERASER) ctl.returnFromEraser()
            }
            Tool.LASSO -> {
                if (st.n >= 3) {
                    val found = HitTest.lasso(page.items, st.xy())
                    if (found.isNotEmpty()) select(st.page, found)
                }
            }
            Tool.LASER -> laserDot = null
            Tool.TEXT -> {
                if (!st.moved) {
                    // formüle dokunulduysa formülü düzenle
                    val fx = page.items.lastOrNull { it is ImageItem && it.source.isNotEmpty() && it.bounds().contains(st.ax, st.ay) } as ImageItem?
                    if (fx != null) ctl.requestFormula(st.page, fx)
                    else ctl.requestText(st.page, st.ax, st.ay, HitTest.textAt(page.items, st.ax, st.ay))
                }
            }
            Tool.SPOT -> {}
            Tool.SOLID -> {
                if (st.moved && st.solidParts.isNotEmpty()) {
                    ctl.commit(page, emptyList(), st.solidParts.map { ShapeItem(Ids.next(), it.geom, st.color, max(st.width, 0.8f), it.hidden) })
                }
            }
            Tool.MARK -> {
                if (!st.moved) {
                    val kind = when (ctl.markKind) { MarkKind.ANGLE -> Marks.Kind.ANGLE; MarkKind.TICK -> Marks.Kind.TICK; MarkKind.PARALLEL -> Marks.Kind.PARALLEL }
                    val geoms = Marks.create(page.items, st.ax, st.ay, kind, ctl.markCount, max(8f, 26f * density / zoom))
                    if (geoms.isEmpty()) ctl.message = "Yakında bir şekil köşesi/kenarı bulunamadı"
                    else ctl.commit(page, emptyList(), geoms.map { ShapeItem(Ids.next(), it, ctl.penColor, min(ctl.penWidth, 1.4f)) as Item } + geoms.mapNotNull { ctl.angleLabel(it) })
                }
            }
            Tool.LINE, Tool.ARROW, Tool.POLYGON -> {
                val g = st.shape
                if (g != null && st.moved) {
                    val all = listOf(g) + st.extra
                    val shapes: List<Item> = all.map { ShapeItem(Ids.next(), it, st.color, max(st.width, 0.8f), st.dashed, fillFor(it, true)) }
                    ctl.commit(page, emptyList(), shapes + if (st.tool == Tool.ARROW) emptyList() else ctl.shapeExtras(page, listOf(g)))
                }
            }
            Tool.COMPASS -> {
                val cs = compass
                if (cs == null) {
                    val r = Geo.dist(st.ax, st.ay, st.lastX, st.lastY)
                    if (st.moved && r > 2f) {
                        compass = CompassState(st.page, st.ax, st.ay, r)
                        ctl.hint = "Pergel açıldı (r = ${String.format("%.1f", r / (Templates.MM * 10f))} cm). Şimdi kalemi merkez etrafında döndürerek yayı çizin; tam çember için dokunun."
                    }
                } else {
                    val sw = st.sweep
                    val g: Geom = if (abs(sw) < 0.05f || abs(sw) >= Geo.TAU - 0.02f) Geom.Circle(cs.cx, cs.cy, cs.r) else Geom.Arc(cs.cx, cs.cy, cs.r, st.ax, sw)
                    ctl.commit(nb.pages[cs.page], emptyList(), listOf(ShapeItem(Ids.next(), g, st.color, max(st.width, 0.8f), st.dashed, fillFor(g, true))))
                    compass = null
                    ctl.hint = "Pergel: kalemi merkeze koyup yarıçap kadar çekin, sonra kalemi döndürerek yayı çizin."
                }
            }
        }
        invalidate()
    }

    /** Dolgu açıksa kapalı şekiller için dolgu rengi. */
    private fun fillFor(g: Geom, allowed: Boolean): Int {
        if (!allowed || !ctl.shapeFill) return 0
        val closed = when (g) { is Geom.Polyline -> g.closed; is Geom.Circle, is Geom.Ellipse -> true; else -> false }
        return if (closed) ctl.fillColor else 0
    }

    private fun penCancel() {
        removeCallbacks(holdRunnable)
        val st = stroke ?: return
        stroke = null
        if (st.tool == Tool.ERASER) {
            // silgi zaten uyguladı; geri alma için kaydet
            val before = st.before
            if (before != null) { val p = nb.pages[st.page]; ctl.undoStack.record(p, before); ctl.pageChanged(p) }
        }
        sel?.live = Similarity()
        invalidate()
    }

    /* ---------- geçici kalem ---------- */

    private class Temp(val page: Int, val item: Item, val until: Long)
    private val temps = ArrayList<Temp>()

    private val tempSweeper = object : Runnable {
        override fun run() {
            val now = SystemClock.uptimeMillis()
            if (temps.removeAll { it.until <= now }) invalidate()
            postDelayed(this, 100)
        }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); post(tempSweeper) }
    override fun onDetachedFromWindow() {
        removeCallbacks(tempSweeper); removeCallbacks(holdRunnable); removeCallbacks(sharpRunnable)
        nodes.values.forEach { it.discardDisplayList() }; nodes.clear()
        super.onDetachedFromWindow()
    }

    init {
        isFocusable = true
        relayoutPages()
    }

    fun ctlListeners() = ctl.frameListeners

    /** Testler için. */
    val controller: EditorController get() = ctl

    /** İkinci ekran / kayıt için o anki görünüm (belge koordinatlarında). */
    fun viewport(): RectF = RectF(sx, sy, sx + width / zoom, sy + height / zoom)
}
