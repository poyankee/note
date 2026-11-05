package com.poyankee.note.core

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * Defterlerin ikili (binary) kayıt formatı. Sürüm numarası sayesinde ileride format genişletilebilir.
 * notebook.bin  → defter bilgisi + sayfa listesi
 * p_<id>.bin    → bir sayfanın öğeleri
 */
object Codec {
    private const val NB_MAGIC = 0x4E4F5445 // "NOTE"
    private const val PG_MAGIC = 0x50414745 // "PAGE"
    private const val VERSION = 3

    fun writeNotebook(nb: Notebook, out: DataOutputStream) {
        out.writeInt(NB_MAGIC); out.writeInt(VERSION)
        out.writeUTF(nb.id); out.writeUTF(nb.name)
        out.writeLong(nb.created); out.writeLong(nb.updated)
        out.writeBoolean(nb.pdfFile != null); nb.pdfFile?.let { out.writeUTF(it) }
        out.writeInt(nb.pageOffset)
        writePaper(nb.defaultPaper, out); out.writeInt(nb.defaultSize.ordinal); out.writeUTF(nb.folder)
        out.writeBoolean(nb.favorite); out.writeUTF(nb.lockHash)
        out.writeInt(nb.pages.size)
        for (p in nb.pages) {
            out.writeLong(p.id); out.writeFloat(p.width); out.writeFloat(p.height)
            when (val bg = p.background) {
                is Background.Paper -> { out.writeByte(0); writePaper(bg, out) }
                is Background.PdfPage -> {
                    out.writeByte(1); out.writeInt(bg.index); out.writeInt(bg.rotation)
                    out.writeFloat(bg.crop.l); out.writeFloat(bg.crop.t); out.writeFloat(bg.crop.r); out.writeFloat(bg.crop.b)
                    out.writeFloat(bg.fullW); out.writeFloat(bg.fullH)
                }
            }
        }
        out.writeInt(nb.bookmarks.size)
        for (b in nb.bookmarks) { out.writeLong(b.pageId); out.writeUTF(b.title) }
    }

    /** Sayfa öğeleri olmadan defteri okur (öğeler [readPage] ile ayrıca yüklenir). */
    fun readNotebook(inp: DataInputStream): Notebook {
        if (inp.readInt() != NB_MAGIC) throw IOException("Geçersiz defter dosyası")
        val ver = inp.readInt()
        if (ver > VERSION) throw IOException("Bu defter uygulamanın daha yeni bir sürümüyle kaydedilmiş")
        val id = inp.readUTF(); val name = inp.readUTF()
        val created = inp.readLong(); val updated = inp.readLong()
        val pdf = if (inp.readBoolean()) inp.readUTF() else null
        val offset = inp.readInt()
        val paper = readPaper(inp, ver)
        val size = PageSize.entries[inp.readInt()]
        val folder = if (ver >= 2) inp.readUTF() else ""
        val fav = if (ver >= 3) inp.readBoolean() else false
        val lock = if (ver >= 3) inp.readUTF() else ""
        val nb = Notebook(id, name, created, updated, pdf, offset, paper, size, folder, fav, lock)
        val pc = inp.readInt()
        repeat(pc) {
            val pid = inp.readLong(); val w = inp.readFloat(); val h = inp.readFloat()
            val bg = when (inp.readByte().toInt()) {
                0 -> readPaper(inp, ver)
                else -> if (ver >= 3) Background.PdfPage(
                    inp.readInt(), inp.readInt(),
                    Crop(inp.readFloat(), inp.readFloat(), inp.readFloat(), inp.readFloat()),
                    inp.readFloat(), inp.readFloat(),
                ) else Background.PdfPage(inp.readInt())
            }
            nb.pages += Page(pid, bg, w, h)
        }
        val bc = inp.readInt()
        repeat(bc) { nb.bookmarks += Bookmark(inp.readLong(), inp.readUTF()) }
        return nb
    }

    private fun writePaper(p: Background.Paper, out: DataOutputStream) {
        out.writeInt(p.template.ordinal); out.writeInt(p.color); out.writeFloat(p.spacing); out.writeFloat(p.opacity)
        out.writeUTF(p.image)
    }

    private fun readPaper(inp: DataInputStream, ver: Int): Background.Paper {
        val t = Template.entries[inp.readInt()]
        if (ver < 2) return Background.Paper(t, inp.readBoolean())
        val c = inp.readInt(); val sp = inp.readFloat(); val op = inp.readFloat()
        val img = if (ver >= 3) inp.readUTF() else ""
        return Background.Paper(t, c, sp, op, img)
    }

    fun writeItems(items: List<Item>, out: DataOutputStream) {
        out.writeInt(PG_MAGIC); out.writeInt(VERSION)
        out.writeInt(items.size)
        for (it in items) writeItem(it, out)
    }

    fun readItems(inp: DataInputStream): List<Item> {
        if (inp.readInt() != PG_MAGIC) throw IOException("Geçersiz sayfa dosyası")
        val ver = inp.readInt()
        val n = inp.readInt()
        val list = ArrayList<Item>(n)
        repeat(n) { list += readItem(inp, ver) }
        return list
    }

    private fun writeFloats(a: FloatArray, out: DataOutputStream) {
        out.writeInt(a.size)
        for (f in a) out.writeFloat(f)
    }

    private fun readFloats(inp: DataInputStream): FloatArray {
        val n = inp.readInt()
        return FloatArray(n) { inp.readFloat() }
    }

    fun writeItem(it: Item, out: DataOutputStream) {
        when (it) {
            is InkItem -> {
                out.writeByte(1); out.writeLong(it.id); out.writeByte(it.tool.ordinal)
                out.writeInt(it.color); out.writeFloat(it.width); out.writeBoolean(it.pressure)
                writeFloats(it.pts, out)
            }
            is ShapeItem -> {
                out.writeByte(2); out.writeLong(it.id); out.writeInt(it.color); out.writeFloat(it.width); out.writeBoolean(it.dashed)
                when (val g = it.geom) {
                    is Geom.Polyline -> { out.writeByte(0); out.writeBoolean(g.closed); writeFloats(g.pts, out) }
                    is Geom.Circle -> { out.writeByte(1); out.writeFloat(g.cx); out.writeFloat(g.cy); out.writeFloat(g.r) }
                    is Geom.Ellipse -> { out.writeByte(2); out.writeFloat(g.cx); out.writeFloat(g.cy); out.writeFloat(g.rx); out.writeFloat(g.ry); out.writeFloat(g.rot) }
                    is Geom.Arc -> { out.writeByte(3); out.writeFloat(g.cx); out.writeFloat(g.cy); out.writeFloat(g.r); out.writeFloat(g.start); out.writeFloat(g.sweep) }
                }
                out.writeInt(it.fill)
            }
            is TextItem -> {
                out.writeByte(3); out.writeLong(it.id); out.writeFloat(it.x); out.writeFloat(it.y)
                out.writeUTF(it.text); out.writeFloat(it.size); out.writeInt(it.color); out.writeFloat(it.rotation)
                out.writeInt(it.bg)
            }
            is ImageItem -> {
                out.writeByte(4); out.writeLong(it.id); out.writeFloat(it.x); out.writeFloat(it.y)
                out.writeFloat(it.w); out.writeFloat(it.h); out.writeUTF(it.file); out.writeFloat(it.rotation)
                out.writeUTF(it.source)
            }
        }
    }

    fun readItem(inp: DataInputStream, ver: Int = VERSION): Item = when (val t = inp.readByte().toInt()) {
        1 -> {
            val id = inp.readLong(); val tool = InkTool.entries[inp.readByte().toInt()]
            val color = inp.readInt(); val w = inp.readFloat(); val pr = inp.readBoolean()
            InkItem(id, tool, color, w, readFloats(inp), pr)
        }
        2 -> {
            val id = inp.readLong(); val color = inp.readInt(); val w = inp.readFloat(); val dashed = inp.readBoolean()
            val g = when (inp.readByte().toInt()) {
                0 -> { val closed = inp.readBoolean(); Geom.Polyline(readFloats(inp), closed) }
                1 -> Geom.Circle(inp.readFloat(), inp.readFloat(), inp.readFloat())
                2 -> Geom.Ellipse(inp.readFloat(), inp.readFloat(), inp.readFloat(), inp.readFloat(), inp.readFloat())
                else -> Geom.Arc(inp.readFloat(), inp.readFloat(), inp.readFloat(), inp.readFloat(), inp.readFloat())
            }
            ShapeItem(id, g, color, w, dashed, if (ver >= 3) inp.readInt() else 0)
        }
        3 -> TextItem(
            inp.readLong(), inp.readFloat(), inp.readFloat(), inp.readUTF(), inp.readFloat(), inp.readInt(), inp.readFloat(),
            if (ver >= 3) inp.readInt() else 0,
        )
        4 -> ImageItem(
            inp.readLong(), inp.readFloat(), inp.readFloat(), inp.readFloat(), inp.readFloat(), inp.readUTF(), inp.readFloat(),
            if (ver >= 3) inp.readUTF() else "",
        )
        else -> throw IOException("Bilinmeyen öğe türü $t")
    }
}
