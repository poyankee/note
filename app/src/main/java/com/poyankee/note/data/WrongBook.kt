package com.poyankee.note.data

import com.poyankee.note.core.L

import android.graphics.Bitmap
import com.poyankee.note.core.Background
import com.poyankee.note.core.Ids
import com.poyankee.note.core.ImageItem
import com.poyankee.note.core.Notebook
import com.poyankee.note.core.Page
import com.poyankee.note.core.PageSize
import com.poyankee.note.core.PaperColors
import com.poyankee.note.core.ShapeItem
import com.poyankee.note.core.Geom
import com.poyankee.note.core.Template
import com.poyankee.note.core.TextItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Yanlış defteri: soru çözerken dikdörtgenle seçilen bölge, ayrı bir "Yanlış defteri"ne kart olarak eklenir.
 * Her soru bir sayfa: üstte kaynak ve tarih, sorunun görüntüsü, altında çözüm alanı ve "Çözüldü / Tekrar" kutuları.
 */
object WrongBook {
    const val NAME = "Yanlış defteri"

    /** Defterin kimliğini döndürür; yoksa oluşturur. */
    fun ensure(store: Store, settings: Settings): String {
        val id = settings.wrongNotebook.value
        if (id.isNotEmpty() && File(store.dir(id), "notebook.bin").exists()) return id
        val nb = store.create(NAME, Background.Paper(Template.BLANK, PaperColors.WHITE), PageSize.A4_PORTRAIT, "")
        settings.wrongNotebook.value = nb.id
        return nb.id
    }

    /** Soruyu ekler. Arka planda çağrılmalı. Eklenen sayfanın sırasını döndürür. */
    fun add(store: Store, settings: Settings, bmp: Bitmap, srcId: String, srcName: String, srcPage: Int): Int {
        val id = ensure(store, settings)
        val nb: Notebook = store.load(id)
        val reuse = nb.pages.size == 1 && nb.pages[0].items.isEmpty() && settings.wrongSource(nb.pages[0].id) == null
        val page = if (reuse) nb.pages[0] else Page(Ids.next(), Background.Paper(Template.BLANK, PaperColors.WHITE), PageSize.A4_PORTRAIT.w, PageSize.A4_PORTRAIT.h).also { nb.pages += it }
        val file = "wq_${Ids.next()}.jpg"
        FileOutputStream(File(store.dir(id), file)).use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        val pw = page.width; val ph = page.height
        val maxW = pw - 60f; val maxH = ph * 0.48f
        val s = minOf(maxW / bmp.width, maxH / bmp.height)
        val iw = bmp.width * s; val ih = bmp.height * s
        val date = SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("tr")).format(Date())
        val n = nb.pages.indexOf(page) + 1
        val gray = 0xFF667085.toInt(); val blue = 0xFF1565C0.toInt()
        page.items += TextItem(Ids.next(), 30f, 22f, "Soru $n", 16f, blue)
        page.items += TextItem(Ids.next(), 30f, 44f, "Kaynak: $srcName, sayfa ${srcPage + 1}  ·  $date", 10f, gray)
        page.items += ImageItem(Ids.next(), 30f, 66f, iw, ih, file)
        page.items += ShapeItem(Ids.next(), Geom.Polyline(floatArrayOf(30f, 66f, 30f + iw, 66f, 30f + iw, 66f + ih, 30f, 66f + ih), true), 0xFFD0D5DD.toInt(), 1f)
        val sy = 66f + ih + 22f
        page.items += TextItem(Ids.next(), 30f, sy, L.t("Çözüm:"), 13f, blue)
        page.items += ShapeItem(Ids.next(), Geom.Polyline(floatArrayOf(30f, sy + 20f, pw - 30f, sy + 20f), false), 0xFFD0D5DD.toInt(), 1f)
        page.items += TextItem(Ids.next(), 30f, ph - 40f, L.t("☐ Çözüldü      ☐ Tekrar et      Konu: "), 12f, gray)
        store.saveAllNow(nb)
        settings.setWrongSource(page.id, srcId, srcPage)
        return nb.pages.indexOf(page)
    }
}
