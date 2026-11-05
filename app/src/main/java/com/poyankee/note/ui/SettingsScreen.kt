@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.poyankee.note.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.poyankee.note.data.ButtonAction
import com.poyankee.note.data.Settings
import kotlin.math.roundToInt

@Suppress("DEPRECATION")
@Composable
fun SettingsScreen(s: Settings, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("Ayarlar") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Geri") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink, titleContentColor = Color.White, navigationIconContentColor = Color.White),
        )
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp).widthIn(max = 760.dp)) {
            SectionTitle("Kalem ve el")
            LabeledRow("Sadece kalemle çiz", "El ve avuç dokunuşları asla çizim yapmaz") { Switch(s.penOnly.value, { s.penOnly.value = it }) }
            LabeledRow("Parmakla kaydır / yakınlaştır", "Kapalıysa parmak dokunuşları tamamen yok sayılır (sadece kalem modunda)") {
                Switch(s.fingerGestures.value, { s.fingerGestures.value = it })
            }
            LabeledRow("İki parmakla dokun = geri al", "Üç parmakla dokun = yinele") { Switch(s.twoFingerUndo.value, { s.twoFingerUndo.value = it }) }
            LabeledRow("Kalem tuşu", "Kalemin yan tuşuna basılıyken çalışacak araç") {}
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ButtonAction.entries.forEach { a -> FilterChip(s.buttonActionValue == a, { s.buttonAction.value = a.name }, { Text(a.title) }) }
            }
            LabeledRow("Solak kullanım", "Araç çubuğu sağ tarafa geçer") { Switch(s.leftHanded.value, { s.leftHanded.value = it }) }

            SectionTitle("Yazı")
            LabeledRow("Basınç duyarlılığı", "Kalemi bastırdıkça çizgi kalınlaşır") { Switch(s.pressure.value, { s.pressure.value = it }) }
            LabeledRow("Çizgi yumuşatma: ${listOf("kapalı", "az", "orta", "çok", "en çok")[s.smoothing.value.coerceIn(0, 4)]}") {}
            Slider(s.smoothing.value.toFloat(), { s.smoothing.value = it.roundToInt() }, valueRange = 0f..4f, steps = 3)

            SectionTitle("Geometri")
            LabeledRow("Çizip bekleyince şekle dönüştür", "Kalemi çizginin sonunda bir an bekletin: düz çizgi, çember, üçgen, dikdörtgen...") {
                Switch(s.shapeHold.value, { s.shapeHold.value = it })
            }
            LabeledRow("Bekleme süresi: ${s.holdMs.value} ms") {}
            Slider(s.holdMs.value.toFloat(), { s.holdMs.value = (it / 50).roundToInt() * 50 }, valueRange = 300f..1200f)
            LabeledRow("Izgaraya ve köşelere yapıştır", "Şekillerin köşeleri kareli sayfanın kesişimlerine ve diğer köşelere yapışır") {
                Switch(s.gridSnap.value, { s.gridSnap.value = it })
            }
            LabeledRow("Açı yapıştırma (15°)", "Çizgiler 0°, 15°, 30°, 45°... açılarına yapışır") { Switch(s.angleSnap.value, { s.angleSnap.value = it }) }

            SectionTitle("Ders anlatımı")
            LabeledRow("Lazer izinin kaybolma süresi: ${"%.1f".format(s.laserMs.value / 1000f)} sn") {}
            Slider(s.laserMs.value.toFloat(), { s.laserMs.value = (it / 100).roundToInt() * 100 }, valueRange = 500f..5000f)
            LabeledRow("Geçici kalemin kaybolma süresi: ${"%.1f".format(s.tempMs.value / 1000f)} sn") {}
            Slider(s.tempMs.value.toFloat(), { s.tempMs.value = (it / 100).roundToInt() * 100 }, valueRange = 1000f..10000f)
            LabeledRow("İkinci ekran (projeksiyon / HDMI)", "Harici ekranda sadece temiz sayfa gösterilir, araçlar tablette kalır") {
                Switch(s.secondScreen.value, { s.secondScreen.value = it })
            }
            LabeledRow("Ders kaydında mikrofon", "Video kaydına sesinizi de ekler") { Switch(s.recordAudio.value, { s.recordAudio.value = it }) }
            Text(
                "Not: Kablosuz yansıtma (ekran yansıtma) tüm ekranı gösterir; temiz görüntü için sunum modunu (tam ekran) kullanın.",
                style = MaterialTheme.typography.bodySmall, color = Color(0xFF607D8B), modifier = Modifier.padding(vertical = 12.dp),
            )
        }
    }
}
