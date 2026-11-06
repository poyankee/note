package com.poyankee.note.ui

import com.poyankee.note.core.L

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val PRIVACY_TEXT = """Note, internete bağlanmayan bir not uygulamasıdır.

• Hiçbir kişisel veri toplanmaz, hiçbir yere gönderilmez. Uygulamanın internet izni yoktur.
• Defterleriniz, PDF'leriniz, görselleriniz ve ayarlarınız yalnızca bu cihazda, uygulamanın kendi klasöründe saklanır.
• Reklam, analiz veya izleme yoktur. Hesap açmanız gerekmez.
• Yedek yalnızca siz "Şimdi yedekle" dediğinizde ya da seçtiğiniz klasöre otomatik yedeği açtığınızda alınır; dosyanın nereye kaydedileceğini siz seçersiniz.
• Android'in otomatik bulut yedeği bu uygulama için kapalıdır.
• Kamera yalnızca "Fotoğraf çek" dediğinizde, mikrofon yalnızca ders kaydında (isterseniz) kullanılır. Görüntü ve ses cihazda kalır.
• Hata kayıtları cihazda tutulur; yalnızca siz "Paylaş" derseniz, seçtiğiniz uygulamayla paylaşılır.
• Uygulamayı kaldırdığınızda bütün veriler cihazdan silinir.

Uygulama çocuklar dahil herkesin güvenle kullanabileceği şekilde tasarlanmıştır."""

const val LICENSES_TEXT = """Bu uygulama aşağıdaki açık kaynak yazılımları kullanır:

• Kotlin ve Kotlin Coroutines — Apache License 2.0 (JetBrains)
• AndroidX, Jetpack Compose, Material 3 — Apache License 2.0 (Google)
• Material Icons — Apache License 2.0 (Google)
• PdfBox-Android — Apache License 2.0 (Tom Roush, Apache PDFBox'a dayanır)

Apache License 2.0 metni: https://www.apache.org/licenses/LICENSE-2.0"""

@Composable
fun InfoDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = UI.Surface,
        title = { Text(L.t(title), fontWeight = FontWeight.SemiBold) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                Text(L.t(text), fontSize = 14.sp, color = UI.Text, modifier = Modifier.padding(bottom = 4.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(L.t("Kapat")) } },
    )
}
