package com.poyankee.note.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Science
import com.poyankee.note.core.L

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ---------------- tasarım renkleri ---------------- */

/**
 * Renk temaları: vurgu rengi ile arka plan / masa tonu. Her biri açık ve koyu hâlde çalışır.
 * Sıra: vurgu (açık, koyu), vurgu zemini (açık, koyu), vurgu yazısı (açık, koyu), arka plan (açık, koyu), masa (açık, koyu).
 */
enum class Palette(val title: String, val c: LongArray) {
    DEFAULT(L.t("Varsayılan"), longArrayOf(0xFF4F46E5, 0xFF7C7FF5, 0xFFEEF0FF, 0xFF262A52, 0xFF3730A3, 0xFFC7C9FF, 0xFFF4F5F8, 0xFF0F1115, 0xFFE9EBEF, 0xFF0B0D10)),
    OCEAN(L.t("Okyanus"), longArrayOf(0xFF0369A1, 0xFF38BDF8, 0xFFE0F2FE, 0xFF0C2D44, 0xFF075985, 0xFFBAE6FD, 0xFFF1F6FA, 0xFF0B1218, 0xFFE3ECF2, 0xFF070D12)),
    FOREST(L.t("Orman"), longArrayOf(0xFF15803D, 0xFF4ADE80, 0xFFDCFCE7, 0xFF113322, 0xFF166534, 0xFFBBF7D0, 0xFFF2F6F2, 0xFF0D130F, 0xFFE4EBE5, 0xFF080C09)),
    SUNSET(L.t("Gün batımı"), longArrayOf(0xFFC2410C, 0xFFFB923C, 0xFFFFEDD5, 0xFF43230F, 0xFF9A3412, 0xFFFED7AA, 0xFFFAF4F0, 0xFF16100C, 0xFFF0E6DF, 0xFF0F0A07)),
    PAPER(L.t("Kağıt"), longArrayOf(0xFF8B5E34, 0xFFD6A46B, 0xFFF5EBDD, 0xFF3A2C1E, 0xFF6B4423, 0xFFF0DCC2, 0xFFF7F2E9, 0xFF15120E, 0xFFEDE5D6, 0xFF0E0C09)),
    NIGHT(L.t("Gece"), longArrayOf(0xFF4338CA, 0xFFA5B4FC, 0xFFE0E7FF, 0xFF1E1B4B, 0xFF312E81, 0xFFE0E7FF, 0xFFEEF0F7, 0xFF080A14, 0xFFE2E5F0, 0xFF04050C)),
    CUSTOM(L.t("Özel"), longArrayOf(0xFF4F46E5, 0xFF7C7FF5, 0xFFEEF0FF, 0xFF262A52, 0xFF3730A3, 0xFFC7C9FF, 0xFFF4F5F8, 0xFF0F1115, 0xFFE9EBEF, 0xFF0B0D10)),
    CONTRAST(L.t("Yüksek kontrast"), longArrayOf(0xFF1D00B8, 0xFFFFE600, 0xFFDCD6FF, 0xFF3A3400, 0xFF12007A, 0xFFFFF59D, 0xFFFFFFFF, 0xFF000000, 0xFFDDDDDD, 0xFF000000)),
}

/** Arayüz stili: Sade (ferah, sakin), Canlı (renkli çubuklar), Koyu (koyu masa, cam görünümlü araçlar). */
enum class UiStyle(val title: String) { CLEAN("Sade"), VIVID("Canlı"), DARK("Koyu") }

object UI {
    /** Arayüz stili (MainActivity ayarlardan okur). */
    var style by mutableStateOf(UiStyle.CLEAN)
    /** Üst çubuk zemini ve yazı rengi. */
    val BarBg get() = when (style) { UiStyle.CLEAN -> Surface; UiStyle.VIVID -> Primary; UiStyle.DARK -> Color(0xF2171A20) }
    val BarFg get() = when (style) { UiStyle.CLEAN -> Text; UiStyle.VIVID -> Color.White; UiStyle.DARK -> Color(0xFFE7E9EE) }
    val BarFgSoft get() = when (style) { UiStyle.CLEAN -> TextSoft; UiStyle.VIVID -> Color(0xD9FFFFFF); UiStyle.DARK -> Color(0xFFA3AAB8) }
    /** Seçili aracın zemini ve simge rengi (üst çubuktaki araçlar için). */
    val ToolSelBg get() = when (style) { UiStyle.CLEAN -> PrimarySoft; UiStyle.VIVID -> Color.White; UiStyle.DARK -> Color(0x33FFFFFF) }
    val ToolSelFg get() = when (style) { UiStyle.CLEAN -> Primary; UiStyle.VIVID -> Primary; UiStyle.DARK -> Color.White }
    /** Seçenek şeridi ve yüzen paneller (Koyu'da yarı saydam cam). */
    val Panel get() = when (style) { UiStyle.DARK -> Color(0xE61E2229); else -> Surface }
    /** Köşe yuvarlaklığı. */
    val radius get() = when (style) { UiStyle.CLEAN -> 16; UiStyle.VIVID -> 20; UiStyle.DARK -> 14 }

    /** Koyu tema açık mı (MainActivity ayarlar). Okuyan her ekran tema değişince yeniden çizilir. */
    var dark by mutableStateOf(false)
    /** Renk teması (MainActivity ayarlardan okur). */
    var palette by mutableStateOf(Palette.DEFAULT)
    /** Arayüz boyutu çarpanı (düğmeler ve yazılar). Sayfadaki yazıları etkilemez. */
    var scale by mutableStateOf(1f)
    /** Göz dostu mod: ekranın üstüne hafif sıcak (turuncu) filtre. */
    var warm by mutableStateOf(false)

    private fun c(light: Long, darkC: Long) = Color(if (dark) darkC else light)
    /** Özel tema rengi (Ayarlar › Renk teması › +). */
    var customAccent by mutableStateOf(0xFFF5A000.toInt())
    private fun mix(a: Int, b: Int, t: Float): Color {
        fun ch(x: Int, s: Int) = ((x shr s) and 0xFF)
        val r = ch(a, 16) + (ch(b, 16) - ch(a, 16)) * t; val g = ch(a, 8) + (ch(b, 8) - ch(a, 8)) * t; val bl = ch(a, 0) + (ch(b, 0) - ch(a, 0)) * t
        return Color(r / 255f, g / 255f, bl / 255f)
    }
    private fun p(i: Int): Color {
        if (palette == Palette.CUSTOM && i >= 3 && !dark) {
            val a = customAccent
            return if (i == 3) mix(a, 0xFFFFFFFF.toInt(), 0.955f) else mix(a, 0xFFF0F0F0.toInt(), 0.92f)
        }
        if (palette == Palette.CUSTOM && i <= 2) {
            val a = customAccent; val w = 0xFFFFFFFF.toInt(); val k = 0xFF000000.toInt(); val dk = 0xFF1A1D23.toInt()
            return when (i) {
                0 -> if (dark) mix(a, w, 0.35f) else Color(a)
                1 -> if (dark) mix(a, dk, 0.75f) else mix(a, w, 0.88f)
                else -> if (dark) mix(a, w, 0.6f) else mix(a, k, 0.35f)
            }
        }
        return Color(palette.c[2 * i + (if (dark) 1 else 0)])
    }
    private val hc get() = palette == Palette.CONTRAST
    val Primary get() = p(0)
    val PrimarySoft get() = p(1)
    val PrimaryText get() = p(2)
    val Bg get() = p(3)
    val Surface get() = if (hc) c(0xFFFFFFFF, 0xFF000000) else c(0xFFFFFFFF, 0xFF1A1D23)
    val SurfaceAlt get() = if (hc) c(0xFFEDEDED, 0xFF1C1C1C) else c(0xFFF2F4F7, 0xFF242830)
    val Border get() = if (hc) c(0xFF000000, 0xFFFFFFFF) else c(0xFFE4E7EC, 0xFF2E333C)
    val Text get() = if (hc) c(0xFF000000, 0xFFFFFFFF) else c(0xFF1D2330, 0xFFE7E9EE)
    val TextSoft get() = if (hc) c(0xFF1F1F1F, 0xFFE6E6E6) else c(0xFF667085, 0xFFA3AAB8)
    val TextFaint get() = if (hc) c(0xFF3D3D3D, 0xFFC8C8C8) else c(0xFF98A2B3, 0xFF6E7684)
    val Desk get() = p(4)
    val Danger get() = c(0xFFE5484D, 0xFFFF6B6F)
    val Shadow = Color(0x1A101828)
}

// Eski adlar (diğer dosyalarla uyum)
val Ink get() = UI.Text
val Accent get() = UI.Primary

@Composable
fun NoteTheme(content: @Composable () -> Unit) {
    val base = Typography()
    MaterialTheme(
        colorScheme = (if (UI.dark) androidx.compose.material3.darkColorScheme(
            primary = UI.Primary,
            onPrimary = if (UI.dark && UI.palette == Palette.CONTRAST) Color.Black else Color.White,
            primaryContainer = UI.PrimarySoft,
            onPrimaryContainer = UI.PrimaryText,
            secondary = Color(0xFF2DD4BF),
            surface = UI.Surface,
            onSurface = UI.Text,
            background = UI.Bg,
            onBackground = UI.Text,
            surfaceVariant = UI.SurfaceAlt,
            onSurfaceVariant = UI.TextSoft,
            outline = UI.Border,
            outlineVariant = UI.Border,
            surfaceContainer = UI.Surface,
            surfaceContainerHigh = UI.Surface,
            surfaceContainerHighest = UI.SurfaceAlt,
            surfaceContainerLow = UI.Surface,
        ) else lightColorScheme(
            primary = UI.Primary,
            onPrimary = if (UI.dark && UI.palette == Palette.CONTRAST) Color.Black else Color.White,
            primaryContainer = UI.PrimarySoft,
            onPrimaryContainer = UI.PrimaryText,
            secondary = Color(0xFF0E9384),
            surface = UI.Surface,
            onSurface = UI.Text,
            background = UI.Bg,
            onBackground = UI.Text,
            surfaceVariant = Color(0xFFF2F4F7),
            onSurfaceVariant = UI.TextSoft,
            outline = UI.Border,
            outlineVariant = UI.Border,
            surfaceContainer = UI.Surface,
            surfaceContainerHigh = UI.Surface,
            surfaceContainerHighest = Color(0xFFF2F4F7),
            surfaceContainerLow = UI.Surface,
        )),
        typography = base.copy(
            headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
            titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        ),
        content = {
            val d = androidx.compose.ui.platform.LocalDensity.current
            val sc = UI.scale
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(d.density * sc, d.fontScale),
            ) {
                androidx.compose.foundation.layout.Box {
                    content()
                    if (UI.warm) androidx.compose.foundation.layout.Box(
                        Modifier.matchParentSize().background(Color(0x1FFF8A00)),
                    )
                }
            }
        },
    )
}

/** Gölgeli, yuvarlak köşeli yüzen panel. */
@Composable
fun FloatingCard(modifier: Modifier = Modifier, radius: Int = 18, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(radius.dp),
        color = UI.Panel,
        shadowElevation = if (UI.style == UiStyle.DARK) 4.dp else 10.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, if (UI.style == UiStyle.DARK) Color(0x1FFFFFFF) else Color(0x0F101828)),
        content = content,
    )
}

/** Küçük, yuvarlak seçilebilir hap düğme. */
@Composable
fun Pill(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) UI.PrimarySoft else UI.SurfaceAlt)
            .border(1.dp, if (selected) UI.Primary.copy(alpha = 0.35f) else Color.Transparent, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 13.sp, color = if (selected) UI.PrimaryText else UI.Text, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
fun PillRow(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, content = content)
}

/** Hazır renkler (renk seçici). */
val PresetColors: List<Int> = listOf(
    0xFF111827, 0xFF374151, 0xFF6B7280, 0xFFD1D5DB, 0xFFFFFFFF, 0xFFFFF59D,
    0xFFDC2626, 0xFFE11D48, 0xFFEA580C, 0xFFF59E0B, 0xFFFACC15, 0xFF92400E,
    0xFF15803D, 0xFF22C55E, 0xFF0D9488, 0xFF06B6D4, 0xFF1D4ED8, 0xFF3B82F6,
    0xFF312E81, 0xFF7C3AED, 0xFFC026D3, 0xFFFCA5A5, 0xFFBBF7D0, 0xFFBAE6FD,
).map { it.toInt() }

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ColorDot(color: Int, selected: Boolean, size: Int = 28, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    val c = Color(color or 0xFF000000.toInt())
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .border(2.dp, if (selected) UI.Primary else Color.Transparent, CircleShape)
            .padding(if (selected) 4.dp else 2.dp)
            .clip(CircleShape)
            .background(c)
            .border(1.dp, Color(0x22000000), CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UI.Surface,
        title = { Text(L.t("Renk seç")) },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PresetColors.forEach { c -> ColorDot(c, (c or 0xFF000000.toInt()) == (current or 0xFF000000.toInt()), 40) { onPick(c) } }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(L.t("Kapat")) } },
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String = L.t("Evet"), onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UI.Surface,
        title = { Text(title) },
        text = { Text(text, color = UI.TextSoft) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm, color = if (confirm == L.t("Sil")) UI.Danger else UI.Primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(L.t("Vazgeç"), color = UI.TextSoft) } },
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text.uppercase(), style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
        color = UI.TextFaint, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

@Composable
fun LabeledRow(label: String, sub: String? = null, modifier: Modifier = Modifier, trailing: @Composable () -> Unit) {
    Row(modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = UI.Text)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = UI.TextSoft)
        }
        trailing()
    }
}

/**
 * Önerilen sabit renkler + kullanıcının eklediği renkler + "+" (ton/doygunluk/parlaklık ile yeni renk).
 * Eklenen renge uzun basınca silinir.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ColorChoiceRow(presets: List<Int>, saved: List<Int>, selected: Int, size: Int = 36, onPick: (Int) -> Unit, onAdd: (Int) -> Unit, onRemove: (Int) -> Unit) {
    var adding by remember { mutableStateOf(false) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { c -> ColorDot(c, c == selected, size) { onPick(c) } }
        if (saved.isNotEmpty()) Box(Modifier.width(1.dp).height(size.dp).background(UI.Border))
        saved.forEach { c -> ColorDot(c, c == selected, size, onLongClick = { onRemove(c) }) { onPick(c) } }
        Box(
            Modifier.size(size.dp).clip(CircleShape).background(UI.SurfaceAlt).border(1.dp, UI.Border, CircleShape).clickable { adding = true },
            contentAlignment = Alignment.Center,
        ) { Text("+", fontSize = 20.sp, color = UI.TextSoft) }
    }
    if (adding) {
        var picked by remember { mutableStateOf(selected) }
        val start = remember { selected }
        AlertDialog(
            onDismissRequest = { adding = false }, containerColor = UI.Surface,
            title = { Text(L.t("Özel renk")) },
            text = { Column { HsvPicker(start) { picked = it or 0xFF000000.toInt() } } },
            confirmButton = { TextButton(onClick = { adding = false; onAdd(picked); onPick(picked) }) { Text(L.t("Ekle")) } },
            dismissButton = { TextButton(onClick = { adding = false }) { Text(L.t("Vazgeç"), color = UI.TextSoft) } },
        )
    }
}

/** Sayfanın arkası (masa) için önerilen renkler. */
val DESK_PRESETS = listOf(0xFFE9EBEF, 0xFFF3EBDD, 0xFFDCE6EE, 0xFFE3EADF, 0xFF5B6472, 0xFF2A2F38, 0xFF111418).map { it.toInt() }

/** Uygulama rengi için önerilenler. */
val ACCENT_PRESETS = listOf(0xFFF5A000, 0xFFF97316, 0xFFEF4444, 0xFFEC4899, 0xFF8B5CF6, 0xFF4F46E5, 0xFF0EA5E9, 0xFF14B8A6, 0xFF22C55E, 0xFF475569).map { it.toInt() }

/** Sol üstteki uygulama simgesi seçenekleri. */
enum class AppIcon(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    PEN("Kalem", Icons.Rounded.Edit),
    BOOK("Kitap", Icons.Rounded.MenuBook),
    SCHOOL("Kep", Icons.Rounded.School),
    CALC("Hesap", Icons.Rounded.Calculate),
    IDEA("Ampul", Icons.Rounded.Lightbulb),
    STAR("Yıldız", Icons.Rounded.Star),
    SCIENCE("Bilim", Icons.Rounded.Science),
    ;
    companion object { fun of(name: String) = entries.firstOrNull { it.name == name } ?: PEN }
}

/** Ana sayfadaki sol menüde sıralanabilir / gizlenebilir bölümler. */
enum class SideItem(val title: String) {
    ALL("Tüm defterler"), FAVORITES("Favoriler"), QUICK("Hızlı notlar"), WRONG("Yanlış defteri"), TODO("Yapılacaklar"), TRASH("Çöp kutusu");
    companion object {
        const val DEFAULT = "ALL,FAVORITES,QUICK,WRONG,TODO,TRASH"
        fun parse(s: String): List<SideItem> = s.split(',').mapNotNull { n -> entries.firstOrNull { it.name == n.trim() } }
    }
}
