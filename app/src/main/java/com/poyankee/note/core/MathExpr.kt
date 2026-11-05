package com.poyankee.note.core

import kotlin.math.abs

/**
 * Basit matematik ifadesi ayrıştırıcı: y = f(x) grafikleri için.
 * Desteklenenler: + - * / ^, parantez, x, sayılar (2.5 veya 2,5), π / pi, e,
 * sin cos tan cot asin acos atan sqrt (kök) abs ln log exp,
 * x² x³ gibi üst simgeler, × · ÷ işaretleri ve "2x", "3sin(x)", "x(x+1)" gibi örtük çarpma.
 */
class MathExpr private constructor(private val root: Node, val source: String) {

    fun eval(x: Double): Double = root.eval(x)

    private sealed class Node {
        abstract fun eval(x: Double): Double
        class Num(val v: Double) : Node() { override fun eval(x: Double) = v }
        object X : Node() { override fun eval(x: Double) = x }
        class Neg(val a: Node) : Node() { override fun eval(x: Double) = -a.eval(x) }
        class Bin(val op: Char, val a: Node, val b: Node) : Node() {
            override fun eval(x: Double): Double {
                val l = a.eval(x); val r = b.eval(x)
                return when (op) {
                    '+' -> l + r; '-' -> l - r; '*' -> l * r; '/' -> l / r
                    else -> pow(l, r)
                }
            }
            private fun pow(l: Double, r: Double): Double {
                // negatif tabanın tek paydalı kesirli üssü (ör. x^(1/3))
                if (l < 0 && r != Math.rint(r)) {
                    val inv = 1.0 / r
                    if (abs(inv - Math.rint(inv)) < 1e-9 && Math.rint(inv).toLong() % 2L != 0L) return -Math.pow(-l, r)
                }
                return Math.pow(l, r)
            }
        }
        class Fn(val name: String, val a: Node) : Node() {
            override fun eval(x: Double): Double {
                val v = a.eval(x)
                return when (name) {
                    "sin" -> Math.sin(v); "cos" -> Math.cos(v); "tan" -> Math.tan(v); "cot" -> 1.0 / Math.tan(v)
                    "asin" -> Math.asin(v); "acos" -> Math.acos(v); "atan" -> Math.atan(v)
                    "sqrt", "kök", "kok" -> Math.sqrt(v); "abs" -> abs(v)
                    "ln" -> Math.log(v); "log" -> Math.log10(v); "exp" -> Math.exp(v)
                    else -> Double.NaN
                }
            }
        }
    }

    companion object {
        private val FUNCS = setOf("sin", "cos", "tan", "cot", "asin", "acos", "atan", "sqrt", "kök", "kok", "abs", "ln", "log", "exp")

        /** İfadeyi ayrıştırır; hatalıysa açıklamalı IllegalArgumentException fırlatır. */
        fun parse(input: String): MathExpr {
            var s = input.trim()
            // "y =" veya "f(x) =" önekini at
            val eq = s.indexOf('=')
            if (eq >= 0) s = s.substring(eq + 1)
            val norm = StringBuilder()
            for (ch in s) {
                when (ch) {
                    '²' -> norm.append("^2")
                    '³' -> norm.append("^3")
                    '×', '·', '⋅' -> norm.append('*')
                    '÷', ':' -> norm.append('/')
                    '−', '–' -> norm.append('-')
                    'π' -> norm.append("pi")
                    '√' -> norm.append("sqrt")
                    'X' -> norm.append('x')
                    else -> norm.append(ch)
                }
            }
            val tokens = tokenize(norm.toString())
            val p = Parser(tokens)
            val node = p.expr()
            if (!p.done()) throw IllegalArgumentException("Beklenmeyen ifade: '${p.peek()}'")
            return MathExpr(node, input.trim())
        }

        private fun tokenize(s: String): List<String> {
            val out = ArrayList<String>()
            var i = 0
            while (i < s.length) {
                val c = s[i]
                when {
                    c.isWhitespace() -> i++
                    c.isDigit() || (c == '.' || c == ',') && i + 1 < s.length && s[i + 1].isDigit() -> {
                        val st = i
                        while (i < s.length && (s[i].isDigit() || s[i] == '.' || s[i] == ',')) i++
                        out += s.substring(st, i).replace(',', '.')
                    }
                    c.isLetter() -> {
                        val st = i
                        while (i < s.length && s[i].isLetter()) i++
                        var word = s.substring(st, i).lowercase()
                        // "xsin" gibi bitişik yazımları ayır
                        while (word.isNotEmpty()) {
                            val f = FUNCS.sortedByDescending { it.length }.firstOrNull { word.startsWith(it) }
                            when {
                                f != null -> { out += f; word = word.substring(f.length) }
                                word.startsWith("pi") -> { out += "pi"; word = word.substring(2) }
                                word[0] == 'x' -> { out += "x"; word = word.substring(1) }
                                word[0] == 'e' -> { out += "e"; word = word.substring(1) }
                                else -> throw IllegalArgumentException("Bilinmeyen sembol: '$word'")
                            }
                        }
                    }
                    c in "+-*/^()" -> { out += c.toString(); i++ }
                    else -> throw IllegalArgumentException("Geçersiz karakter: '$c'")
                }
            }
            // örtük çarpma: 2x, 2(…), x(…), )(…), )x, 2sin(x), x pi
            val res = ArrayList<String>()
            for ((k, t) in out.withIndex()) {
                if (k > 0) {
                    val prev = out[k - 1]
                    val prevValue = prev == ")" || prev == "x" || prev == "pi" || prev == "e" || prev[0].isDigit() || prev[0] == '.'
                    val startsValue = t == "(" || t == "x" || t == "pi" || t == "e" || t in FUNCS || t[0].isDigit() || t[0] == '.'
                    if (prevValue && startsValue) res += "*"
                }
                res += t
            }
            return res
        }

        private class Parser(val t: List<String>) {
            var i = 0
            fun peek() = t.getOrNull(i)
            fun done() = i >= t.size
            fun eat(s: String): Boolean { if (peek() == s) { i++; return true }; return false }

            fun expr(): Node {
                var n = term()
                while (true) {
                    n = when {
                        eat("+") -> Node.Bin('+', n, term())
                        eat("-") -> Node.Bin('-', n, term())
                        else -> return n
                    }
                }
            }

            fun term(): Node {
                var n = unary()
                while (true) {
                    n = when {
                        eat("*") -> Node.Bin('*', n, unary())
                        eat("/") -> Node.Bin('/', n, unary())
                        else -> return n
                    }
                }
            }

            fun unary(): Node = when {
                eat("-") -> Node.Neg(unary())
                eat("+") -> unary()
                else -> power()
            }

            fun power(): Node {
                val base = atom()
                return if (eat("^")) Node.Bin('^', base, unary()) else base
            }

            fun atom(): Node {
                val tok = peek() ?: throw IllegalArgumentException("İfade eksik")
                i++
                return when {
                    tok == "(" -> { val n = expr(); if (!eat(")")) throw IllegalArgumentException("Kapanmayan parantez"); n }
                    tok == "x" -> Node.X
                    tok == "pi" -> Node.Num(Math.PI)
                    tok == "e" -> Node.Num(Math.E)
                    tok in FUNCS -> {
                        // sin x  veya  sin(x)
                        val arg = if (peek() == "(") atom() else power()
                        Node.Fn(tok, arg)
                    }
                    tok[0].isDigit() || tok[0] == '.' -> Node.Num(tok.toDoubleOrNull() ?: throw IllegalArgumentException("Geçersiz sayı: $tok"))
                    else -> throw IllegalArgumentException("Beklenmeyen: '$tok'")
                }
            }
        }
    }
}

/** Fonksiyon grafiğini sayfa koordinatlarında kırık çizgilere çevirir. */
object Plot {
    /**
     * [ox],[oy] = sayfadaki orijin, [unit] = 1 birimin nokta karşılığı.
     * Sayfanın görünen x aralığı boyunca örnekler; süreksizliklerde (tan, 1/x) çizgiyi böler.
     */
    fun polylines(f: MathExpr, ox: Float, oy: Float, unit: Float, w: Float, h: Float, step: Float = 0.75f): List<FloatArray> {
        val out = ArrayList<FloatArray>()
        var cur = ArrayList<Float>()
        var prevY = Float.NaN
        val margin = h * 2
        var px = 0f
        while (px <= w + 0.01f) {
            val x = (px - ox) / unit
            val y = f.eval(x.toDouble())
            val py = (oy - y * unit).toFloat()
            val ok = y.isFinite() && py > -margin && py < h + margin
            val jump = !prevY.isNaN() && ok && abs(py - prevY) > h * 0.8f
            if (!ok || jump) {
                if (cur.size >= 4) out += cur.toFloatArray()
                cur = ArrayList()
            }
            if (ok) { cur += px; cur += py }
            prevY = if (ok) py else Float.NaN
            px += step
        }
        if (cur.size >= 4) out += cur.toFloatArray()
        // sayfa dışındaki kısımlar zaten görünmez (sayfa kırpılır); noktaları sadeleştir
        return out.filter { it.size >= 4 }.map { Geo.simplify(it, 0.15f) }
}
}
