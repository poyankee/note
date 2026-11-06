package com.poyankee.note.core

/**
 * Dil desteği: uygulamanın metinleri Türkçe yazılır; İngilizce seçiliyse [t] sözlükten çevirir
 * (sözlük: assets/i18n/en.tsv, MainActivity yükler). Sözlükte olmayan metin Türkçe kalır.
 */
object L {
    @Volatile var english = false
    @Volatile var dict: Map<String, String> = emptyMap()

    fun t(s: String): String = if (english) dict[s] ?: s else s

    /** "türkçe<TAB>english" satırları; \n ve \t kaçışları çözülür. */
    fun parse(text: String): Map<String, String> {
        fun un(x: String) = x.replace("\\n", "\n").replace("\\t", "\t")
        val m = HashMap<String, String>()
        for (line in text.split('\n')) {
            if (line.isBlank() || line.startsWith("#")) continue
            val i = line.indexOf('\t')
            if (i <= 0) continue
            m[un(line.substring(0, i))] = un(line.substring(i + 1))
        }
        return m
    }
}
