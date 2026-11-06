package com.poyankee.note.core

import kotlin.math.cos
import kotlin.math.sin

/**
 * Bağlı oklar (kavram haritası): seçilen kutular taşınınca, ucu o kutulara değen düz çizgi ve oklar
 * da uçlarıyla birlikte gelir. Ok başı (3 noktalı çizgi) yeni yöne göre yeniden çizilir.
 */
object Connectors {
    private const val TOUCH = 10f

    private fun isShaft(it: Item) = it is ShapeItem && it.geom is Geom.Polyline && (it.geom as Geom.Polyline).let { g -> !g.closed && g.n == 2 }
    private fun isHead(it: Item) = it is ShapeItem && it.geom is Geom.Polyline && (it.geom as Geom.Polyline).let { g -> !g.closed && g.n == 3 }

    /**
     * [items] sayfadaki öğeler, [movedIds] taşınan öğeler, [oldBox] taşınmadan önceki kapsayan kutu, [t] uygulanan dönüşüm.
     * Değişmesi gereken (eski, yeni) çiftlerini döndürür.
     */
    fun follow(items: List<Item>, movedIds: Set<Long>, oldBox: Box, t: Similarity): List<Pair<Item, Item>> {
        if (t.isIdentity) return emptyList()
        val zone = oldBox.inflate(TOUCH)
        val out = ArrayList<Pair<Item, Item>>()
        // uç noktası taşınan gövdeler: eski uç → yeni uç
        val movedEnds = ArrayList<Triple<Float, Float, Pair<Float, Float>>>() // eski x, eski y, yeni (x,y) ve yön için diğer uç
        val newShafts = HashMap<Long, FloatArray>()
        for (it in items) {
            if (it.id in movedIds || !isShaft(it)) continue
            val s = it as ShapeItem
            val p = (s.geom as Geom.Polyline).pts
            val aIn = zone.contains(p[0], p[1]); val bIn = zone.contains(p[2], p[3])
            if (!aIn && !bIn) continue
            if (aIn && bIn) continue // iki ucu da aynı kutuda: kutunun içindeki bir çizgi, dokunma
            val np = p.copyOf()
            if (aIn) { np[0] = t.mapX(p[0], p[1]); np[1] = t.mapY(p[0], p[1]) }
            if (bIn) { np[2] = t.mapX(p[2], p[3]); np[3] = t.mapY(p[2], p[3]) }
            newShafts[s.id] = np
            out += s to s.withGeom(Geom.Polyline(np, false))
            // ok başı genellikle 2. uçtadır; her iki uç için de bilgi tut
            movedEnds += Triple(p[2], p[3], np[2] to np[3])
            movedEnds += Triple(p[0], p[1], np[0] to np[1])
        }
        if (newShafts.isEmpty()) return out
        // ok başlarını yeni uca ve yöne göre yeniden çiz
        for (it in items) {
            if (it.id in movedIds || !isHead(it)) continue
            val h = it as ShapeItem
            val hp = (h.geom as Geom.Polyline).pts
            val tipX = hp[2]; val tipY = hp[3]
            for ((sid, np) in newShafts) {
                val old = (items.first { x -> x.id == sid } as ShapeItem).geom as Geom.Polyline
                val atB = Geo.dist(old.pts[2], old.pts[3], tipX, tipY) < 0.6f
                val atA = Geo.dist(old.pts[0], old.pts[1], tipX, tipY) < 0.6f
                if (!atA && !atB) continue
                val (fx, fy, tx, ty) = if (atB) listOf(np[0], np[1], np[2], np[3]) else listOf(np[2], np[3], np[0], np[1])
                val len = Geo.dist(hp[0], hp[1], tipX, tipY)
                val a = Geo.angleOf(fx, fy, tx, ty)
                val h1 = a + Geo.PI - 0.45f; val h2 = a + Geo.PI + 0.45f
                val nh = floatArrayOf(tx + len * cos(h1), ty + len * sin(h1), tx, ty, tx + len * cos(h2), ty + len * sin(h2))
                out += h to h.withGeom(Geom.Polyline(nh, false))
                break
            }
        }
        return out
    }
}
