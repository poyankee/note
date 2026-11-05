package com.poyankee.note.core

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Sayfa şablonlarının (çizgili, kareli, koordinat...) çizim verisi. Hem ekranda hem PDF dışa aktarmada kullanılır. */
object Templates {

    const val GRID = 20f              // kareli / noktalı / koordinat aralığı (≈7 mm)
    const val LINE = 26f              // çizgili defter satır aralığı
    const val MM = 72f / 25.4f        // 1 mm

    enum class Kind { MINOR, MEDIUM, MAJOR, AXIS, MARGIN }

    class TLine(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val kind: Kind)
    class TLabel(val x: Float, val y: Float, val text: String)
    class Geometry(val lines: List<TLine>, val dots: FloatArray, val labels: List<TLabel>, val dotRadius: Float = 1.1f)

    /** Izgaranın başlangıç noktası (ızgara sayfanın ortasına hizalı). */
    fun gridOrigin(w: Float, h: Float, s: Float): Pair<Float, Float> {
        val cx = w / 2f; val cy = h / 2f
        return (cx - floor(cx / s) * s) to (cy - floor(cy / s) * s)
    }

    fun geometry(t: Template, w: Float, h: Float): Geometry {
        val lines = ArrayList<TLine>()
        val labels = ArrayList<TLabel>()
        var dots = FloatArray(0)
        when (t) {
            Template.BLANK -> {}
            Template.LINED -> {
                var y = 80f
                while (y < h - 20f) { lines += TLine(0f, y, w, y, Kind.MINOR); y += LINE }
                lines += TLine(56f, 0f, 56f, h, Kind.MARGIN)
            }
            Template.GRID -> grid(lines, w, h, GRID, 0)
            Template.DOTS -> {
                val (ox, oy) = gridOrigin(w, h, GRID)
                val list = ArrayList<Float>()
                var y = oy
                while (y <= h) { var x = ox; while (x <= w) { list += x; list += y; x += GRID }; y += GRID }
                dots = list.toFloatArray()
            }
            Template.ISOMETRIC -> {
                val list = ArrayList<Float>()
                val dy = GRID * sqrt(3f) / 2f
                var row = 0
                var y = (h / 2f) - floor((h / 2f) / dy) * dy
                while (y <= h) {
                    // satır indeksi, orta satır çift olacak şekilde
                    val ri = ((y - h / 2f) / dy).roundToInt()
                    var x = (if (abs(ri) % 2 == 1) GRID / 2f else 0f) + (w / 2f - floor((w / 2f) / GRID) * GRID)
                    while (x <= w) { list += x; list += y; x += GRID }
                    y += dy; row++
                }
                dots = list.toFloatArray()
            }
            Template.COORDINATE -> {
                grid(lines, w, h, GRID, 0)
                val (ox, oy) = gridOrigin(w, h, GRID)
                val ax = ox + floor((w / 2f - ox) / GRID) * GRID
                val ay = oy + floor((h / 2f - oy) / GRID) * GRID
                lines += TLine(0f, ay, w, ay, Kind.AXIS)
                lines += TLine(ax, 0f, ax, h, Kind.AXIS)
                // ok uçları
                lines += TLine(w - 2f, ay, w - 10f, ay - 5f, Kind.AXIS)
                lines += TLine(w - 2f, ay, w - 10f, ay + 5f, Kind.AXIS)
                lines += TLine(ax, 2f, ax - 5f, 10f, Kind.AXIS)
                lines += TLine(ax, 2f, ax + 5f, 10f, Kind.AXIS)
                labels += TLabel(w - 12f, ay - 8f, "x")
                labels += TLabel(ax + 8f, 14f, "y")
                labels += TLabel(ax - 9f, ay + 10f, "0")
                // sayılar ve çentikler
                var i = 1
                while (ax + i * GRID < w - 14f || ax - i * GRID > 6f) {
                    for (sgn in intArrayOf(1, -1)) {
                        val x = ax + sgn * i * GRID
                        if (x in 6f..(w - 14f)) {
                            lines += TLine(x, ay - 3f, x, ay + 3f, Kind.AXIS)
                            labels += TLabel(x, ay + 11f, (sgn * i).toString())
                        }
                    }
                    i++
                }
                i = 1
                while (ay + i * GRID < h - 6f || ay - i * GRID > 16f) {
                    for (sgn in intArrayOf(1, -1)) {
                        val y = ay + sgn * i * GRID
                        if (y in 16f..(h - 6f)) {
                            lines += TLine(ax - 3f, y, ax + 3f, y, Kind.AXIS)
                            labels += TLabel(ax - 9f, y + 3f, (-sgn * i).toString())
                        }
                    }
                    i++
                }
            }
            Template.MILLIMETER -> {
                var k = 0
                var x = 0f
                while (x <= w) {
                    lines += TLine(x, 0f, x, h, if (k % 10 == 0) Kind.MAJOR else if (k % 5 == 0) Kind.MEDIUM else Kind.MINOR)
                    x += MM; k++
                }
                k = 0
                var y = 0f
                while (y <= h) {
                    lines += TLine(0f, y, w, y, if (k % 10 == 0) Kind.MAJOR else if (k % 5 == 0) Kind.MEDIUM else Kind.MINOR)
                    y += MM; k++
                }
            }
        }
        return Geometry(lines, dots, labels)
    }

    private fun grid(lines: MutableList<TLine>, w: Float, h: Float, s: Float, majorEvery: Int) {
        val (ox, oy) = gridOrigin(w, h, s)
        var x = ox
        var k = 0
        while (x <= w) { lines += TLine(x, 0f, x, h, if (majorEvery > 0 && k % majorEvery == 0) Kind.MAJOR else Kind.MINOR); x += s; k++ }
        var y = oy
        k = 0
        while (y <= h) { lines += TLine(0f, y, w, y, if (majorEvery > 0 && k % majorEvery == 0) Kind.MAJOR else Kind.MINOR); y += s; k++ }
    }

    /** Şablonun yapışma kafesi; şablonda kafes yoksa null. Varsayılan [GRID] kafesi kullanılabilir. */
    fun snapLattice(t: Template?, w: Float, h: Float): Lattice? = when (t) {
        Template.GRID, Template.DOTS, Template.COORDINATE -> Lattice.Square(gridOrigin(w, h, GRID), GRID)
        Template.MILLIMETER -> Lattice.Square(0f to 0f, MM * 5f)
        Template.ISOMETRIC -> Lattice.Iso(w / 2f - floor((w / 2f) / GRID) * GRID, h / 2f, GRID)
        else -> null
    }

    sealed class Lattice {
        abstract fun nearest(x: Float, y: Float): Pair<Float, Float>

        class Square(val origin: Pair<Float, Float>, val s: Float) : Lattice() {
            override fun nearest(x: Float, y: Float): Pair<Float, Float> {
                val gx = origin.first + ((x - origin.first) / s).roundToInt() * s
                val gy = origin.second + ((y - origin.second) / s).roundToInt() * s
                return gx to gy
            }
        }

        class Iso(val ox: Float, val oy: Float, val s: Float) : Lattice() {
            override fun nearest(x: Float, y: Float): Pair<Float, Float> {
                val dy = s * sqrt(3f) / 2f
                val r0 = floor((y - oy) / dy).toInt()
                var best = x to y; var bd = Float.MAX_VALUE
                for (r in r0..r0 + 1) {
                    val shift = if (abs(r) % 2 == 1) s / 2f else 0f
                    val gx = ox + shift + ((x - ox - shift) / s).roundToInt() * s
                    val gy = oy + r * dy
                    val d = Geo.dist(x, y, gx, gy)
                    if (d < bd) { bd = d; best = gx to gy }
                }
                return best
            }
        }
    }
}
