package com.poyankee.note.data

import com.poyankee.note.core.L

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.graphics.pdf.PdfRenderer
import com.poyankee.note.core.Background
import com.poyankee.note.core.Ids
import com.poyankee.note.core.Notebook
import com.poyankee.note.core.Page
import com.poyankee.note.core.PageSize
import com.poyankee.note.core.PaperColors
import com.poyankee.note.core.Similarity
import com.poyankee.note.core.Template
import com.poyankee.note.editor.Exporter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File
import java.io.FileOutputStream

/**
 * PDF ve defter düzenleme: birleştirme, sayfaları ayrı deftere çıkarma, başka PDF'ten sayfa ekleme,
 * fotoğraflardan defter. Hepsi arka planda çağrılmalı; açık defter varsa önce kapatılmalı.
 */
object PdfOps {
    private val skip = Regex("^(notebook\\.bin|p_.*\\.bin|.*\\.tmp|.*\\.pdf|cover\\.png|deleted\\.txt|.*\\.broken)$")

    /** Defter klasöründeki görselleri (img_, fx_, bg_, wq_ ...) hedefe kopyalar. */
    private fun copyAssets(from: File, to: File) {
        from.listFiles()?.forEach { f ->
            if (f.isFile && !skip.matches(f.name)) {
                val t = File(to, f.name)
                if (!t.exists()) f.copyTo(t)
            }
        }
    }

    private fun copyPage(p: Page, bg: Background): Page {
        val np = Page(Ids.next(), bg, p.width, p.height)
        np.items.addAll(p.items.map { it.transformed(Similarity(), Ids.next()) })
        return np
    }

    /**
     * Verilen (defter, sayfa sıraları) parçalarından yeni bir defter kurar. PDF sayfaları tek bir yeni
     * kaynak PDF'te toplanır (aynı sayfa bir kez alınır).
     */
    private fun assemble(context: Context, store: Store, parts: List<Pair<Notebook, List<Int>>>, name: String, folder: String): Notebook {
        Exporter.init(context)
        val id = store.newId()
        val dir = store.dir(id)
        val now = System.currentTimeMillis()
        val first = parts.first().first
        val nb = Notebook(id, name, now, now, null, 0, first.defaultPaper, first.defaultSize, folder)
        val needsPdf = parts.any { (src, idx) -> src.pdfFile != null && idx.any { src.pages.getOrNull(it)?.background is Background.PdfPage } }
        val out = if (needsPdf) PDDocument() else null
        val opened = ArrayList<PDDocument>()
        try {
            for ((src, indices) in parts) {
                val srcDir = store.dir(src.id)
                copyAssets(srcDir, dir)
                val srcPdf = if (out != null && src.pdfFile != null) File(srcDir, src.pdfFile!!).takeIf { it.exists() }?.let { PDDocument.load(it).also { d -> opened += d } } else null
                val map = HashMap<Int, Int>()
                for (i in indices) {
                    val p = src.pages.getOrNull(i) ?: continue
                    val bg = p.background
                    val newBg: Background = if (bg is Background.PdfPage && srcPdf != null && out != null && bg.index < srcPdf.numberOfPages) {
                        val ni = map.getOrPut(bg.index) { out.importPage(srcPdf.getPage(bg.index)); out.numberOfPages - 1 }
                        bg.copy(index = ni)
                    } else if (bg is Background.PdfPage) Background.Paper(Template.BLANK, PaperColors.WHITE) else bg
                    nb.pages += copyPage(p, newBg)
                }
            }
            if (out != null && out.numberOfPages > 0) {
                FileOutputStream(File(dir, "source.pdf")).use { out.save(it) }
                nb.pdfFile = "source.pdf"
            }
        } finally {
            try { out?.close() } catch (_: Exception) {}
            opened.forEach { try { it.close() } catch (_: Exception) {} }
        }
        if (nb.pages.isEmpty()) { dir.deleteRecursively(); throw IllegalStateException(L.t("Sayfa seçilmedi")) }
        store.saveAllNow(nb)
        return nb
    }

    /** Defterleri sırayla birleştirip yeni bir defter yapar (asıllar değişmez). */
    fun merge(context: Context, store: Store, ids: List<String>, name: String): Notebook {
        val parts = ids.map { store.load(it) }.map { it to it.pages.indices.toList() }
        if (parts.isEmpty()) throw IllegalStateException(L.t("Defter seçilmedi"))
        return assemble(context, store, parts, name, parts.first().first.folder)
    }

    /** Seçilen sayfaları yeni bir deftere çıkarır (asıl defter değişmez). */
    fun extract(context: Context, store: Store, id: String, indices: List<Int>, name: String): Notebook {
        val src = store.load(id)
        return assemble(context, store, listOf(src to indices), name, src.folder)
    }

    /**
     * Başka bir PDF'in bütün sayfalarını defterde [after] sırasındaki sayfadan sonra ekler.
     * Eklenen sayfa sayısını döndürür.
     */
    fun insertPdf(context: Context, store: Store, id: String, uri: Uri, after: Int): Int {
        Exporter.init(context)
        val nb = store.load(id)
        val dir = store.dir(id)
        val incoming = File(dir, "incoming.tmp")
        context.contentResolver.openInputStream(uri).use { inp ->
            requireNotNull(inp) { L.t("Dosya açılamadı") }
            FileOutputStream(incoming).use { inp.copyTo(it, 1 shl 16) }
        }
        try {
            // yeni sayfaların boyutları
            val sizes = ArrayList<Pair<Float, Float>>()
            ParcelFileDescriptor.open(incoming, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { r -> for (i in 0 until r.pageCount) r.openPage(i).use { p -> sizes += p.width.toFloat() to p.height.toFloat() } }
            }
            if (sizes.isEmpty()) throw IllegalStateException(L.t("PDF'te sayfa yok"))
            val newName = "source_${System.currentTimeMillis()}.pdf"
            val oldFile = nb.pdfFile?.let { File(dir, it) }?.takeIf { it.exists() }
            var offset = 0
            PDDocument().use { out ->
                val add = PDDocument.load(incoming)
                val old = oldFile?.let { PDDocument.load(it) }
                try {
                    if (old != null) { for (i in 0 until old.numberOfPages) out.importPage(old.getPage(i)); offset = old.numberOfPages }
                    for (i in 0 until add.numberOfPages) out.importPage(add.getPage(i))
                    FileOutputStream(File(dir, newName)).use { out.save(it) }
                } finally { add.close(); old?.close() }
            }
            val at = (after + 1).coerceIn(0, nb.pages.size)
            val newPages = sizes.mapIndexed { i, (w, h) -> Page(Ids.next(), Background.PdfPage(offset + i), w, h) }
            nb.pages.addAll(at, newPages)
            nb.pdfFile = newName
            nb.updated = System.currentTimeMillis()
            store.saveAllNow(nb)
            oldFile?.delete()
            return newPages.size
        } finally {
            incoming.delete()
        }
    }

    /** Fotoğraflardan (ör. çözülmüş kağıtlar) yeni defter: her fotoğraf bir sayfanın zemini olur. */
    fun fromImages(context: Context, store: Store, uris: List<Uri>, name: String, folder: String): Notebook {
        val id = store.newId()
        val now = System.currentTimeMillis()
        val nb = Notebook(id, name, now, now, null, 0, Background.Paper(Template.BLANK, PaperColors.WHITE), PageSize.A4_PORTRAIT, folder)
        try {
            for (u in uris) {
                val (file, w, h) = store.importImage(context, id, u)
                val pw = 595f
                val ph = (pw * h / w.coerceAtLeast(1)).coerceIn(200f, 2000f)
                nb.pages += Page(Ids.next(), Background.Paper(Template.BLANK, PaperColors.WHITE, image = file), pw, ph)
            }
        } catch (e: Exception) {
            if (nb.pages.isEmpty()) { store.dir(id).deleteRecursively(); throw e }
        }
        if (nb.pages.isEmpty()) { store.dir(id).deleteRecursively(); throw IllegalStateException(L.t("Fotoğraf okunamadı")) }
        store.saveAllNow(nb)
        return nb
    }
}
