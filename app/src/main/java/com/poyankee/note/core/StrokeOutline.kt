package com.poyankee.note.core

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Basınca duyarlı vuruşun dış hattını (doldurulacak çokgen) hesaplar.
 * Çıktı: x,y çiftleri; sol kenar ileri + uç yuvarlağı + sağ kenar geri + başlangıç yuvarlağı.
 */
object StrokeOutline {

    /** Basınca göre yarıçap. Basınç kapalıysa sabit kalınlık. */
    fun radius(width: Float, p: Float, pressure: Boolean): Float =
        if (!pressure) width / 2f else width / 2f * (0.3f + 0.85f * p.coerceIn(0f, 1f))

    /**
     * Ham noktaları hafifçe yumuşatır (hareketli ortalama). [level] 0 = kapalı, 1..5.
     * Uç noktalar korunur, böylece çizgi kaleme "yapışık" kalır.
     */
    fun smooth(pts: FloatArray, level: Int): FloatArray {
        val n = pts.size / 3
        if (level <= 0 || n < 3) return pts
        var cur = pts
        repeat(level) {
            val out = cur.copyOf()
            for (i in 1 until n - 1) {
                for (k in 0..2) {
                    out[3 * i + k] = (cur[3 * (i - 1) + k] + 2f * cur[3 * i + k] + cur[3 * (i + 1) + k]) / 4f
                }
            }
            cur = out
        }
        return cur
    }

    /** Çok yakın noktaları atar (gürültü ve gereksiz veri). */
    fun dedupe(pts: FloatArray, minDist: Float): FloatArray {
        val n = pts.size / 3
        if (n < 3) return pts
        val out = ArrayList<Float>(pts.size)
        out += pts[0]; out += pts[1]; out += pts[2]
        var lx = pts[0]; var ly = pts[1]
        for (i in 1 until n) {
            val x = pts[3 * i]; val y = pts[3 * i + 1]
            if (i == n - 1 || Geo.dist(x, y, lx, ly) >= minDist) {
                out += x; out += y; out += pts[3 * i + 2]
                lx = x; ly = y
            }
        }
        return out.toFloatArray()
    }

    fun outline(pts: FloatArray, width: Float, pressure: Boolean): FloatArray {
        val n = pts.size / 3
        if (n == 0) return FloatArray(0)
        if (n == 1 || Geo.pathLength(Geo.xyOf(pts)) < 0.01f) {
            val r = max(radius(width, pts[2], pressure), 0.3f)
            return Geo.ellipsePoints(pts[0], pts[1], r, r, 0f, 0f, Geo.TAU, r / 2f, true)
        }
        val left = FloatArray(n * 2)
        val right = FloatArray(n * 2)
        val radii = FloatArray(n)
        for (i in 0 until n) {
            // Teğet: komşu noktaların farkı
            val a = max(0, i - 1); val b = minOf(n - 1, i + 1)
            var tx = pts[3 * b] - pts[3 * a]
            var ty = pts[3 * b + 1] - pts[3 * a + 1]
            var l = sqrt(tx * tx + ty * ty)
            if (l < 1e-6f) {
                // Aynı noktalar: en yakın farklı noktayı bul
                tx = pts[3 * (n - 1)] - pts[0]; ty = pts[3 * (n - 1) + 1] - pts[1]
                l = max(sqrt(tx * tx + ty * ty), 1e-6f)
            }
            tx /= l; ty /= l
            var r = radius(width, pts[3 * i + 2], pressure)
            // Uçlarda hafif incelme olmasın diye basınç kapalıyken sabit.
            r = max(r, 0.25f)
            radii[i] = r
            val nx = -ty * r; val ny = tx * r
            left[2 * i] = pts[3 * i] + nx; left[2 * i + 1] = pts[3 * i + 1] + ny
            right[2 * i] = pts[3 * i] - nx; right[2 * i + 1] = pts[3 * i + 1] - ny
        }
        val out = ArrayList<Float>(n * 4 + 64)
        for (i in 0 until n) { out += left[2 * i]; out += left[2 * i + 1] }
        // Bitiş yuvarlağı
        cap(out, pts[3 * (n - 1)], pts[3 * (n - 1) + 1], radii[n - 1],
            Geo.angleOf(pts[3 * (n - 1)], pts[3 * (n - 1) + 1], left[2 * (n - 1)], left[2 * (n - 1) + 1]))
        for (i in n - 1 downTo 0) { out += right[2 * i]; out += right[2 * i + 1] }
        cap(out, pts[0], pts[1], radii[0], Geo.angleOf(pts[0], pts[1], right[0], right[1]))
        return out.toFloatArray()
    }

    /** Merkez etrafında [from] açısından başlayarak dışa doğru yarım daire ekler. */
    private fun cap(out: MutableList<Float>, cx: Float, cy: Float, r: Float, from: Float) {
        val steps = 8
        for (k in 1 until steps) {
            val a = from - Geo.PI * k / steps
            out += cx + r * cos(a); out += cy + r * sin(a)
        }
    }
}
