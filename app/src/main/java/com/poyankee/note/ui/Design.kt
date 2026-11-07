package com.poyankee.note.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.poyankee.note.core.L

/** Uygulamanın adı (sol üstte). Ad ve simge daha sonra belirlenecek; kullanıcı değiştiremez. */
const val APP_NAME = "Note+"

/** Sol menünün üstündeki görsel. Resim dosyası yok; hepsi kodla çizilir (hafif). */
enum class SideArt(val title: String) {
    MOUNTAIN("Dağlar"), SEA("Deniz"), HILLS("Tepeler"), NIGHT("Gece"), NONE("Yok");

    companion object {
        fun of(name: String) = entries.firstOrNull { it.name == name } ?: MOUNTAIN
    }
}

@Composable
fun SideArtView(art: SideArt, modifier: Modifier) {
    val accent = UI.Primary
    val dark = UI.dark
    Canvas(modifier) {
        when (art) {
            SideArt.MOUNTAIN -> mountains(accent, dark)
            SideArt.SEA -> sea(accent, dark)
            SideArt.HILLS -> hills(accent, dark)
            SideArt.NIGHT -> night()
            SideArt.NONE -> drawRect(Brush.verticalGradient(listOf(lerp(accent, Color.White, 0.80f), lerp(accent, Color.White, 0.94f))))
        }
    }
}

private fun DrawScope.ridge(base: Float, amp: Float, seed: Int, peaks: Int, color: Color) {
    val w = size.width; val h = size.height
    val p = Path()
    p.moveTo(0f, h)
    p.lineTo(0f, base)
    var x = 0f
    val step = w / peaks
    for (i in 0 until peaks) {
        val r = ((seed * 31 + i * 17) % 10) / 10f
        val peakX = x + step * (0.35f + r * 0.3f)
        val peakY = base - amp * (0.55f + r * 0.45f)
        p.lineTo(peakX, peakY)
        x += step
        p.lineTo(x, base - amp * 0.15f * (1 - r))
    }
    p.lineTo(w, h)
    p.close()
    drawPath(p, color)
}

private fun DrawScope.pines(base: Float, height: Float, color: Color, seed: Int) {
    val w = size.width
    var x = -10f
    var i = 0
    while (x < w + 10f) {
        val r = ((seed * 13 + i * 29) % 10) / 10f
        val th = height * (0.55f + r * 0.6f)
        val tw = th * 0.42f
        val p = Path()
        p.moveTo(x, base - th)
        p.lineTo(x + tw / 2, base)
        p.lineTo(x - tw / 2, base)
        p.close()
        drawPath(p, color)
        x += tw * (0.55f + r * 0.5f) + if (i % 5 == 4) tw * 2.5f else 0f
        i++
    }
}

private fun DrawScope.mountains(accent: Color, dark: Boolean) {
    val h = size.height; val w = size.width
    val sky = if (dark) listOf(Color(0xFF1E2A3A), Color(0xFF2E3D52)) else listOf(lerp(Color(0xFFBFD6EA), accent, 0.08f), Color(0xFFEAF1F7))
    drawRect(Brush.verticalGradient(sky))
    drawCircle(Color.White.copy(alpha = if (dark) 0.15f else 0.55f), radius = h * 0.10f, center = Offset(w * 0.78f, h * 0.22f))
    ridge(h * 0.62f, h * 0.38f, 3, 3, if (dark) Color(0xFF3A4B63) else Color(0xFFAFC4D8))
    ridge(h * 0.74f, h * 0.30f, 7, 4, if (dark) Color(0xFF2F3F55) else Color(0xFF8DA8C2))
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = if (dark) 0.05f else 0.35f)), startY = h * 0.6f, endY = h * 0.85f))
    pines(h * 0.92f, h * 0.22f, if (dark) Color(0xFF1C2838) else Color(0xFF4F6B85), 5)
    pines(h * 1.02f, h * 0.26f, if (dark) Color(0xFF141E2B) else Color(0xFF3B556E), 11)
}

private fun DrawScope.sea(accent: Color, dark: Boolean) {
    val h = size.height; val w = size.width
    drawRect(Brush.verticalGradient(if (dark) listOf(Color(0xFF1B2636), Color(0xFF2B3B50)) else listOf(Color(0xFFFFE2C2), Color(0xFFE8F1F8))))
    drawCircle(if (dark) Color(0x33FFFFFF) else Color(0xFFFFC98B), radius = h * 0.13f, center = Offset(w * 0.3f, h * 0.48f))
    val waves = listOf(0.55f to (if (dark) Color(0xFF34506E) else lerp(accent, Color.White, 0.55f)), 0.68f to (if (dark) Color(0xFF29405A) else lerp(accent, Color.White, 0.35f)), 0.82f to (if (dark) Color(0xFF1F3247) else lerp(accent, Color.White, 0.15f)))
    for ((k, pair) in waves.withIndex()) {
        val (yy, c) = pair
        val p = Path()
        val base = h * yy
        p.moveTo(0f, h); p.lineTo(0f, base)
        val n = 4 + k
        val seg = w / n
        for (i in 0 until n) {
            val x0 = i * seg
            p.quadraticTo(x0 + seg / 2, base - h * 0.05f * (if (i % 2 == 0) 1 else -1), x0 + seg, base)
        }
        p.lineTo(w, h); p.close()
        drawPath(p, c)
    }
}

private fun DrawScope.hills(accent: Color, dark: Boolean) {
    val h = size.height; val w = size.width
    drawRect(Brush.verticalGradient(if (dark) listOf(Color(0xFF1C2A24), Color(0xFF2C3F36)) else listOf(Color(0xFFD9ECF7), Color(0xFFF2F8EE))))
    val cols = if (dark) listOf(Color(0xFF2F4A3C), Color(0xFF253D31), Color(0xFF1B2F25)) else listOf(Color(0xFFB9D9A8), Color(0xFF8FC07C), Color(0xFF6AA45C))
    for ((k, c) in cols.withIndex()) {
        val base = h * (0.62f + 0.12f * k)
        val p = Path()
        p.moveTo(0f, h); p.lineTo(0f, base)
        p.cubicTo(w * 0.3f, base - h * 0.2f, w * 0.6f, base + h * 0.08f, w, base - h * 0.12f * (k % 2))
        p.lineTo(w, h); p.close()
        drawPath(p, c)
    }
}

private fun DrawScope.night() {
    val h = size.height; val w = size.width
    drawRect(Brush.verticalGradient(listOf(Color(0xFF101A33), Color(0xFF2B3A64))))
    for (i in 0 until 40) {
        val x = ((i * 73) % 100) / 100f * w
        val y = ((i * 37) % 60) / 100f * h
        drawCircle(Color.White.copy(alpha = 0.35f + (i % 4) * 0.15f), radius = 1f + (i % 3), center = Offset(x, y))
    }
    drawCircle(Color(0xFFFFF4D6), radius = h * 0.09f, center = Offset(w * 0.75f, h * 0.22f))
    ridge(h * 0.78f, h * 0.30f, 5, 4, Color(0xFF1A2442))
    pines(h * 1.02f, h * 0.24f, Color(0xFF0C1226), 9)
}

/** Ayarlarda seçim için küçük önizleme adları. */
fun sideArtTitle(a: SideArt) = L.t(a.title)
