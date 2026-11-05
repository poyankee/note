package com.poyankee.note.core

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Öğe isabet testi, silgi, kement seçimi. */
object HitTest {

    /** Silginin [a→b] hareketi (yarıçap [r]) öğeye değiyor mu? */
    fun touches(item: Item, ax: Float, ay: Float, bx: Float, by: Float, r: Float): Boolean {
        val eb = Box(minOf(ax, bx), minOf(ay, by), maxOf(ax, bx), maxOf(ay, by)).inflate(r)
        if (!item.bounds().intersects(eb)) return false
        val probes = eraserProbes(ax, ay, bx, by, r)
        return when (item) {
            is InkItem -> {
                val n = item.count
                if (n == 1) return anyProbe(probes) { px, py -> Geo.dist(px, py, item.pts[0], item.pts[1]) <= r + item.width / 2 }
                for (i in 1 until n) {
                    val x0 = item.pts[3 * (i - 1)]; val y0 = item.pts[3 * (i - 1) + 1]
                    val x1 = item.pts[3 * i]; val y1 = item.pts[3 * i + 1]
                    val rr = r + StrokeOutline.radius(item.width, item.pts[3 * i + 2], item.pressure)
                    if (anyProbe(probes) { px, py -> Geo.segDist(px, py, x0, y0, x1, y1) <= rr }) return true
                }
                false
            }
            is ShapeItem -> {
                val s = item.geom.sample(3f)
                val rr = r + item.width / 2
                for (i in 1 until s.size / 2) {
                    val x0 = s[2 * (i - 1)]; val y0 = s[2 * (i - 1) + 1]; val x1 = s[2 * i]; val y1 = s[2 * i + 1]
                    if (anyProbe(probes) { px, py -> Geo.segDist(px, py, x0, y0, x1, y1) <= rr }) return true
                }
                false
            }
            is TextItem, is ImageItem -> anyProbe(probes) { px, py -> item.bounds().inflate(r).contains(px, py) }
        }
    }

    private fun eraserProbes(ax: Float, ay: Float, bx: Float, by: Float, r: Float): FloatArray {
        val d = Geo.dist(ax, ay, bx, by)
        val k = max(1, (d / max(r * 0.5f, 0.5f)).toInt())
        val out = FloatArray((k + 1) * 2)
        for (i in 0..k) {
            val t = i.toFloat() / k
            out[2 * i] = ax + (bx - ax) * t; out[2 * i + 1] = ay + (by - ay) * t
        }
        return out
    }

    private inline fun anyProbe(p: FloatArray, f: (Float, Float) -> Boolean): Boolean {
        var i = 0
        while (i + 1 < p.size) { if (f(p[i], p[i + 1])) return true; i += 2 }
        return false
    }

    /**
     * Kısmi silgi: öğenin silginin değdiği kısmını çıkarır, kalan parçaları döndürür.
     * Değmiyorsa null. Metin/görsel kısmi silgiden etkilenmez.
     */
    fun partialErase(item: Item, ax: Float, ay: Float, bx: Float, by: Float, r: Float, newId: () -> Long): List<Item>? {
        val ink: InkItem = when (item) {
            is InkItem -> item
            is ShapeItem -> {
                if (!touches(item, ax, ay, bx, by, r)) return null
                val s = item.geom.sample(1.5f)
                val pts = FloatArray(s.size / 2 * 3)
                for (i in 0 until s.size / 2) { pts[3 * i] = s[2 * i]; pts[3 * i + 1] = s[2 * i + 1]; pts[3 * i + 2] = 1f }
                InkItem(item.id, InkTool.PEN, item.color, item.width, pts, pressure = false)
            }
            else -> return null
        }
        if (!touches(ink, ax, ay, bx, by, r)) return null
        val probes = eraserProbes(ax, ay, bx, by, r)
        val n = ink.count
        val keep = BooleanArray(n)
        for (i in 0 until n) {
            val x = ink.pts[3 * i]; val y = ink.pts[3 * i + 1]
            val rr = r + StrokeOutline.radius(ink.width, ink.pts[3 * i + 2], ink.pressure) * 0.5f
            keep[i] = !anyProbe(probes) { px, py -> Geo.dist(px, py, x, y) <= rr }
        }
        // Silgi iki nokta arasından geçmişse (uzun parçalar) parçayı kes
        val out = ArrayList<Item>()
        var start = -1
        fun flush(end: Int) {
            if (start >= 0 && end - start >= 2) out += InkItem(newId(), ink.tool, ink.color, ink.width, ink.pts.copyOfRange(3 * start, 3 * end), ink.pressure)
            start = -1
        }
        for (i in 0 until n) {
            if (keep[i]) {
                if (start >= 0 && i > 0) {
                    val x0 = ink.pts[3 * (i - 1)]; val y0 = ink.pts[3 * (i - 1) + 1]
                    val x1 = ink.pts[3 * i]; val y1 = ink.pts[3 * i + 1]
                    if (anyProbe(probes) { px, py -> Geo.segDist(px, py, x0, y0, x1, y1) <= r }) flush(i)
                }
                if (start < 0) start = i
            } else flush(i)
        }
        flush(n)
        return out
    }

    /** Kement seçimi: çokgenin içinde kalan öğeler. */
    fun lasso(items: List<Item>, poly: FloatArray): List<Item> {
        val pb = Box.ofPoints(poly)
        return items.filter { item ->
            if (!pb.intersects(item.bounds())) return@filter false
            val xy: FloatArray = when (item) {
                is InkItem -> Geo.xyOf(item.pts)
                is ShapeItem -> item.geom.sample(4f)
                is TextItem, is ImageItem -> { val b = item.bounds(); floatArrayOf(b.cx, b.cy) }
            }
            val n = xy.size / 2
            if (n == 0) return@filter false
            var inside = 0
            for (i in 0 until n) if (Geo.pointInPolygon(xy[2 * i], xy[2 * i + 1], poly)) inside++
            inside * 2 >= n
        }
    }

    /** Dokunulan en üstteki metin öğesi. */
    fun textAt(items: List<Item>, x: Float, y: Float): TextItem? =
        items.lastOrNull { it is TextItem && it.bounds().inflate(4f).contains(x, y) } as TextItem?
}

/** Geometri işaretleri: dik açı, açı yayı, eşit uzunluk çentikleri, paralel okları. */
object Marks {
    enum class Kind { ANGLE, TICK, PARALLEL }

    private class Seg(val ax: Float, val ay: Float, val bx: Float, val by: Float)

    private fun segments(items: List<Item>): List<Seg> {
        val out = ArrayList<Seg>()
        for (it in items) {
            val g = (it as? ShapeItem)?.geom as? Geom.Polyline ?: continue
            val n = g.n
            val m = if (g.closed) n else n - 1
            for (i in 0 until m) {
                val j = (i + 1) % n
                out += Seg(g.pts[2 * i], g.pts[2 * i + 1], g.pts[2 * j], g.pts[2 * j + 1])
            }
        }
        return out
    }

    /**
     * (x,y) dokunuşuna göre işaret geometrisi üretir.
     * ANGLE: en yakın köşede iki kenar arasına dik açı karesi ya da açı yayı.
     * TICK / PARALLEL: en yakın kenarın ortasına [count] adet çentik / ok.
     */
    fun create(items: List<Item>, x: Float, y: Float, kind: Kind, count: Int, tol: Float): List<Geom> {
        val segs = segments(items)
        if (segs.isEmpty()) return emptyList()
        if (kind == Kind.ANGLE) {
            // köşe adayları: iki kenarın buluştuğu noktalar
            class Ray(val dx: Float, val dy: Float)
            var best: Pair<Float, Float>? = null
            var bestD = tol * 1.5f
            val ends = ArrayList<FloatArray>()
            for (s in segs) { ends += floatArrayOf(s.ax, s.ay); ends += floatArrayOf(s.bx, s.by) }
            for (e in ends) {
                val d = Geo.dist(x, y, e[0], e[1])
                if (d < bestD) {
                    val c = ends.count { Geo.dist(it[0], it[1], e[0], e[1]) < 1.5f }
                    if (c >= 2) { bestD = d; best = e[0] to e[1] }
                }
            }
            val v = best ?: return emptyList()
            val rays = ArrayList<Ray>()
            for (s in segs) {
                if (Geo.dist(s.ax, s.ay, v.first, v.second) < 1.5f) {
                    val l = Geo.dist(s.ax, s.ay, s.bx, s.by); if (l > 0.1f) rays += Ray((s.bx - s.ax) / l, (s.by - s.ay) / l)
                } else if (Geo.dist(s.bx, s.by, v.first, v.second) < 1.5f) {
                    val l = Geo.dist(s.ax, s.ay, s.bx, s.by); if (l > 0.1f) rays += Ray((s.ax - s.bx) / l, (s.ay - s.by) / l)
                }
            }
            if (rays.size < 2) return emptyList()
            // dokunuş yönüne en yakın açıortaylı çifti seç
            var tdx = x - v.first; var tdy = y - v.second
            val tl = Geo.len(tdx, tdy)
            if (tl > 0.01f) { tdx /= tl; tdy /= tl }
            var pair: Pair<Ray, Ray>? = null
            var bestScore = -Float.MAX_VALUE
            for (i in rays.indices) for (j in i + 1 until rays.size) {
                val a = rays[i]; val b = rays[j]
                var bx = a.dx + b.dx; var by = a.dy + b.dy
                val bl = Geo.len(bx, by)
                if (bl < 1e-3f) continue
                bx /= bl; by /= bl
                val score = if (tl > 0.01f) bx * tdx + by * tdy else 0f
                if (score > bestScore) { bestScore = score; pair = a to b }
            }
            val (r1, r2) = pair ?: return emptyList()
            val ang = acos((r1.dx * r2.dx + r1.dy * r2.dy).coerceIn(-1f, 1f))
            return if (abs(Geo.toDeg(ang) - 90f) < 2.5f) {
                val s = 9f
                listOf(Geom.Polyline(floatArrayOf(
                    v.first + r1.dx * s, v.second + r1.dy * s,
                    v.first + (r1.dx + r2.dx) * s, v.second + (r1.dy + r2.dy) * s,
                    v.first + r2.dx * s, v.second + r2.dy * s,
                ), false))
            } else {
                val a1 = Geo.angleOf(0f, 0f, r1.dx, r1.dy)
                val a2 = Geo.angleOf(0f, 0f, r2.dx, r2.dy)
                val sweep = Geo.normAngle(a2 - a1)
                val rr = 13f
                val list = ArrayList<Geom>()
                for (k in 0 until count.coerceIn(1, 3)) list += Geom.Arc(v.first, v.second, rr + k * 3f, a1, sweep)
                list
            }
        }
        // en yakın kenar
        var seg: Seg? = null
        var bd = tol * 2
        for (s in segs) {
            val d = Geo.segDist(x, y, s.ax, s.ay, s.bx, s.by)
            if (d < bd) { bd = d; seg = s }
        }
        val s = seg ?: return emptyList()
        val l = Geo.dist(s.ax, s.ay, s.bx, s.by)
        if (l < 1f) return emptyList()
        val ux = (s.bx - s.ax) / l; val uy = (s.by - s.ay) / l
        val nx = -uy; val ny = ux
        val mx = (s.ax + s.bx) / 2f; val my = (s.ay + s.by) / 2f
        val k = count.coerceIn(1, 3)
        val out = ArrayList<Geom>()
        for (i in 0 until k) {
            val off = (i - (k - 1) / 2f) * 4f
            val cx = mx + ux * off; val cy = my + uy * off
            out += if (kind == Kind.TICK) {
                Geom.Polyline(floatArrayOf(cx - nx * 5f, cy - ny * 5f, cx + nx * 5f, cy + ny * 5f), false)
            } else {
                Geom.Polyline(floatArrayOf(
                    cx - ux * 3f + nx * 4f, cy - uy * 3f + ny * 4f,
                    cx + ux * 2f, cy + uy * 2f,
                    cx - ux * 3f - nx * 4f, cy - uy * 3f - ny * 4f,
                ), false)
            }
        }
        return out
    }
}

object Shapes {
    /** Merkez (cx,cy) ve bir köşe (vx,vy) ile düzgün çokgen. */
    fun regularPolygon(cx: Float, cy: Float, vx: Float, vy: Float, n: Int): Geom.Polyline {
        val r = Geo.dist(cx, cy, vx, vy)
        val a0 = Geo.angleOf(cx, cy, vx, vy)
        val pts = FloatArray(n * 2)
        for (i in 0 until n) {
            val a = a0 + Geo.TAU * i / n
            pts[2 * i] = cx + r * cos(a); pts[2 * i + 1] = cy + r * sin(a)
        }
        return Geom.Polyline(pts, true)
    }

    /** Ok: gövde + iki kanat. */
    fun arrow(ax: Float, ay: Float, bx: Float, by: Float, head: Float): List<Geom> {
        val a = Geo.angleOf(ax, ay, bx, by)
        val h1 = a + Geo.PI - 0.45f; val h2 = a + Geo.PI + 0.45f
        return listOf(
            Geom.Polyline(floatArrayOf(ax, ay, bx, by), false),
            Geom.Polyline(floatArrayOf(bx + head * cos(h1), by + head * sin(h1), bx, by, bx + head * cos(h2), by + head * sin(h2)), false),
        )
    }
}

/* ---------------- Geri al / Yinele ---------------- */

class Edit(val pageId: Long, val removed: List<Pair<Int, Item>>, val added: List<Item>)

class UndoStack(private val limit: Int = 300) {
    private val undo = ArrayDeque<Edit>()
    private val redo = ArrayDeque<Edit>()
    val canUndo get() = undo.isNotEmpty()
    val canRedo get() = redo.isNotEmpty()

    /** Sayfaya değişikliği uygular ve geri alma listesine kaydeder. */
    fun apply(page: Page, remove: Collection<Item>, add: List<Item>): Edit? {
        if (remove.isEmpty() && add.isEmpty()) return null
        val ids = remove.map { it.id }.toHashSet()
        val removed = ArrayList<Pair<Int, Item>>()
        page.items.forEachIndexed { i, it -> if (it.id in ids) removed += i to it }
        for (k in removed.indices.reversed()) page.items.removeAt(removed[k].first)
        page.items.addAll(add)
        val e = Edit(page.id, removed, add)
        undo.addLast(e)
        if (undo.size > limit) undo.removeFirst()
        redo.clear()
        return e
    }

    /**
     * Sayfa doğrudan değiştirildikten sonra (ör. silgi hareketi boyunca) tek bir geri alma adımı kaydeder.
     * Koşul: [before]'dan bazı öğeler çıkarılmış, yeni öğeler sona eklenmiş olmalı.
     */
    fun record(page: Page, before: List<Item>): Edit? {
        val nowIds = page.items.map { it.id }.toHashSet()
        val beforeIds = before.map { it.id }.toHashSet()
        val removed = ArrayList<Pair<Int, Item>>()
        before.forEachIndexed { i, it -> if (it.id !in nowIds) removed += i to it }
        val added = page.items.filter { it.id !in beforeIds }
        if (removed.isEmpty() && added.isEmpty()) return null
        val e = Edit(page.id, removed, added)
        undo.addLast(e)
        if (undo.size > limit) undo.removeFirst()
        redo.clear()
        return e
    }

    fun undo(nb: Notebook): Long? {
        val e = undo.removeLastOrNull() ?: return null
        val page = nb.pages.firstOrNull { it.id == e.pageId } ?: return undo(nb)
        val ids = e.added.map { it.id }.toHashSet()
        page.items.removeAll { it.id in ids }
        for ((i, item) in e.removed) page.items.add(i.coerceAtMost(page.items.size), item)
        redo.addLast(e)
        return page.id
    }

    fun redo(nb: Notebook): Long? {
        val e = redo.removeLastOrNull() ?: return null
        val page = nb.pages.firstOrNull { it.id == e.pageId } ?: return redo(nb)
        val ids = e.removed.map { it.second.id }.toHashSet()
        page.items.removeAll { it.id in ids }
        page.items.addAll(e.added)
        undo.addLast(e)
        return page.id
    }

    fun clear() { undo.clear(); redo.clear() }
}

/* ---------------- Sayfa numaraları ---------------- */

object PageNumbers {
    /** Kitaptaki sayfa numarası [book] olan sayfanın defterdeki sırası (0'dan). Yoksa null. */
    fun indexOfBookPage(nb: Notebook, book: Int): Int? {
        val pdfIndex = book - 1 + nb.pageOffset
        val i = nb.pages.indexOfFirst { (it.background as? Background.PdfPage)?.index == pdfIndex }
        return if (i >= 0) i else null
    }

    /** Sayfanın kitap numarası (PDF sayfası değilse null). */
    fun bookNumberOf(nb: Notebook, page: Page): Int? {
        val bg = page.background as? Background.PdfPage ?: return null
        return bg.index + 1 - nb.pageOffset
    }
}
