package com.poyankee.note.editor

import android.app.Activity
import android.app.Presentation
import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.hardware.display.DisplayManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.view.Choreographer
import android.view.Display
import android.view.Surface
import android.view.View
import android.view.WindowManager
import java.io.File
import java.io.FileInputStream
import kotlin.math.min

/** Görünümü (InkView) başka bir yüzeye sığdırarak çizer: kenarlarda boşluk bırakır, oranı korur. */
fun drawFitted(ink: InkView, c: Canvas, w: Float, h: Float) {
    val vp = ink.viewport()
    if (vp.width() <= 0f || vp.height() <= 0f) return
    val z = min(w / vp.width(), h / vp.height())
    val ox = vp.left - (w / z - vp.width()) / 2f
    val oy = vp.top - (h / z - vp.height()) / 2f
    ink.drawScene(c, z, ox, oy, w, h, forOutput = true)
}

/** Projeksiyon / HDMI ekranında sadece temiz sayfayı gösteren pencere. */
class MirrorPresentation(context: Context, display: Display, private val ink: InkView) : Presentation(context, display) {
    private lateinit var view: View
    private val listener: () -> Unit = { if (::view.isInitialized) view.invalidate() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = object : View(context) {
            override fun onDraw(canvas: Canvas) = drawFitted(ink, canvas, width.toFloat(), height.toFloat())
        }
        setContentView(view)
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStart() { super.onStart(); ink.ctlListeners().add(listener) }
    override fun onStop() { ink.ctlListeners().remove(listener); super.onStop() }
}

/** Harici ekran takılınca/çıkarılınca sunumu açıp kapatır. */
class SecondScreen(
    private val activity: Activity,
    private val ink: InkView,
    private val enabled: () -> Boolean,
    private val onChange: (Boolean) -> Unit,
) : DisplayManager.DisplayListener {
    private val dm = activity.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private var pres: MirrorPresentation? = null
    var active = false
        private set(v) { if (field != v) { field = v; onChange(v) } }

    fun start() { dm.registerDisplayListener(this, null); update() }
    fun stop() { dm.unregisterDisplayListener(this); dismiss() }

    fun update() {
        val d = dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).firstOrNull()
        if (d == null || !enabled()) { dismiss(); return }
        if (pres?.display?.displayId == d.displayId) return
        dismiss()
        try {
            pres = MirrorPresentation(activity, d, ink).also { it.show() }
            active = true
        } catch (_: Exception) { pres = null; active = false }
    }

    private fun dismiss() { try { pres?.dismiss() } catch (_: Exception) {}; pres = null; active = false }

    override fun onDisplayAdded(displayId: Int) = update()
    override fun onDisplayRemoved(displayId: Int) = update()
    override fun onDisplayChanged(displayId: Int) {}
}

/**
 * Dersi videoya kaydeder: ekrandaki sayfa görünümü (araç çubukları olmadan) + mikrofon → MP4.
 * Kayıt bitince video Galeri'de "Movies/Note" klasörüne kaydedilir.
 */
class LessonRecorder(private val context: Context, private val ink: InkView) {
    private var rec: MediaRecorder? = null
    private var surface: Surface? = null
    private var file: File? = null
    private var w = 0
    private var h = 0
    var paused = false; private set
    var startedAt = 0L; private set
    private var pausedTotal = 0L
    private var pauseStart = 0L
    private var lastFrame = 0L

    val isRecording get() = rec != null

    /** Geçen kayıt süresi (ms), duraklatmalar hariç. */
    fun elapsed(): Long {
        if (!isRecording) return 0
        val now = SystemClock.elapsedRealtime()
        return now - startedAt - pausedTotal - (if (paused) now - pauseStart else 0)
    }

    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val s = surface ?: return
            val now = SystemClock.uptimeMillis()
            if (!paused && now - lastFrame >= 32) {
                lastFrame = now
                try {
                    val c = s.lockHardwareCanvas()
                    try { drawFitted(ink, c, w.toFloat(), h.toFloat()) } finally { s.unlockCanvasAndPost(c) }
                } catch (_: Exception) {}
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun start(withAudio: Boolean) {
        if (isRecording) return
        // Video boyutu: görünüm oranında, uzun kenar en fazla 1920, 16'nın katı
        val vw = ink.width.coerceAtLeast(16); val vh = ink.height.coerceAtLeast(16)
        val s = min(1f, 1920f / maxOf(vw, vh))
        w = ((vw * s).toInt() / 16) * 16
        h = ((vh * s).toInt() / 16) * 16
        val f = File(context.cacheDir, "kayit_${System.currentTimeMillis()}.mp4")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            if (withAudio) r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setOutputFile(f.absolutePath)
            r.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            if (withAudio) {
                r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                r.setAudioEncodingBitRate(128_000)
                r.setAudioSamplingRate(44_100)
            }
            r.setVideoSize(w, h)
            r.setVideoFrameRate(30)
            r.setVideoEncodingBitRate(6_000_000)
            r.prepare()
            surface = r.surface
            r.start()
        } catch (e: Exception) {
            try { r.release() } catch (_: Exception) {}
            f.delete()
            surface = null
            throw e
        }
        rec = r; file = f
        paused = false; pausedTotal = 0; startedAt = SystemClock.elapsedRealtime()
        Choreographer.getInstance().postFrameCallback(frame)
    }

    fun pause() {
        val r = rec ?: return
        if (paused) return
        try { r.pause(); paused = true; pauseStart = SystemClock.elapsedRealtime() } catch (_: Exception) {}
    }

    fun resume() {
        val r = rec ?: return
        if (!paused) return
        try { r.resume(); paused = false; pausedTotal += SystemClock.elapsedRealtime() - pauseStart } catch (_: Exception) {}
    }

    /** Kaydı bitirir ve Galeri'ye kaydeder. Video adını döndürür (başarısızsa null). */
    fun stop(): String? {
        val r = rec ?: return null
        Choreographer.getInstance().removeFrameCallback(frame)
        var ok = true
        try { r.stop() } catch (_: Exception) { ok = false }
        try { r.release() } catch (_: Exception) {}
        rec = null
        surface = null
        val f = file ?: return null
        file = null
        if (!ok) { f.delete(); return null }
        return try {
            val name = "Ders_" + java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.getDefault()).format(java.util.Date()) + ".mp4"
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Note")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val cr = context.contentResolver
            val uri = cr.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            cr.openOutputStream(uri)?.use { out -> FileInputStream(f).use { it.copyTo(out, 1 shl 16) } }
            values.clear(); values.put(MediaStore.Video.Media.IS_PENDING, 0)
            cr.update(uri, values, null, null)
            f.delete()
            name
        } catch (_: Exception) { null }
    }
}
