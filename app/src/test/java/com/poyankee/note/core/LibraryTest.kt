package com.poyankee.note.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryTest {
    @Test
    fun everyEntryBuildsAndFits() {
        assertTrue(Library.entries.size >= 60)
        val ids = HashSet<String>()
        for (e in Library.entries) {
            assertTrue("aynı kimlik: ${e.id}", ids.add(e.id))
            assertTrue("bilinmeyen kategori: ${e.category}", e.category in Library.categories)
            val items = e.build()
            assertTrue("${e.title} boş", items.isNotEmpty())
            val b = Library.bounds(items)!!
            assertTrue("${e.title} çok büyük: ${b.width}x${b.height}", b.width < 700f && b.height < 800f)
            assertTrue("${e.title} sayı değil", !b.left.isNaN() && !b.top.isNaN())
        }
    }

    @Test
    fun placeCentersAndShrinks() {
        val items = Library.entries.first { it.id == "unit_circle" }.build()
        val placed = Library.place(items, 300f, 400f, 100f, 100f)
        val b = Library.bounds(placed)!!
        assertEquals(300f, b.cx, 2f)
        assertEquals(400f, (b.top + b.bottom) / 2, 2f)
        assertTrue(b.width <= 101f && b.height <= 101f)
        assertEquals(items.size, placed.size)
        assertTrue(placed.map { it.id }.toSet().size == placed.size)
    }

    @Test
    fun fractions() {
        val pie = Library.fractionPie(3, 8)
        assertEquals(8, pie.count { it is ShapeItem })
        assertEquals(3, pie.count { it is ShapeItem && it.fill != 0 })
        assertEquals(5, Library.fractionBar(2, 5).count { it is ShapeItem })
        assertTrue(Library.fractionOnLine(7, 4).isNotEmpty())
    }
}

class PageRangesTest {
    @Test
    fun parse() {
        assertEquals(listOf(2, 3, 4, 5, 6, 11), PageRanges.parse("3-7, 12", 20))
        assertEquals(listOf(0), PageRanges.parse("1", 1))
        assertEquals(listOf(4, 3, 2).sorted(), PageRanges.parse("5-3", 9)!!.sorted())
        assertEquals(null, PageRanges.parse("0", 5))
        assertEquals(null, PageRanges.parse("3-9", 5))
        assertEquals(null, PageRanges.parse("a", 5))
        assertEquals(null, PageRanges.parse("", 5))
    }
}

class MeasureTest {
    @Test
    fun rightTriangle() {
        val k = Measure.PT_PER_CM
        // 3-4-5 üçgeni (y aşağı)
        val m = Measure.polygon(floatArrayOf(0f, 0f, 4 * k, 0f, 0f, 3 * k))
        assertEquals(4f, m.sides[0], 0.01f); assertEquals(5f, m.sides[1], 0.01f); assertEquals(3f, m.sides[2], 0.01f)
        assertEquals(6f, m.area, 0.01f); assertEquals(12f, m.perimeter, 0.01f)
        assertEquals(90f, m.angles[0], 0.1f)
        assertEquals(180f, m.angles.sum(), 0.1f)
        // içbükey dörtgen: iç açılar toplamı 360
        val c = Measure.polygon(floatArrayOf(0f, 0f, 100f, 0f, 50f, 30f, 50f, 100f))
        assertEquals(360f, c.angles.sum(), 0.5f)
        assertTrue(c.angles.any { it > 180f })
        assertEquals("2,5", Measure.fmt(2.46f)); assertEquals("3", Measure.fmt(3.01f))
    }
}

class ConnectorsTest {
    @Test
    fun arrowFollowsBox() {
        val box = ShapeItem(1, Geom.Polyline(floatArrayOf(0f, 0f, 100f, 0f, 100f, 50f, 0f, 50f), true), 0, 1f)
        val parts = Shapes.arrow(100f, 25f, 200f, 25f, 8f)
        val shaft = ShapeItem(2, parts[0], 0, 1f)
        val head = ShapeItem(3, parts[1], 0, 1f)
        val far = ShapeItem(4, Geom.Polyline(floatArrayOf(300f, 300f, 400f, 300f), false), 0, 1f)
        val items = listOf(box, shaft, head, far)
        val t = Similarity.translate(0f, 100f)
        val r = Connectors.follow(items, setOf(1L), box.bounds(), t)
        assertEquals(2, r.size)
        val ns = (r.first { it.first.id == 2L }.second as ShapeItem).geom as Geom.Polyline
        assertEquals(125f, ns.pts[1], 0.01f)   // kutuya değen uç taşındı
        assertEquals(25f, ns.pts[3], 0.01f)    // öbür uç yerinde
        val nh = (r.first { it.first.id == 3L }.second as ShapeItem).geom as Geom.Polyline
        assertEquals(200f, nh.pts[2], 0.01f); assertEquals(25f, nh.pts[3], 0.01f)
        assertTrue(r.none { it.first.id == 4L })
    }
}
