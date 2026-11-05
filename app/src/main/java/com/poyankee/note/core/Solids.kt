package com.poyankee.note.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Hazır 3B şekiller: görünen kenarlar düz, arkada kalan kenarlar kesikli çizilir. */
object Solids {
    enum class Kind(val title: String) {
        CUBE("Küp"), PRISM("Dikdörtgenler prizması"), CYLINDER("Silindir"), CONE("Koni"), PYRAMID("Kare piramit"), SPHERE("Küre")
    }

    class Part(val geom: Geom, val hidden: Boolean)

    private fun line(ax: Float, ay: Float, bx: Float, by: Float, hidden: Boolean = false) = Part(Geom.Polyline(floatArrayOf(ax, ay, bx, by), false), hidden)

    private fun halfEllipse(cx: Float, cy: Float, rx: Float, ry: Float, front: Boolean, hidden: Boolean): Part {
        val pts = Geo.ellipsePoints(cx, cy, rx, ry, 0f, if (front) 0f else Geo.PI, Geo.PI, 2f, false)
        return Part(Geom.Polyline(pts, false), hidden)
    }

    /** (ax,ay)-(bx,by) sürükleme kutusundan şekil üretir. */
    fun create(kind: Kind, ax: Float, ay: Float, bx: Float, by: Float): List<Part> {
        val x0 = min(ax, bx); val x1 = max(ax, bx); val y0 = min(ay, by); val y1 = max(ay, by)
        val w = x1 - x0; val h = y1 - y0
        if (w < 8f || h < 8f) return emptyList()
        val out = ArrayList<Part>()
        when (kind) {
            Kind.CUBE, Kind.PRISM -> {
                val d = min(w, h) * 0.32f
                var fw = w - d; var fh = h - d
                if (kind == Kind.CUBE) { val s = min(fw, fh); fw = s; fh = s }
                // ön yüz: sol alt köşe (x0, y1)
                val fl = x0; val fr = x0 + fw; val fb = y1; val ft = y1 - fh
                val bl = fl + d; val br = fr + d; val bt = ft - d; val bb = fb - d
                out += Part(Geom.Polyline(floatArrayOf(fl, ft, fr, ft, fr, fb, fl, fb), true), false)
                out += line(bl, bt, br, bt)            // arka üst
                out += line(br, bt, br, bb)            // arka sağ
                out += line(fl, ft, bl, bt)            // sol üst bağ
                out += line(fr, ft, br, bt)            // sağ üst bağ
                out += line(fr, fb, br, bb)            // sağ alt bağ
                out += line(bl, bt, bl, bb, true)      // arka sol (gizli)
                out += line(bl, bb, br, bb, true)      // arka alt (gizli)
                out += line(fl, fb, bl, bb, true)      // sol alt bağ (gizli)
            }
            Kind.CYLINDER -> {
                val rx = w / 2f; val ry = min(rx * 0.3f, h * 0.18f)
                val cx = x0 + rx; val top = y0 + ry; val bot = y1 - ry
                out += Part(Geom.Ellipse(cx, top, rx, ry, 0f), false)
                out += line(x0, top, x0, bot); out += line(x1, top, x1, bot)
                out += halfEllipse(cx, bot, rx, ry, front = true, hidden = false)
                out += halfEllipse(cx, bot, rx, ry, front = false, hidden = true)
            }
            Kind.CONE -> {
                val rx = w / 2f; val ry = min(rx * 0.3f, h * 0.18f)
                val cx = x0 + rx; val bot = y1 - ry
                out += line(cx, y0, x0, bot); out += line(cx, y0, x1, bot)
                out += halfEllipse(cx, bot, rx, ry, front = true, hidden = false)
                out += halfEllipse(cx, bot, rx, ry, front = false, hidden = true)
                out += line(cx, y0, cx, bot, true)     // yükseklik (gizli)
            }
            Kind.PYRAMID -> {
                val d = min(w, h) * 0.3f
                val flx = x0; val fly = y1
                val frx = x1 - d; val fry = y1
                val brx = x1; val bry = y1 - d
                val blx = x0 + d; val bly = y1 - d
                val apx = (x0 + x1) / 2f; val apy = y0
                out += line(flx, fly, frx, fry); out += line(frx, fry, brx, bry)
                out += line(apx, apy, flx, fly); out += line(apx, apy, frx, fry); out += line(apx, apy, brx, bry)
                out += line(blx, bly, brx, bry, true); out += line(flx, fly, blx, bly, true); out += line(apx, apy, blx, bly, true)
            }
            Kind.SPHERE -> {
                val r = min(w, h) / 2f
                val cx = (x0 + x1) / 2f; val cy = (y0 + y1) / 2f
                out += Part(Geom.Circle(cx, cy, r), false)
                out += halfEllipse(cx, cy, r, r * 0.28f, front = true, hidden = false)
                out += halfEllipse(cx, cy, r, r * 0.28f, front = false, hidden = true)
            }
        }
        return out
    }
}

/** Çokgen köşelerine A, B, C... harfleri: her harf köşeden dışarı doğru yerleştirilir. */
object VertexLabels {
    /** Sayfada kullanılmış tek harfli etiketlerden sonra gelen harfler. */
    fun nextLetters(existing: List<Item>, count: Int): List<String> {
        val used = existing.filterIsInstance<TextItem>().map { it.text.trim() }.filter { it.length == 1 && it[0] in 'A'..'Z' }.toSet()
        val out = ArrayList<String>()
        var c = 'A'
        while (out.size < count && c <= 'Z') { if (c.toString() !in used) out += c.toString(); c++ }
        while (out.size < count) out += "P${out.size}"
        return out
    }

    /** Harflerin sol üst köşe konumları (x, y) — yazı boyutu [size] için. */
    fun positions(poly: FloatArray, size: Float): List<Pair<Float, Float>> {
        val (mx, my) = Geo.centroid(poly)
        val n = poly.size / 2
        return (0 until n).map { i ->
            val vx = poly[2 * i]; val vy = poly[2 * i + 1]
            var dx = vx - mx; var dy = vy - my
            val l = max(Geo.len(dx, dy), 0.001f)
            dx /= l; dy /= l
            val off = size * 0.9f
            val cx = vx + dx * off; val cy = vy + dy * off
            // yazının merkezini köşenin dışına koy
            (cx - size * 0.32f) to (cy - size * 0.62f)
        }
    }

    /** Doğru parçası uzunluğu etiketi için konum (kenar ortasının dışı). */
    fun midLabel(ax: Float, ay: Float, bx: Float, by: Float, size: Float): Pair<Float, Float> {
        val mx = (ax + bx) / 2f; val my = (ay + by) / 2f
        val l = max(Geo.dist(ax, ay, bx, by), 0.001f)
        var nx = -(by - ay) / l; var ny = (bx - ax) / l
        if (ny > 0 || (abs(ny) < 1e-3f && nx > 0)) { nx = -nx; ny = -ny } // yukarı tarafa
        return (mx + nx * size * 0.9f - size) to (my + ny * size * 0.9f - size * 0.6f)
    }
}
