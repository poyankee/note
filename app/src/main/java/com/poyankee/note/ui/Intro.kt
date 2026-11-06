package com.poyankee.note.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.poyankee.note.editor.Tool

private class Slide(val tool: Tool, val title: String, val text: String)

/** İlk açılışta kısa tanıtım (Ayarlar › Hakkında › Tanıtımı göster ile yeniden açılır). */
@Composable
fun IntroDialog(onDone: () -> Unit) {
    val slides = listOf(
        Slide(Tool.PEN, "Kalemle yazın, eliniz yazmaz", "Avucunuzu ekrana koyabilirsiniz; yalnızca kalem yazar. İki parmakla dokunmak geri alır, iki parmakla kaydırıp yakınlaştırırsınız."),
        Slide(Tool.HIGHLIGHTER, "Seçili araca tekrar dokunun", "Kalem, fosforlu, silgi veya geometri aracına ikinci kez dokununca renkler, kalınlık ve diğer seçenekler açılır."),
        Slide(Tool.SHAPE, "Geometri ve hazır kütüphane", "Çizip bekleyince şekil düzelir. Üstteki Kütüphane'de birim çember, üçgenler, açınımlar, formül kartları ve daha fazlası tek dokunuşla eklenir."),
        Slide(Tool.LASSO, "PDF, yanlış defteri, ders anlatımı", "PDF açıp üzerine yazın, sayfa numarasına atlayın. Soruyu dikdörtgenle seçip yanlış defterine ekleyin. Odak modu ve projeksiyonla temiz bir sayfa gösterin."),
        Slide(Tool.TEXT, "Her şey cihazınızda kalır", "Note internete bağlanmaz, reklam yoktur. Yedeği Ayarlar › Yedekleme'den siz alırsınız."),
    )
    var i by remember { mutableIntStateOf(0) }
    Dialog(onDismissRequest = onDone, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(24.dp), color = UI.Surface, modifier = Modifier.padding(24.dp).widthIn(max = 520.dp)) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val s = slides[i]
                Box(Modifier.size(96.dp).clip(CircleShape).background(UI.PrimarySoft), contentAlignment = Alignment.Center) {
                    ToolGlyph(s.tool, UI.Primary, size = 48.dp)
                }
                Spacer(Modifier.height(20.dp))
                Text(s.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = UI.Text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(s.text, fontSize = 16.sp, color = UI.TextSoft, textAlign = TextAlign.Center, modifier = Modifier.height(120.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                    slides.indices.forEach { k -> Box(Modifier.size(if (k == i) 10.dp else 8.dp).clip(CircleShape).background(if (k == i) UI.Primary else UI.Border)) }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDone) { Text("Geç", color = UI.TextSoft) }
                    Spacer(Modifier.weight(1f))
                    if (i > 0) TextButton(onClick = { i-- }) { Text("Geri") }
                    Spacer(Modifier.width(6.dp))
                    Button(onClick = { if (i < slides.lastIndex) i++ else onDone() }, shape = RoundedCornerShape(12.dp)) {
                        Text(if (i < slides.lastIndex) "İleri" else "Başla")
                    }
                }
            }
        }
    }
}
