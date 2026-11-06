package com.poyankee.note.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * Yapılacaklar listesindeki bir görev. [color]: 0 yok, 1 kırmızı, 2 turuncu, 3 yeşil.
 * [nbId] boş değilse görev o deftere (ve [page] sayfasına) bağlıdır.
 */
data class Todo(
    val id: Long,
    val text: String,
    val done: Boolean = false,
    val color: Int = 0,
    val nbId: String = "",
    val nbName: String = "",
    val page: Int = 0,
)

/**
 * Yapılacaklar listesi. Cihazda notebooks/todos.txt dosyasında durur (yedeğe dahil).
 * Her satır bir görev: alanlar sekmeyle ayrılır, metindeki sekme / satır sonu kaçışlanır.
 */
class TodoList private constructor(context: Context) {
    private val file = File(context.filesDir, "notebooks/todos.txt")
    var items by mutableStateOf(load())
        private set

    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n")
    private fun unesc(s: String): String {
        val b = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) { 't' -> b.append('\t'); 'n' -> b.append('\n'); else -> b.append(s[i + 1]) }
                i += 2
            } else { b.append(c); i++ }
        }
        return b.toString()
    }

    private fun load(): List<Todo> = try {
        if (!file.exists()) emptyList() else file.readLines().mapNotNull { decode(it) }
    } catch (_: Exception) { emptyList() }

    fun decode(line: String): Todo? = try {
        val p = line.split('\t')
        Todo(p[0].toLong(), unesc(p[1]), p[2] == "1", p[3].toInt(), unesc(p.getOrElse(4) { "" }), unesc(p.getOrElse(5) { "" }), p.getOrElse(6) { "0" }.toInt())
    } catch (_: Exception) { null }

    fun encode(t: Todo) = listOf(t.id.toString(), esc(t.text), if (t.done) "1" else "0", t.color.toString(), esc(t.nbId), esc(t.nbName), t.page.toString()).joinToString("\t")

    private fun save(list: List<Todo>) {
        items = list
        try {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, "todos.tmp")
            tmp.writeText(list.joinToString("\n") { encode(it) })
            if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
        } catch (_: Exception) {}
    }

    fun add(text: String, nbId: String = "", nbName: String = "", page: Int = 0) {
        val t = text.trim()
        if (t.isEmpty()) return
        save(items + Todo(System.currentTimeMillis(), t, nbId = nbId, nbName = nbName, page = page))
    }

    fun update(t: Todo) = save(items.map { if (it.id == t.id) t else it })
    fun toggle(t: Todo) = update(t.copy(done = !t.done))
    fun delete(t: Todo) = save(items.filter { it.id != t.id })
    fun clearDone() = save(items.filter { !it.done })

    fun move(t: Todo, delta: Int) {
        val l = items.toMutableList()
        val i = l.indexOfFirst { it.id == t.id }
        val j = i + delta
        if (i < 0 || j !in l.indices) return
        l.removeAt(i); l.add(j, t)
        save(l)
    }

    /** Yedekten geri yüklenen listeyi mevcut listeyle birleştirir (aynı kimlikliler atlanır). */
    fun mergeFrom(f: File) {
        val incoming = try { f.readLines().mapNotNull { decode(it) } } catch (_: Exception) { emptyList() }
        val ids = items.map { it.id }.toHashSet()
        save(items + incoming.filter { it.id !in ids })
    }

    companion object {
        @Volatile private var inst: TodoList? = null
        fun get(context: Context): TodoList = inst ?: synchronized(this) { inst ?: TodoList(context.applicationContext).also { inst = it } }
    }
}
