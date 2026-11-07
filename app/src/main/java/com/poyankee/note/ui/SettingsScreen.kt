package com.poyankee.note.ui

import com.poyankee.note.core.L

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedTextField
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
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    s: Settings,
    onBack: () -> Unit,
    onBackupNow: (Uri) -> Unit = {},
    onRestore: (Uri) -> Unit = {},
    onBackupFolder: (Uri) -> Unit = {},
    editorOpen: Boolean = false,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var usage by remember { mutableStateOf<com.poyankee.note.data.StorageInfo.Usage?>(null) }
    var usageTick by remember { mutableStateOf(0) }
    var crashCount by remember { mutableStateOf(com.poyankee.note.data.CrashLog.list(ctx).size) }
    var storageMsg by remember { mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(usageTick) {
        usage = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try { com.poyankee.note.data.StorageInfo.usage(ctx, com.poyankee.note.data.Store(ctx)) } catch (_: Throwable) { null }
        }
    }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var info by remember { mutableStateOf<Pair<String, String>?>(null) }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> if (uri != null) onBackupNow(uri) }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) onRestore(uri) }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> if (uri != null) onBackupFolder(uri) }
    var learning by remember { mutableStateOf(false) }
    var penTest by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(UI.Bg)) {
        Row(Modifier.fillMaxWidth().background(UI.Surface).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, L.t("Geri"), tint = UI.Text) }
            Text(L.t("Ayarlar"), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = UI.Text)
        }
        HorizontalDivider(color = UI.Border)
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                Group(L.t("Görünüm")) {
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text("Dil / Language", fontSize = 16.sp, color = UI.Text)
                        Spacer(Modifier.height(8.dp))
                        val act = ctx as? android.app.Activity
                        PillRow {
                            Pill("Türkçe", s.language.value == "tr") { if (s.language.value != "tr") { s.language.value = "tr"; act?.let { com.poyankee.note.MainActivity.restart(it) } } }
                            Pill("English", s.language.value == "en") { if (s.language.value != "en") { s.language.value = "en"; act?.let { com.poyankee.note.MainActivity.restart(it) } } }
                        }
                    }
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text(L.t("Tema"), fontSize = 16.sp, color = UI.Text)
                        Spacer(Modifier.height(8.dp))
                        PillRow { ThemeMode.entries.forEach { m -> Pill(m.title, s.themeValue == m) { s.theme.value = m.name } } }
                    }
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text(L.t("Uygulama rengi"), fontSize = 16.sp, color = UI.Text)
                        Text(L.t("Önerilen renklerden birini seçin ya da + ile kendi renginizi ekleyin (eklediğinize uzun basınca silinir)"), fontSize = 13.sp, color = UI.TextSoft)
                        Spacer(Modifier.height(8.dp))
                        ColorChoiceRow(
                            presets = ACCENT_PRESETS,
                            saved = s.savedAccentList(),
                            selected = s.customAccent.value,
                            onPick = { c -> s.customAccent.value = c; s.colorTheme.value = Palette.CUSTOM.name },
                            onAdd = { c -> s.addColor(s.savedAccents, c) },
                            onRemove = { c -> s.removeColor(s.savedAccents, c) },
                        )
                    }
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text(L.t("Sayfanın arkası (masa) rengi"), fontSize = 16.sp, color = UI.Text)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill(L.t("Temaya göre"), s.deskColor.value == 0) { s.deskColor.value = 0 }
                            Spacer(Modifier.width(10.dp))
                            ColorChoiceRow(
                                presets = DESK_PRESETS, saved = s.savedDeskList(), selected = s.deskColor.value, size = 32,
                                onPick = { c -> s.deskColor.value = c },
                                onAdd = { c -> s.addColor(s.savedDesks, c) },
                                onRemove = { c -> s.removeColor(s.savedDesks, c) },
                            )
                        }
                    }
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text(L.t("Arayüz boyutu"), fontSize = 16.sp, color = UI.Text)
                        Text("Düğmeler, menüler ve yazılar. \"Çok büyük\" görme güçlüğü olanlar için (erişilebilirlik).", fontSize = 13.sp, color = UI.TextSoft)
                        Spacer(Modifier.height(8.dp))
                        PillRow { listOf(L.t("Küçük"), L.t("Normal"), L.t("Büyük"), L.t("Çok büyük")).forEachIndexed { i, t -> Pill(t, s.uiSize.value == i) { s.uiSize.value = i } } }
                    }
                    Toggle(L.t("Göz dostu mod"), L.t("Koyu tema ve ekrana hafif sıcak renk filtresi; akşam çalışırken gözü yormaz"), s.eyeComfort.value) { s.eyeComfort.value = it }
                    Toggle(L.t("Solak kullanım"), L.t("Araç menüsü sağ tarafa geçer"), s.leftHanded.value) { s.leftHanded.value = it }
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text(L.t("Panelleri varsayılan yerine al"), fontSize = 16.sp, color = UI.Text)
                        Text(L.t("Araç menüsü ve sayfa gezgini taşındıysa"), fontSize = 13.sp, color = UI.TextSoft)
                        Spacer(Modifier.height(6.dp))
                        Pill(L.t("Sıfırla"), false) {
                            s.dockX.value = 0f; s.dockY.value = 0f; s.navX.value = 0f; s.navY.value = 0f; s.presX.value = 0f; s.presY.value = 0f
                            s.dockCollapsed.value = false
                        }
                    }
                }
                Group(L.t("Kişiselleştirme")) {
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text(L.t("Sol üstteki başlık"), fontSize = 16.sp, color = UI.Text)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(s.appTitle.value, { s.appTitle.value = it.take(24) }, label = { Text(L.t("Başlık")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(s.appSubtitle.value, { s.appSubtitle.value = it.take(40) }, label = { Text(L.t("Alt başlık")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Text(L.t("Simge"), fontSize = 14.sp, color = UI.TextSoft)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppIcon.entries.forEach { ic ->
                                val sel = s.appIcon.value == ic.name
                                Box(
                                    Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(if (sel) UI.Primary else UI.SurfaceAlt)
                                        .clickable { s.appIcon.value = ic.name },
                                    contentAlignment = Alignment.Center,
                                ) { Icon(ic.icon, L.t(ic.title), tint = if (sel) androidx.compose.ui.graphics.Color.White else UI.TextSoft) }
                            }
                        }
                    }
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text(L.t("Profil (sol altta)"), fontSize = 16.sp, color = UI.Text)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(s.profileName.value, { s.profileName.value = it.take(32) }, label = { Text(L.t("Ad")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(s.profileSubtitle.value, { s.profileSubtitle.value = it.take(40) }, label = { Text(L.t("Açıklama (ör. Matematik öğretmeni)")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(s.folderTitle.value, { s.folderTitle.value = it.take(24) }, label = { Text(L.t("Klasörler bölümünün adı (ör. Dersler)")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text(L.t("Ana sayfadaki sol menü"), fontSize = 16.sp, color = UI.Text)
                        Text(L.t("Kutucukla göster / gizle, oklarla sırasını değiştirin"), fontSize = 13.sp, color = UI.TextSoft)
                        ReorderList(
                            all = SideItem.entries.map { it.name to L.t(it.title) },
                            chosen = SideItem.parse(s.sideItems.value).map { it.name },
                        ) { s.sideItems.value = it.joinToString(",") }
                        TextButton(onClick = { s.sideItems.value = SideItem.DEFAULT }) { Text(L.t("Varsayılana dön")) }
                    }
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text(L.t("Defterdeki araç listesi"), fontSize = 16.sp, color = UI.Text)
                        Text(L.t("Kutucukla göster / gizle, oklarla sırasını değiştirin"), fontSize = 13.sp, color = UI.TextSoft)
                        ReorderList(
                            all = com.poyankee.note.ui.RailItem.entries.map { it.name to L.t(it.title) },
                            chosen = com.poyankee.note.ui.RailItem.parse(s.railItems.value).map { it.name },
                        ) { s.railItems.value = it.joinToString(",") }
                        TextButton(onClick = { s.railItems.value = com.poyankee.note.ui.RailItem.DEFAULT }) { Text(L.t("Varsayılana dön")) }
                    }
                }
                Group(L.t("Kalem ve el")) {
                    Toggle(L.t("Sadece kalemle çiz"), L.t("El ve avuç dokunuşları asla çizim yapmaz"), s.penOnly.value) { s.penOnly.value = it }
                    Toggle(L.t("Parmakla kaydır / yakınlaştır"), L.t("Kapalıysa parmak dokunuşları tamamen yok sayılır"), s.fingerGestures.value) { s.fingerGestures.value = it }
                    Toggle(L.t("İki parmakla dokun = geri al"), L.t("Üç parmakla dokun = yinele"), s.twoFingerUndo.value) { s.twoFingerUndo.value = it }
                    SliderRow(L.t("Avuç algılama"), when {
                        s.palmSizeDp.value < 50 -> L.t("çok hassas"); s.palmSizeDp.value < 80 -> "normal"; else -> L.t("az hassas")
                    }, s.palmSizeDp.value.toFloat(), 30f..140f) { s.palmSizeDp.value = it.roundToInt() }
                    Text(L.t("Elin yanlışlıkla kaydırıyorsa sola (daha hassas) çekin."), fontSize = 12.sp, color = UI.TextFaint)
                    SliderRow(L.t("Kalem kalkınca parmakları yok say"), "${s.palmHoldMs.value} ms", s.palmHoldMs.value.toFloat(), 0f..1200f) {
                        s.palmHoldMs.value = (it / 50).roundToInt() * 50
                    }
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text(L.t("Kalem yan tuşu (basılı tutarken)"), fontSize = 16.sp, color = UI.Text)
                        Spacer(Modifier.height(8.dp))
                        PillRow { ButtonAction.entries.forEach { a -> Pill(a.title, s.buttonActionValue == a) { s.buttonAction.value = a.name } } }
                    }
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text(L.t("Kalemde çift dokunuş / tuşa basma"), fontSize = 16.sp, color = UI.Text)
                        Text(L.t("Kaleminiz çift dokunuşu bir tuş olarak gönderiyorsa burada ne yapacağını seçin."), fontSize = 13.sp, color = UI.TextSoft)
                        Spacer(Modifier.height(8.dp))
                        PillRow { PenAction.entries.take(3).forEach { a -> Pill(a.title, s.penKeyActionValue == a) { s.penKeyAction.value = a.name } } }
                        Spacer(Modifier.height(6.dp))
                        PillRow { PenAction.entries.drop(3).forEach { a -> Pill(a.title, s.penKeyActionValue == a) { s.penKeyAction.value = a.name } } }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill(L.t("Tuşu öğret"), false) { learning = true }
                            Spacer(Modifier.width(10.dp))
                            val codes = s.penKeyCodeSet()
                            Text(if (codes.isEmpty()) L.t("Öğretilmiş tuş yok (standart kalem tuşları tanınır)") else "Öğretilen tuş kodları: ${codes.joinToString()}",
                                fontSize = 12.sp, color = UI.TextSoft)
                        }
                        if (s.penKeyCodeSet().isNotEmpty()) TextButton(onClick = { s.penKeyCodes.value = "" }) { Text(L.t("Öğretilen tuşları temizle")) }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill(L.t("Kalem testi"), false) { penTest = true }
                            Spacer(Modifier.width(10.dp))
                            Text(L.t("Kalemin ve tuşlarının uygulamaya ne gönderdiğini gösterir"), fontSize = 12.sp, color = UI.TextSoft)
                        }
                    }
                }
                Group(L.t("Yazı")) {
                    Toggle(L.t("Basınç duyarlılığı"), L.t("Kalemi bastırdıkça çizgi kalınlaşır"), s.pressure.value) { s.pressure.value = it }
                    Toggle(L.t("Yazarken otomatik yeni sayfa"), L.t("Son sayfanın alt kısmına yazınca yeni düz sayfa eklenir"), s.autoNewPage.value) { s.autoNewPage.value = it }
                    Toggle(L.t("Yakınlaştırma kilidi"), L.t("İki parmak sayfayı yakınlaştırmaz, sadece kaydırır"), s.zoomLock.value) { s.zoomLock.value = it }
                    Toggle(L.t("Silgiden kaleme otomatik dön"), L.t("Silgiyle sildikten sonra önceki araca geri geçer"), s.eraserReturn.value) { s.eraserReturn.value = it }
                    SliderRow(L.t("Kalem basıncı eğrisi"), listOf(L.t("Çok yumuşak"), L.t("Yumuşak"), L.t("Normal"), L.t("Sert"), L.t("Çok sert"))[s.pressureCurve.value.coerceIn(0, 4)],
                        s.pressureCurve.value.toFloat(), 0f..4f, 3) { s.pressureCurve.value = it.roundToInt() }
                    Toggle(L.t("PDF'te metni vurgula"), L.t("Fosforlu kalem PDF yazısının üzerinde satırlara oturur"), s.textHighlight.value) { s.textHighlight.value = it }
                    Toggle(L.t("Gecikme azaltma (tahmin)"), L.t("Çizgi kalemin ucunu daha yakından takip eder"), s.prediction.value) { s.prediction.value = it }
                    SliderRow(L.t("Çizgi yumuşatma"), listOf(L.t("Kapalı"), L.t("Az"), L.t("Orta"), L.t("Çok"), L.t("En çok"))[s.smoothing.value.coerceIn(0, 4)],
                        s.smoothing.value.toFloat(), 0f..4f, 3) { s.smoothing.value = it.roundToInt() }
                }
                Group(L.t("Sayfalar")) {
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text(L.t("Yeni eklenen sayfa"), fontSize = 16.sp, color = UI.Text)
                        Spacer(Modifier.height(8.dp))
                        PillRow {
                            Pill(L.t("Düz beyaz"), !s.newPageSame.value) { s.newPageSame.value = false }
                            Pill(L.t("Önceki sayfa gibi"), s.newPageSame.value) { s.newPageSame.value = true }
                        }
                    }
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text(L.t("Sayfa altında"), fontSize = 16.sp, color = UI.Text)
                        Text(L.t("Sağ alt köşeye sayfa numarası (ve sayfanın açıldığı tarih); dışa aktarılan PDF'te de görünür"), fontSize = 13.sp, color = UI.TextSoft)
                        Spacer(Modifier.height(8.dp))
                        PillRow { listOf(L.t("Hiçbir şey"), L.t("Sayfa no"), L.t("Sayfa no + tarih")).forEachIndexed { i, t -> Pill(t, s.pageFooter.value == i) { s.pageFooter.value = i } } }
                    }
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text(L.t("Çözüm katmanı"), fontSize = 16.sp, color = UI.Text)
                        Text("Bu renkle yazdıklarınız \"çözüm\" sayılır: defterde ⋮ › Çözümleri gizle ile saklanır, dışa aktarırken soru kağıdı olarak çıkarılabilir.", fontSize = 13.sp, color = UI.TextSoft)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Pill(L.t("Kapalı"), s.solutionColor.value == 0) { s.solutionColor.value = 0 }
                            s.paletteColors().forEach { c -> ColorDot(c, (s.solutionColor.value or 0xFF000000.toInt()) == (c or 0xFF000000.toInt()) && s.solutionColor.value != 0, 30) { s.solutionColor.value = c } }
                        }
                    }
                }
                Group(L.t("Geometri")) {
                    Toggle(L.t("Çizip bekleyince şekle dönüştür"), L.t("Kalemi çizginin sonunda bir an bekletin"), s.shapeHold.value) { s.shapeHold.value = it }
                    SliderRow(L.t("Bekleme süresi"), "${s.holdMs.value} ms", s.holdMs.value.toFloat(), 300f..1200f) { s.holdMs.value = (it / 50).roundToInt() * 50 }
                    Toggle(L.t("Izgaraya ve köşelere yapıştır"), L.t("Şekil köşeleri desene ve diğer köşelere yapışır"), s.gridSnap.value) { s.gridSnap.value = it }
                    Toggle(L.t("Açı yapıştırma (15°)"), L.t("Çizgiler 0°, 15°, 30°, 45°... açılarına yapışır"), s.angleSnap.value) { s.angleSnap.value = it }
                    Toggle(L.t("Köşelere harf (A, B, C)"), L.t("Çokgen çizince köşelere otomatik harf"), s.autoLetters.value) { s.autoLetters.value = it }
                    Toggle(L.t("Ölçüleri yaz"), L.t("Doğru parçası uzunluğu ve açı dereceleri sayfada kalır"), s.autoMeasures.value) { s.autoMeasures.value = it }
                }
                Group(L.t("Ders anlatımı")) {
                    SliderRow(L.t("Lazer izinin kaybolma süresi"), "${"%.1f".format(s.laserMs.value / 1000f)} sn", s.laserMs.value.toFloat(), 500f..5000f) {
                        s.laserMs.value = (it / 100).roundToInt() * 100
                    }
                    SliderRow(L.t("Geçici kalemin kaybolma süresi"), "${"%.1f".format(s.tempMs.value / 1000f)} sn", s.tempMs.value.toFloat(), 1000f..10000f) {
                        s.tempMs.value = (it / 100).roundToInt() * 100
                    }
                    Toggle(L.t("İkinci ekran (projeksiyon / HDMI)"), L.t("Harici ekranda sadece temiz sayfa görünür, araçlar tablette kalır"), s.secondScreen.value) { s.secondScreen.value = it }
                    Toggle(L.t("Ders kaydında mikrofon"), L.t("Video kaydına sesinizi de ekler"), s.recordAudio.value) { s.recordAudio.value = it }
                }
                Group(L.t("Yedekleme")) {
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text(L.t("Tüm defterleriniz tek bir .zip dosyasına yedeklenir. Dosyayı Google Drive'a veya bilgisayara kaydedebilirsiniz."),
                            fontSize = 13.sp, color = UI.TextSoft)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill(L.t("Şimdi yedekle"), false) {
                                val name = L.t("Note-yedek-") + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".zip"
                                backupLauncher.launch(name)
                            }
                            Pill(L.t("Yedekten geri yükle"), false) { confirmRestore = true }
                        }
                    }
                    Toggle(L.t("Otomatik yedek (günde bir)"), L.t("Uygulama açılınca, seçtiğiniz klasöre yedek alınır (son 5 yedek saklanır)"), s.autoBackup.value) {
                        s.autoBackup.value = it
                        if (it && s.backupTree.value.isEmpty()) folderLauncher.launch(null)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                        Pill(if (s.backupTree.value.isEmpty()) L.t("Yedek klasörü seç") else L.t("Klasörü değiştir"), false) { folderLauncher.launch(null) }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (s.lastBackup.value == 0L) L.t("Henüz otomatik yedek alınmadı")
                            else L.t("Son yedek: ") + SimpleDateFormat("d MMM yyyy HH:mm", Locale.forLanguageTag("tr")).format(Date(s.lastBackup.value)),
                            fontSize = 12.sp, color = UI.TextSoft,
                        )
                    }
                }
                Group(L.t("Depolama")) {
                    val u = usage
                    Column(Modifier.padding(vertical = 10.dp)) {
                        if (u == null) Text(L.t("Hesaplanıyor…"), fontSize = 13.sp, color = UI.TextSoft)
                        else {
                            Text("Toplam: ${com.poyankee.note.data.StorageInfo.human(u.total)}", fontSize = 16.sp, color = UI.Text)
                            Text("Çöp kutusu: ${com.poyankee.note.data.StorageInfo.human(u.trash)}   ·   Geçici dosyalar: ${com.poyankee.note.data.StorageInfo.human(u.cache)}", fontSize = 13.sp, color = UI.TextSoft)
                            Spacer(Modifier.height(6.dp))
                            u.notebooks.take(5).forEach { (n, b) ->
                                Row { Text(n, fontSize = 13.sp, color = UI.Text, modifier = Modifier.weight(1f), maxLines = 1); Text(com.poyankee.note.data.StorageInfo.human(b), fontSize = 13.sp, color = UI.TextSoft) }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill(L.t("Geçici dosyaları sil"), false) {
                                scope.launch {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.poyankee.note.data.StorageInfo.clearCache(ctx) }
                                    storageMsg = L.t("Geçici dosyalar silindi"); usageTick++
                                }
                            }
                            if (!editorOpen) Pill(L.t("Kullanılmayan görselleri sil"), false) {
                                scope.launch {
                                    val freed = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.poyankee.note.data.StorageInfo.cleanUnusedImages(com.poyankee.note.data.Store(ctx)) }
                                    storageMsg = "${com.poyankee.note.data.StorageInfo.human(freed)} boşaltıldı"; usageTick++
                                }
                            }
                        }
                        storageMsg?.let { Text(it, fontSize = 13.sp, color = UI.Primary, modifier = Modifier.padding(top = 6.dp)) }
                        Text("Çöp kutusu ana sayfadaki \"Çöp kutusu\" bölümünden boşaltılır.", fontSize = 12.sp, color = UI.TextFaint, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                Group(L.t("Hata kayıtları")) {
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text(
                            if (crashCount == 0) L.t("Kayıtlı hata yok.") else "$crashCount hata kaydı var. Bir sorun yaşadıysanız bu kaydı paylaşarak bildirebilirsiniz; hiçbir şey kendiliğinden gönderilmez.",
                            fontSize = 13.sp, color = UI.TextSoft,
                        )
                        if (crashCount > 0) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            Pill(L.t("Paylaş"), false) {
                                com.poyankee.note.data.CrashLog.bundle(ctx)?.let { f ->
                                    val uri = androidx.core.content.FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
                                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
                                        .putExtra(android.content.Intent.EXTRA_STREAM, uri).addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    ctx.startActivity(android.content.Intent.createChooser(send, L.t("Hata kaydını paylaş")))
                                }
                            }
                            Pill(L.t("Sil"), false) { com.poyankee.note.data.CrashLog.clear(ctx); crashCount = 0 }
                        }
                    }
                }
                Group(L.t("Hakkında")) {
                    Column(Modifier.padding(vertical = 10.dp)) {
                        val ver = try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName } catch (_: Exception) { "" }
                        Text("Note $ver", fontSize = 16.sp, color = UI.Text)
                        Text(L.t("Öğretmenler ve öğrenciler için ücretsiz, reklamsız, internete bağlanmayan not uygulaması."), fontSize = 13.sp, color = UI.TextSoft)
                        Text(L.t("Geliştiren") + ": Poyraz DOĞAN", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = UI.Text, modifier = Modifier.padding(top = 6.dp))
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill(L.t("Gizlilik politikası"), false) { info = L.t("Gizlilik politikası") to PRIVACY_TEXT }
                            Pill(L.t("Sorun bildir"), false) {
                                try {
                                    ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/poyankee/note/issues")))
                                } catch (_: Exception) {}
                            }
                            Pill(L.t("Açık kaynak lisansları"), false) { info = L.t("Açık kaynak lisansları") to LICENSES_TEXT }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row {
                            Pill(L.t("Tanıtımı göster"), false) { s.introShown.value = false; onBack() }
                        }
                    }
                }
                Text(
                    L.t("Not: Kablosuz yansıtma tüm ekranı gösterir; temiz görüntü için odak modunu (menüler gizli) kullanın."),
                    fontSize = 13.sp, color = UI.TextSoft, modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }
    }

    if (penTest) PenTestDialog(s) { penTest = false }
    info?.let { (t, body) -> InfoDialog(t, body) { info = null } }
    if (learning) {
        DisposableEffect(Unit) {
            PenKeys.learner = { code -> s.addPenKeyCode(code); learning = false }
            onDispose { PenKeys.learner = null }
        }
        AlertDialog(
            onDismissRequest = { learning = false },
            containerColor = UI.Surface,
            title = { Text(L.t("Kalem tuşunu öğret")) },
            text = {
                Column {
                    Text(L.t("Şimdi kalemde çift dokunun ya da tuşa basın. Algılanırsa bu pencere kendiliğinden kapanır.\n\nHiçbir şey olmazsa kaleminiz bu hareketi uygulamalara iletmiyor demektir (bazı kalemlerde yalnızca sistem kullanabilir). Ne geldiğini görmek için Kalem testi'ni kullanın."), color = UI.TextSoft)
                    KeyCatcher()
                }
            },
            confirmButton = { TextButton(onClick = { learning = false }) { Text(L.t("Vazgeç")) } },
        )
    }
    if (confirmRestore) {
        ConfirmDialog(
            L.t("Yedekten geri yükle"),
            L.t("Yedekteki defterler eklenecek; aynı defterler varsa yedektekiyle değiştirilecek. Devam edilsin mi?"),
            L.t("Dosya seç"), { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }, { confirmRestore = false },
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

/** Seçilebilir ve sırası değiştirilebilir liste: kutucuk = göster/gizle, ↑ ↓ = sıra. */
@Composable
fun ReorderList(all: List<Pair<String, String>>, chosen: List<String>, onChange: (List<String>) -> Unit) {
    val order = chosen + all.map { it.first }.filter { it !in chosen }
    Column(Modifier.padding(top = 6.dp)) {
        order.forEachIndexed { i, key ->
            val title = all.firstOrNull { it.first == key }?.second ?: key
            val on = key in chosen
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (on) UI.PrimarySoft.copy(alpha = 0.5f) else androidx.compose.ui.graphics.Color.Transparent).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Checkbox(on, { v -> onChange(if (v) chosen + key else chosen - key) })
                Text(title, fontSize = 15.sp, color = if (on) UI.Text else UI.TextFaint, modifier = Modifier.weight(1f))
                if (on) {
                    val ci = chosen.indexOf(key)
                    TextButton(onClick = { if (ci > 0) onChange(chosen.toMutableList().apply { removeAt(ci); add(ci - 1, key) }) }, enabled = ci > 0) { Text("↑") }
                    TextButton(onClick = { if (ci < chosen.size - 1) onChange(chosen.toMutableList().apply { removeAt(ci); add(ci + 1, key) }) }, enabled = ci < chosen.size - 1) { Text("↓") }
                }
            }
        }
    }
}
