package com.poyankee.note.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.poyankee.note.core.PageSize
import com.poyankee.note.core.Template

enum class ButtonAction(val title: String) { ERASER("Silgi"), LASSO("Kement seçim"), LASER("Lazer"), NONE("Hiçbir şey") }

/** Kalemin çift dokunuşu / tuşu ile yapılacak iş. */
enum class PenAction(val title: String) {
    ERASER_TOGGLE("Silgi ↔ kalem"), UNDO("Geri al"), LASER_TOGGLE("Lazer ↔ kalem"), PEN_HIGHLIGHTER("Kalem ↔ fosforlu"), NONE("Hiçbir şey")
}

enum class ThemeMode(val title: String) { SYSTEM("Sistem"), LIGHT("Açık"), DARK("Koyu") }

enum class LibraryView(val title: String) { SMALL("Küçük"), MEDIUM("Orta"), LARGE("Büyük"), LIST("Liste") }

enum class SortMode(val title: String) { UPDATED("Son düzenlenen"), NAME("Ada göre"), CREATED("Oluşturma tarihi") }

/** Kayıtlı kalem: tür (kalem / fosforlu), renk ve kalınlık. */
data class PenPreset(val highlighter: Boolean, val color: Int, val width: Float) {
    fun encode() = "${if (highlighter) 1 else 0}:${String.format("%08X", color)}:$width"
    companion object {
        fun decode(s: String): PenPreset? = try {
            val p = s.split(':')
            PenPreset(p[0] == "1", p[1].toLong(16).toInt(), p[2].toFloat())
        } catch (_: Exception) { null }
    }
}

/** Uygulama ayarları. Compose ekranları değişiklikleri anında görür; her değişiklik hemen kaydedilir. */
class Settings(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun bool(key: String, def: Boolean) = Pref(def, { sp.getBoolean(key, def) }, { sp.edit().putBoolean(key, it).apply() })
    private fun int(key: String, def: Int) = Pref(def, { sp.getInt(key, def) }, { sp.edit().putInt(key, it).apply() })
    private fun long(key: String, def: Long) = Pref(def, { sp.getLong(key, def) }, { sp.edit().putLong(key, it).apply() })
    private fun float(key: String, def: Float) = Pref(def, { sp.getFloat(key, def) }, { sp.edit().putFloat(key, it).apply() })
    private fun str(key: String, def: String) = Pref(def, { sp.getString(key, def) ?: def }, { sp.edit().putString(key, it).apply() })

    class Pref<T>(def: T, read: () -> T, private val write: (T) -> Unit) {
        private val state = mutableStateOf(try { read() } catch (_: Exception) { def })
        var value: T
            get() = state.value
            set(v) { state.value = v; write(v) }
    }

    // Giriş
    val penOnly = bool("penOnly", true)
    val fingerGestures = bool("fingerGestures", true)
    val twoFingerUndo = bool("twoFingerUndo", true)
    val buttonAction = str("buttonAction", ButtonAction.ERASER.name)
    val leftHanded = bool("leftHanded", false)
    /** Avuç algılama: bu boyuttan (dp) büyük temaslar avuç sayılır. Küçük = daha hassas. */
    val palmSizeDp = int("palmSizeDp", 70)
    /** Kalem ekrandan kalktıktan sonra parmakların yok sayıldığı süre (ms). */
    val palmHoldMs = int("palmHoldMs", 400)
    val penKeyAction = str("penKeyAction", PenAction.ERASER_TOGGLE.name)
    /** "Tuşu öğret" ile kaydedilen tuş kodları (virgülle). */
    val penKeyCodes = str("penKeyCodes", "")

    // Kalem
    val pressure = bool("pressure", true)
    val smoothing = int("smoothing", 2)
    val prediction = bool("prediction", true)
    /** Silgiyle sildikten sonra önceki araca (kaleme) kendiliğinden dön. */
    val eraserReturn = bool("eraserReturn", true)

    // Geometri
    val shapeHold = bool("shapeHold", true)
    val holdMs = int("holdMs", 550)
    val gridSnap = bool("gridSnap", true)
    val angleSnap = bool("angleSnap", true)
    val autoLetters = bool("autoLetters", false)
    val autoMeasures = bool("autoMeasures", false)

    // Ders
    val laserMs = int("laserMs", 1500)
    val tempMs = int("tempMs", 3000)
    val spotRadiusDp = int("spotRadiusDp", 140)
    val secondScreen = bool("secondScreen", true)
    val recordAudio = bool("recordAudio", true)

    // Sayfalar ve defterler
    val newTemplate = str("newTemplate", Template.BLANK.name)
    val newSize = str("newSize", PageSize.A4_PORTRAIT.name)
    val newDark = bool("newDark", false)
    /** Yeni sayfa: false = düz beyaz, true = önceki sayfa gibi. */
    val newPageSame = bool("newPageSame", false)
    val sortMode = str("sortMode", SortMode.UPDATED.name)
    val libraryView = str("libraryView", LibraryView.MEDIUM.name)
    /** Klasör renkleri: "ad=AARRGGBB" çiftleri, ayraç \u0001. */
    val folderColors = str("folderColors", "")

    // Görünüm
    val theme = str("theme", ThemeMode.SYSTEM.name)
    val dockCollapsed = bool("dockCollapsed", false)
    val dockX = float("dockX", 0f)
    val dockY = float("dockY", 0f)
    val navX = float("navX", 0f)
    val navY = float("navY", 0f)
    val presX = float("presX", 0f)
    val presY = float("presY", 0f)

    // Yedekleme
    val backupTree = str("backupTree", "")
    val autoBackup = bool("autoBackup", false)
    val lastBackup = long("lastBackup", 0L)

    // Araç durumu
    val palette = str("palette", "FF1A1A1A,FF1565C0,FFD32F2F,FF2E7D32,FFF57C00,FF6A1B9A")
    val hlPalette = str("hlPalette", "FFFFEB3B,FF7CF29A,FFFF9EC7,FF8EC5FF,FFFFB74D")
    val presets = str("presets", "")
    val tool = str("tool", "PEN")
    val penColor = int("penColor", 0xFF1A1A1A.toInt())
    val penWidth = int("penWidthX10", 20)
    val hlColor = int("hlColor", 0x66FFEB3B)
    val hlWidthX10 = int("hlWidthX10", 140)
    val eraserPartial = bool("eraserPartial", false)
    val eraserSize = int("eraserSize", 1)
    val polygonSides = int("polygonSides", 6)

    val buttonActionValue get() = try { ButtonAction.valueOf(buttonAction.value) } catch (_: Exception) { ButtonAction.ERASER }
    val penKeyActionValue get() = try { PenAction.valueOf(penKeyAction.value) } catch (_: Exception) { PenAction.ERASER_TOGGLE }
    val themeValue get() = try { ThemeMode.valueOf(theme.value) } catch (_: Exception) { ThemeMode.SYSTEM }
    val libraryViewValue get() = try { LibraryView.valueOf(libraryView.value) } catch (_: Exception) { LibraryView.MEDIUM }
    private fun folderColorMap(): Map<String, Int> = folderColors.value.split('\u0001').mapNotNull {
        val i = it.lastIndexOf('='); if (i <= 0) null else it.substring(i + 1).toLongOrNull(16)?.let { c -> it.substring(0, i) to c.toInt() }
    }.toMap()
    fun folderColor(name: String): Int? = folderColorMap()[name]
    fun setFolderColor(name: String, color: Int?) {
        val m = folderColorMap().toMutableMap()
        if (color == null) m.remove(name) else m[name] = color
        folderColors.value = m.entries.joinToString("\u0001") { "${it.key}=" + String.format("%08X", it.value) }
    }
    val sortValue get() = try { SortMode.valueOf(sortMode.value) } catch (_: Exception) { SortMode.UPDATED }
    fun penKeyCodeSet(): Set<Int> = penKeyCodes.value.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
    fun addPenKeyCode(code: Int) { penKeyCodes.value = (penKeyCodeSet() + code).joinToString(",") }

    private fun colors(p: Pref<String>) = p.value.split(',').mapNotNull { it.trim().toLongOrNull(16)?.toInt() }
    private fun setColor(p: Pref<String>, index: Int, color: Int) {
        val l = colors(p).toMutableList()
        if (index in l.indices) l[index] = color else l += color
        p.value = l.joinToString(",") { String.format("%08X", it) }
    }

    fun paletteColors(): List<Int> = colors(palette)
    fun setPaletteColor(index: Int, color: Int) = setColor(palette, index, color)
    fun highlighterColors(): List<Int> = colors(hlPalette)
    fun setHighlighterColor(index: Int, color: Int) = setColor(hlPalette, index, color or 0xFF000000.toInt())
    fun resetPalettes() {
        palette.value = "FF1A1A1A,FF1565C0,FFD32F2F,FF2E7D32,FFF57C00,FF6A1B9A"
        hlPalette.value = "FFFFEB3B,FF7CF29A,FFFF9EC7,FF8EC5FF,FFFFB74D"
    }

    /** Defterin en son bakılan sayfası (kaldığın yerden devam). */
    fun lastPage(id: String) = sp.getInt("lastPage_$id", 0)
    fun setLastPage(id: String, page: Int) = sp.edit().putInt("lastPage_$id", page).apply()

    fun presetList(): List<PenPreset> = presets.value.split(';').mapNotNull { if (it.isBlank()) null else PenPreset.decode(it) }
    fun addPreset(p: PenPreset) { presets.value = (presetList().filter { it != p } + p).takeLast(8).joinToString(";") { it.encode() } }
    fun removePreset(p: PenPreset) { presets.value = presetList().filter { it != p }.joinToString(";") { it.encode() } }

    companion object {
        @Volatile private var inst: Settings? = null
        fun get(context: Context): Settings = inst ?: synchronized(this) { inst ?: Settings(context.applicationContext).also { inst = it } }
    }
}
