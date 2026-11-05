@file:OptIn(ExperimentalLayoutApi::class)

package com.poyankee.note.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poyankee.note.core.Background
import com.poyankee.note.core.PdfGeom
import com.poyankee.note.editor.EditorController
import com.poyankee.note.editor.FormulaRequest
import com.poyankee.note.editor.TextRequest
import com.poyankee.note.render.Formula
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/* ======================= formül ======================= */

private val FORMULA_KEYS = listOf(
    "a/b" to "\\frac{}{}", "x²" to "^{2}", "xⁿ" to "^{}", "xₙ" to "_{}", "√" to "\\sqrt{}", "∛" to "\\sqrt[3]{}",
    "π" to "\\pi ", "α" to "\\alpha ", "β" to "\\beta ", "θ" to "\\theta ", "Δ" to "\\Delta ", "∑" to "\\sum_{}^{}",
    "∫" to "\\int_{}^{}", "lim" to "\\lim_{x \\to }", "≤" to "\\le ", "≥" to "\\ge ", "≠" to "\\neq ", "±" to "\\pm ",
    "·" to "\\cdot ", "×" to "\\times ", "∞" to "\\infty ", "°" to "^{\\circ}", "∠" to "\\angle ", "△" to "\\triangle ",
)

@Composable
fun FormulaDialog(ctl: EditorController, req: FormulaRequest) {
    var value by remember(req) {
        val init = req.existing?.source ?: ""
        mutableStateOf(TextFieldValue(init, TextRange(init.length)))
    }
    var size by remember(req) { mutableFloatStateOf(22f) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(req) { delay(150); try { focus.requestFocus() } catch (_: Exception) {} }
    val preview: ImageBitmap? = remember(value.text) {
        if (value.text.isBlank()) null else try { Formula.render(value.text, 22f, 0xFF000000.toInt(), 2f).first.asImageBitmap() } catch (_: Throwable) { null }
    }
    fun insert(snippet: String) {
        val t = value.text
        val sel = value.selection
        val nt = t.substring(0, sel.start) + snippet + t.substring(sel.end)
        // imleci ilk boş süslü parantezin içine koy
        val brace = snippet.indexOf("{}")
        val cur = sel.start + if (brace >= 0) brace + 1 else snippet.length
        value = TextFieldValue(nt, TextRange(cur))
    }
    AlertDialog(
        onDismissRequest = { ctl.formulaRequest = null },
        containerColor = UI.Surface,
        title = { Text(if (req.existing != null) "Formülü düzenle" else "Formül ekle") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 90.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)
                        .border(1.dp, UI.Border, RoundedCornerShape(12.dp)).padding(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (preview != null) Image(preview, null)
                    else Text("Önizleme burada görünür", color = Color(0xFF98A2B3))
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value, { value = it }, label = { Text("Formül (ör. \\frac{a}{b}, x^{2}, \\sqrt{x})") },
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FORMULA_KEYS.forEach { (label, snip) ->
                        Box(
                            Modifier.size(44.dp, 38.dp).clip(RoundedCornerShape(10.dp)).background(UI.SurfaceAlt).clickable { insert(snip) },
                            contentAlignment = Alignment.Center,
                        ) { Text(label, fontSize = 16.sp, color = UI.Text) }
                    }
                }
                SectionTitle("Boyut")
                PillRow { for (sz in listOf(14f, 18f, 22f, 30f, 40f)) Pill("${sz.toInt()}", size == sz) { size = sz } }
            }
        },
        confirmButton = { Button(onClick = { ctl.finishFormula(req, value.text, size) }, shape = RoundedCornerShape(10.dp)) { Text("Ekle") } },
        dismissButton = { TextButton(onClick = { ctl.formulaRequest = null }) { Text("Vazgeç", color = UI.TextSoft) } },
    )
}

/* ======================= fonksiyon grafiği ======================= */

@Composable
fun PlotDialog(ctl: EditorController, onDismiss: () -> Unit) {
    val palette = ctl.settings.paletteColors()
    val exprs = remember { mutableStateListOf("", "", "") }
    val colors = remember { mutableStateListOf(palette.getOrElse(1) { 0xFF1565C0.toInt() }, palette.getOrElse(2) { 0xFFD32F2F.toInt() }, palette.getOrElse(3) { 0xFF2E7D32.toInt() }) }
    var error by remember { mutableStateOf<String?>(null) }
    var active by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UI.Surface,
        title = { Text("Fonksiyon grafiği") },
        text = {
            Column {
                Text("Grafik, ekrandaki sayfanın koordinat sistemine çizilir (koordinat düzlemi deseninde eksenlerle birlikte).", fontSize = 13.sp, color = UI.TextSoft)
                Spacer(Modifier.height(8.dp))
                for (k in 0 until 3) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Box(
                            Modifier.size(28.dp).clip(CircleShape).background(Color(colors[k]))
                                .clickable { colors[k] = palette.getOrElse((palette.indexOf(colors[k]) + 1) % maxOf(palette.size, 1)) { colors[k] } },
                        )
                        Spacer(Modifier.width(10.dp))
                        OutlinedTextField(
                            exprs[k], { exprs[k] = it; error = null; active = k },
                            label = { Text("y = ") }, singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                error?.let { Text(it, color = UI.Danger, fontSize = 13.sp) }
                SectionTitle("Örnekler")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (ex in listOf("x^2", "2x+1", "-x^2+4", "x^3-3x", "sin(x)", "cos(x)", "1/x", "√x", "abs(x)", "2^x", "ln(x)")) {
                        Pill(ex, false) {
                            val slot = exprs.indexOfFirst { it.isBlank() }.let { if (it < 0) active else it }
                            exprs[slot] = ex
                        }
                    }
                }
                Text("Kullanılabilir: + − × ÷ ^ ( ) √ sin cos tan ln log abs π e  •  örtük çarpma: 2x, 3sin(x)", fontSize = 12.sp, color = UI.TextFaint, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = {
            Button(onClick = {
                val err = ctl.plotFunctions(exprs.indices.map { exprs[it] to colors[it] })
                if (err == null) onDismiss() else error = err
            }, shape = RoundedCornerShape(10.dp)) { Text("Çiz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç", color = UI.TextSoft) } },
    )
}

/* ======================= damgalar ======================= */

private class Stamp(val text: String, val color: Int, val bg: Int)

private val STAMPS = listOf(
    Stamp("✓", 0xFF16A34A.toInt(), 0), Stamp("✗", 0xFFDC2626.toInt(), 0), Stamp("★", 0xFFF59E0B.toInt(), 0),
    Stamp("!", 0xFFDC2626.toInt(), 0), Stamp("?", 0xFF2563EB.toInt(), 0), Stamp("♥", 0xFFE11D48.toInt(), 0),
    Stamp("Dikkat!", 0xFFB91C1C.toInt(), 0xFFFEE2E2.toInt()), Stamp("Önemli", 0xFF92400E.toInt(), 0xFFFEF3C7.toInt()),
    Stamp("Formül", 0xFF1E40AF.toInt(), 0xFFDBEAFE.toInt()), Stamp("Ödev", 0xFF6B21A8.toInt(), 0xFFF3E8FF.toInt()),
    Stamp("Sınavda çıkar", 0xFF9F1239.toInt(), 0xFFFFE4E6.toInt()), Stamp("Tekrar et", 0xFF115E59.toInt(), 0xFFCCFBF1.toInt()),
    Stamp("Aferin!", 0xFF166534.toInt(), 0xFFDCFCE7.toInt()), Stamp("Örnek", 0xFF334155.toInt(), 0xFFE2E8F0.toInt()),
    Stamp("Tanım", 0xFF0E7490.toInt(), 0xFFCFFAFE.toInt()), Stamp("İpucu", 0xFF854D0E.toInt(), 0xFFFEF9C3.toInt()),
)

@Composable
fun StampDialog(ctl: EditorController, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UI.Surface,
        title = { Text("Damga ekle") },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                STAMPS.forEach { st ->
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp)).background(if (st.bg != 0) Color(st.bg) else UI.SurfaceAlt)
                            .clickable { ctl.insertStamp(st.text, st.color, st.bg); onDismiss() }.padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(st.text, color = Color(st.color), fontSize = if (st.text.length <= 2) 26.sp else 15.sp, fontWeight = FontWeight.Bold) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat", color = UI.TextSoft) } },
    )
}

/* ======================= sayaç ======================= */

@Composable
fun TimerDialog(ctl: EditorController, onDismiss: () -> Unit) {
    var custom by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UI.Surface,
        title = { Text("Sayaç") },
        text = {
            Column {
                Text("Geri sayım ekranın sağ üstünde, projeksiyonda ve ders kaydında da görünür.", fontSize = 13.sp, color = UI.TextSoft)
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (m in listOf(1, 2, 3, 5, 10, 15, 20, 30)) Pill("$m dk", false) { ctl.startTimer(m * 60); onDismiss() }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        custom, { custom = it.filter { c -> c.isDigit() }.take(3) }, label = { Text("Dakika") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(140.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    TextButton(onClick = { custom.toIntOrNull()?.let { if (it > 0) { ctl.startTimer(it * 60); onDismiss() } } }) { Text("Başlat") }
                }
            }
        },
        confirmButton = {
            if (ctl.timerEnd > 0L) TextButton(onClick = { ctl.stopTimer(); onDismiss() }) { Text("Sayacı kapat", color = UI.Danger) }
            else TextButton(onClick = onDismiss) { Text("Kapat", color = UI.TextSoft) }
        },
    )
}

/* ======================= PDF sayfasını kırp ======================= */

@Composable
fun CropDialog(ctl: EditorController, index: Int, onDismiss: () -> Unit) {
    val page = ctl.nb.pages.getOrNull(index)
    val bg = page?.background as? Background.PdfPage
    if (bg == null) { onDismiss(); return }
    val m = remember(index) { PdfGeom.displayMargins(bg.rotation, bg.crop) }
    var top by remember(index) { mutableFloatStateOf(m[0]) }
    var right by remember(index) { mutableFloatStateOf(m[1]) }
    var bottom by remember(index) { mutableFloatStateOf(m[2]) }
    var left by remember(index) { mutableFloatStateOf(m[3]) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UI.Surface,
        title = { Text("Sayfayı kırp") },
        text = {
            Column {
                Text("Kenarlardan kesilecek payı ayarlayın (beyaz kenar boşluklarını atmak için). Yazılarınız yerinde kalır.", fontSize = 13.sp, color = UI.TextSoft)
                @Composable
                fun side(name: String, v: Float, set: (Float) -> Unit) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(name, modifier = Modifier.width(56.dp), color = UI.Text)
                        Slider(v, set, valueRange = 0f..0.45f, modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = UI.Primary, activeTrackColor = UI.Primary))
                        Text("%${(v * 100).roundToInt()}", modifier = Modifier.width(44.dp), color = UI.TextSoft)
                    }
                }
                side("Üst", top) { top = it }
                side("Alt", bottom) { bottom = it }
                side("Sol", left) { left = it }
                side("Sağ", right) { right = it }
            }
        },
        confirmButton = {
            Button(onClick = { ctl.setCrop(index, top, right, bottom, left); onDismiss() }, shape = RoundedCornerShape(10.dp)) { Text("Uygula") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { ctl.setCrop(index, 0f, 0f, 0f, 0f); onDismiss() }) { Text("Kırpmayı kaldır") }
                TextButton(onClick = onDismiss) { Text("Vazgeç", color = UI.TextSoft) }
            }
        },
    )
}

/* ======================= renk paleti düzenleme ======================= */

/** Kalem veya fosforlu kalem renklerini düzenler: bir yuvaya dokun, aşağıdan rengini seç. */
@Composable
fun PaletteDialog(ctl: EditorController, highlighter: Boolean, onDismiss: () -> Unit) {
    val s = ctl.settings
    val colors = if (highlighter) s.highlighterColors() else s.paletteColors()
    var slot by remember { mutableIntStateOf(0) }
    val current = colors.getOrElse(slot) { 0xFF000000.toInt() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UI.Surface,
        title = { Text(if (highlighter) "Fosforlu kalem renkleri" else "Kalem renkleri") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Değiştirmek istediğiniz renge dokunun, sonra yeni rengi seçin.", fontSize = 13.sp, color = UI.TextSoft)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colors.forEachIndexed { i, c -> ColorDot(c, i == slot, 44) { slot = i } }
                }
                SectionTitle("Hazır renkler")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetColors.forEach { c ->
                        ColorDot(c, (c or 0xFF000000.toInt()) == (current or 0xFF000000.toInt()), 36) {
                            if (highlighter) s.setHighlighterColor(slot, c) else s.setPaletteColor(slot, c)
                            ctl.setColor(c)
                        }
                    }
                }
                SectionTitle("Özel renk")
                HsvPicker(current) { c ->
                    if (highlighter) s.setHighlighterColor(slot, c) else s.setPaletteColor(slot, c)
                }
            }
        },
        confirmButton = { Button(onClick = { ctl.setColor((if (highlighter) s.highlighterColors() else s.paletteColors()).getOrElse(slot) { current }); onDismiss() }, shape = RoundedCornerShape(10.dp)) { Text("Tamam") } },
        dismissButton = { TextButton(onClick = { s.resetPalettes(); onDismiss() }) { Text("Varsayılana dön", color = UI.TextSoft) } },
    )
}

/** Ton / doygunluk / parlaklık kaydırıcılarıyla renk seçici. */
@Composable
fun HsvPicker(color: Int, onChange: (Int) -> Unit) {
    val hsv = remember(color) { FloatArray(3).also { android.graphics.Color.colorToHSV(color, it) } }
    var h by remember(color) { mutableFloatStateOf(hsv[0]) }
    var sat by remember(color) { mutableFloatStateOf(hsv[1]) }
    var v by remember(color) { mutableFloatStateOf(hsv[2]) }
    fun out() = android.graphics.Color.HSVToColor(floatArrayOf(h, sat, v))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(Color(out())).border(1.dp, UI.Border, CircleShape))
        Spacer(Modifier.width(12.dp))
        Text("#" + String.format("%06X", out() and 0xFFFFFF), color = UI.TextSoft, fontFamily = FontFamily.Monospace)
    }
    val hues = (0..6).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it * 60f, 1f, 1f))) }
    Box(Modifier.fillMaxWidth().height(10.dp).padding(horizontal = 10.dp).clip(RoundedCornerShape(5.dp)).background(Brush.horizontalGradient(hues)))
    Slider(h, { h = it; onChange(out()) }, valueRange = 0f..360f, colors = SliderDefaults.colors(thumbColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(h, 1f, 1f))), activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent))
    Text("Doygunluk", fontSize = 12.sp, color = UI.TextSoft)
    Slider(sat, { sat = it; onChange(out()) }, colors = SliderDefaults.colors(thumbColor = UI.Primary, activeTrackColor = Color(out())))
    Text("Parlaklık", fontSize = 12.sp, color = UI.TextSoft)
    Slider(v, { v = it; onChange(out()) }, colors = SliderDefaults.colors(thumbColor = UI.Primary, activeTrackColor = Color(out())))
}

/* ======================= metin / yapışkan not ======================= */

@Composable
fun TextDialog(ctl: EditorController, req: TextRequest) {
    var text by remember(req) { mutableStateOf(req.existing?.text ?: "") }
    var size by remember(req) { mutableFloatStateOf(req.existing?.size ?: if (req.sticky) 14f else 16f) }
    var note by remember(req) { mutableIntStateOf(req.existing?.bg?.takeIf { it != 0 } ?: EditorController.STICKY_COLORS[0]) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(req) { delay(150); try { focus.requestFocus() } catch (_: Exception) {} }
    AlertDialog(
        onDismissRequest = { ctl.textRequest = null },
        containerColor = UI.Surface,
        title = { Text(if (req.sticky) (if (req.existing != null) "Notu düzenle" else "Yapışkan not") else if (req.existing != null) "Metni düzenle" else "Metin ekle") },
        text = {
            Column {
                OutlinedTextField(
                    text, { text = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp).focusRequester(focus)
                        .let { if (req.sticky) it.background(Color(note), RoundedCornerShape(6.dp)) else it },
                )
                if (req.sticky) {
                    SectionTitle("Not rengi")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        EditorController.STICKY_COLORS.forEach { c -> ColorDot(c, c == note, 36) { note = c } }
                    }
                }
                SectionTitle("Yazı boyutu")
                PillRow { for (sz in listOf(10f, 14f, 18f, 24f, 36f)) Pill("${sz.toInt()}", size == sz) { size = sz } }
            }
        },
        confirmButton = { Button(onClick = { ctl.finishText(req, text, size, note) }, shape = RoundedCornerShape(10.dp)) { Text("Tamam") } },
        dismissButton = {
            Row {
                if (req.existing != null) TextButton(onClick = { ctl.finishText(req, "", size) }) { Text("Sil", color = UI.Danger) }
                TextButton(onClick = { ctl.textRequest = null }) { Text("Vazgeç", color = UI.TextSoft) }
            }
        },
    )
}

@Suppress("unused")
private fun colorArgb(c: Color) = c.toArgb()
