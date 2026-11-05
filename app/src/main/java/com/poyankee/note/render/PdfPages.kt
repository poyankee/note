package com.poyankee.note.render

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.LruCache
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.ceil
import kotlin.math.min

/**
 * PDF sayfalarını arka planda bitmap'e çevirir.
 * - "Temel" görüntü: tüm sayfa, orta çözünürlükte (hızlı kaydırma için önbellekte).
 * - "Keskin" görüntü: yalnızca ekranda görünen bölge, tam ekran çözünürlüğünde (yakınlaştırınca net görünsün).
 * PdfRenderer iş parçacığı güvenli olmadığından her şey tek bir iş parçacığında yapılır.
 */
class PdfPages(file: File, private val onUpdate: () -> Unit) {

    class Sharp(val index: Int, val rect: RectF, val zoom: Float, val bitmap: Bitmap)

    private val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(fd)
    val pageCount = renderer.pageCount
    private val exec = Executors.newSingleThreadExecutor { r -> Thread(r, "pdf-render") }
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var closed = false
    /** Kapatma istendi: yeni iş alınmaz, sıradaki işler (ör. kapak) yine de tamamlanır. */
    @Volatile private var closing = false

    private val base = object : LruCache<Int, Bitmap>(140 * 1024 * 1024) {
        override fun sizeOf(key: Int, value: Bitmap) = value.byteCount
    }
    private val pendingBase = HashSet<Int>()
    private val sharp = HashMap<Int, Sharp>()
    private val sharpGen = java.util.concurrent.ConcurrentHashMap<Int, Int>()

    /** Temel görüntü; hazır değilse null döner ve hazırlamaya başlar. */
    fun base(index: Int): Bitmap? {
        base.get(index)?.let { return it }
        if (closing || closed || index !in 0 until pageCount || !pendingBase.add(index)) return null
        exec.execute {
            if (closed) return@execute
            val bmp = try {
                renderer.openPage(index).use { p ->
                    val scale = min(1.6f, 1800f / maxOf(p.width, p.height))
                    val w = ceil(p.width * scale).toInt().coerceAtLeast(1)
                    val h = ceil(p.height * scale).toInt().coerceAtLeast(1)
                    val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    b.eraseColor(0xFFFFFFFF.toInt())
                    p.render(b, null, Matrix().apply { setScale(w / p.width.toFloat(), h / p.height.toFloat()) }, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    b
                }
            } catch (_: Throwable) { null }
            main.post {
                pendingBase.remove(index)
                if (bmp != null && !closed) { base.put(index, bmp); onUpdate() }
            }
        }
        return null
    }

    fun sharp(index: Int): Sharp? = sharp[index]

    /**
     * Sayfanın [rect] bölgesini (sayfa koordinatı) [zoom] piksel/nokta ölçeğinde çizer.
     * Aynı sayfa için daha yeni bir istek gelirse eskisi atlanır.
     */
    fun requestSharp(index: Int, rect: RectF, zoom: Float) {
        if (closing || closed || index !in 0 until pageCount) return
        val cur = sharp[index]
        if (cur != null && cur.zoom == zoom && cur.rect.contains(rect)) return
        val gen = (sharpGen[index] ?: 0) + 1
        sharpGen[index] = gen
        val r = RectF(rect)
        exec.execute {
            if (closed || sharpGen[index] != gen) return@execute
            val w = ceil(r.width() * zoom).toInt()
            val h = ceil(r.height() * zoom).toInt()
            if (w <= 0 || h <= 0 || w * h > 12_000_000) return@execute
            val bmp = try {
                renderer.openPage(index).use { p ->
                    val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    b.eraseColor(0xFFFFFFFF.toInt())
                    val m = Matrix()
                    m.setTranslate(-r.left, -r.top)
                    m.postScale(zoom, zoom)
                    p.render(b, null, m, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    b
                }
            } catch (_: Throwable) { null }
            main.post {
                if (bmp == null || closed) return@post
                if (sharpGen[index] != gen) { bmp.recycle(); return@post }
                sharp.put(index, Sharp(index, r, zoom, bmp))?.bitmap?.recycle()
                onUpdate()
            }
        }
    }

    /** Ekranda olmayan sayfaların keskin görüntülerini bırakır. */
    fun dropSharpExcept(keep: Set<Int>) {
        val it = sharp.entries.iterator()
        while (it.hasNext()) {
            val e = it.next()
            if (e.key !in keep) { e.value.bitmap.recycle(); it.remove(); sharpGen[e.key] = (sharpGen[e.key] ?: 0) + 1 }
        }
    }

    /** Arka plan iş parçacığında çalışan işler için (küçük resim, dışa aktarma). [block] PDF iş parçacığında çalışır. */
    fun runOnPdfThread(block: () -> Unit) { if (!closing && !closed) exec.execute { if (!closed) block() } }

    /** Yalnızca PDF iş parçacığında çağrılmalı. */
    fun renderBlocking(index: Int, scale: Float): Bitmap? = try {
        renderer.openPage(index).use { p ->
            val w = ceil(p.width * scale).toInt().coerceAtLeast(1)
            val h = ceil(p.height * scale).toInt().coerceAtLeast(1)
            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            b.eraseColor(0xFFFFFFFF.toInt())
            p.render(b, null, Matrix().apply { setScale(w / p.width.toFloat(), h / p.height.toFloat()) }, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            b
        }
    } catch (_: Throwable) { null }

    fun close() {
        if (closing) return
        closing = true
        exec.execute {
            closed = true
            try { renderer.close() } catch (_: Throwable) {}
            try { fd.close() } catch (_: Throwable) {}
        }
        exec.shutdown()
        base.evictAll()
        sharp.values.forEach { it.bitmap.recycle() }
        sharp.clear()
    }
}
