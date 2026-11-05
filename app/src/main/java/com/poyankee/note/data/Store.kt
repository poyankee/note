package com.poyankee.note.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.poyankee.note.core.Background
import com.poyankee.note.core.Codec
import com.poyankee.note.core.Ids
import com.poyankee.note.core.Item
import com.poyankee.note.core.Notebook
import com.poyankee.note.core.Page
import com.poyankee.note.core.PageSize
import com.poyankee.note.core.Template
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Defter listesinde gösterilen özet bilgi. */
data class NotebookInfo(val id: String, val name: String, val updated: Long, val pageCount: Int, val isPdf: Boolean)

/**
 * Defterleri cihazın uygulama klasöründe saklar:
 * files/notebooks/<id>/notebook.bin, p_<sayfaId>.bin, source.pdf, img_*.jpg
 * Tüm yazma işlemleri tek bir arka plan iş parçacığında, geçici dosya + yeniden adlandırma ile (yarım kalan kayıt olmaz).
 */
class Store(context: Context) {
    private val root = File(context.filesDir, "notebooks").apply { mkdirs() }
    private val io = Executors.newSingleThreadExecutor { r -> Thread(r, "note-io").apply { priority = Thread.NORM_PRIORITY - 1 } }

    fun dir(id: String) = File(root, id)

    fun list(): List<NotebookInfo> {
        val out = ArrayList<NotebookInfo>()
        root.listFiles()?.forEach { d ->
            val f = File(d, "notebook.bin")
            if (!f.exists()) return@forEach
            try {
                DataInputStream(BufferedInputStream(FileInputStream(f))).use { inp ->
                    val nb = Codec.readNotebook(inp)
                    out += NotebookInfo(nb.id, nb.name, nb.updated, nb.pages.size, nb.pdfFile != null)
                }
            } catch (_: Exception) {
            }
        }
        return out.sortedByDescending { it.updated }
    }

    fun create(name: String, template: Template, dark: Boolean, size: PageSize): Notebook {
        val id = UUID.randomUUID().toString()
        dir(id).mkdirs()
        val now = System.currentTimeMillis()
        val nb = Notebook(id, name, now, now, null, 0, template, dark, size)
        nb.pages += Page(Ids.next(), Background.Paper(template, dark), size.w, size.h)
        writeMetaNow(nb)
        return nb
    }

    /** PDF'i kopyalayıp yeni defter oluşturur. Arka planda çağrılmalı. */
    fun importPdf(context: Context, uri: Uri): Notebook {
        val id = UUID.randomUUID().toString()
        val d = dir(id).apply { mkdirs() }
        val pdf = File(d, "source.pdf")
        context.contentResolver.openInputStream(uri).use { inp ->
            requireNotNull(inp) { "Dosya açılamadı" }
            FileOutputStream(pdf).use { inp.copyTo(it, 1 shl 16) }
        }
        val now = System.currentTimeMillis()
        val nb = Notebook(id, displayName(context, uri) ?: "PDF", now, now, "source.pdf", 0, Template.GRID, false, PageSize.A4_PORTRAIT)
        try {
            ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { r ->
                    for (i in 0 until r.pageCount) {
                        r.openPage(i).use { p -> nb.pages += Page(Ids.next(), Background.PdfPage(i), p.width.toFloat(), p.height.toFloat()) }
                    }
                }
            }
        } catch (e: Exception) {
            d.deleteRecursively()
            throw IllegalStateException("PDF açılamadı (şifreli veya bozuk olabilir)", e)
        }
        if (nb.pages.isEmpty()) { d.deleteRecursively(); throw IllegalStateException("PDF'te sayfa yok") }
        writeMetaNow(nb)
        return nb
    }

    private fun displayName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0)?.removeSuffix(".pdf")?.removeSuffix(".PDF") else null
        }
    } catch (_: Exception) { null }

    /** Defteri tüm sayfa öğeleriyle yükler. Arka planda çağrılmalı. */
    fun load(id: String): Notebook {
        val d = dir(id)
        val nb = DataInputStream(BufferedInputStream(FileInputStream(File(d, "notebook.bin")))).use { Codec.readNotebook(it) }
        for (p in nb.pages) {
            val f = File(d, "p_${p.id}.bin")
            if (f.exists()) {
                try {
                    DataInputStream(BufferedInputStream(FileInputStream(f), 1 shl 16)).use { p.items += Codec.readItems(it) }
                } catch (_: Exception) {
                    // bozuk sayfa dosyası: yedeğe kopyala, boş başla
                    f.copyTo(File(d, "p_${p.id}.broken"), overwrite = true)
                }
            }
        }
        return nb
    }

    private fun atomicWrite(file: File, block: (DataOutputStream) -> Unit) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        FileOutputStream(tmp).use { fos ->
            val out = DataOutputStream(BufferedOutputStream(fos, 1 shl 16))
            block(out)
            out.flush()
            fos.fd.sync()
        }
        if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
    }

    private fun writeMetaNow(nb: Notebook) {
        val d = dir(nb.id).apply { mkdirs() }
        atomicWrite(File(d, "notebook.bin")) { Codec.writeNotebook(nb, it) }
    }

    /** Defter bilgisini (sayfa listesi dahil) kaydeder. Ana iş parçacığından çağrılır; anlık görüntü alınır. */
    fun saveMeta(nb: Notebook) {
        nb.updated = System.currentTimeMillis()
        val snap = Notebook(nb.id, nb.name, nb.created, nb.updated, nb.pdfFile, nb.pageOffset, nb.defaultTemplate, nb.defaultDark, nb.defaultSize)
        for (p in nb.pages) snap.pages += Page(p.id, p.background, p.width, p.height)
        snap.bookmarks += nb.bookmarks
        io.execute { try { writeMetaNow(snap) } catch (_: Exception) {} }
    }

    fun savePage(nbId: String, page: Page) {
        val items: List<Item> = ArrayList(page.items)
        val pid = page.id
        io.execute {
            try { atomicWrite(File(dir(nbId), "p_$pid.bin")) { Codec.writeItems(items, it) } } catch (_: Exception) {}
        }
    }

    fun deletePageFile(nbId: String, pageId: Long) {
        io.execute { File(dir(nbId), "p_$pageId.bin").delete() }
    }

    fun rename(id: String, name: String) {
        io.execute {
            try {
                val nb = DataInputStream(BufferedInputStream(FileInputStream(File(dir(id), "notebook.bin")))).use { Codec.readNotebook(it) }
                nb.name = name
                writeMetaNow(nb)
            } catch (_: Exception) {}
        }
    }

    fun delete(id: String) { io.execute { dir(id).deleteRecursively() } }

    /** Bekleyen tüm kayıtların bitmesini bekler (uygulama arka plana geçerken). */
    fun flush() {
        val f = io.submit {}
        try { f.get(3, TimeUnit.SECONDS) } catch (_: Exception) {}
    }

    /** Görseli deftere kopyalar (en fazla 2048 px), dosya adını ve boyutunu döndürür. Arka planda çağrılmalı. */
    fun importImage(context: Context, nbId: String, uri: Uri): Triple<String, Int, Int> {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, opts) }
        var sample = 1
        while (opts.outWidth / sample > 2048 || opts.outHeight / sample > 2048) sample *= 2
        val bmp = context.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IllegalStateException("Görsel okunamadı")
        val name = "img_${Ids.next()}.jpg"
        FileOutputStream(File(dir(nbId), name)).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        val r = Triple(name, bmp.width, bmp.height)
        bmp.recycle()
        return r
    }
}
