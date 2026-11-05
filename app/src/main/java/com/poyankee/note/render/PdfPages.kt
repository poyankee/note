package com.poyankee.note.render

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.LruCache
import com.poyankee.note.core.Affine
import com.poyankee.note.core.Background
import com.poyankee.note.core.PdfGeom
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.ceil
import kotlin.math.min

/**
 * PDF sayfalarını arka planda bitmap'e çevirir (döndürme ve kırpma dahil).
 * - "Temel" görüntü: tüm sayfa, orta çözünürlükte (hızlı kaydırma için önbellekte).
 * - "Keskin" görüntü: yalnızca ekranda görünen bölge, tam ekran çözünürlüğünde (yakınlaştırınca net görünsün).
 * PdfRenderer iş parçacığı güvenli olmadığından her şey tek bir iş parçacığında yapılır.
 */
class PdfPages(file: File, private val onUpdate: () -> Unit) {

    class Sharp(val key: Background.PdfPage, val rect: RectF, val zoom: Float, val bitmap: Bitmap)

    private val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(fd)
    val pageCount = renderer.pageCount
    private val exec = Executors.newSingleThreadExecutor { r -> Thread(r, "pdf-render") }
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var closed = false
    /** Kapatma istendi: yeni iş alınmaz, sıradaki işler (ör. kapak) yine de tamamlanır. */
    @Volatile private var closing = false

    private val base = object : LruCache<Background.PdfPage, Bitmap>(140 * 1024 * 1024) {
        override fun sizeOf(key: Background.PdfPage, value: Bitmap) = value.byteCount
    }
    private val pendingBase = HashSet<Background.PdfPage>()
    private val sharp = HashMap<Int, Sharp>()
    private val sharpGen = java.util.concurrent.ConcurrentHashMap<Int, Int>()

    /** Tam sayfa → görünen sayfa dönüşümü, ardından [post]. */
    private fun matrixFor(bg: Background.PdfPage, p: PdfRenderer.Page, post: Affine): Matrix {
        val a = post.after(PdfGeom.fullToDisplay(p.width.toFloat(), p.height.toFloat(), bg.rotation, bg.crop))
        return Matrix().apply { setValues(floatArrayOf(a.a, a.c, a.e, a.b, a.d, a.f, 0f, 0f, 1f)) }
    }

    private fun displaySize(bg: Background.PdfPage, p: PdfRenderer.Page) =
        PdfGeom.displaySize(p.width.toFloat(), p.height.toFloat(), bg.rotation, bg.crop)

    /** Temel görüntü; hazır değilse null döner ve hazırlamaya başlar. */
    fun base(bg: Background.PdfPage): Bitmap? {
        base.get(bg)?.let { return it }
        if (closing || closed || bg.index !in 0 until pageCount || !pendingBase.add(bg)) return null
        exec.execute {
            if (closed) return@execute
            val bmp = try {
                renderer.openPage(bg.index).use { p ->
                    val (dw, dh) = displaySize(bg, p)
                    val scale = min(1.6f, 1800f / maxOf(dw, dh))
                    renderPage(bg, p, scale, dw, dh)
                }
            } catch (_: Throwable) { null }
            main.post {
                pendingBase.remove(bg)
                if (bmp != null && !closed) { base.put(bg, bmp); onUpdate() }
            }
        }
        return null
    }

    private fun renderPage(bg: Background.PdfPage, p: PdfRenderer.Page, scale: Float, dw: Float, dh: Float): Bitmap {
        val w = ceil(dw * scale).toInt().coerceAtLeast(1)
        val h = ceil(dh * scale).toInt().coerceAtLeast(1)
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        b.eraseColor(0xFFFFFFFF.toInt())
        p.render(b, null, matrixFor(bg, p, Affine(w / dw, 0f, 0f, h / dh, 0f, 0f)), PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return b
    }

    fun sharp(bg: Background.PdfPage): Sharp? = sharp[bg.index]?.takeIf { it.key == bg }

    /**
     * Sayfanın [rect] bölgesini (görünen sayfa koordinatı) [zoom] piksel/nokta ölçeğinde çizer.
     * Aynı sayfa için daha yeni bir istek gelirse eskisi atlanır.
     */
    fun requestSharp(bg: Background.PdfPage, rect: RectF, zoom: Float) {
        val index = bg.index
        if (closing || closed || index !in 0 until pageCount) return
        val cur = sharp[index]
        if (cur != null && cur.key == bg && cur.zoom == zoom && cur.rect.contains(rect)) return
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
                    val post = Affine.scale(zoom).after(Affine.translate(-r.left, -r.top))
                    p.render(b, null, matrixFor(bg, p, post), PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    b
                }
            } catch (_: Throwable) { null }
            main.post {
                if (bmp == null || closed) return@post
                if (sharpGen[index] != gen) { bmp.recycle(); return@post }
                sharp.put(index, Sharp(bg, r, zoom, bmp))?.bitmap?.recycle()
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
    fun renderBlocking(bg: Background.PdfPage, scale: Float): Bitmap? = try {
        renderer.openPage(bg.index).use { p ->
            val (dw, dh) = displaySize(bg, p)
            renderPage(bg, p, scale, dw, dh)
        }
    } catch (_: Throwable) { null }

    /** Yalnızca PDF iş parçacığında: sayfanın döndürme/kırpma öncesi boyutu. */
    fun fullSizeBlocking(index: Int): Pair<Float, Float>? = try {
        renderer.openPage(index).use { p -> p.width.toFloat() to p.height.toFloat() }
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
