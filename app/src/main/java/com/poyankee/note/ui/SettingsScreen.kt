package com.poyankee.note.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poyankee.note.data.ButtonAction
import com.poyankee.note.data.PenAction
import com.poyankee.note.data.PenKeys
import com.poyankee.note.data.Settings
import com.poyankee.note.data.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Suppress("DEPRECATION")
@Composable
fun SettingsScreen(
    s: Settings,
    onBack: () -> Unit,
    onBackupNow: (Uri) -> Unit = {},
    onRestore: (Uri) -> Unit = {},
    onBackupFolder: (Uri) -> Unit = {},
) {
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> if (uri != null) onBackupNow(uri) }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) onRestore(uri) }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> if (uri != null) onBackupFolder(uri) }
    var learning by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(UI.Bg)) {
        Row(Modifier.fillMaxWidth().background(UI.Surface).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Geri", tint = UI.Text) }
            Text("Ayarlar", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = UI.Text)
        }
        HorizontalDivider(color = UI.Border)
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                Group("Görünüm") {
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text("Tema", fontSize = 16.sp, color = UI.Text)
                        Spacer(Modifier.height(8.dp))
                        PillRow { ThemeMode.entries.forEach { m -> Pill(m.title, s.themeValue == m) { s.theme.value = m.name } } }
                    }
                    Toggle("Solak kullanım", "Araç menüsü sağ tarafa geçer", s.leftHanded.value) { s.leftHanded.value = it }
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text("Panelleri varsayılan yerine al", fontSize = 16.sp, color = UI.Text)
                        Text("Araç menüsü ve sayfa gezgini taşındıysa", fontSize = 13.sp, color = UI.TextSoft)
                        Spacer(Modifier.height(6.dp))
                        Pill("Sıfırla", false) {
                            s.dockX.value = 0f; s.dockY.value = 0f; s.navX.value = 0f; s.navY.value = 0f; s.presX.value = 0f; s.presY.value = 0f
                            s.dockCollapsed.value = false
                        }
                    }
                }
                Group("Kalem ve el") {
                    Toggle("Sadece kalemle çiz", "El ve avuç dokunuşları asla çizim yapmaz", s.penOnly.value) { s.penOnly.value = it }
                    Toggle("Parmakla kaydır / yakınlaştır", "Kapalıysa parmak dokunuşları tamamen yok sayılır", s.fingerGestures.value) { s.fingerGestures.value = it }
                    Toggle("İki parmakla dokun = geri al", "Üç parmakla dokun = yinele", s.twoFingerUndo.value) { s.twoFingerUndo.value = it }
                    SliderRow("Avuç algılama", when {
                        s.palmSizeDp.value < 50 -> "çok hassas"; s.palmSizeDp.value < 80 -> "normal"; else -> "az hassas"
                    }, s.palmSizeDp.value.toFloat(), 30f..140f) { s.palmSizeDp.value = it.roundToInt() }
                    Text("Elin yanlışlıkla kaydırıyorsa sola (daha hassas) çekin.", fontSize = 12.sp, color = UI.TextFaint)
                    SliderRow("Kalem kalkınca parmakları yok say", "${s.palmHoldMs.value} ms", s.palmHoldMs.value.toFloat(), 0f..1200f) {
                        s.palmHoldMs.value = (it / 50).roundToInt() * 50
                    }
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text("Kalem yan tuşu (basılı tutarken)", fontSize = 16.sp, color = UI.Text)
                        Spacer(Modifier.height(8.dp))
                        PillRow { ButtonAction.entries.forEach { a -> Pill(a.title, s.buttonActionValue == a) { s.buttonAction.value = a.name } } }
                    }
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text("Kalemde çift dokunuş / tuşa basma", fontSize = 16.sp, color = UI.Text)
                        Text("Kaleminiz çift dokunuşu bir tuş olarak gönderiyorsa burada ne yapacağını seçin.", fontSize = 13.sp, color = UI.TextSoft)
                        Spacer(Modifier.height(8.dp))
                        PillRow { PenAction.entries.take(3).forEach { a -> Pill(a.title, s.penKeyActionValue == a) { s.penKeyAction.value = a.name } } }
                        Spacer(Modifier.height(6.dp))
                        PillRow { PenAction.entries.drop(3).forEach { a -> Pill(a.title, s.penKeyActionValue == a) { s.penKeyAction.value = a.name } } }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill("Tuşu öğret", false) { learning = true }
                            Spacer(Modifier.width(10.dp))
                            val codes = s.penKeyCodeSet()
                            Text(if (codes.isEmpty()) "Öğretilmiş tuş yok (standart kalem tuşları tanınır)" else "Öğretilen tuş kodları: ${codes.joinToString()}",
                                fontSize = 12.sp, color = UI.TextSoft)
                        }
                        if (s.penKeyCodeSet().isNotEmpty()) TextButton(onClick = { s.penKeyCodes.value = "" }) { Text("Öğretilen tuşları temizle") }
                    }
                }
                Group("Yazı") {
                    Toggle("Basınç duyarlılığı", "Kalemi bastırdıkça çizgi kalınlaşır", s.pressure.value) { s.pressure.value = it }
                    Toggle("Silgiden kaleme otomatik dön", "Silgiyle sildikten sonra önceki araca geri geçer", s.eraserReturn.value) { s.eraserReturn.value = it }
                    Toggle("Gecikme azaltma (tahmin)", "Çizgi kalemin ucunu daha yakından takip eder", s.prediction.value) { s.prediction.value = it }
                    SliderRow("Çizgi yumuşatma", listOf("Kapalı", "Az", "Orta", "Çok", "En çok")[s.smoothing.value.coerceIn(0, 4)],
                        s.smoothing.value.toFloat(), 0f..4f, 3) { s.smoothing.value = it.roundToInt() }
                }
                Group("Sayfalar") {
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text("Yeni eklenen sayfa", fontSize = 16.sp, color = UI.Text)
                        Spacer(Modifier.height(8.dp))
                        PillRow {
                            Pill("Düz beyaz", !s.newPageSame.value) { s.newPageSame.value = false }
                            Pill("Önceki sayfa gibi", s.newPageSame.value) { s.newPageSame.value = true }
                        }
                    }
                }
                Group("Geometri") {
                    Toggle("Çizip bekleyince şekle dönüştür", "Kalemi çizginin sonunda bir an bekletin", s.shapeHold.value) { s.shapeHold.value = it }
                    SliderRow("Bekleme süresi", "${s.holdMs.value} ms", s.holdMs.value.toFloat(), 300f..1200f) { s.holdMs.value = (it / 50).roundToInt() * 50 }
                    Toggle("Izgaraya ve köşelere yapıştır", "Şekil köşeleri desene ve diğer köşelere yapışır", s.gridSnap.value) { s.gridSnap.value = it }
                    Toggle("Açı yapıştırma (15°)", "Çizgiler 0°, 15°, 30°, 45°... açılarına yapışır", s.angleSnap.value) { s.angleSnap.value = it }
                    Toggle("Köşelere harf (A, B, C)", "Çokgen çizince köşelere otomatik harf", s.autoLetters.value) { s.autoLetters.value = it }
                    Toggle("Ölçüleri yaz", "Doğru parçası uzunluğu ve açı dereceleri sayfada kalır", s.autoMeasures.value) { s.autoMeasures.value = it }
                }
                Group("Ders anlatımı") {
                    SliderRow("Lazer izinin kaybolma süresi", "${"%.1f".format(s.laserMs.value / 1000f)} sn", s.laserMs.value.toFloat(), 500f..5000f) {
                        s.laserMs.value = (it / 100).roundToInt() * 100
                    }
                    SliderRow("Geçici kalemin kaybolma süresi", "${"%.1f".format(s.tempMs.value / 1000f)} sn", s.tempMs.value.toFloat(), 1000f..10000f) {
                        s.tempMs.value = (it / 100).roundToInt() * 100
                    }
                    SliderRow("Spot ışığı boyutu", "${s.spotRadiusDp.value}", s.spotRadiusDp.value.toFloat(), 60f..300f) { s.spotRadiusDp.value = it.roundToInt() }
                    Toggle("İkinci ekran (projeksiyon / HDMI)", "Harici ekranda sadece temiz sayfa görünür, araçlar tablette kalır", s.secondScreen.value) { s.secondScreen.value = it }
                    Toggle("Ders kaydında mikrofon", "Video kaydına sesinizi de ekler", s.recordAudio.value) { s.recordAudio.value = it }
                }
                Group("Yedekleme") {
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text("Tüm defterleriniz tek bir .zip dosyasına yedeklenir. Dosyayı Google Drive'a veya bilgisayara kaydedebilirsiniz.",
                            fontSize = 13.sp, color = UI.TextSoft)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill("Şimdi yedekle", false) {
                                val name = "Note-yedek-" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".zip"
                                backupLauncher.launch(name)
                            }
                            Pill("Yedekten geri yükle", false) { confirmRestore = true }
                        }
                    }
                    Toggle("Otomatik yedek (günde bir)", "Uygulama açılınca, seçtiğiniz klasöre yedek alınır (son 5 yedek saklanır)", s.autoBackup.value) {
                        s.autoBackup.value = it
                        if (it && s.backupTree.value.isEmpty()) folderLauncher.launch(null)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                        Pill(if (s.backupTree.value.isEmpty()) "Yedek klasörü seç" else "Klasörü değiştir", false) { folderLauncher.launch(null) }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (s.lastBackup.value == 0L) "Henüz otomatik yedek alınmadı"
                            else "Son yedek: " + SimpleDateFormat("d MMM yyyy HH:mm", Locale.forLanguageTag("tr")).format(Date(s.lastBackup.value)),
                            fontSize = 12.sp, color = UI.TextSoft,
                        )
                    }
                }
                Text(
                    "Not: Kablosuz yansıtma tüm ekranı gösterir; temiz görüntü için sunum modunu (tam ekran) kullanın.",
                    fontSize = 13.sp, color = UI.TextSoft, modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }
    }

    if (learning) {
        DisposableEffect(Unit) {
            PenKeys.learner = { code -> s.addPenKeyCode(code); learning = false }
            onDispose { PenKeys.learner = null }
        }
        AlertDialog(
            onDismissRequest = { learning = false },
            containerColor = UI.Surface,
            title = { Text("Kalem tuşunu öğret") },
            text = { Text("Şimdi kalemde çift dokunun ya da tuşa basın. Algılanırsa bu pencere kendiliğinden kapanır.\n\nHiçbir şey olmazsa kaleminiz bu hareketi uygulamalara iletmiyor demektir (bazı kalemlerde yalnızca sistem kullanabilir).", color = UI.TextSoft) },
            confirmButton = { TextButton(onClick = { learning = false }) { Text("Vazgeç") } },
        )
    }
    if (confirmRestore) {
        ConfirmDialog(
            "Yedekten geri yükle",
            "Yedekteki defterler eklenecek; aynı defterler varsa yedektekiyle değiştirilecek. Devam edilsin mi?",
            "Dosya seç", { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }, { confirmRestore = false },
        )
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
