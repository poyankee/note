package com.poyankee.note.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.poyankee.note.core.PageSize
import com.poyankee.note.core.Template

enum class ButtonAction(val title: String) { ERASER("Silgi"), LASSO("Kement seçim"), LASER("Lazer"), NONE("Hiçbir şey") }

/** Uygulama ayarları. Compose ekranları değişiklikleri anında görür; her değişiklik hemen kaydedilir. */
class Settings(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun bool(key: String, def: Boolean) = Pref(def, { sp.getBoolean(key, def) }, { sp.edit().putBoolean(key, it).apply() })
    private fun int(key: String, def: Int) = Pref(def, { sp.getInt(key, def) }, { sp.edit().putInt(key, it).apply() })
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

    // Kalem
    val pressure = bool("pressure", true)
    val smoothing = int("smoothing", 2)

    // Geometri
    val shapeHold = bool("shapeHold", true)
    val holdMs = int("holdMs", 550)
    val gridSnap = bool("gridSnap", true)
    val angleSnap = bool("angleSnap", true)

    // Ders
    val laserMs = int("laserMs", 1500)
    val tempMs = int("tempMs", 3000)
    val secondScreen = bool("secondScreen", true)
    val recordAudio = bool("recordAudio", true)

    // Yeni defter varsayılanları
    val newTemplate = str("newTemplate", Template.GRID.name)
    val newSize = str("newSize", PageSize.A4_PORTRAIT.name)
    val newDark = bool("newDark", false)

    // Araç durumu
    val palette = str("palette", "FF1A1A1A,FF1565C0,FFD32F2F,FF2E7D32,FFF57C00,FF6A1B9A")
    val tool = str("tool", "PEN")
    val penColor = int("penColor", 0xFF1A1A1A.toInt())
    val penWidth = int("penWidthX10", 20)
    val hlColor = int("hlColor", 0x66FFEB3B)
    val hlWidthX10 = int("hlWidthX10", 140)
    val eraserPartial = bool("eraserPartial", false)
    val eraserSize = int("eraserSize", 1)
    val polygonSides = int("polygonSides", 6)

    val buttonActionValue get() = try { ButtonAction.valueOf(buttonAction.value) } catch (_: Exception) { ButtonAction.ERASER }

    fun paletteColors(): List<Int> = palette.value.split(',').mapNotNull { it.trim().toLongOrNull(16)?.toInt() }
    fun setPaletteColor(index: Int, color: Int) {
        val l = paletteColors().toMutableList()
        if (index in l.indices) l[index] = color
        palette.value = l.joinToString(",") { String.format("%08X", it) }
    }

    companion object {
        @Volatile private var inst: Settings? = null
        fun get(context: Context): Settings = inst ?: synchronized(this) { inst ?: Settings(context.applicationContext).also { inst = it } }
    }
}
