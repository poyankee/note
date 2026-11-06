package com.poyankee.note.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.poyankee.note.core.Codec
import com.poyankee.note.core.ImageItem
import com.poyankee.note.core.Item
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Kullanıcının kendi kütüphanesi: seçilen çizimler "Kütüphaneye kaydet" ile saklanır.
 * Cihazda notebooks/library/<kimlik>.bin (yedeğe dahil): ad + öğeler.
 */
class UserLibrary private constructor(context: Context) {
    class Entry(val id: String, val name: String, val items: List<Item>)

    private val dir = File(context.filesDir, "notebooks/library").apply { mkdirs() }
    var entries by mutableStateOf(load())
        private set

    private fun load(): List<Entry> = (dir.listFiles() ?: emptyArray()).filter { it.name.endsWith(".bin") }.mapNotNull { f ->
        try {
            DataInputStream(BufferedInputStream(FileInputStream(f))).use { inp ->
                val name = inp.readUTF()
                Entry(f.name.removeSuffix(".bin"), name, Codec.readItems(inp))
            }
        } catch (_: Exception) { null }
    }.sortedBy { it.name.lowercase() }

    /** Görseller (defter klasörüne bağlı oldukları için) kaydedilmez. Kaydedilen öğe sayısını döndürür. */
    fun save(name: String, items: List<Item>): Int {
        val keep = items.filter { it !is ImageItem }
        if (keep.isEmpty()) return 0
        val id = "u" + System.currentTimeMillis()
        DataOutputStream(BufferedOutputStream(FileOutputStream(File(dir, "$id.bin")))).use { out ->
            out.writeUTF(name.ifBlank { "Çizimim" })
            Codec.writeItems(keep, out)
        }
        entries = load()
        return keep.size
    }

    fun delete(e: Entry) {
        File(dir, "${e.id}.bin").delete()
        entries = load()
    }

    companion object {
        @Volatile private var inst: UserLibrary? = null
        fun get(context: Context): UserLibrary = inst ?: synchronized(this) { inst ?: UserLibrary(context.applicationContext).also { inst = it } }
    }
}
