package com.poyankee.note.core

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/** Hareketli geometri: şeklin kenar uzunlukları, iç açıları, alanı ve çevresi (canlı ölçüm). */
object Measure {
    /** 1 cm = 28.35 nokta (A4 = 21 cm). */
    const val PT_PER_CM = 28.3465f

    class Polygon(val sides: List<Float>, val angles: List<Float>, val area: Float, val perimeter: Float)

    /** Kapalı çokgen (x,y çiftleri). Uzunluklar cm, alan cm², açılar derece. */
    fun polygon(p: FloatArray): Polygon {
        val n = p.size / 2
        val sides = ArrayList<Float>(); val angles = ArrayList<Float>()
        var a2 = 0f
        for (i in 0 until n) {
            val j = (i + 1) % n
            sides += Geo.dist(p[2 * i], p[2 * i + 1], p[2 * j], p[2 * j + 1]) / PT_PER_CM
            a2 += p[2 * i] * p[2 * j + 1] - p[2 * j] * p[2 * i + 1]
        }
        val orient = if (a2 >= 0) 1f else -1f
        for (i in 0 until n) {
            val pr = (i + n - 1) % n; val nx = (i + 1) % n
            val ux = p[2 * pr] - p[2 * i]; val uy = p[2 * pr + 1] - p[2 * i + 1]
            val vx = p[2 * nx] - p[2 * i]; val vy = p[2 * nx + 1] - p[2 * i + 1]
            val lu = sqrt(ux * ux + uy * uy); val lv = sqrt(vx * vx + vy * vy)
            if (lu < 1e-3f || lv < 1e-3f) { angles += 0f; continue }
            val cos = ((ux * vx + uy * vy) / (lu * lv)).coerceIn(-1f, 1f)
            var deg = Math.toDegrees(acos(cos).toDouble()).toFloat()
            val cross = vx * uy - vy * ux
            if (cross * orient < 0) deg = 360f - deg
            angles += deg
        }
        return Polygon(sides, angles, abs(a2) / 2f / (PT_PER_CM * PT_PER_CM), sides.sum())
    }

    fun fmt(v: Float): String {
        val r = Math.round(v * 10f) / 10f
        return if (r == r.toInt().toFloat()) r.toInt().toString() else r.toString().replace('.', ',')
    }
}
