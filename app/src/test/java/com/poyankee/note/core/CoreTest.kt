package com.poyankee.note.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class CoreTest {
    private val rnd = Random(42)

    /** x,y çiftlerinden gürültülü x,y,p üçlüleri. */
    private fun ink(xy: FloatArray, noise: Float = 0.6f): FloatArray {
        val n = xy.size / 2
        return FloatArray(n * 3) { i ->
            when (i % 3) {
                2 -> 0.5f
                else -> xy[(i / 3) * 2 + (i % 3)] + (rnd.nextFloat() - 0.5f) * 2 * noise
            }
        }
    }

    private fun polyPath(vararg v: Float, closed: Boolean, step: Float = 2f): FloatArray {
        val pts = ArrayList<Float>()
        val n = v.size / 2
        val segs = if (closed) n else n - 1
        for (s in 0 until segs) {
            val a = s; val b = (s + 1) % n
            val len = Geo.dist(v[2 * a], v[2 * a + 1], v[2 * b], v[2 * b + 1])
            val k = (len / step).toInt().coerceAtLeast(1)
            for (i in 0 until k) {
                val t = i.toFloat() / k
                pts += v[2 * a] + (v[2 * b] - v[2 * a]) * t
                pts += v[2 * a + 1] + (v[2 * b + 1] - v[2 * a + 1]) * t
            }
        }
        val e = if (closed) 0 else n - 1
        pts += v[2 * e]; pts += v[2 * e + 1]
        return pts.toFloatArray()
    }

    private fun circlePath(cx: Float, cy: Float, rx: Float, ry: Float, from: Float = 0f, sweep: Float = Geo.TAU * 1.03f): FloatArray {
        val n = 120
        return FloatArray((n + 1) * 2) { i ->
            val a = from + sweep * (i / 2) / n
            if (i % 2 == 0) cx + rx * cos(a) else cy + ry * sin(a)
        }
    }

    @Test fun recognizesLine() {
        val g = ShapeRecognizer.recognize(ink(polyPath(10f, 10f, 200f, 120f, closed = false)))
        assertTrue(g is Geom.Polyline && g.n == 2 && !g.closed)
    }

    @Test fun recognizesCircle() {
        val g = ShapeRecognizer.recognize(ink(circlePath(100f, 100f, 60f, 60f), 1.2f))
        assertTrue("got $g", g is Geom.Circle)
        g as Geom.Circle
        assertEquals(60f, g.r, 3f)
        assertEquals(100f, g.cx, 3f)
    }

    @Test fun recognizesEllipse() {
        val g = ShapeRecognizer.recognize(ink(circlePath(100f, 100f, 90f, 40f), 0.8f))
        assertTrue("got $g", g is Geom.Ellipse)
    }

    @Test fun recognizesRectangle() {
        val g = ShapeRecognizer.recognize(ink(polyPath(20f, 20f, 180f, 22f, 179f, 120f, 21f, 118f, closed = true), 1f))
        assertTrue("got $g", g is Geom.Polyline && g.closed && g.n == 4)
        g as Geom.Polyline
        // eksene hizalanmış olmalı
        assertEquals(g.pts[1], g.pts[3], 0.01f)
    }

    @Test fun recognizesSquareNotCircle() {
        val g = ShapeRecognizer.recognize(ink(polyPath(0f, 0f, 100f, 0f, 100f, 100f, 0f, 100f, closed = true), 0.8f))
        assertTrue("got $g", g is Geom.Polyline && g.n == 4)
    }

    @Test fun recognizesTriangle() {
        val g = ShapeRecognizer.recognize(ink(polyPath(50f, 10f, 150f, 160f, 0f, 150f, closed = true), 1f))
        assertTrue("got $g", g is Geom.Polyline && g.closed && g.n == 3)
    }

    @Test fun recognizesAngle() {
        val g = ShapeRecognizer.recognize(ink(polyPath(10f, 10f, 100f, 150f, 200f, 20f, closed = false), 0.8f))
        assertTrue("got $g", g is Geom.Polyline && !g.closed && g.n == 3)
    }

    @Test fun recognizesArc() {
        val g = ShapeRecognizer.recognize(ink(circlePath(100f, 100f, 70f, 70f, 0f, Geo.PI), 0.6f))
        assertTrue("got $g", g is Geom.Arc)
    }

    @Test fun scribbleIsNotAShape() {
        val xy = FloatArray(200) { (rnd.nextFloat() * 100f) }
        assertNull(ShapeRecognizer.recognize(ink(xy, 0f)))
    }

    @Test fun snapToGridAndAngle() {
        val lat = Templates.snapLattice(Template.GRID, 595f, 842f)!!
        val sn = Snapper(lat, FloatArray(0), 5f, true)
        val (ox, oy) = Templates.gridOrigin(595f, 842f, Templates.GRID)
        val p = sn.point(ox + 41f, oy + 59f)
        assertEquals(ox + 40f, p.first, 0.01f); assertEquals(oy + 60f, p.second, 0.01f)
        val noGrid = Snapper(null, FloatArray(0), 5f, true)
        val line = noGrid.snap(Geom.Polyline(floatArrayOf(0f, 0f, 100f, 3f), false)) as Geom.Polyline
        assertEquals(0f, line.pts[3], 0.01f)
    }

    @Test fun snapToAnchor() {
        val sn = Snapper(null, floatArrayOf(50f, 50f), 6f, false)
        val g = sn.snap(Geom.Polyline(floatArrayOf(53f, 48f, 200f, 200f), false)) as Geom.Polyline
        assertEquals(50f, g.pts[0], 0.001f); assertEquals(50f, g.pts[1], 0.001f)
    }

    @Test fun outlineIsClosedPolygon() {
        val o = StrokeOutline.outline(ink(polyPath(0f, 0f, 100f, 0f, closed = false), 0f), 4f, true)
        assertTrue(o.size > 20)
        val b = Box.ofPoints(o)
        assertTrue(b.top < -0.5f && b.bottom > 0.5f && b.left < 0f && b.right > 100f)
        val dot = StrokeOutline.outline(floatArrayOf(5f, 5f, 1f), 4f, true)
        assertTrue(dot.size >= 16)
    }

    @Test fun partialEraseSplitsStroke() {
        val s = InkItem(1, InkTool.PEN, 0, 2f, ink(polyPath(0f, 0f, 100f, 0f, closed = false, step = 1f), 0f), false)
        val parts = HitTest.partialErase(s, 50f, -10f, 50f, 10f, 3f) { Ids.next() }
        assertNotNull(parts)
        assertEquals(2, parts!!.size)
        val miss = HitTest.partialErase(s, 50f, 20f, 60f, 20f, 3f) { Ids.next() }
        assertNull(miss)
    }

    @Test fun strokeEraserHitsShape() {
        val c = ShapeItem(2, Geom.Circle(100f, 100f, 50f), 0, 2f)
        assertTrue(HitTest.touches(c, 150f, 100f, 150f, 100f, 2f))
        assertTrue(!HitTest.touches(c, 100f, 100f, 100f, 100f, 2f))
    }

    @Test fun lassoSelects() {
        val a = InkItem(1, InkTool.PEN, 0, 2f, ink(polyPath(10f, 10f, 40f, 10f, closed = false), 0f))
        val b = InkItem(2, InkTool.PEN, 0, 2f, ink(polyPath(200f, 200f, 240f, 200f, closed = false), 0f))
        val sel = HitTest.lasso(listOf(a, b), floatArrayOf(0f, 0f, 100f, 0f, 100f, 100f, 0f, 100f))
        assertEquals(listOf(1L), sel.map { it.id })
    }

    @Test fun undoRedo() {
        val nb = Notebook("x", "x", 0, 0, null)
        val p = Page(1, Background.Paper(Template.BLANK, false), 100f, 100f)
        nb.pages += p
        val u = UndoStack()
        val a = ShapeItem(10, Geom.Circle(0f, 0f, 1f), 0, 1f)
        val b = ShapeItem(11, Geom.Circle(0f, 0f, 2f), 0, 1f)
        val c = ShapeItem(12, Geom.Circle(0f, 0f, 3f), 0, 1f)
        u.apply(p, emptyList(), listOf(a, b, c))
        u.apply(p, listOf(b), emptyList())
        assertEquals(listOf(10L, 12L), p.items.map { it.id })
        u.undo(nb)
        assertEquals(listOf(10L, 11L, 12L), p.items.map { it.id })
        u.redo(nb)
        assertEquals(listOf(10L, 12L), p.items.map { it.id })
        u.undo(nb); u.undo(nb)
        assertTrue(p.items.isEmpty())
        assertTrue(!u.canUndo)
    }

    @Test fun recordBatchedEdit() {
        val nb = Notebook("x", "x", 0, 0, null)
        val p = Page(1, Background.Paper(Template.BLANK, false), 100f, 100f)
        nb.pages += p
        val a = ShapeItem(10, Geom.Circle(0f, 0f, 1f), 0, 1f)
        val b = ShapeItem(11, Geom.Circle(0f, 0f, 2f), 0, 1f)
        val c = ShapeItem(12, Geom.Circle(0f, 0f, 3f), 0, 1f)
        p.items += listOf(a, b, c)
        val before = p.items.toList()
        p.items.remove(b); p.items.remove(a)
        val d = ShapeItem(13, Geom.Circle(0f, 0f, 4f), 0, 1f)
        p.items += d
        val u = UndoStack()
        u.record(p, before)
        u.undo(nb)
        assertEquals(listOf(10L, 11L, 12L), p.items.map { it.id })
        u.redo(nb)
        assertEquals(listOf(12L, 13L), p.items.map { it.id })
    }

    @Test fun codecRoundTrip() {
        val nb = Notebook("id1", "Türev — ğüşıöç", 1, 2, "source.pdf", 8, Background.Paper(Template.COORDINATE, true), PageSize.WIDE, "Matematik 9")
        nb.pages += Page(5, Background.PdfPage(3), 595f, 842f)
        nb.pages += Page(6, Background.Paper(Template.ISOMETRIC, PaperColors.CREAM, 14f, 0.35f), 960f, 540f)
        nb.bookmarks += Bookmark(6, "Test 3")
        val bo = ByteArrayOutputStream()
        Codec.writeNotebook(nb, DataOutputStream(bo))
        val nb2 = Codec.readNotebook(DataInputStream(ByteArrayInputStream(bo.toByteArray())))
        assertEquals(nb.name, nb2.name); assertEquals(8, nb2.pageOffset); assertEquals("source.pdf", nb2.pdfFile)
        assertEquals(Background.PdfPage(3), nb2.pages[0].background)
        assertEquals(Background.Paper(Template.ISOMETRIC, PaperColors.CREAM, 14f, 0.35f), nb2.pages[1].background)
        assertEquals("Matematik 9", nb2.folder)
        assertTrue(nb2.defaultPaper.dark)
        assertEquals("Test 3", nb2.bookmarks[0].title)

        val items = listOf(
            InkItem(1, InkTool.HIGHLIGHTER, 0x55FF0000, 12f, floatArrayOf(1f, 2f, 0.5f, 3f, 4f, 0.7f), true),
            ShapeItem(2, Geom.Arc(1f, 2f, 3f, 0.1f, -1f), 7, 2f, true),
            ShapeItem(3, Geom.Polyline(floatArrayOf(0f, 0f, 1f, 1f), false), 7, 2f),
            TextItem(4, 10f, 20f, "x² + ğ\nikinci satır", 14f, -1, 0.3f),
            ImageItem(5, 1f, 2f, 30f, 40f, "img1.jpg", 0f),
        )
        val io = ByteArrayOutputStream()
        Codec.writeItems(items, DataOutputStream(io))
        val back = Codec.readItems(DataInputStream(ByteArrayInputStream(io.toByteArray())))
        assertEquals(items.map { it.id }, back.map { it.id })
        val ink = back[0] as InkItem
        assertEquals(InkTool.HIGHLIGHTER, ink.tool); assertEquals(0.7f, ink.pts[5], 0f)
        val arc = (back[1] as ShapeItem).geom as Geom.Arc
        assertEquals(-1f, arc.sweep, 0f)
        assertEquals("x² + ğ\nikinci satır", (back[3] as TextItem).text)
        assertEquals("img1.jpg", (back[4] as ImageItem).file)
    }

    @Test fun readsVersion1Notebook() {
        // Sürüm 1 dosyası elle yazılır: eski defterler yeni sürümde açılabilmeli
        val bo = ByteArrayOutputStream()
        val o = DataOutputStream(bo)
        o.writeInt(0x4E4F5445); o.writeInt(1)
        o.writeUTF("old"); o.writeUTF("Eski"); o.writeLong(1); o.writeLong(2)
        o.writeBoolean(false); o.writeInt(0)
        o.writeInt(Template.DOTS.ordinal); o.writeBoolean(true); o.writeInt(PageSize.A4_PORTRAIT.ordinal)
        o.writeInt(1)
        o.writeLong(9); o.writeFloat(595f); o.writeFloat(842f); o.writeByte(0); o.writeInt(Template.GRID.ordinal); o.writeBoolean(true)
        o.writeInt(0)
        val nb = Codec.readNotebook(DataInputStream(ByteArrayInputStream(bo.toByteArray())))
        assertEquals(Template.DOTS, nb.defaultPaper.template)
        assertTrue(nb.defaultPaper.dark)
        val bg = nb.pages[0].background as Background.Paper
        assertEquals(Template.GRID, bg.template); assertTrue(bg.dark); assertEquals(Templates.GRID, bg.spacing, 0f)
        assertEquals("", nb.folder)
    }

    @Test fun spacingChangesGeometry() {
        val a = Templates.geometry(Template.GRID, 595f, 842f, 20f).lines.size
        val b = Templates.geometry(Template.GRID, 595f, 842f, 10f).lines.size
        assertTrue(b > a * 1.8)
    }

    @Test fun bookPageNumbers() {
        val nb = Notebook("x", "x", 0, 0, "a.pdf", pageOffset = 2)
        nb.pages += Page(1, Background.PdfPage(0), 1f, 1f)
        nb.pages += Page(2, Background.PdfPage(1), 1f, 1f)
        nb.pages += Page(3, Background.Paper(Template.GRID, false), 1f, 1f) // araya eklenmiş boş sayfa
        nb.pages += Page(4, Background.PdfPage(2), 1f, 1f)
        nb.pages += Page(5, Background.PdfPage(3), 1f, 1f)
        assertEquals(3, PageNumbers.indexOfBookPage(nb, 1))
        assertEquals(4, PageNumbers.indexOfBookPage(nb, 2))
        assertNull(PageNumbers.indexOfBookPage(nb, 9))
        assertEquals(-1, PageNumbers.bookNumberOf(nb, nb.pages[0]))
        assertNull(PageNumbers.bookNumberOf(nb, nb.pages[2]))
    }

    @Test fun marksRightAngle() {
        val tri = ShapeItem(1, Geom.Polyline(floatArrayOf(0f, 0f, 100f, 0f, 0f, 100f), true), 0, 1f)
        val g = Marks.create(listOf(tri), 3f, 3f, Marks.Kind.ANGLE, 1, 10f)
        assertEquals(1, g.size)
        assertTrue(g[0] is Geom.Polyline) // dik açı karesi
        val g2 = Marks.create(listOf(tri), 95f, 2f, Marks.Kind.ANGLE, 1, 10f)
        assertTrue(g2[0] is Geom.Arc)
        val t = Marks.create(listOf(tri), 50f, 1f, Marks.Kind.TICK, 2, 10f)
        assertEquals(2, t.size)
    }

    @Test fun similarityKeepsCircle() {
        val c = ShapeItem(1, Geom.Circle(10f, 0f, 5f), 0, 2f)
        val t = c.transformed(Similarity(scale = 2f, angle = Geo.PI / 2, ox = 0f, oy = 0f))
        val g = (t as ShapeItem).geom as Geom.Circle
        assertEquals(0f, g.cx, 0.001f); assertEquals(20f, g.cy, 0.001f); assertEquals(10f, g.r, 0.001f)
        assertEquals(4f, t.width, 0.001f)
    }

    @Test fun templatesProduceGeometry() {
        for (t in Template.entries) {
            val g = Templates.geometry(t, 595f, 842f)
            if (t != Template.BLANK) assertTrue(t.name, g.lines.isNotEmpty() || g.dots.isNotEmpty())
        }
        val iso = Templates.snapLattice(Template.ISOMETRIC, 595f, 842f)!!
        val p = iso.nearest(300f, 421f)
        assertTrue(abs(p.first - 300f) < 20f)
    }
}
