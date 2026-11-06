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
