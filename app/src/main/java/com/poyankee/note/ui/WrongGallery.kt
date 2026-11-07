package com.poyankee.note.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poyankee.note.core.ImageItem
import com.poyankee.note.core.L
import com.poyankee.note.data.Settings
import com.poyankee.note.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private class WrongQ(val page: Int, val file: File?, val tag: String, val done: Boolean, val source: String)

/**
 * Yanlış defterinin galeri görünümü: tüm sorular küçük kartlar halinde.
 * Karta dokununca defter o sayfada açılır. Görseller küçültülerek (inSampleSize) okunur.
 */
@Composable
fun WrongGallery(store: Store, settings: Settings, id: String, onOpenPage: (Int) -> Unit, onBack: () -> Unit) {
    var list by remember { mutableStateOf<List<WrongQ>?>(null) }
    var filter by remember { mutableStateOf(0) }
    LaunchedEffect(id) {
        list = withContext(Dispatchers.IO) {
            try {
                val nb = store.load(id)
                val dir = store.dir(id)
                nb.pages.mapIndexedNotNull { i, p ->
                    val img = p.items.filterIsInstance<ImageItem>().firstOrNull { it.file.startsWith("wq_") }
                    if (img == null && p.items.isEmpty()) return@mapIndexedNotNull null
                    val src = settings.wrongSource(p.id)
                    WrongQ(i, img?.let { File(dir, it.file) }, settings.wrongTag(p.id), settings.wrongDone(p.id), src?.let { store.nameOf(it.first) } ?: "")
                }
            } catch (_: Exception) { emptyList() }
        }
    }
    Column(Modifier.fillMaxSize().background(UI.Bg)) {
        Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 12.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, L.t("Geri"), tint = UI.Text) }
            Column(Modifier.weight(1f)) {
                Text(L.t("Yanlış defteri"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = UI.Text)
                list?.let { l -> Text("${l.size} " + L.t("soru") + " · ${l.count { it.done }} " + L.t("çözüldü"), fontSize = 13.sp, color = UI.TextSoft) }
            }
            TextButton(onClick = { onOpenPage(-1) }) {
                Icon(Icons.Rounded.MenuBook, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(L.t("Defter olarak aç"))
            }
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(L.t("Tümü"), L.t("Çözülmedi"), L.t("Çözüldü")).forEachIndexed { i, t -> Pill(t, filter == i) { filter = i } }
        }
        val l = list
        when {
            l == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            l.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(L.t("Henüz soru yok. Bir defterde soruyu seçim aracıyla seçip \"Yanlış defterine ekle\"ye dokunun."), color = UI.TextSoft, fontSize = 15.sp)
            }
            else -> {
                val shown = l.filter { when (filter) { 1 -> !it.done; 2 -> it.done; else -> true } }
                LazyVerticalGrid(
                    GridCells.Adaptive(220.dp), contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(shown, key = { it.page }) { q -> QuestionCard(q) { onOpenPage(q.page) } }
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(q: WrongQ, onClick: () -> Unit) {
    var bmp by remember(q.file) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(q.file) {
        val f = q.file ?: return@LaunchedEffect
        bmp = withContext(Dispatchers.IO) {
            try {
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(f.path, o)
                var s = 1
                while (o.outWidth / (s * 2) >= 480) s *= 2
                BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = s })?.asImageBitmap()
            } catch (_: Throwable) { null }
        }
    }
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).background(UI.Surface)
            .border(1.dp, if (q.done) Color(0xFF12B76A) else UI.Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(10.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.4f).clip(RoundedCornerShape(10.dp)).background(Color.White), contentAlignment = Alignment.Center) {
            bmp?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
            if (q.done) Icon(Icons.Rounded.CheckCircle, L.t("Çözüldü"), tint = Color(0xFF12B76A), modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(26.dp))
        }
        Spacer(Modifier.size(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L.t("Soru") + " ${q.page + 1}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = UI.Text)
            if (q.tag.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Text(q.tag, fontSize = 12.sp, color = UI.Primary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(UI.PrimarySoft).padding(horizontal = 8.dp, vertical = 2.dp))
            }
        }
        if (q.source.isNotEmpty()) Text(q.source, fontSize = 12.sp, color = UI.TextSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
