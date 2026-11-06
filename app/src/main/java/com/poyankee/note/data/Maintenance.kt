package com.poyankee.note.data

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Çökme kaydı: beklenmedik bir hata olursa ayrıntılar cihazda files/crash/ klasörüne yazılır.
 * Hiçbir yere gönderilmez; kullanıcı isterse Ayarlar'dan paylaşır veya siler.
 * (Yazılar her değişiklikte hemen kaydedildiği için çökmede kaybolmaz.)
 */
object CrashLog {
    fun dir(context: Context) = File(context.filesDir, "crash").apply { mkdirs() }

    fun install(context: Context) {
        val app = context.applicationContext
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                val ver = try { app.packageManager.getPackageInfo(app.packageName, 0).versionName } catch (_: Exception) { "?" }
                val text = "Note $ver · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                    "İş parçacığı: ${t.name}\n\n$sw"
                val name = "hata-" + SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date()) + ".txt"
                File(dir(app), name).writeText(text)
                // en fazla 10 kayıt
                dir(app).listFiles()?.sortedByDescending { it.name }?.drop(10)?.forEach { it.delete() }
            } catch (_: Throwable) {}
            prev?.uncaughtException(t, e)
        }
    }

    fun list(context: Context): List<File> = dir(context).listFiles()?.sortedByDescending { it.name } ?: emptyList()
    fun clear(context: Context) { dir(context).listFiles()?.forEach { it.delete() } }

    /** Paylaşmak için bütün kayıtları tek dosyada birleştirir (önbellek klasöründe). */
    fun bundle(context: Context): File? {
        val l = list(context)
        if (l.isEmpty()) return null
        val out = File(context.cacheDir, "note-hata-kayitlari.txt")
        out.writeText(l.joinToString("\n\n==========\n\n") { it.name + "\n" + it.readText() })
        return out
    }
}

/** Depolama: defterlerin, çöp kutusunun ve geçici dosyaların kapladığı alan; temizleme. */
object StorageInfo {
    class Usage(val notebooks: List<Pair<String, Long>>, val trash: Long, val cache: Long, val total: Long)

    fun size(f: File): Long = if (f.isFile) f.length() else (f.listFiles()?.sumOf { size(it) } ?: 0L)

    fun usage(context: Context, store: Store): Usage {
        val nbs = store.list().map { it.name to size(store.dir(it.id)) }.sortedByDescending { it.second }
        val trash = size(File(context.filesDir, "trash"))
        val cache = size(context.cacheDir)
        return Usage(nbs, trash, cache, size(context.filesDir) + cache)
    }

    fun clearCache(context: Context) { context.cacheDir.listFiles()?.forEach { it.deleteRecursively() } }

    /** Defter klasöründe artık hiçbir sayfada kullanılmayan görselleri siler. Silinen bayt. Defter kapalıyken çağrılmalı. */
    fun cleanUnusedImages(store: Store): Long {
        var freed = 0L
        for (info in store.list()) {
            val nb = try { store.load(info.id) } catch (_: Exception) { continue }
            val used = HashSet<String>()
            for (p in nb.pages) {
                (p.background as? com.poyankee.note.core.Background.Paper)?.image?.takeIf { it.isNotEmpty() }?.let { used += it }
                for (it in p.items) if (it is com.poyankee.note.core.ImageItem) used += it.file
            }
            store.dir(info.id).listFiles()?.forEach { f ->
                val n = f.name
                val isImage = (n.startsWith("img_") || n.startsWith("fx_") || n.startsWith("bg_") || n.startsWith("wq_")) && f.isFile
                if (isImage && n !in used) { freed += f.length(); f.delete() }
            }
        }
        return freed
    }

    fun human(b: Long): String = when {
        b >= 1L shl 30 -> String.format(Locale.US, "%.1f GB", b / (1L shl 30).toDouble())
        b >= 1L shl 20 -> String.format(Locale.US, "%.1f MB", b / (1L shl 20).toDouble())
        b >= 1L shl 10 -> String.format(Locale.US, "%d KB", b / (1L shl 10))
        else -> "$b B"
    }
}
