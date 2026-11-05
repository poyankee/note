package com.poyankee.note.core

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Benzerlik dönüşümü: önce [scale] ve [angle] ile (ox, oy) etrafında ölçekle/döndür, sonra (dx, dy) kadar kaydır.
 * Çember çember olarak kalır; bu yüzden seçim aracı yalnızca bu dönüşümü kullanır.
 */
data class Similarity(
    val dx: Float = 0f,
    val dy: Float = 0f,
    val scale: Float = 1f,
    val angle: Float = 0f,
    val ox: Float = 0f,
    val oy: Float = 0f,
) {
    private val c = cos(angle) * scale
    private val s = sin(angle) * scale
    fun mapX(x: Float, y: Float) = ox + (x - ox) * c - (y - oy) * s + dx
    fun mapY(x: Float, y: Float) = oy + (x - ox) * s + (y - oy) * c + dy
    fun mapXY(xy: FloatArray): FloatArray {
        val out = FloatArray(xy.size)
        var i = 0
        while (i + 1 < xy.size) {
            out[i] = mapX(xy[i], xy[i + 1]); out[i + 1] = mapY(xy[i], xy[i + 1]); i += 2
        }
        return out
    }
    val isIdentity get() = dx == 0f && dy == 0f && scale == 1f && angle == 0f

    companion object {
        fun translate(dx: Float, dy: Float) = Similarity(dx = dx, dy = dy)
    }
}

object Geo {
    const val PI = Math.PI.toFloat()
    const val TAU = (Math.PI * 2).toFloat()

    fun dist(ax: Float, ay: Float, bx: Float, by: Float) = hypot(ax - bx, ay - by)

    /** (px,py) noktasının [a,b] doğru parçasına uzaklığı. */
    fun segDist(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val vx = bx - ax; val vy = by - ay
        val l2 = vx * vx + vy * vy
        if (l2 < 1e-9f) return dist(px, py, ax, ay)
        val t = (((px - ax) * vx + (py - ay) * vy) / l2).coerceIn(0f, 1f)
        return dist(px, py, ax + t * vx, ay + t * vy)
    }

    /** Noktanın, [a,b] doğrusu üzerindeki izdüşümü (sonsuz doğru). */
    fun projectOnLine(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Pair<Float, Float> {
        val vx = bx - ax; val vy = by - ay
        val l2 = vx * vx + vy * vy
        if (l2 < 1e-9f) return ax to ay
        val t = ((px - ax) * vx + (py - ay) * vy) / l2
        return (ax + t * vx) to (ay + t * vy)
    }

    /** Çift-tek kuralı ile çokgen içinde mi? [poly] = x,y çiftleri. */
    fun pointInPolygon(x: Float, y: Float, poly: FloatArray): Boolean {
        val n = poly.size / 2
        if (n < 3) return false
        var inside = false
        var j = n - 1
        for (i in 0 until n) {
            val xi = poly[2 * i]; val yi = poly[2 * i + 1]
            val xj = poly[2 * j]; val yj = poly[2 * j + 1]
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) inside = !inside
            j = i
        }
        return inside
    }

    fun ellipsePoints(
        cx: Float, cy: Float, rx: Float, ry: Float, rot: Float,
        start: Float, sweep: Float, step: Float, closed: Boolean,
    ): FloatArray {
        val circumference = abs(sweep) * max(rx, ry)
        val n = max(8, min(720, (circumference / max(step, 0.5f)).toInt()))
        val cr = cos(rot); val sr = sin(rot)
        val count = if (closed) n + 1 else n + 1
        val out = FloatArray(count * 2)
        for (i in 0 until count) {
            val a = start + sweep * i / n
            val ex = rx * cos(a); val ey = ry * sin(a)
            out[2 * i] = cx + ex * cr - ey * sr
            out[2 * i + 1] = cy + ex * sr + ey * cr
        }
        return out
    }

    /** Ramer–Douglas–Peucker sadeleştirmesi. Girdi/çıktı x,y çiftleri. */
    fun simplify(xy: FloatArray, eps: Float): FloatArray {
        val n = xy.size / 2
        if (n < 3) return xy.copyOf()
        val keep = BooleanArray(n)
        keep[0] = true; keep[n - 1] = true
        val stack = ArrayDeque<IntArray>()
        stack.addLast(intArrayOf(0, n - 1))
        while (stack.isNotEmpty()) {
            val (s, e) = stack.removeLast().let { it[0] to it[1] }
            var maxD = 0f; var idx = -1
            for (i in s + 1 until e) {
                val d = segDist(xy[2 * i], xy[2 * i + 1], xy[2 * s], xy[2 * s + 1], xy[2 * e], xy[2 * e + 1])
                if (d > maxD) { maxD = d; idx = i }
            }
            if (idx >= 0 && maxD > eps) {
                keep[idx] = true
                stack.addLast(intArrayOf(s, idx)); stack.addLast(intArrayOf(idx, e))
            }
        }
        val out = ArrayList<Float>()
        for (i in 0 until n) if (keep[i]) { out += xy[2 * i]; out += xy[2 * i + 1] }
        return out.toFloatArray()
    }

    fun pathLength(xy: FloatArray): Float {
        var len = 0f
        var i = 2
        while (i + 1 < xy.size) { len += dist(xy[i - 2], xy[i - 1], xy[i], xy[i + 1]); i += 2 }
        return len
    }

    /** Üçlü (x,y,p) diziden x,y çiftleri. */
    fun xyOf(pts: FloatArray): FloatArray {
        val n = pts.size / 3
        val out = FloatArray(n * 2)
        for (i in 0 until n) { out[2 * i] = pts[3 * i]; out[2 * i + 1] = pts[3 * i + 1] }
        return out
    }

    fun angleOf(ax: Float, ay: Float, bx: Float, by: Float) = atan2(by - ay, bx - ax)

    /** Açıyı (-π, π] aralığına getirir. */
    fun normAngle(a: Float): Float {
        var r = a % TAU
        if (r > PI) r -= TAU
        if (r <= -PI) r += TAU
        return r
    }

    fun toDeg(rad: Float) = rad * 180f / PI
    fun toRad(deg: Float) = deg * PI / 180f

    fun centroid(xy: FloatArray): Pair<Float, Float> {
        val n = xy.size / 2
        if (n == 0) return 0f to 0f
        var sx = 0f; var sy = 0f
        for (i in 0 until n) { sx += xy[2 * i]; sy += xy[2 * i + 1] }
        return sx / n to sy / n
    }

    fun len(x: Float, y: Float) = sqrt(x * x + y * y)
}

/** Benzersiz öğe kimlikleri. */
object Ids {
    private var last = 0L
    @Synchronized
    fun next(): Long {
        val now = System.currentTimeMillis() * 1000
        last = if (now > last) now else last + 1
        return last
    }
}
