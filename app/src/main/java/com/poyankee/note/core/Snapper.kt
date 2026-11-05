package com.poyankee.note.core

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Şekilleri ızgaraya, var olan köşelere ve 15°'lik açılara yapıştırır.
 */
class Snapper(
    val lattice: Templates.Lattice?,
    /** Var olan şekillerin köşe / merkez / orta noktaları (x,y çiftleri). */
    val anchors: FloatArray,
    val tolerance: Float,
    val angleSnap: Boolean,
) {
    /** Noktayı yapıştırır. İkinci değer: bir köşeye/ızgaraya yapıştı mı. */
    fun point(x: Float, y: Float): Triple<Float, Float, Boolean> {
        var bx = x; var by = y; var bd = tolerance
        var hit = false
        var i = 0
        while (i + 1 < anchors.size) {
            val d = Geo.dist(x, y, anchors[i], anchors[i + 1])
            if (d < bd) { bd = d; bx = anchors[i]; by = anchors[i + 1]; hit = true }
            i += 2
        }
        if (hit) return Triple(bx, by, true)
        if (lattice != null) {
            val (gx, gy) = lattice.nearest(x, y)
            if (Geo.dist(x, y, gx, gy) < tolerance) return Triple(gx, gy, true)
        }
        return Triple(x, y, false)
    }

    fun snap(g: Geom): Geom = when (g) {
        is Geom.Polyline -> snapPolyline(g)
        is Geom.Circle -> {
            val (cx, cy) = point(g.cx, g.cy)
            var r = g.r
            if (lattice is Templates.Lattice.Square) {
                val rr = (r / lattice.s).roundToInt() * lattice.s
                if (rr > 0 && abs(rr - r) < tolerance) r = rr
            }
            Geom.Circle(cx, cy, r)
        }
        is Geom.Ellipse -> {
            val (cx, cy) = point(g.cx, g.cy)
            var rot = g.rot
            if (angleSnap) rot = snapAngle(rot, Geo.PI / 2f, Geo.toRad(8f))
            Geom.Ellipse(cx, cy, g.rx, g.ry, rot)
        }
        is Geom.Arc -> {
            val (cx, cy) = point(g.cx, g.cy)
            Geom.Arc(cx, cy, g.r, g.start, g.sweep)
        }
    }

    private fun snapPolyline(g: Geom.Polyline): Geom.Polyline {
        val n = g.n
        val out = g.pts.copyOf()
        val snapped = BooleanArray(n)
        for (i in 0 until n) {
            val (x, y, hit) = point(out[2 * i], out[2 * i + 1])
            out[2 * i] = x; out[2 * i + 1] = y; snapped[i] = hit
        }
        // Tek doğru parçası: uç noktası yapışmadıysa açıyı 15°'nin katına yuvarla.
        if (angleSnap && n == 2 && !g.closed && !snapped[1]) {
            val ax = out[0]; val ay = out[1]
            val len = Geo.dist(ax, ay, out[2], out[3])
            val a = Geo.angleOf(ax, ay, out[2], out[3])
            val sa = snapAngle(a, Geo.toRad(15f), Geo.toRad(4f))
            if (sa != a) { out[2] = ax + len * cos(sa); out[3] = ay + len * sin(sa) }
        }
        return Geom.Polyline(out, g.closed)
    }

    companion object {
        fun snapAngle(a: Float, step: Float, tol: Float): Float {
            val k = (a / step).roundToInt()
            val t = k * step
            return if (abs(a - t) < tol) t else a
        }

        /** Sayfadaki şekillerden yapışma noktaları: köşeler, kenar ortaları, merkezler, yay uçları. */
        fun anchorsOf(items: List<Item>): FloatArray {
            val out = ArrayList<Float>()
            for (it in items) {
                if (it !is ShapeItem) continue
                when (val g = it.geom) {
                    is Geom.Polyline -> {
                        val n = g.n
                        for (i in 0 until n) { out += g.pts[2 * i]; out += g.pts[2 * i + 1] }
                        val segs = if (g.closed) n else n - 1
                        for (i in 0 until segs) {
                            val j = (i + 1) % n
                            out += (g.pts[2 * i] + g.pts[2 * j]) / 2f
                            out += (g.pts[2 * i + 1] + g.pts[2 * j + 1]) / 2f
                        }
                    }
                    is Geom.Circle -> { out += g.cx; out += g.cy }
                    is Geom.Ellipse -> { out += g.cx; out += g.cy }
                    is Geom.Arc -> {
                        out += g.cx; out += g.cy
                        out += g.cx + g.r * cos(g.start); out += g.cy + g.r * sin(g.start)
                        out += g.cx + g.r * cos(g.start + g.sweep); out += g.cy + g.r * sin(g.start + g.sweep)
                    }
                }
            }
            return out.toFloatArray()
        }
    }
}
