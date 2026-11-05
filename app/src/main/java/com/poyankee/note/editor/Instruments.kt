package com.poyankee.note.editor

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.poyankee.note.core.Geo
import com.poyankee.note.core.Templates
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** Kalemin yapışacağı kılavuz: düz kenar ya da çember (iletki yayı). */
sealed class Guide {
    abstract fun project(x: Float, y: Float): Pair<Float, Float>
    class Line(val ax: Float, val ay: Float, val bx: Float, val by: Float) : Guide() {
        override fun project(x: Float, y: Float) = Geo.projectOnLine(x, y, ax, ay, bx, by)
    }
    class Circle(val cx: Float, val cy: Float, val r: Float) : Guide() {
        override fun project(x: Float, y: Float): Pair<Float, Float> {
            val d = Geo.dist(x, y, cx, cy)
            if (d < 1e-3f) return (cx + r) to cy
            return (cx + (x - cx) * r / d) to (cy + (y - cy) * r / d)
        }
    }
}

enum class InstrumentKind(val title: String) {
    RULER("Cetvel"), PROTRACTOR("İletki"), SET_SQUARE_45("Gönye 45°"), SET_SQUARE_60("Gönye 30°-60°")
}

/**
 * Ekrandaki geometri aracı. Belge koordinatlarında durur (sayfayla birlikte kayar/yakınlaşır).
 * Parmakla taşınır, iki parmakla döndürülür; kalem kenarına yaklaşınca çizgi kenara yapışır.
 */
class Instrument(val kind: InstrumentKind, var cx: Float, var cy: Float, var angle: Float = 0f) {

    companion object {
        const val RULER_L = 480f
        const val RULER_H = 58f
        const val PROT_R = 160f
        const val SQ_A = 270f
        val SQ60_B = SQ_A * tan(Geo.toRad(30f))
    }

    fun toLocal(x: Float, y: Float): Pair<Float, Float> {
        val dx = x - cx; val dy = y - cy
        val c = cos(angle); val s = sin(angle)
        return (dx * c + dy * s) to (-dx * s + dy * c)
    }

    fun toWorld(u: Float, v: Float): Pair<Float, Float> {
        val c = cos(angle); val s = sin(angle)
        return (cx + u * c - v * s) to (cy + u * s + v * c)
    }

    private fun polygonLocal(): FloatArray = when (kind) {
        InstrumentKind.RULER -> floatArrayOf(-RULER_L / 2, -RULER_H / 2, RULER_L / 2, -RULER_H / 2, RULER_L / 2, RULER_H / 2, -RULER_L / 2, RULER_H / 2)
        InstrumentKind.PROTRACTOR -> floatArrayOf() // özel
        InstrumentKind.SET_SQUARE_45 -> floatArrayOf(0f, 0f, SQ_A, 0f, 0f, -SQ_A)
        InstrumentKind.SET_SQUARE_60 -> floatArrayOf(0f, 0f, SQ_A, 0f, 0f, -SQ60_B)
    }

    fun contains(x: Float, y: Float): Boolean {
        val (u, v) = toLocal(x, y)
        return when (kind) {
            InstrumentKind.PROTRACTOR -> (v <= 16f && v >= -PROT_R && Geo.len(u, v) <= PROT_R && v <= 0f) || (v in 0f..16f && abs(u) <= PROT_R)
            else -> Geo.pointInPolygon(u, v, polygonLocal())
        }
    }

    /** Kenarlar (yerel koordinat: ax, ay, bx, by). */
    private fun edgesLocal(): List<FloatArray> = when (kind) {
        InstrumentKind.RULER -> listOf(
            floatArrayOf(-RULER_L / 2, -RULER_H / 2, RULER_L / 2, -RULER_H / 2),
            floatArrayOf(-RULER_L / 2, RULER_H / 2, RULER_L / 2, RULER_H / 2),
        )
        InstrumentKind.PROTRACTOR -> listOf(floatArrayOf(-PROT_R, 0f, PROT_R, 0f))
        else -> {
            val p = polygonLocal()
            listOf(
                floatArrayOf(p[0], p[1], p[2], p[3]),
                floatArrayOf(p[2], p[3], p[4], p[5]),
                floatArrayOf(p[4], p[5], p[0], p[1]),
            )
        }
    }

    /** Kalem (x,y) bir kenara [tol] kadar yakınsa o kenarın kılavuzu. */
    fun guideNear(x: Float, y: Float, tol: Float): Guide? {
        val (u, v) = toLocal(x, y)
        var best: FloatArray? = null
        var bd = tol
        for (e in edgesLocal()) {
            val d = Geo.segDist(u, v, e[0], e[1], e[2], e[3])
            if (d < bd) { bd = d; best = e }
        }
        if (kind == InstrumentKind.PROTRACTOR && v <= 0f) {
            val d = abs(Geo.len(u, v) - PROT_R)
            if (d < bd) {
                return Guide.Circle(cx, cy, PROT_R)
            }
        }
        val e = best ?: return null
        val (ax, ay) = toWorld(e[0], e[1]); val (bx, by) = toWorld(e[2], e[3])
        return Guide.Line(ax, ay, bx, by)
    }

    /** İletki için: (x,y) noktasının taban çizgisine göre açısı (0..180°), değilse null. */
    fun protractorAngle(x: Float, y: Float): Float? {
        if (kind != InstrumentKind.PROTRACTOR) return null
        val (u, v) = toLocal(x, y)
        if (v > 2f || Geo.len(u, v) > PROT_R * 1.5f || Geo.len(u, v) < 8f) return null
        return Geo.toDeg(kotlin.math.atan2(-v, u)).coerceIn(0f, 180f)
    }

    /** Görüntülenen dönüş açısı (0..179°). */
    fun angleDegrees(): Int {
        var d = Geo.toDeg(angle) % 180f
        if (d < 0) d += 180f
        return (if (d.roundToInt() == 180) 0 else d.roundToInt())
    }

    /* ---------------- çizim (belge koordinatlarında) ---------------- */

    private val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0x30607D8B }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0xCC37474F.toInt(); strokeWidth = 0.8f }
    private val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0xDD263238.toInt(); strokeWidth = 0.5f }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF263238.toInt(); textSize = 8f; textAlign = Paint.Align.CENTER }
    private val big = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D47A1.toInt(); textSize = 13f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }

    fun draw(c: Canvas) {
        c.save()
        c.translate(cx, cy)
        c.rotate(Geo.toDeg(angle))
        when (kind) {
            InstrumentKind.RULER -> drawRuler(c)
            InstrumentKind.PROTRACTOR -> drawProtractor(c)
            else -> drawSquare(c)
        }
        c.restore()
    }

    private fun drawRuler(c: Canvas) {
        val r = RectF(-RULER_L / 2, -RULER_H / 2, RULER_L / 2, RULER_H / 2)
        c.drawRoundRect(r, 4f, 4f, body)
        c.drawRoundRect(r, 4f, 4f, edge)
        val mm = Templates.MM
        val start = -RULER_L / 2 + 10f
        var k = 0
        while (start + k * mm <= RULER_L / 2 - 6f) {
            val x = start + k * mm
            val len = if (k % 10 == 0) 12f else if (k % 5 == 0) 8f else 4.5f
            c.drawLine(x, -RULER_H / 2, x, -RULER_H / 2 + len, tick)
            c.drawLine(x, RULER_H / 2, x, RULER_H / 2 - len, tick)
            if (k % 10 == 0) c.drawText((k / 10).toString(), x, -RULER_H / 2 + 21f, label)
            k++
        }
        c.drawText("${angleDegrees()}°", 0f, 9f, big)
    }

    private fun drawProtractor(c: Canvas) {
        val R = PROT_R
        val p = Path()
        p.moveTo(-R, 16f); p.lineTo(-R, 0f)
        p.arcTo(RectF(-R, -R, R, R), 180f, 180f)
        p.lineTo(R, 16f); p.close()
        c.drawPath(p, body); c.drawPath(p, edge)
        c.drawLine(-R, 0f, R, 0f, edge)
        for (d in 0..180) {
            val a = Geo.toRad(d.toFloat())
            val len = if (d % 10 == 0) 14f else if (d % 5 == 0) 9f else 5f
            val ca = cos(a); val sa = -sin(a)
            c.drawLine(R * ca, R * sa, (R - len) * ca, (R - len) * sa, tick)
            if (d % 10 == 0) {
                c.drawText(d.toString(), (R - 22f) * ca, (R - 22f) * sa + 3f, label)
                c.drawText((180 - d).toString(), (R - 36f) * ca, (R - 36f) * sa + 3f, label.apply { color = 0xFFB71C1C.toInt() })
                label.color = 0xFF263238.toInt()
            }
        }
        c.drawCircle(0f, 0f, 2.5f, tick)
        c.drawLine(0f, 0f, 0f, -12f, tick)
        c.drawText("${angleDegrees()}°", 0f, 13f, big)
    }

    private fun drawSquare(c: Canvas) {
        val pts = polygonLocal()
        val p = Path()
        p.moveTo(pts[0], pts[1]); p.lineTo(pts[2], pts[3]); p.lineTo(pts[4], pts[5]); p.close()
        c.drawPath(p, body); c.drawPath(p, edge)
        // kenar cetvel çentikleri (yatay kenar)
        val mm = Templates.MM
        var k = 0
        while (k * mm <= pts[2] - 6f) {
            val x = k * mm
            val len = if (k % 10 == 0) 10f else if (k % 5 == 0) 7f else 4f
            c.drawLine(x, 0f, x, -len, tick)
            if (k % 10 == 0 && k > 0) c.drawText((k / 10).toString(), x, -15f, label)
            k++
        }
        k = 0
        val vLen = -pts[5]
        while (k * mm <= vLen - 6f) {
            val y = -k * mm
            val len = if (k % 10 == 0) 10f else if (k % 5 == 0) 7f else 4f
            c.drawLine(0f, y, len, y, tick)
            k++
        }
        // iç açı etiketleri
        val small = if (kind == InstrumentKind.SET_SQUARE_45) "45°" else "30°"
        val top = if (kind == InstrumentKind.SET_SQUARE_45) "45°" else "60°"
        c.drawText("90°", 22f, -10f, label)
        c.drawText(small, pts[2] - 34f, -6f, label)
        c.drawText(top, 10f, pts[5] + 40f, label)
        val cxl = pts[2] / 3f; val cyl = pts[5] / 3f
        c.drawText("${angleDegrees()}°", cxl, cyl + 4f, big)
    }

    /** Aracın sığması için yaklaşık yarıçap. */
    fun radius(): Float = when (kind) {
        InstrumentKind.RULER -> RULER_L / 2
        InstrumentKind.PROTRACTOR -> PROT_R
        else -> sqrt(SQ_A * SQ_A + SQ_A * SQ_A) / 2
    }
}
