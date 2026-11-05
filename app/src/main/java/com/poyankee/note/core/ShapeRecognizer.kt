package com.poyankee.note.core

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * El çizimini düzgün şekle çevirir: düz çizgi, açı (kırık çizgi), yay, çember, elips, üçgen, dörtgen/dikdörtgen, çokgen.
 * Tanınamazsa null döner (çizim olduğu gibi kalır).
 */
object ShapeRecognizer {

    fun recognize(pts: FloatArray): Geom? {
        val raw = Geo.xyOf(pts)
        if (raw.size < 4) return null
        val box = Box.ofPoints(raw)
        val size = max(box.width, box.height)
        if (size < 6f) return null
        val len = Geo.pathLength(raw)
        val xy = resample(raw, max(size / 64f, 0.5f))
        val n = xy.size / 2
        if (n < 4) return null

        val sx = xy[0]; val sy = xy[1]; val ex = xy[2 * (n - 1)]; val ey = xy[2 * (n - 1) + 1]
        val gap = Geo.dist(sx, sy, ex, ey)
        val chord = gap

        // 1) Düz çizgi
        if (chord / len > 0.94f) {
            var maxDev = 0f
            for (i in 0 until n) maxDev = max(maxDev, Geo.segDist(xy[2 * i], xy[2 * i + 1], sx, sy, ex, ey))
            if (maxDev < 0.06f * chord + 1f) return Geom.Polyline(floatArrayOf(sx, sy, ex, ey), false)
        }

        val closed = gap < 0.22f * size && len > 2.0f * size

        if (!closed) {
            // 2) Açı / kırık çizgi (2-3 kenar)
            val simp = Geo.simplify(xy, 0.06f * size)
            val k = simp.size / 2
            if (k in 3..4) {
                val err = polylineError(xy, simp, false) / size
                if (err < 0.03f) return Geom.Polyline(simp, false)
            }
            // 3) Yay
            val c = fitCircle(xy) ?: return null
            val cerr = circleError(xy, c) / size
            if (cerr < 0.035f && c.third < size * 4) {
                val start = atan2(sy - c.second, sx - c.first)
                var sweep = 0f
                var prev = start
                for (i in 1 until n) {
                    val a = atan2(xy[2 * i + 1] - c.second, xy[2 * i] - c.first)
                    sweep += Geo.normAngle(a - prev)
                    prev = a
                }
                if (abs(sweep) > Geo.TAU * 0.92f) return Geom.Circle(c.first, c.second, c.third)
                if (abs(sweep) > Geo.toRad(25f)) return Geom.Arc(c.first, c.second, c.third, start, sweep)
            }
            return null
        }

        // Kapalı şekiller
        val circle = fitCircle(xy)
        val cErr = if (circle != null) circleError(xy, circle) / size else Float.MAX_VALUE

        val poly = closedPolygon(xy, size)
        val k = poly.size / 2
        val pErr = if (k in 3..8) polylineError(xy, poly, true) / size else Float.MAX_VALUE

        if (k in 3..6 && pErr < 0.028f && pErr < cErr) return regularize(poly)
        if (cErr < 0.045f && circle != null) return Geom.Circle(circle.first, circle.second, circle.third)
        val ell = fitEllipse(xy)
        if (ell != null) {
            val eErr = ellipseError(xy, ell) / size
            if (eErr < 0.04f) {
                return if (abs(ell.rx - ell.ry) < 0.08f * max(ell.rx, ell.ry))
                    Geom.Circle(ell.cx, ell.cy, (ell.rx + ell.ry) / 2f)
                else Geom.Ellipse(ell.cx, ell.cy, ell.rx, ell.ry, ell.rot)
            }
        }
        if (k in 3..8 && pErr < 0.045f) return regularize(poly)
        return null
    }

    /* ---------- yardımcılar ---------- */

    /** Eşit aralıklı yeniden örnekleme. */
    fun resample(xy: FloatArray, step: Float): FloatArray {
        val out = ArrayList<Float>()
        val n = xy.size / 2
        out += xy[0]; out += xy[1]
        var px = xy[0]; var py = xy[1]
        var acc = 0f
        for (i in 1 until n) {
            var ax = px; var ay = py
            val bx = xy[2 * i]; val by = xy[2 * i + 1]
            var d = Geo.dist(ax, ay, bx, by)
            while (acc + d >= step && d > 0f) {
                val t = (step - acc) / d
                val nx = ax + (bx - ax) * t; val ny = ay + (by - ay) * t
                out += nx; out += ny
                ax = nx; ay = ny
                d = Geo.dist(ax, ay, bx, by)
                acc = 0f
            }
            acc += d
            px = bx; py = by
        }
        if (out[out.size - 2] != xy[2 * (n - 1)] || out[out.size - 1] != xy[2 * (n - 1) + 1]) {
            out += xy[2 * (n - 1)]; out += xy[2 * (n - 1) + 1]
        }
        return out.toFloatArray()
    }

    /** Kasa yöntemiyle çember uydurma: (cx, cy, r). */
    fun fitCircle(xy: FloatArray): Triple<Float, Float, Float>? {
        val n = xy.size / 2
        if (n < 3) return null
        // x² + y² + D x + E y + F = 0  → en küçük kareler
        var sxx = 0.0; var sxy = 0.0; var syy = 0.0; var sx = 0.0; var sy = 0.0
        var sxz = 0.0; var syz = 0.0; var sz = 0.0
        val (mx, my) = Geo.centroid(xy)
        for (i in 0 until n) {
            val x = (xy[2 * i] - mx).toDouble(); val y = (xy[2 * i + 1] - my).toDouble()
            val z = x * x + y * y
            sxx += x * x; sxy += x * y; syy += y * y; sx += x; sy += y
            sxz += x * z; syz += y * z; sz += z
        }
        // [sxx sxy sx][D]   [-sxz]
        // [sxy syy sy][E] = [-syz]
        // [sx  sy  n ][F]   [-sz ]
        val a = arrayOf(
            doubleArrayOf(sxx, sxy, sx, -sxz),
            doubleArrayOf(sxy, syy, sy, -syz),
            doubleArrayOf(sx, sy, n.toDouble(), -sz),
        )
        val sol = solve3(a) ?: return null
        val cx = -sol[0] / 2; val cy = -sol[1] / 2
        val r2 = cx * cx + cy * cy - sol[2]
        if (r2 <= 0) return null
        return Triple((cx + mx).toFloat(), (cy + my).toFloat(), sqrt(r2).toFloat())
    }

    private fun solve3(m: Array<DoubleArray>): DoubleArray? {
        for (col in 0 until 3) {
            var piv = col
            for (r in col + 1 until 3) if (abs(m[r][col]) > abs(m[piv][col])) piv = r
            if (abs(m[piv][col]) < 1e-12) return null
            val tmp = m[col]; m[col] = m[piv]; m[piv] = tmp
            for (r in 0 until 3) {
                if (r == col) continue
                val f = m[r][col] / m[col][col]
                for (c in col..3) m[r][c] -= f * m[col][c]
            }
        }
        return DoubleArray(3) { m[it][3] / m[it][it] }
    }

    fun circleError(xy: FloatArray, c: Triple<Float, Float, Float>): Float {
        val n = xy.size / 2
        var s = 0f
        for (i in 0 until n) s += abs(Geo.dist(xy[2 * i], xy[2 * i + 1], c.first, c.second) - c.third)
        return s / n
    }

    class EllipseFit(val cx: Float, val cy: Float, val rx: Float, val ry: Float, val rot: Float)

    /** Ana eksen analizi ile elips: eksenler kovaryanstan, yarıçaplar uç değerlerden. */
    fun fitEllipse(xy: FloatArray): EllipseFit? {
        val n = xy.size / 2
        if (n < 5) return null
        val (mx, my) = Geo.centroid(xy)
        var cxx = 0f; var cxy = 0f; var cyy = 0f
        for (i in 0 until n) {
            val x = xy[2 * i] - mx; val y = xy[2 * i + 1] - my
            cxx += x * x; cxy += x * y; cyy += y * y
        }
        val rot = 0.5f * atan2(2 * cxy, cxx - cyy)
        val c = cos(rot); val s = sin(rot)
        var minU = Float.MAX_VALUE; var maxU = -Float.MAX_VALUE
        var minV = Float.MAX_VALUE; var maxV = -Float.MAX_VALUE
        for (i in 0 until n) {
            val x = xy[2 * i] - mx; val y = xy[2 * i + 1] - my
            val u = x * c + y * s; val v = -x * s + y * c
            minU = min(minU, u); maxU = max(maxU, u); minV = min(minV, v); maxV = max(maxV, v)
        }
        val rx = (maxU - minU) / 2f; val ry = (maxV - minV) / 2f
        if (rx < 1f || ry < 1f) return null
        val ocx = mx + ((maxU + minU) / 2f) * c - ((maxV + minV) / 2f) * s
        val ocy = my + ((maxU + minU) / 2f) * s + ((maxV + minV) / 2f) * c
        return EllipseFit(ocx, ocy, rx, ry, rot)
    }

    fun ellipseError(xy: FloatArray, e: EllipseFit): Float {
        val n = xy.size / 2
        val c = cos(e.rot); val s = sin(e.rot)
        var sum = 0f
        for (i in 0 until n) {
            val x = xy[2 * i] - e.cx; val y = xy[2 * i + 1] - e.cy
            val u = x * c + y * s; val v = -x * s + y * c
            val q = sqrt((u / e.rx) * (u / e.rx) + (v / e.ry) * (v / e.ry))
            sum += abs(q - 1f) * min(e.rx, e.ry)
        }
        return sum / n
    }

    /** Kapalı yol için köşe noktaları (son nokta ilkini tekrar etmez). */
    fun closedPolygon(xy: FloatArray, size: Float): FloatArray {
        // Başlangıç noktası köşe olmayabilir; en uzak noktadan başlatarak döndür.
        val n = xy.size / 2
        val (mx, my) = Geo.centroid(xy)
        var far = 0; var fd = -1f
        for (i in 0 until n) {
            val d = Geo.dist(xy[2 * i], xy[2 * i + 1], mx, my)
            if (d > fd) { fd = d; far = i }
        }
        val rot = FloatArray((n + 1) * 2)
        for (i in 0..n) {
            val j = (far + i) % n
            rot[2 * i] = xy[2 * j]; rot[2 * i + 1] = xy[2 * j + 1]
        }
        var simp = Geo.simplify(rot, 0.055f * size)
        // son nokta = ilk nokta → at
        var k = simp.size / 2
        if (k >= 2 && Geo.dist(simp[0], simp[1], simp[2 * (k - 1)], simp[2 * (k - 1) + 1]) < 0.2f * size) {
            simp = simp.copyOf(simp.size - 2); k--
        }
        // Neredeyse düz (180°) köşeleri birleştir
        var changed = true
        while (changed && k > 3) {
            changed = false
            for (i in 0 until k) {
                val p = (i - 1 + k) % k; val q = (i + 1) % k
                val a1 = Geo.angleOf(simp[2 * p], simp[2 * p + 1], simp[2 * i], simp[2 * i + 1])
                val a2 = Geo.angleOf(simp[2 * i], simp[2 * i + 1], simp[2 * q], simp[2 * q + 1])
                if (abs(Geo.normAngle(a2 - a1)) < Geo.toRad(22f)) {
                    val out = FloatArray(simp.size - 2)
                    var w = 0
                    for (j in 0 until k) if (j != i) { out[w++] = simp[2 * j]; out[w++] = simp[2 * j + 1] }
                    simp = out; k--; changed = true
                    break
                }
            }
        }
        return simp
    }

    fun polylineError(xy: FloatArray, poly: FloatArray, closed: Boolean): Float {
        val k = poly.size / 2
        val segs = if (closed) k else k - 1
        val n = xy.size / 2
        var sum = 0f
        for (i in 0 until n) {
            var best = Float.MAX_VALUE
            for (s in 0 until segs) {
                val t = (s + 1) % k
                best = min(best, Geo.segDist(xy[2 * i], xy[2 * i + 1], poly[2 * s], poly[2 * s + 1], poly[2 * t], poly[2 * t + 1]))
            }
            sum += best
        }
        return sum / n
    }

    /** Dörtgen dik açılara yakınsa düzgün dikdörtgene çevirir; eksene yakınsa eksene hizalar. */
    fun regularize(poly: FloatArray): Geom {
        val k = poly.size / 2
        if (k == 4) {
            var allRight = true
            for (i in 0 until 4) {
                val p = (i + 3) % 4; val q = (i + 1) % 4
                val a1 = Geo.angleOf(poly[2 * i], poly[2 * i + 1], poly[2 * p], poly[2 * p + 1])
                val a2 = Geo.angleOf(poly[2 * i], poly[2 * i + 1], poly[2 * q], poly[2 * q + 1])
                val inner = abs(Geo.toDeg(Geo.normAngle(a2 - a1)))
                if (abs(inner - 90f) > 14f) allRight = false
            }
            if (allRight) {
                // Ortalama yön (mod 90°)
                var sx = 0f; var sy = 0f
                for (i in 0 until 4) {
                    val q = (i + 1) % 4
                    val a = Geo.angleOf(poly[2 * i], poly[2 * i + 1], poly[2 * q], poly[2 * q + 1])
                    sx += cos(4 * a); sy += sin(4 * a)
                }
                var ang = atan2(sy, sx) / 4f
                if (abs(Geo.toDeg(ang)) < 7f) ang = 0f
                val c = cos(ang); val s = sin(ang)
                val (mx, my) = Geo.centroid(poly)
                var minU = Float.MAX_VALUE; var maxU = -Float.MAX_VALUE
                var minV = Float.MAX_VALUE; var maxV = -Float.MAX_VALUE
                for (i in 0 until 4) {
                    val x = poly[2 * i] - mx; val y = poly[2 * i + 1] - my
                    val u = x * c + y * s; val v = -x * s + y * c
                    minU = min(minU, u); maxU = max(maxU, u); minV = min(minV, v); maxV = max(maxV, v)
                }
                val us = floatArrayOf(minU, maxU, maxU, minU); val vs = floatArrayOf(minV, minV, maxV, maxV)
                val out = FloatArray(8)
                for (i in 0 until 4) {
                    out[2 * i] = mx + us[i] * c - vs[i] * s
                    out[2 * i + 1] = my + us[i] * s + vs[i] * c
                }
                return Geom.Polyline(out, true)
            }
        }
        return Geom.Polyline(poly, true)
    }
}
