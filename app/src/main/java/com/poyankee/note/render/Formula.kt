package com.poyankee.note.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import kotlin.math.max

/**
 * Küçük bir formül dizgi motoru (LaTeX'in sık kullanılan kısmı):
 *   x^2  x^{n+1}  a_1  \frac{a}{b}  \sqrt{x}  \sqrt[3]{x}  \sum  \int  \pi  \alpha ... \le \ge \neq \cdot \times \pm \infty
 * Sonuç yüksek çözünürlüklü bir resimdir; sayfaya görsel olarak eklenir (PDF'e de aynen çıkar).
 */
object Formula {

    private val SYMBOLS = mapOf(
        "cdot" to "·", "times" to "×", "div" to "÷", "pm" to "±", "mp" to "∓",
        "le" to "≤", "leq" to "≤", "ge" to "≥", "geq" to "≥", "neq" to "≠", "ne" to "≠", "approx" to "≈", "equiv" to "≡",
        "infty" to "∞", "pi" to "π", "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ", "Delta" to "Δ",
        "theta" to "θ", "lambda" to "λ", "mu" to "μ", "sigma" to "σ", "Sigma" to "Σ", "phi" to "φ", "omega" to "ω", "Omega" to "Ω",
        "epsilon" to "ε", "rho" to "ρ", "tau" to "τ",
        "to" to "→", "rightarrow" to "→", "Rightarrow" to "⇒", "Leftrightarrow" to "⇔", "leftarrow" to "←",
        "degree" to "°", "circ" to "°", "angle" to "∠", "triangle" to "△", "perp" to "⊥", "parallel" to "∥",
        "in" to "∈", "notin" to "∉", "subset" to "⊂", "cup" to "∪", "cap" to "∩", "emptyset" to "∅",
        "forall" to "∀", "exists" to "∃", "mathbb{R}" to "ℝ", "R" to "ℝ", "N" to "ℕ", "Z" to "ℤ", "Q" to "ℚ",
        "sin" to "sin", "cos" to "cos", "tan" to "tan", "cot" to "cot", "log" to "log", "ln" to "ln", "lim" to "lim",
        "," to " ", ";" to "  ", "quad" to "    ", " " to " ",
    )
    private val BIG = mapOf("sum" to "∑", "int" to "∫", "prod" to "∏", "oint" to "∮")

    /* ---------- düzen kutuları ---------- */

    private abstract class Box {
        var w = 0f; var asc = 0f; var desc = 0f
        abstract fun draw(c: Canvas, x: Float, base: Float)
    }

    private class TextBox(val s: String, val size: Float, val italic: Boolean) : Box() {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            typeface = Typeface.create(Typeface.SERIF, if (italic) Typeface.ITALIC else Typeface.NORMAL)
            color = 0xFF000000.toInt()
        }
        init {
            w = paint.measureText(s) + if (italic) size * 0.04f else 0f
            asc = -paint.fontMetrics.ascent * 0.82f
            desc = paint.fontMetrics.descent * 0.9f
        }
        override fun draw(c: Canvas, x: Float, base: Float) { c.drawText(s, x, base, paint) }
    }

    private class RowBox(val items: List<Box>, gap: Float = 0f) : Box() {
        init {
            w = items.sumOf { it.w.toDouble() }.toFloat()
            asc = items.maxOfOrNull { it.asc } ?: 0f
            desc = items.maxOfOrNull { it.desc } ?: 0f
        }
        override fun draw(c: Canvas, x: Float, base: Float) {
            var cx = x
            for (b in items) { b.draw(c, cx, base); cx += b.w }
        }
    }

    private class ScriptBox(val base: Box, val sup: Box?, val sub: Box?, size: Float) : Box() {
        val up = size * 0.42f
        val down = size * 0.22f
        init {
            w = base.w + max(sup?.w ?: 0f, sub?.w ?: 0f) + size * 0.04f
            asc = max(base.asc, (sup?.let { it.asc + up } ?: 0f))
            desc = max(base.desc, (sub?.let { it.desc + down } ?: 0f))
        }
        override fun draw(c: Canvas, x: Float, base0: Float) {
            base.draw(c, x, base0)
            sup?.draw(c, x + base.w + 1f, base0 - up)
            sub?.draw(c, x + base.w + 1f, base0 + down)
        }
    }

    private class FracBox(val num: Box, val den: Box, val size: Float) : Box() {
        val pad = size * 0.12f
        val axis = size * 0.28f
        val gap = size * 0.12f
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = max(1f, size * 0.055f); color = 0xFF000000.toInt() }
        init {
            w = max(num.w, den.w) + 2 * pad
            asc = axis + gap + num.desc + num.asc
            desc = -axis + gap + den.asc + den.desc
        }
        override fun draw(c: Canvas, x: Float, base: Float) {
            val ly = base - axis
            c.drawLine(x + pad * 0.5f, ly, x + w - pad * 0.5f, ly, line)
            num.draw(c, x + (w - num.w) / 2f, ly - gap - num.desc)
            den.draw(c, x + (w - den.w) / 2f, ly + gap + den.asc)
        }
    }

    private class SqrtBox(val inner: Box, val index: Box?, val size: Float) : Box() {
        val hook = size * 0.55f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = max(1f, size * 0.055f); color = 0xFF000000.toInt()
            strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
        }
        val idxW = index?.w ?: 0f
        init {
            w = max(idxW * 0.7f, 0f) + hook + inner.w + size * 0.15f
            asc = inner.asc + size * 0.18f
            desc = inner.desc + size * 0.05f
        }
        override fun draw(c: Canvas, x: Float, base: Float) {
            val x0 = x + max(idxW * 0.7f, 0f)
            val top = base - asc + 1f
            val bot = base + desc
            val p = Path()
            p.moveTo(x0, base - size * 0.2f)
            p.lineTo(x0 + hook * 0.3f, base - size * 0.3f)
            p.lineTo(x0 + hook * 0.6f, bot)
            p.lineTo(x0 + hook, top)
            p.lineTo(x + w, top)
            c.drawPath(p, paint)
            index?.draw(c, x, base - size * 0.42f)
            inner.draw(c, x0 + hook + size * 0.08f, base)
        }
    }

    private class SpaceBox(width: Float) : Box() {
        init { w = width }
        override fun draw(c: Canvas, x: Float, base: Float) {}
    }

    /* ---------- ayrıştırıcı ---------- */

    private class Parser(val s: String) {
        var i = 0

        fun row(size: Float, stop: Char? = null): Box {
            val items = ArrayList<Box>()
            while (i < s.length && (stop == null || s[i] != stop)) {
                var a = atom(size) ?: continue
                // üst ve alt simgeler
                var sup: Box? = null; var sub: Box? = null
                while (i < s.length && (s[i] == '^' || s[i] == '_')) {
                    val up = s[i] == '^'; i++
                    val sc = scriptArg(size * 0.68f)
                    if (up) sup = sc else sub = sc
                }
                if (sup != null || sub != null) a = ScriptBox(a, sup, sub, size)
                items += a
            }
            if (stop != null && i < s.length && s[i] == stop) i++
            return RowBox(items)
        }

        private fun scriptArg(size: Float): Box {
            if (i >= s.length) return SpaceBox(0f)
            if (s[i] == '{') { i++; return row(size, '}') }
            return atom(size) ?: SpaceBox(0f)
        }

        private fun group(size: Float): Box {
            while (i < s.length && s[i] == ' ') i++
            if (i < s.length && s[i] == '{') { i++; return row(size, '}') }
            return atom(size) ?: SpaceBox(0f)
        }

        fun atom(size: Float): Box? {
            val ch = s[i]
            when {
                ch == '{' -> { i++; return row(size, '}') }
                ch == '}' -> { i++; return null }
                ch == ' ' -> { i++; return SpaceBox(size * 0.12f) }
                ch == '\\' -> {
                    i++
                    val st = i
                    if (i < s.length && !s[i].isLetter()) { i++ } else while (i < s.length && s[i].isLetter()) i++
                    val cmd = s.substring(st, i)
                    return when (cmd) {
                        "frac", "dfrac" -> FracBox(group(size * 0.9f), group(size * 0.9f), size)
                        "sqrt" -> {
                            var idx: Box? = null
                            if (i < s.length && s[i] == '[') {
                                i++
                                idx = row(size * 0.55f, ']')
                            }
                            SqrtBox(group(size), idx, size)
                        }
                        "left", "right", "big", "Big" -> null
                        "text", "mathrm" -> { val g = rawGroup(); TextBox(g, size, false) }
                        else -> BIG[cmd]?.let { TextBox(it, size * 1.45f, false) }
                            ?: SYMBOLS[cmd]?.let { TextBox(it, size, false) }
                            ?: TextBox(cmd, size, true)
                    }
                }
                else -> {
                    i++
                    val italic = ch.isLetter() && ch.code < 0x370
                    val str = when (ch) { '*' -> "·"; '-' -> "−"; else -> ch.toString() }
                    val spaced = ch in "=+-<>≤≥≠±×÷*"
                    val t = TextBox(str, size, italic)
                    return if (spaced) RowBox(listOf(SpaceBox(size * 0.18f), t, SpaceBox(size * 0.18f))) else t
                }
            }
        }

        private fun rawGroup(): String {
            if (i < s.length && s[i] == '{') {
                val end = s.indexOf('}', i)
                if (end > i) { val r = s.substring(i + 1, end); i = end + 1; return r }
            }
            return ""
        }
    }

    /**
     * Formülü siyah renkte, [size] nokta yazı boyutunda, [scale] kat çözünürlükte çizer.
     * Dönen boyut (genişlik, yükseklik) nokta cinsindendir.
     */
    fun render(src: String, size: Float, color: Int, scale: Float = 4f): Triple<Bitmap, Float, Float> {
        val root = Parser(src).row(size * scale)
        val pad = size * scale * 0.2f
        val bw = (root.w + 2 * pad).toInt().coerceIn(1, 6000)
        val bh = (root.asc + root.desc + 2 * pad).toInt().coerceIn(1, 3000)
        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        root.draw(c, pad, pad + root.asc)
        // renklendir: siyah pikselleri istenen renge çevir (alfa korunur)
        if ((color and 0xFFFFFF) != 0) {
            val px = IntArray(bw * bh)
            bmp.getPixels(px, 0, bw, 0, 0, bw, bh)
            val rgb = color and 0x00FFFFFF
            for (k in px.indices) { val a = px[k] ushr 24; if (a != 0) px[k] = (a shl 24) or rgb }
            bmp.setPixels(px, 0, bw, 0, 0, bw, bh)
        }
        return Triple(bmp, bw / scale, bh / scale)
    }
}
