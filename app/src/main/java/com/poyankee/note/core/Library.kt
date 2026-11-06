package com.poyankee.note.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Hazır kütüphane: tek dokunuşla sayfaya eklenen, düzenlenebilir vektör çizimler.
 * Her çizim (0,0) çevresinde nokta (pt) biriminde üretilir; eklerken ekranın ortasına taşınır.
 * y aşağı doğru artar (ekran düzeni).
 */
object Library {
    class Entry(val id: String, val category: String, val title: String, val build: () -> List<Item>)

    const val INK = 0xFF1A1A1A.toInt()
    const val BLUE = 0xFF1565C0.toInt()
    const val RED = 0xFFD32F2F.toInt()
    const val GREEN = 0xFF2E7D32.toInt()
    const val ORANGE = 0xFFF57C00.toInt()
    const val GRAY = 0xFF667085.toInt()

    val categories = listOf("Trigonometri", "Geometri", "Analitik", "Katı cisimler", "Olasılık ve istatistik", "Cebir karoları", "Formül kartları", "Genel")

    /* ---------------- yardımcılar ---------------- */

    private class B {
        val items = ArrayList<Item>()
        fun line(ax: Float, ay: Float, bx: Float, by: Float, color: Int = INK, w: Float = 1.4f, dashed: Boolean = false) {
            items += ShapeItem(Ids.next(), Geom.Polyline(floatArrayOf(ax, ay, bx, by), false), color, w, dashed)
        }
        fun poly(vararg p: Float, closed: Boolean = true, color: Int = INK, w: Float = 1.6f, fill: Int = 0, dashed: Boolean = false) {
            items += ShapeItem(Ids.next(), Geom.Polyline(p.copyOf(), closed), color, w, dashed, fill)
        }
        fun polyArr(p: FloatArray, closed: Boolean, color: Int = INK, w: Float = 1.6f, fill: Int = 0, dashed: Boolean = false) {
            items += ShapeItem(Ids.next(), Geom.Polyline(p, closed), color, w, dashed, fill)
        }
        fun circle(cx: Float, cy: Float, r: Float, color: Int = INK, w: Float = 1.6f, fill: Int = 0, dashed: Boolean = false) {
            items += ShapeItem(Ids.next(), Geom.Circle(cx, cy, r), color, w, dashed, fill)
        }
        fun dot(cx: Float, cy: Float, color: Int = INK, r: Float = 2.4f) = circle(cx, cy, r, color, 1f, color)
        fun arc(cx: Float, cy: Float, r: Float, start: Float, sweep: Float, color: Int = INK, w: Float = 1.2f) {
            items += ShapeItem(Ids.next(), Geom.Arc(cx, cy, r, start, sweep), color, w)
        }
        /** (x,y) sol üst köşe. */
        fun text(x: Float, y: Float, s: String, size: Float = 12f, color: Int = INK, bg: Int = 0) {
            items += TextItem(Ids.next(), x, y, s, size, color, 0f, bg)
        }
        /** (cx,cy) metnin ortası. */
        fun textC(cx: Float, cy: Float, s: String, size: Float = 12f, color: Int = INK) {
            val lines = s.split('\n')
            val w = (lines.maxOfOrNull { it.length } ?: 1) * size * 0.6f
            val h = lines.size * size * 1.25f
            text(cx - w / 2f, cy - h / 2f, s, size, color)
        }
        fun arrow(ax: Float, ay: Float, bx: Float, by: Float, color: Int = INK, w: Float = 1.4f, head: Float = 8f) {
            line(ax, ay, bx, by, color, w)
            val a = Geo.angleOf(ax, ay, bx, by)
            val l1x = bx - head * cos(a - 0.45f); val l1y = by - head * sin(a - 0.45f)
            val l2x = bx - head * cos(a + 0.45f); val l2y = by - head * sin(a + 0.45f)
            poly(l1x, l1y, bx, by, l2x, l2y, closed = false, color = color, w = w)
        }
        /** (x,y) köşesindeki dik açı işareti; (ux,uy) ve (vx,vy) kenar yönleri (birim). */
        fun rightMark(x: Float, y: Float, ux: Float, uy: Float, vx: Float, vy: Float, s: Float = 10f, color: Int = INK) {
            poly(x + ux * s, y + uy * s, x + ux * s + vx * s, y + uy * s + vy * s, x + vx * s, y + vy * s, closed = false, color = color, w = 1.1f)
        }
        /** A köşesinde AB ile AC arasındaki açı yayı (küçük açı). */
        fun angleArc(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float, r: Float = 18f, color: Int = INK, label: String? = null, labelR: Float = r + 12f) {
            val a1 = Geo.angleOf(ax, ay, bx, by); val a2 = Geo.angleOf(ax, ay, cx, cy)
            var sweep = Geo.normAngle(a2 - a1)
            val start = if (sweep < 0) { sweep = -sweep; a2 } else a1
            arc(ax, ay, r, start, sweep, color)
            if (label != null) {
                val mid = start + sweep / 2f
                textC(ax + labelR * cos(mid), ay + labelR * sin(mid), label, 11f, color)
            }
        }
        /** Eşit uzunluk çentiği. */
        fun tick(ax: Float, ay: Float, bx: Float, by: Float, count: Int = 1, color: Int = INK) {
            val mx = (ax + bx) / 2; val my = (ay + by) / 2
            val len = Geo.dist(ax, ay, bx, by).coerceAtLeast(1f)
            val ux = (bx - ax) / len; val uy = (by - ay) / len
            val nx = -uy; val ny = ux
            for (i in 0 until count) {
                val o = (i - (count - 1) / 2f) * 4f
                val px = mx + ux * o; val py = my + uy * o
                line(px - nx * 6f, py - ny * 6f, px + nx * 6f, py + ny * 6f, color, 1.2f)
            }
        }
        fun rect(x: Float, y: Float, w: Float, h: Float, color: Int = INK, fill: Int = 0, wd: Float = 1.4f) =
            poly(x, y, x + w, y, x + w, y + h, x, y + h, color = color, fill = fill, w = wd)
        /** Doğru parçasının dışına (verilen tarafa) etiket. */
        fun sideLabel(ax: Float, ay: Float, bx: Float, by: Float, s: String, out: Float = 14f, color: Int = INK) {
            val mx = (ax + bx) / 2; val my = (ay + by) / 2
            val len = Geo.dist(ax, ay, bx, by).coerceAtLeast(1f)
            val nx = (by - ay) / len; val ny = -(bx - ax) / len
            textC(mx + nx * out, my + ny * out, s, 12f, color)
        }
        fun table(x: Float, y: Float, cols: List<Float>, rowH: Float, cells: List<List<String>>, size: Float = 11f) {
            val w = cols.sum(); val h = rowH * cells.size
            rect(x, y, w, h, INK, 0, 1.2f)
            var cx = x
            for (i in 0 until cols.size - 1) { cx += cols[i]; line(cx, y, cx, y + h, INK, 1f) }
            for (r in 1 until cells.size) line(x, y + r * rowH, x + w, y + r * rowH, INK, 1f)
            for ((r, row) in cells.withIndex()) {
                var left = x
                for ((c, s) in row.withIndex()) {
                    if (c < cols.size) {
                        if (s.isNotEmpty()) textC(left + cols[c] / 2, y + r * rowH + rowH / 2, s, size, if (r == 0 || c == 0) BLUE else INK)
                        left += cols[c]
                    }
                }
            }
        }
        fun axes(xMin: Float, xMax: Float, yMin: Float, yMax: Float, unit: Float, numbers: Boolean = true, xs: String = "x", ys: String = "y") {
            arrow(xMin * unit - 10f, 0f, xMax * unit + 14f, 0f, INK, 1.2f)
            arrow(0f, -yMin * unit + 10f, 0f, -yMax * unit - 14f, INK, 1.2f)
            for (i in xMin.toInt()..xMax.toInt()) if (i != 0) {
                line(i * unit, -3f, i * unit, 3f, INK, 1f)
                if (numbers) textC(i * unit, 12f, "$i", 9f, GRAY)
            }
            for (i in yMin.toInt()..yMax.toInt()) if (i != 0) {
                line(-3f, -i * unit, 3f, -i * unit, INK, 1f)
                if (numbers) textC(-12f, -i * unit, "$i", 9f, GRAY)
            }
            textC(xMax * unit + 18f, -12f, xs, 12f)
            textC(12f, -yMax * unit - 18f, ys, 12f)
            textC(-9f, 10f, "O", 10f, GRAY)
        }
        fun card(title: String, body: String, color: Int, fill: Int) {
            val lines = body.split('\n')
            val w = maxOf(title.length * 15f * 0.6f, (lines.maxOfOrNull { it.length } ?: 1) * 13f * 0.6f) + 28f
            val h = 34f + lines.size * 13f * 1.25f + 14f
            poly(0f, 0f, w, 0f, w, h, 0f, h, color = color, fill = fill, w = 1.6f)
            text(14f, 10f, title, 15f, color)
            line(14f, 30f, w - 14f, 30f, color, 1f)
            text(14f, 38f, body, 13f, INK)
        }
    }

    private fun b(block: B.() -> Unit): List<Item> = B().apply(block).items

    private fun lightOf(c: Int): Int = (c and 0x00FFFFFF) or 0x33000000

    /* ---------------- trigonometri ---------------- */

    private fun unitCircle() = b {
        val r = 120f
        axes(-1.2f, 1.2f, -1.2f, 1.2f, r, numbers = false)
        circle(0f, 0f, r, BLUE, 1.8f)
        val special = listOf(0, 30, 45, 60, 90, 120, 135, 150, 180, 210, 225, 240, 270, 300, 315, 330)
        for (d in special) {
            val a = -d * PI.toFloat() / 180f
            val x = r * cos(a); val y = r * sin(a)
            dot(x, y, BLUE)
            if (d % 90 != 0) line(0f, 0f, x, y, GRAY, 0.8f, dashed = true)
            textC((r + 18f) * cos(a), (r + 14f) * sin(a), "$d°", 9f, GRAY)
        }
        val s3 = "√3/2"; val s2 = "√2/2"
        textC(r * cos(-PI.toFloat() / 6) + 40f, r * sin(-PI.toFloat() / 6) - 2f, "($s3, 1/2)", 9f, RED)
        textC(r * cos(-PI.toFloat() / 4) + 38f, r * sin(-PI.toFloat() / 4) - 6f, "($s2, $s2)", 9f, RED)
        textC(r * cos(-PI.toFloat() / 3) + 34f, r * sin(-PI.toFloat() / 3) - 10f, "(1/2, $s3)", 9f, RED)
        textC(r + 26f, 12f, "(1, 0)", 9f, RED)
        textC(26f, -r - 10f, "(0, 1)", 9f, RED)
        textC(-r - 26f, 12f, "(-1, 0)", 9f, RED)
        textC(26f, r + 12f, "(0, -1)", 9f, RED)
        textC(0f, r + 40f, "Birim çember: x = cos α, y = sin α", 11f, INK)
    }

    private fun tri306090() = b {
        val k = 70f
        val cx = -k * sqrt(3f) / 2; val cy = k / 2
        val bx = cx + k * sqrt(3f); val ay = cy - k
        poly(cx, cy, bx, cy, cx, ay)
        rightMark(cx, cy, 1f, 0f, 0f, -1f)
        angleArc(bx, cy, cx, cy, cx, ay, 22f, BLUE, "30°", 40f)
        angleArc(cx, ay, cx, cy, bx, cy, 18f, BLUE, "60°", 32f)
        sideLabel(cx, ay, cx, cy, "a", 14f, RED)
        sideLabel(cx, cy, bx, cy, "a√3", 14f, RED)
        sideLabel(bx, cy, cx, ay, "2a", 14f, RED)
    }

    private fun tri454590() = b {
        val k = 110f
        val cx = -k / 2; val cy = k / 2; val bx = cx + k; val ay = cy - k
        poly(cx, cy, bx, cy, cx, ay)
        rightMark(cx, cy, 1f, 0f, 0f, -1f)
        angleArc(bx, cy, cx, cy, cx, ay, 22f, BLUE, "45°", 40f)
        angleArc(cx, ay, cx, cy, bx, cy, 22f, BLUE, "45°", 38f)
        sideLabel(cx, ay, cx, cy, "a", 14f, RED)
        sideLabel(cx, cy, bx, cy, "a", 14f, RED)
        sideLabel(bx, cy, cx, ay, "a√2", 16f, RED)
        tick(cx, ay, cx, cy); tick(cx, cy, bx, cy)
    }

    private fun trigRatios() = b {
        val cx = -150f; val cy = 50f; val bx = 30f; val ay = -60f
        poly(cx, cy, bx, cy, bx, ay)
        rightMark(bx, cy, -1f, 0f, 0f, -1f)
        angleArc(cx, cy, bx, cy, bx, ay, 26f, BLUE, "α", 40f)
        sideLabel(cx, cy, bx, cy, "komşu", 14f, GREEN)
        sideLabel(bx, cy, bx, ay, "karşı", 26f, RED)
        sideLabel(bx, ay, cx, cy, "hipotenüs", 16f, BLUE)
        text(70f, -60f, "sin α = karşı / hipotenüs\ncos α = komşu / hipotenüs\ntan α = karşı / komşu\ncot α = komşu / karşı", 12f)
    }

    private fun specialAngles() = b {
        val cells = listOf(
            listOf("", "0°", "30°", "45°", "60°", "90°"),
            listOf("sin", "0", "1/2", "√2/2", "√3/2", "1"),
            listOf("cos", "1", "√3/2", "√2/2", "1/2", "0"),
            listOf("tan", "0", "√3/3", "1", "√3", "—"),
            listOf("cot", "—", "√3", "1", "√3/3", "0"),
        )
        table(-160f, -60f, listOf(50f, 54f, 54f, 54f, 54f, 54f), 24f, cells)
    }

    private fun trigGraphs() = b {
        val ux = 40f; val uy = 50f
        arrow(-10f, 0f, 2 * PI.toFloat() * ux + 20f, 0f, INK, 1.2f)
        arrow(0f, 1.4f * uy, 0f, -1.4f * uy, INK, 1.2f)
        val sinP = FloatArray(2 * 101); val cosP = FloatArray(2 * 101)
        for (i in 0..100) {
            val t = i / 100f * 2 * PI.toFloat()
            sinP[2 * i] = t * ux; sinP[2 * i + 1] = -sin(t) * uy
            cosP[2 * i] = t * ux; cosP[2 * i + 1] = -cos(t) * uy
        }
        polyArr(sinP, false, BLUE, 1.8f)
        polyArr(cosP, false, RED, 1.6f, dashed = true)
        val names = listOf("π/2", "π", "3π/2", "2π")
        for (k in 1..4) { val x = k * PI.toFloat() / 2 * ux; line(x, -3f, x, 3f); textC(x, 14f, names[k - 1], 9f, GRAY) }
        line(-3f, -uy, 3f, -uy); textC(-12f, -uy, "1", 9f, GRAY)
        line(-3f, uy, 3f, uy); textC(-14f, uy, "-1", 9f, GRAY)
        text(2 * PI.toFloat() * ux - 60f, -1.4f * uy, "y = sin x", 11f, BLUE)
        text(2 * PI.toFloat() * ux - 60f, -1.4f * uy + 16f, "y = cos x", 11f, RED)
    }

    /* ---------------- geometri ---------------- */

    private fun triangle(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float, b: B, letters: Boolean = true) {
        b.poly(ax, ay, bx, by, cx, cy)
        if (letters) {
            val gx = (ax + bx + cx) / 3; val gy = (ay + by + cy) / 3
            for ((p, s) in listOf(Triple(ax, ay, "A"), Triple(bx, by, "B"), Triple(cx, cy, "C")).map { (x, y, s) -> (x to y) to s }) {
                val (x, y) = p
                val d = Geo.dist(gx, gy, x, y).coerceAtLeast(1f)
                b.textC(x + (x - gx) / d * 14f, y + (y - gy) / d * 14f, s, 13f, BLUE)
            }
        }
    }

    private fun pythagoras() = b {
        val ax = -70f; val ay = -50f; val cx = -70f; val cy = 50f; val bx = 90f; val by = 50f
        triangle(ax, ay, bx, by, cx, cy, this)
        rightMark(cx, cy, 1f, 0f, 0f, -1f)
        sideLabel(cx, cy, ax, ay, "b", 12f, RED)
        sideLabel(bx, by, cx, cy, "a", 12f, RED)
        sideLabel(ax, ay, bx, by, "c", 14f, RED)
        textC(10f, 96f, "a² + b² = c²", 14f, INK)
    }

    private fun equilateral() = b {
        val s = 140f; val h = s * sqrt(3f) / 2
        val ax = 0f; val ay = -h * 2 / 3; val bx = -s / 2; val by = h / 3; val cx = s / 2; val cy = h / 3
        triangle(ax, ay, bx, by, cx, cy, this)
        tick(ax, ay, bx, by); tick(bx, by, cx, cy); tick(cx, cy, ax, ay)
        angleArc(ax, ay, bx, by, cx, cy, 16f, BLUE, "60°", 30f)
        angleArc(bx, by, cx, cy, ax, ay, 16f, BLUE, "60°", 30f)
        angleArc(cx, cy, ax, ay, bx, by, 16f, BLUE, "60°", 30f)
    }

    private fun isosceles() = b {
        val ax = 0f; val ay = -80f; val bx = -55f; val by = 60f; val cx = 55f; val cy = 60f
        triangle(ax, ay, bx, by, cx, cy, this)
        tick(ax, ay, bx, by, 2); tick(ax, ay, cx, cy, 2)
        angleArc(bx, by, cx, cy, ax, ay, 16f, BLUE)
        angleArc(cx, cy, ax, ay, bx, by, 16f, BLUE)
    }

    private fun altitude() = b {
        val ax = -20f; val ay = -70f; val bx = -90f; val by = 60f; val cx = 100f; val cy = 60f
        triangle(ax, ay, bx, by, cx, cy, this)
        line(ax, ay, ax, by, RED, 1.4f, dashed = true)
        rightMark(ax, by, 1f, 0f, 0f, -1f, 9f, RED)
        textC(ax, by + 14f, "H", 12f, RED)
        textC(ax + 12f, (ay + by) / 2, "h", 13f, RED)
    }

    private fun median() = b {
        val ax = -20f; val ay = -70f; val bx = -90f; val by = 60f; val cx = 100f; val cy = 60f
        triangle(ax, ay, bx, by, cx, cy, this)
        val mx = (bx + cx) / 2; val my = (by + cy) / 2
        line(ax, ay, mx, my, GREEN, 1.4f, dashed = true)
        tick(bx, by, mx, my, 1, GREEN); tick(mx, my, cx, cy, 1, GREEN)
        textC(mx, my + 14f, "D", 12f, GREEN)
        textC((ax + mx) / 2 + 14f, (ay + my) / 2, "Va", 11f, GREEN)
    }

    private fun bisector() = b {
        val ax = -20f; val ay = -70f; val bx = -90f; val by = 60f; val cx = 100f; val cy = 60f
        triangle(ax, ay, bx, by, cx, cy, this)
        val ab = Geo.dist(ax, ay, bx, by); val ac = Geo.dist(ax, ay, cx, cy)
        val t = ab / (ab + ac)
        val dx = bx + (cx - bx) * t; val dy = by + (cy - by) * t
        line(ax, ay, dx, dy, ORANGE, 1.4f, dashed = true)
        angleArc(ax, ay, bx, by, dx, dy, 20f, ORANGE)
        angleArc(ax, ay, dx, dy, cx, cy, 24f, ORANGE)
        textC(dx, dy + 14f, "D", 12f, ORANGE)
    }

    private fun circleAngles() = b {
        val r = 90f
        circle(0f, 0f, r)
        dot(0f, 0f); textC(-10f, 10f, "O", 12f, BLUE)
        val a1 = 0.35f; val a2 = PI.toFloat() - 0.35f; val ap = -PI.toFloat() / 2 - 0.4f
        val ax = r * cos(a1); val ay = r * sin(a1); val bx = r * cos(a2); val by = r * sin(a2); val px = r * cos(ap); val py = r * sin(ap)
        line(0f, 0f, ax, ay, RED); line(0f, 0f, bx, by, RED)
        line(px, py, ax, ay, BLUE); line(px, py, bx, by, BLUE)
        angleArc(0f, 0f, ax, ay, bx, by, 16f, RED, "2α", 30f)
        angleArc(px, py, ax, ay, bx, by, 20f, BLUE, "α", 34f)
        textC(ax + 12f, ay + 6f, "A", 12f, BLUE); textC(bx - 12f, by + 6f, "B", 12f, BLUE); textC(px, py - 12f, "P", 12f, BLUE)
        textC(0f, r + 30f, "Çevre açı = Merkez açı / 2", 12f)
    }

    private fun tangent() = b {
        val r = 70f
        circle(0f, 0f, r)
        dot(0f, 0f); textC(-10f, -10f, "O", 12f, BLUE)
        line(0f, 0f, r, 0f, RED)
        textC(r / 2, -10f, "r", 12f, RED)
        line(r, -100f, r, 100f, BLUE, 1.6f)
        rightMark(r, 0f, -1f, 0f, 0f, -1f, 10f, BLUE)
        textC(r + 12f, 12f, "T", 12f, BLUE)
        textC(r + 40f, -90f, "teğet", 11f, BLUE)
    }

    private fun parallels() = b {
        line(-150f, -40f, 150f, -40f, INK, 1.6f); line(-150f, 40f, 150f, 40f, INK, 1.6f)
        textC(165f, -40f, "d₁", 12f); textC(165f, 40f, "d₂", 12f)
        line(-110f, -120f, 110f, 120f, BLUE, 1.6f)
        val k = 240f / 220f
        val x1 = -110f + (80f) / k; val x2 = -110f + 160f / k
        angleArc(x1, -40f, 150f, -40f, 110f, 120f, 16f, RED, "α", 28f)
        angleArc(x2, 40f, 150f, 40f, 110f, 120f, 16f, RED, "α", 28f)
        angleArc(x1, -40f, -150f, -40f, -110f, -120f, 16f, GREEN, "α", 28f)
        textC(0f, 100f, "d₁ ∥ d₂ : yöndeş ve ters açılar eşit", 11f)
    }

    private fun quad(kind: Int) = b {
        val pts: FloatArray = when (kind) {
            0 -> floatArrayOf(-60f, -60f, 60f, -60f, 60f, 60f, -60f, 60f)          // kare
            1 -> floatArrayOf(-90f, -50f, 90f, -50f, 90f, 50f, -90f, 50f)          // dikdörtgen
            2 -> floatArrayOf(-60f, -50f, 100f, -50f, 60f, 50f, -100f, 50f)        // paralelkenar
            3 -> floatArrayOf(0f, -70f, 55f, 0f, 0f, 70f, -55f, 0f)                // eşkenar dörtgen
            else -> floatArrayOf(-50f, -50f, 50f, -50f, 100f, 50f, -100f, 50f)     // yamuk
        }
        polyArr(pts, true)
        val names = listOf("A", "B", "C", "D")
        for (i in 0 until 4) {
            val x = pts[2 * i]; val y = pts[2 * i + 1]
            val d = sqrt(x * x + y * y).coerceAtLeast(1f)
            textC(x + x / d * 14f, y + y / d * 14f, names[i], 13f, BLUE)
        }
        when (kind) {
            0 -> { for (i in 0 until 4) tick(pts[2 * i], pts[2 * i + 1], pts[(2 * i + 2) % 8], pts[(2 * i + 3) % 8]); rightMark(-60f, 60f, 1f, 0f, 0f, -1f) }
            1 -> rightMark(-90f, 50f, 1f, 0f, 0f, -1f)
            3 -> for (i in 0 until 4) tick(pts[2 * i], pts[2 * i + 1], pts[(2 * i + 2) % 8], pts[(2 * i + 3) % 8])
            else -> {}
        }
    }

    private fun regular(n: Int) = b {
        val r = 80f
        val p = FloatArray(2 * n)
        for (i in 0 until n) {
            val a = -PI.toFloat() / 2 + i * 2 * PI.toFloat() / n
            p[2 * i] = r * cos(a); p[2 * i + 1] = r * sin(a)
        }
        polyArr(p, true)
        for (i in 0 until n) tick(p[2 * i], p[2 * i + 1], p[(2 * i + 2) % (2 * n)], p[(2 * i + 3) % (2 * n)])
        textC(0f, r + 26f, "İç açılar toplamı: (n − 2)·180° = ${(n - 2) * 180}°", 11f)
    }

    private fun circleParts() = b {
        val r = 90f
        circle(0f, 0f, r)
        dot(0f, 0f); textC(-10f, 12f, "O", 12f, BLUE)
        line(-r, 0f, r, 0f, BLUE); textC(-r / 2, 10f, "çap (2r)", 10f, BLUE)
        line(0f, 0f, r * cos(-1.0f), r * sin(-1.0f), RED); textC(r * 0.35f + 14f, -r * 0.45f, "r", 12f, RED)
        val c1 = 2.3f; val c2 = 0.9f
        line(r * cos(c1), r * sin(c1), r * cos(c2), r * sin(c2), GREEN); textC(0f, r * 0.85f, "kiriş", 10f, GREEN)
        textC(0f, r + 26f, "Çevre = 2πr   Alan = πr²", 12f)
    }

    /* ---------------- analitik ---------------- */

    private fun coordinateAxes() = b { axes(-6f, 6f, -5f, 5f, 22f) }

    private fun numberLine(min: Int, max: Int, unit: Float) = b {
        arrow(min * unit - 16f, 0f, max * unit + 18f, 0f, INK, 1.4f)
        line(min * unit - 16f, 0f, min * unit - 22f, 0f)
        for (i in min..max) { line(i * unit, -5f, i * unit, 5f); textC(i * unit, 16f, "$i", 11f) }
    }

    private fun fractionLine() = b {
        val unit = 160f
        arrow(-12f, 0f, 2 * unit + 18f, 0f, INK, 1.4f)
        for (i in 0..8) {
            val x = i * unit / 4
            line(x, if (i % 4 == 0) -7f else -4f, x, if (i % 4 == 0) 7f else 4f)
            val s = when (i) { 0 -> "0"; 4 -> "1"; 8 -> "2"; 2 -> "1/2"; 6 -> "3/2"; else -> "$i/4" }
            textC(x, 18f, s, 10f, if (i % 4 == 0) INK else GRAY)
        }
    }

    private fun interval() = b {
        val unit = 30f
        arrow(-6 * unit, 0f, 6 * unit, 0f, INK, 1.2f)
        for (i in -5..5) { line(i * unit, -4f, i * unit, 4f); textC(i * unit, 16f, "$i", 10f, GRAY) }
        line(-2 * unit, 0f, 3 * unit, 0f, BLUE, 3.4f)
        dot(-2 * unit, 0f, BLUE, 4.5f)
        circle(3 * unit, 0f, 4.5f, BLUE, 1.6f, 0xFFFFFFFF.toInt())
        textC(0.5f * unit, -22f, "[−2, 3)   −2 ≤ x < 3", 12f, BLUE)
    }

    private fun graphOf(f: (Float) -> Float, label: String) = b {
        val u = 20f
        axes(-6f, 6f, -4f, 6f, u, numbers = true)
        val pts = ArrayList<Float>()
        var x = -6f
        while (x <= 6f) {
            val y = f(x)
            if (y in -4.2f..6.2f) { pts += x * u; pts += -y * u } else if (pts.size >= 4) { polyArr(pts.toFloatArray(), false, BLUE, 1.8f); pts.clear() } else pts.clear()
            x += 0.05f
        }
        if (pts.size >= 4) polyArr(pts.toFloatArray(), false, BLUE, 1.8f)
        text(2.5f * u, -5.5f * u, label, 12f, BLUE)
    }

    private fun circleEq() = b {
        val u = 20f
        axes(-2f, 7f, -2f, 7f, u, numbers = true)
        circle(3 * u, -3 * u, 2.5f * u, BLUE, 1.8f)
        dot(3 * u, -3 * u, BLUE); textC(3 * u + 16f, -3 * u + 8f, "M(a, b)", 10f, BLUE)
        line(3 * u, -3 * u, 3 * u + 2.5f * u, -3 * u, RED); textC(3 * u + 1.25f * u, -3 * u - 8f, "r", 11f, RED)
        text(-2 * u, 2.6f * u, "(x − a)² + (y − b)² = r²", 12f)
    }

    /* ---------------- katı cisimler ve açınımları ---------------- */

    private fun solid(kind: Solids.Kind) = b {
        for (p in Solids.create(kind, -70f, -70f, 70f, 70f)) items += ShapeItem(Ids.next(), p.geom, INK, 1.6f, p.hidden)
    }

    private fun cubeNet() = b {
        val s = 40f
        val cells = listOf(1 to 0, 0 to 1, 1 to 1, 2 to 1, 3 to 1, 1 to 2)
        for ((c, r) in cells) rect(c * s - 2 * s, r * s - 1.5f * s, s, s, INK, lightOf(BLUE))
        textC(0f, 2f * s, "Küpün açınımı", 11f, GRAY)
    }

    private fun prismNet() = b {
        val a = 60f; val bb = 30f; val h = 40f
        // ortada sıra: b, a, b, a ; üst ve alt a×b
        var x = -(2 * a + 2 * bb) / 2
        for (w in listOf(bb, a, bb, a)) { rect(x, -h / 2, w, h, INK, lightOf(GREEN)); x += w }
        val ax = -(2 * a + 2 * bb) / 2 + bb
        rect(ax, -h / 2 - bb, a, bb, INK, lightOf(GREEN)); rect(ax, h / 2, a, bb, INK, lightOf(GREEN))
        textC(0f, h / 2 + bb + 18f, "Dikdörtgenler prizmasının açınımı", 11f, GRAY)
    }

    private fun cylinderNet() = b {
        val r = 26f; val h = 80f; val w = 2 * PI.toFloat() * r
        rect(-w / 2, -h / 2, w, h, INK, lightOf(ORANGE))
        circle(0f, -h / 2 - r, r, INK, 1.6f, lightOf(ORANGE)); circle(0f, h / 2 + r, r, INK, 1.6f, lightOf(ORANGE))
        textC(0f, -h / 2 - 2 * r - 14f, "Silindirin açınımı", 11f, GRAY)
        textC(0f, 0f, "2πr", 12f, RED); textC(-w / 2 - 14f, 0f, "h", 12f, RED)
    }

    private fun coneNet() = b {
        val l = 110f; val r = 35f
        val sweep = 2 * PI.toFloat() * r / l
        val start = PI.toFloat() / 2 - sweep / 2
        val pts = ArrayList<Float>(); pts += 0f; pts += -70f
        val n = 40
        for (i in 0..n) { val a = start + sweep * i / n; pts += l * cos(a); pts += -70f + l * sin(a) }
        polyArr(pts.toFloatArray(), true, INK, 1.6f, lightOf(RED))
        circle(0f, -70f + l + r + 4f, r, INK, 1.6f, lightOf(RED))
        textC(-l * 0.45f, -70f + l * 0.4f, "l", 12f, RED)
        textC(0f, -70f - 16f, "Koninin açınımı", 11f, GRAY)
    }

    private fun pyramidNet() = b {
        val s = 60f; val h = 52f
        rect(-s / 2, -s / 2, s, s, INK, lightOf(BLUE))
        poly(-s / 2, -s / 2, s / 2, -s / 2, 0f, -s / 2 - h, fill = lightOf(BLUE))
        poly(-s / 2, s / 2, s / 2, s / 2, 0f, s / 2 + h, fill = lightOf(BLUE))
        poly(-s / 2, -s / 2, -s / 2, s / 2, -s / 2 - h, 0f, fill = lightOf(BLUE))
        poly(s / 2, -s / 2, s / 2, s / 2, s / 2 + h, 0f, fill = lightOf(BLUE))
        textC(0f, s / 2 + h + 18f, "Kare piramidin açınımı", 11f, GRAY)
    }

    /* ---------------- olasılık ve istatistik ---------------- */

    private fun venn2() = b {
        rect(-150f, -90f, 300f, 180f, INK, 0, 1.2f); text(-144f, -86f, "E", 12f)
        circle(-40f, 0f, 70f, BLUE, 1.8f, lightOf(BLUE)); circle(40f, 0f, 70f, RED, 1.8f, lightOf(RED))
        textC(-80f, -60f, "A", 14f, BLUE); textC(80f, -60f, "B", 14f, RED)
        textC(0f, 0f, "A∩B", 11f)
    }

    private fun venn3() = b {
        rect(-150f, -130f, 300f, 250f, INK, 0, 1.2f); text(-144f, -126f, "E", 12f)
        circle(-35f, -25f, 65f, BLUE, 1.8f, lightOf(BLUE)); circle(35f, -25f, 65f, RED, 1.8f, lightOf(RED)); circle(0f, 35f, 65f, GREEN, 1.8f, lightOf(GREEN))
        textC(-85f, -80f, "A", 14f, BLUE); textC(85f, -80f, "B", 14f, RED); textC(0f, 112f, "C", 14f, GREEN)
    }

    private fun probTree() = b {
        val x0 = -150f; val x1 = -30f; val x2 = 100f
        dot(x0, 0f)
        for ((i, y1) in listOf(-60f, 60f).withIndex()) {
            line(x0, 0f, x1, y1, BLUE); dot(x1, y1)
            textC((x0 + x1) / 2 - 6f, y1 / 2 - 10f, if (i == 0) "p" else "1 − p", 10f, BLUE)
            textC(x1 + 4f, y1 - 14f, if (i == 0) "A" else "A'", 12f)
            for ((j, d) in listOf(-30f, 30f).withIndex()) {
                val y2 = y1 + d
                line(x1, y1, x2, y2, GREEN); dot(x2, y2)
                textC(x2 + 16f, y2, if (j == 0) "B" else "B'", 12f)
                textC((x1 + x2) / 2, (y1 + y2) / 2 - 9f, if (j == 0) "q" else "1 − q", 9f, GREEN)
            }
        }
    }

    private fun die() = b {
        val s = 70f
        poly(-s / 2, -s / 2, s / 2, -s / 2, s / 2, s / 2, -s / 2, s / 2, w = 2f)
        for ((x, y) in listOf(-1 to -1, 1 to -1, 0 to 0, -1 to 1, 1 to 1)) dot(x * s / 4, y * s / 4, INK, 5f)
        val o = s + 20f
        poly(o - s / 2, -s / 2, o + s / 2, -s / 2, o + s / 2, s / 2, o - s / 2, s / 2, w = 2f)
        for ((x, y) in listOf(-1 to -1, 1 to -1, -1 to 0, 1 to 0, -1 to 1, 1 to 1)) dot(o + x * s / 4, y * s / 4, INK, 5f)
    }

    private fun coin() = b {
        circle(-50f, 0f, 36f, ORANGE, 2f, lightOf(ORANGE)); textC(-50f, 0f, "Y", 22f, ORANGE)
        circle(50f, 0f, 36f, ORANGE, 2f, lightOf(ORANGE)); textC(50f, 0f, "T", 22f, ORANGE)
        textC(0f, 54f, "Yazı / Tura", 11f, GRAY)
    }

    private fun normalCurve() = b {
        val u = 40f
        arrow(-3.6f * u, 0f, 3.6f * u, 0f, INK, 1.2f)
        val pts = FloatArray(2 * 121)
        for (i in 0..120) { val x = -3.4f + i * 6.8f / 120; pts[2 * i] = x * u; pts[2 * i + 1] = -exp(-x * x / 2) * 120f }
        polyArr(pts, false, BLUE, 1.8f)
        line(0f, 0f, 0f, -120f, GRAY, 1f, dashed = true)
        for (k in listOf(-2, -1, 1, 2)) { line(k * u, -4f, k * u, 4f); textC(k * u, 14f, if (k < 0) "μ${k}σ" else "μ+${k}σ", 9f, GRAY) }
        textC(0f, 14f, "μ", 11f)
    }

    private fun boxPlot() = b {
        val y = 0f
        line(-150f, y, -80f, y); line(80f, y, 150f, y)
        line(-150f, y - 12f, -150f, y + 12f); line(150f, y - 12f, 150f, y + 12f)
        rect(-80f, y - 24f, 160f, 48f, BLUE, lightOf(BLUE), 1.6f)
        line(10f, y - 24f, 10f, y + 24f, RED, 2f)
        for ((x, s) in listOf(-150f to "en küçük", -80f to "Q1", 10f to "ortanca", 80f to "Q3", 150f to "en büyük")) textC(x, y + 40f, s, 10f, GRAY)
    }

    private fun barChart() = b {
        val u = 26f
        arrow(-10f, 0f, 6 * u + 30f, 0f, INK, 1.2f); arrow(0f, 10f, 0f, -6 * u, INK, 1.2f)
        val hs = listOf(3f, 5f, 2f, 4f)
        val cs = listOf(BLUE, RED, GREEN, ORANGE)
        for (i in hs.indices) { rect(14f + i * 1.5f * u, -hs[i] * u, u, hs[i] * u, cs[i], lightOf(cs[i]), 1.4f); textC(14f + i * 1.5f * u + u / 2, 14f, "${'A' + i}", 11f) }
        for (k in 1..5) { line(-3f, -k * u, 3f, -k * u); textC(-12f, -k * u, "$k", 9f, GRAY) }
    }

    private fun pieChart() = b {
        val r = 80f
        circle(0f, 0f, r, INK, 1.6f)
        var a = -PI.toFloat() / 2
        val parts = listOf(0.4f, 0.25f, 0.2f, 0.15f)
        for ((i, p) in parts.withIndex()) {
            line(0f, 0f, r * cos(a), r * sin(a))
            val mid = a + p * PI.toFloat()
            textC(r * 0.6f * cos(mid), r * 0.6f * sin(mid), "%${(p * 100).toInt()}", 11f, listOf(BLUE, RED, GREEN, ORANGE)[i])
            a += p * 2 * PI.toFloat()
        }
    }

    /* ---------------- cebir karoları ---------------- */

    const val TILE = 60f
    const val UNIT = 18f
    private val X2 = 0xFF8EC5FF.toInt(); private val XT = 0xFF7CF29A.toInt(); private val ONE = 0xFFFFE07A.toInt(); private val NEG = 0xFFFF9EAE.toInt()

    private fun tilesSet() = b {
        rect(-150f, -30f, TILE, TILE, BLUE, X2); textC(-150f + TILE / 2, 0f, "x²", 14f)
        rect(-70f, -30f, UNIT, TILE, GREEN, XT); textC(-70f + UNIT / 2, 40f, "x", 12f)
        rect(-30f, -30f, UNIT, UNIT, ORANGE, ONE); textC(-30f + UNIT / 2, 0f, "1", 10f)
        rect(10f, -30f, TILE, TILE, RED, NEG); textC(10f + TILE / 2, 0f, "−x²", 14f)
        rect(90f, -30f, UNIT, TILE, RED, NEG); textC(90f + UNIT / 2, 40f, "−x", 12f)
        rect(130f, -30f, UNIT, UNIT, RED, NEG); textC(130f + UNIT / 2, 0f, "−1", 10f)
    }

    /** x² + 3x + 2 = (x + 1)(x + 2) dikdörtgeni. */
    private fun tilesFactor() = b {
        val x0 = -60f; val y0 = -50f
        rect(x0, y0, TILE, TILE, BLUE, X2)
        for (i in 0 until 2) rect(x0 + TILE + i * UNIT, y0, UNIT, TILE, GREEN, XT)
        rect(x0, y0 + TILE, TILE, UNIT, GREEN, XT)
        for (i in 0 until 2) rect(x0 + TILE + i * UNIT, y0 + TILE, UNIT, UNIT, ORANGE, ONE)
        textC(x0 + (TILE + 2 * UNIT) / 2, y0 - 14f, "x + 2", 12f, RED)
        textC(x0 - 26f, y0 + (TILE + UNIT) / 2, "x + 1", 12f, RED)
        textC(x0 + 48f, y0 + TILE + UNIT + 22f, "x² + 3x + 2 = (x + 1)(x + 2)", 12f)
    }

    /* ---------------- kesir modelleri (pay/payda ile) ---------------- */

    fun fractionPie(num: Int, den: Int): List<Item> = b {
        val d = den.coerceIn(1, 24); val k = num.coerceIn(0, d)
        val r = 70f
        for (i in 0 until d) {
            val a0 = -PI.toFloat() / 2 + i * 2 * PI.toFloat() / d
            val a1 = a0 + 2 * PI.toFloat() / d
            val pts = ArrayList<Float>(); pts += 0f; pts += 0f
            val n = maxOf(4, 48 / d)
            for (j in 0..n) { val a = a0 + (a1 - a0) * j / n; pts += r * cos(a); pts += r * sin(a) }
            polyArr(pts.toFloatArray(), true, INK, 1.4f, if (i < k) lightOf(BLUE) or 0x66000000 else 0)
        }
        textC(0f, r + 22f, "$k/$d", 16f, BLUE)
    }

    fun fractionBar(num: Int, den: Int): List<Item> = b {
        val d = den.coerceIn(1, 24); val k = num.coerceIn(0, d)
        val w = 240f; val h = 40f; val cw = w / d
        for (i in 0 until d) rect(-w / 2 + i * cw, -h / 2, cw, h, INK, if (i < k) lightOf(GREEN) or 0x66000000 else 0, 1.4f)
        textC(0f, h / 2 + 20f, "$k/$d", 16f, GREEN)
    }

    fun fractionOnLine(num: Int, den: Int): List<Item> = b {
        val d = den.coerceIn(1, 24); val k = num.coerceAtLeast(0)
        val unit = 200f
        val maxV = maxOf(1, (k + d - 1) / d)
        arrow(-10f, 0f, maxV * unit + 20f, 0f, INK, 1.4f)
        for (i in 0..maxV * d) {
            val x = i * unit / d
            val whole = i % d == 0
            line(x, if (whole) -7f else -4f, x, if (whole) 7f else 4f)
            if (whole) textC(x, 18f, "${i / d}", 11f)
        }
        val x = k * unit / d
        dot(x, 0f, RED, 4.5f)
        textC(x, -18f, "$k/$d", 13f, RED)
    }

    /* ---------------- formül kartları ---------------- */

    private val cards = listOf(
        Triple("Alan formülleri", "Kare: a²\nDikdörtgen: a · b\nÜçgen: (a · h) / 2\nParalelkenar: a · h\nYamuk: (a + c) · h / 2\nDaire: π r²", BLUE),
        Triple("Hacim formülleri", "Küp: a³\nPrizma: taban alanı · h\nSilindir: π r² h\nKoni: (π r² h) / 3\nPiramit: (taban alanı · h) / 3\nKüre: (4/3) π r³", GREEN),
        Triple("Özdeşlikler", "(a + b)² = a² + 2ab + b²\n(a − b)² = a² − 2ab + b²\na² − b² = (a − b)(a + b)\n(a + b)³ = a³ + 3a²b + 3ab² + b³\na³ − b³ = (a − b)(a² + ab + b²)\na³ + b³ = (a + b)(a² − ab + b²)", RED),
        Triple("Üslü ve köklü sayılar", "aᵐ · aⁿ = aᵐ⁺ⁿ\naᵐ / aⁿ = aᵐ⁻ⁿ\n(aᵐ)ⁿ = aᵐⁿ\na⁰ = 1   a⁻ⁿ = 1 / aⁿ\n√(a·b) = √a · √b\nⁿ√aᵐ = aᵐ/ⁿ", ORANGE),
        Triple("Logaritma", "logₐ(x·y) = logₐx + logₐy\nlogₐ(x/y) = logₐx − logₐy\nlogₐ(xⁿ) = n · logₐx\nlogₐx = log x / log a\nlogₐa = 1   logₐ1 = 0\na^(logₐx) = x", BLUE),
        Triple("Trigonometrik özdeşlikler", "sin²x + cos²x = 1\ntan x = sin x / cos x\nsin 2x = 2 sin x cos x\ncos 2x = cos²x − sin²x\nsin(a ± b) = sin a cos b ± cos a sin b\ncos(a ± b) = cos a cos b ∓ sin a sin b", GREEN),
        Triple("Türev kuralları", "(xⁿ)' = n xⁿ⁻¹\n(c)' = 0\n(f · g)' = f'g + fg'\n(f / g)' = (f'g − fg') / g²\n(f(g(x)))' = f'(g(x)) · g'(x)\n(sin x)' = cos x   (cos x)' = −sin x\n(eˣ)' = eˣ   (ln x)' = 1/x", RED),
        Triple("İntegral kuralları", "∫ xⁿ dx = xⁿ⁺¹ / (n + 1) + c\n∫ 1/x dx = ln|x| + c\n∫ eˣ dx = eˣ + c\n∫ sin x dx = −cos x + c\n∫ cos x dx = sin x + c\n∫ₐᵇ f(x) dx = F(b) − F(a)", ORANGE),
        Triple("İkinci derece denklem", "ax² + bx + c = 0\nΔ = b² − 4ac\nx₁,₂ = (−b ± √Δ) / 2a\nx₁ + x₂ = −b/a\nx₁ · x₂ = c/a\nTepe: r = −b/2a", BLUE),
        Triple("Analitik geometri", "İki nokta arası: √((x₂−x₁)² + (y₂−y₁)²)\nOrta nokta: ((x₁+x₂)/2, (y₁+y₂)/2)\nEğim: m = (y₂ − y₁) / (x₂ − x₁)\nDoğru: y − y₁ = m(x − x₁)\nParalel: m₁ = m₂   Dik: m₁·m₂ = −1", GREEN),
    )

    /* ---------------- genel ---------------- */

    private fun tableN(rows: Int, cols: Int) = b {
        val cw = 70f; val rh = 28f
        table(-cols * cw / 2, -rows * rh / 2, List(cols) { cw }, rh, List(rows) { List(cols) { "" } })
    }

    private fun brace() = b {
        poly(10f, -60f, 4f, -58f, 1f, -53f, 0f, -46f, 0f, -10f, -2f, -4f, -8f, 0f, -2f, 4f, 0f, 10f, 0f, 46f, 1f, 53f, 4f, 58f, 10f, 60f, closed = false, w = 1.8f)
    }

    private fun conceptMap() = b {
        rect(-60f, -20f, 120f, 40f, BLUE, lightOf(BLUE), 1.8f); textC(0f, 0f, "Ana kavram", 13f, BLUE)
        val spots = listOf(-170f to -110f, 110f to -110f, -170f to 70f, 110f to 70f)
        for ((x, y) in spots) {
            rect(x, y, 100f, 36f, GREEN, lightOf(GREEN), 1.4f)
            textC(x + 50f, y + 18f, "Alt kavram", 11f, GREEN)
            val tx = x + 50f; val ty = if (y < 0) y + 36f else y
            arrow(0f + (tx / 4), if (y < 0) -20f else 20f, tx, ty, GRAY, 1.2f)
        }
    }

    private fun timeline() = b {
        arrow(-180f, 0f, 190f, 0f, INK, 1.8f)
        for (i in 0..4) {
            val x = -150f + i * 75f
            dot(x, 0f, BLUE, 4f)
            textC(x, -18f, "Olay ${i + 1}", 10f, BLUE)
            textC(x, 18f, "tarih", 9f, GRAY)
        }
    }

    private fun questionBoxes(n: Int) = b {
        val cols = if (n <= 2) 1 else 2
        val rows = (n + cols - 1) / cols
        val w = if (cols == 1) 500f else 245f
        val h = if (rows == 1) 300f else (680f / rows)
        val gap = 10f
        val totalW = cols * w + (cols - 1) * gap; val totalH = rows * h + (rows - 1) * gap
        for (i in 0 until n) {
            val c = i % cols; val r = i / cols
            val x = -totalW / 2 + c * (w + gap); val y = -totalH / 2 + r * (h + gap)
            rect(x, y, w, h, GRAY, 0, 1.2f)
            text(x + 8f, y + 6f, "Soru ${i + 1}", 12f, BLUE)
        }
    }

    /* ---------------- katalog ---------------- */

    val entries: List<Entry> = listOf(
        Entry("unit_circle", "Trigonometri", "Birim çember") { unitCircle() },
        Entry("t306090", "Trigonometri", "30-60-90 üçgeni") { tri306090() },
        Entry("t454590", "Trigonometri", "45-45-90 üçgeni") { tri454590() },
        Entry("trig_ratios", "Trigonometri", "Trigonometrik oranlar") { trigRatios() },
        Entry("special_angles", "Trigonometri", "Özel açılar tablosu") { specialAngles() },
        Entry("trig_graphs", "Trigonometri", "sin ve cos grafikleri") { trigGraphs() },

        Entry("pythagoras", "Geometri", "Dik üçgen (Pisagor)") { pythagoras() },
        Entry("equilateral", "Geometri", "Eşkenar üçgen") { equilateral() },
        Entry("isosceles", "Geometri", "İkizkenar üçgen") { isosceles() },
        Entry("altitude", "Geometri", "Üçgende yükseklik") { altitude() },
        Entry("median", "Geometri", "Üçgende kenarortay") { median() },
        Entry("bisector", "Geometri", "Üçgende açıortay") { bisector() },
        Entry("circle_angles", "Geometri", "Merkez ve çevre açı") { circleAngles() },
        Entry("tangent", "Geometri", "Çembere teğet") { tangent() },
        Entry("circle_parts", "Geometri", "Çemberin elemanları") { circleParts() },
        Entry("parallels", "Geometri", "Paralel doğrular ve kesen") { parallels() },
        Entry("square", "Geometri", "Kare") { quad(0) },
        Entry("rectangle", "Geometri", "Dikdörtgen") { quad(1) },
        Entry("parallelogram", "Geometri", "Paralelkenar") { quad(2) },
        Entry("rhombus", "Geometri", "Eşkenar dörtgen") { quad(3) },
        Entry("trapezoid", "Geometri", "Yamuk") { quad(4) },
        Entry("pentagon", "Geometri", "Düzgün beşgen") { regular(5) },
        Entry("hexagon", "Geometri", "Düzgün altıgen") { regular(6) },
        Entry("octagon", "Geometri", "Düzgün sekizgen") { regular(8) },

        Entry("axes", "Analitik", "Koordinat ekseni") { coordinateAxes() },
        Entry("number_line", "Analitik", "Sayı doğrusu") { numberLine(-5, 5, 30f) },
        Entry("fraction_line", "Analitik", "Kesirli sayı doğrusu") { fractionLine() },
        Entry("interval", "Analitik", "Aralık gösterimi") { interval() },
        Entry("line_graph", "Analitik", "Doğru: y = x + 1") { graphOf({ it + 1 }, "y = x + 1") },
        Entry("parabola", "Analitik", "Parabol: y = x² − 2") { graphOf({ it * it - 2 }, "y = x² − 2") },
        Entry("circle_eq", "Analitik", "Çember denklemi") { circleEq() },

        Entry("cube", "Katı cisimler", "Küp") { solid(Solids.Kind.CUBE) },
        Entry("prism", "Katı cisimler", "Dikdörtgenler prizması") { solid(Solids.Kind.PRISM) },
        Entry("cylinder", "Katı cisimler", "Silindir") { solid(Solids.Kind.CYLINDER) },
        Entry("cone", "Katı cisimler", "Koni") { solid(Solids.Kind.CONE) },
        Entry("pyramid", "Katı cisimler", "Kare piramit") { solid(Solids.Kind.PYRAMID) },
        Entry("sphere", "Katı cisimler", "Küre") { solid(Solids.Kind.SPHERE) },
        Entry("cube_net", "Katı cisimler", "Küp açınımı") { cubeNet() },
        Entry("prism_net", "Katı cisimler", "Prizma açınımı") { prismNet() },
        Entry("cylinder_net", "Katı cisimler", "Silindir açınımı") { cylinderNet() },
        Entry("cone_net", "Katı cisimler", "Koni açınımı") { coneNet() },
        Entry("pyramid_net", "Katı cisimler", "Piramit açınımı") { pyramidNet() },

        Entry("venn2", "Olasılık ve istatistik", "Venn şeması (2 küme)") { venn2() },
        Entry("venn3", "Olasılık ve istatistik", "Venn şeması (3 küme)") { venn3() },
        Entry("tree", "Olasılık ve istatistik", "Olasılık ağacı") { probTree() },
        Entry("dice", "Olasılık ve istatistik", "Zar") { die() },
        Entry("coin", "Olasılık ve istatistik", "Madeni para") { coin() },
        Entry("normal", "Olasılık ve istatistik", "Normal dağılım") { normalCurve() },
        Entry("boxplot", "Olasılık ve istatistik", "Kutu grafiği") { boxPlot() },
        Entry("bars", "Olasılık ve istatistik", "Sütun grafiği") { barChart() },
        Entry("pie", "Olasılık ve istatistik", "Pasta grafiği") { pieChart() },

        Entry("tiles", "Cebir karoları", "Karolar (x², x, 1)") { tilesSet() },
        Entry("tiles_factor", "Cebir karoları", "x² + 3x + 2 çarpanlara ayırma") { tilesFactor() },
        Entry("tile_x2", "Cebir karoları", "x² karosu") { b { rect(0f, 0f, TILE, TILE, BLUE, X2); textC(TILE / 2, TILE / 2, "x²", 14f) } },
        Entry("tile_x", "Cebir karoları", "x karosu") { b { rect(0f, 0f, UNIT, TILE, GREEN, XT); textC(UNIT / 2, TILE / 2, "x", 11f) } },
        Entry("tile_1", "Cebir karoları", "1 karosu") { b { rect(0f, 0f, UNIT, UNIT, ORANGE, ONE); textC(UNIT / 2, UNIT / 2, "1", 9f) } },
        Entry("tile_neg", "Cebir karoları", "Negatif karolar") { b { rect(0f, 0f, TILE, TILE, RED, NEG); textC(TILE / 2, TILE / 2, "−x²", 13f); rect(TILE + 12f, 0f, UNIT, TILE, RED, NEG); rect(TILE + UNIT + 24f, 0f, UNIT, UNIT, RED, NEG) } },
    ) + cards.mapIndexed { i, (t, body, c) ->
        Entry("card_$i", "Formül kartları", t) { b { card(t, body, c, lightOf(c)) } }
    } + listOf(
        Entry("table3", "Genel", "Tablo 3 × 3") { tableN(3, 3) },
        Entry("table4", "Genel", "Tablo 5 × 4") { tableN(5, 4) },
        Entry("value_table", "Genel", "Değer tablosu (x, y)") { b { table(-150f, -30f, listOf(40f) + List(6) { 44f }, 26f, listOf(listOf("x", "", "", "", "", "", ""), listOf("y", "", "", "", "", "", ""))) } },
        Entry("arrow", "Genel", "Ok") { b { arrow(-80f, 0f, 80f, 0f, INK, 2f, 12f) } },
        Entry("brace", "Genel", "Süslü parantez") { brace() },
        Entry("concept", "Genel", "Kavram haritası") { conceptMap() },
        Entry("timeline", "Genel", "Zaman çizelgesi") { timeline() },
        Entry("q2", "Genel", "Soru alanları (2)") { questionBoxes(2) },
        Entry("q4", "Genel", "Soru alanları (4)") { questionBoxes(4) },
        Entry("q6", "Genel", "Soru alanları (6)") { questionBoxes(6) },
    )

    /** Öğelerin kapsayan kutusu. */
    fun bounds(items: List<Item>): Box? = items.map { it.bounds() }.reduceOrNull { a, b -> a.union(b) }

    /**
     * Öğeleri (cx, cy) merkezine taşır; [maxW]×[maxH]'ye sığmıyorsa küçültür. Yeni kimliklerle döndürür.
     */
    fun place(items: List<Item>, cx: Float, cy: Float, maxW: Float, maxH: Float): List<Item> {
        val bx = bounds(items) ?: return emptyList()
        val s = minOf(1f, maxW / bx.width.coerceAtLeast(1f), maxH / bx.height.coerceAtLeast(1f))
        val t = Similarity(dx = cx - bx.cx, dy = cy - (bx.top + bx.bottom) / 2f, scale = s, ox = bx.cx, oy = (bx.top + bx.bottom) / 2f)
        return items.map { it.transformed(t, Ids.next()) }
    }
}
