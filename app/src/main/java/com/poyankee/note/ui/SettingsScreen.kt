package com.poyankee.note.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poyankee.note.data.ButtonAction
import com.poyankee.note.data.Settings
import kotlin.math.roundToInt

@Suppress("DEPRECATION")
@Composable
fun SettingsScreen(s: Settings, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(UI.Bg)) {
        Row(Modifier.fillMaxWidth().background(UI.Surface).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Geri", tint = UI.Text) }
            Text("Ayarlar", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = UI.Text)
        }
        HorizontalDivider(color = UI.Border)
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                Group("Kalem ve el") {
                    Toggle("Sadece kalemle çiz", "El ve avuç dokunuşları asla çizim yapmaz", s.penOnly.value) { s.penOnly.value = it }
                    Toggle("Parmakla kaydır / yakınlaştır", "Kapalıysa parmak dokunuşları tamamen yok sayılır", s.fingerGestures.value) { s.fingerGestures.value = it }
                    Toggle("İki parmakla dokun = geri al", "Üç parmakla dokun = yinele", s.twoFingerUndo.value) { s.twoFingerUndo.value = it }
                    Toggle("Solak kullanım", "Araç menüsü sağ tarafa geçer", s.leftHanded.value) { s.leftHanded.value = it }
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text("Kalem tuşu", fontSize = 16.sp, color = UI.Text)
                        Text("Kalemin yan tuşuna basılıyken çalışacak araç", fontSize = 13.sp, color = UI.TextSoft)
                        Spacer(Modifier.height(8.dp))
                        PillRow { ButtonAction.entries.forEach { a -> Pill(a.title, s.buttonActionValue == a) { s.buttonAction.value = a.name } } }
                    }
                }
                Group("Yazı") {
                    Toggle("Basınç duyarlılığı", "Kalemi bastırdıkça çizgi kalınlaşır", s.pressure.value) { s.pressure.value = it }
                    SliderRow("Çizgi yumuşatma", listOf("Kapalı", "Az", "Orta", "Çok", "En çok")[s.smoothing.value.coerceIn(0, 4)],
                        s.smoothing.value.toFloat(), 0f..4f, 3) { s.smoothing.value = it.roundToInt() }
                }
                Group("Geometri") {
                    Toggle("Çizip bekleyince şekle dönüştür", "Kalemi çizginin sonunda bir an bekletin", s.shapeHold.value) { s.shapeHold.value = it }
                    SliderRow("Bekleme süresi", "${s.holdMs.value} ms", s.holdMs.value.toFloat(), 300f..1200f) { s.holdMs.value = (it / 50).roundToInt() * 50 }
                    Toggle("Izgaraya ve köşelere yapıştır", "Şekil köşeleri desene ve diğer köşelere yapışır", s.gridSnap.value) { s.gridSnap.value = it }
                    Toggle("Açı yapıştırma (15°)", "Çizgiler 0°, 15°, 30°, 45°... açılarına yapışır", s.angleSnap.value) { s.angleSnap.value = it }
                }
                Group("Ders anlatımı") {
                    SliderRow("Lazer izinin kaybolma süresi", "${"%.1f".format(s.laserMs.value / 1000f)} sn", s.laserMs.value.toFloat(), 500f..5000f) {
                        s.laserMs.value = (it / 100).roundToInt() * 100
                    }
                    SliderRow("Geçici kalemin kaybolma süresi", "${"%.1f".format(s.tempMs.value / 1000f)} sn", s.tempMs.value.toFloat(), 1000f..10000f) {
                        s.tempMs.value = (it / 100).roundToInt() * 100
                    }
                    Toggle("İkinci ekran (projeksiyon / HDMI)", "Harici ekranda sadece temiz sayfa görünür, araçlar tablette kalır", s.secondScreen.value) { s.secondScreen.value = it }
                    Toggle("Ders kaydında mikrofon", "Video kaydına sesinizi de ekler", s.recordAudio.value) { s.recordAudio.value = it }
                }
                Text(
                    "Not: Kablosuz yansıtma tüm ekranı gösterir; temiz görüntü için sunum modunu (tam ekran) kullanın.",
                    fontSize = 13.sp, color = UI.TextSoft, modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    SectionTitle(title)
    Surface(shape = RoundedCornerShape(16.dp), color = UI.Surface, border = BorderStroke(1.dp, UI.Border), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(0.dp), content = content)
    }
}

@Composable
private fun Toggle(title: String, sub: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = UI.Text)
            Text(sub, fontSize = 13.sp, color = UI.TextSoft)
        }
        Spacer(Modifier.width(12.dp))
        Switch(value, onChange)
    }
}

@Composable
private fun SliderRow(title: String, valueText: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int = 0, onChange: (Float) -> Unit) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 16.sp, color = UI.Text, modifier = Modifier.weight(1f))
            Text(valueText, fontSize = 14.sp, color = UI.Primary, fontWeight = FontWeight.SemiBold)
        }
        Slider(value, onChange, valueRange = range, steps = steps, colors = SliderDefaults.colors(thumbColor = UI.Primary, activeTrackColor = UI.Primary))
    }
}
