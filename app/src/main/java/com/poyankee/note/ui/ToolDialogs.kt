package com.poyankee.note.ui

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poyankee.note.core.PageRanges
import com.poyankee.note.editor.EditorController
import com.poyankee.note.editor.Exporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** Defterde arama: metin kutuları, formüller ve PDF yazısı. Sonuca dokununca o sayfaya gidilir. */
@Composable
fun SearchDialog(ctl: EditorController, onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Pair<Int, String>>?>(null) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(q) {
        if (q.trim().length < 2) { results = null; return@LaunchedEffect }
        delay(350)
        busy = true
        results = withContext(Dispatchers.Default) { try { ctl.searchBlocking(q) } catch (_: Throwable) { emptyList() } }
        busy = false
    }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = UI.Surface,
        title = { Text("Defterde ara") },
        text = {
            Column {
                OutlinedTextField(q, { q = it }, label = { Text("Aranacak kelime") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                if (busy) Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp))
                    Text("Aranıyor…", color = UI.TextSoft, fontSize = 13.sp)
                }
                val r = results
                if (r != null && !busy) {
                    Text(if (r.isEmpty()) "Bulunamadı" else "${r.size} sonuç", color = UI.TextSoft, fontSize = 13.sp)
                    LazyColumn(Modifier.heightIn(max = 380.dp)) {
                        items(r) { (page, snip) ->
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { ctl.goToPage(page); onDismiss() }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                            ) {
                                Text("Sayfa ${page + 1}", fontWeight = FontWeight.SemiBold, color = UI.Primary, fontSize = 13.sp)
                                Text(snip, color = UI.Text, fontSize = 13.sp, maxLines = 2)
                            }
                        }
                    }
                }
                Text("Metin kutularında, formüllerde ve PDF'in yazılarında arar. El yazısı aranamaz.", color = UI.TextFaint, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } },
    )
}

/** Değer tablosu: x ve y değerleri girilir, noktalar koordinat düzlemine işlenir. */
@Composable
fun ValueTableDialog(ctl: EditorController, onDismiss: () -> Unit) {
    val xs = remember { mutableStateListOf("-2", "-1", "0", "1", "2") }
    val ys = remember { mutableStateListOf("", "", "", "", "") }
    var connect by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var fx by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = UI.Surface,
        title = { Text("Değer tablosundan grafik") },
        text = {
            Column {
                Text("x ve y değerlerini yazın (ondalık için nokta). İsterseniz y'yi bir fonksiyondan doldurun.", fontSize = 13.sp, color = UI.TextSoft)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(fx, { fx = it }, label = { Text("y = (ör. 2x+1)") }, singleLine = true, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        error = try {
                            val f = com.poyankee.note.core.MathExpr.parse(fx)
                            for (i in xs.indices) {
                                val x = xs[i].replace(',', '.').toDoubleOrNull() ?: continue
                                val y = f.eval(x)
                                ys[i] = if (y.isNaN() || y.isInfinite()) "" else (Math.round(y * 100) / 100.0).let { v -> if (v == Math.floor(v)) v.toLong().toString() else v.toString() }
                            }
                            null
                        } catch (e: Exception) { e.message ?: "Fonksiyon anlaşılamadı" }
                    }) { Text("Doldur") }
                }
                Spacer(Modifier.height(8.dp))
                for (row in 0..1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (row == 0) "x" else "y", fontWeight = FontWeight.Bold, color = UI.Primary, modifier = Modifier.width(18.dp))
                        val list = if (row == 0) xs else ys
                        for (i in list.indices) {
                            OutlinedTextField(
                                list[i], { list[i] = it.take(7) }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                modifier = Modifier.width(64.dp),
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { if (xs.size < 9) { xs += ""; ys += "" } }) { Text("+ Sütun") }
                    TextButton(onClick = { if (xs.size > 2) { xs.removeAt(xs.lastIndex); ys.removeAt(ys.lastIndex) } }) { Text("− Sütun") }
                    Spacer(Modifier.weight(1f))
                    Text("Noktaları birleştir", fontSize = 13.sp, color = UI.Text)
                    Switch(connect, { connect = it })
                }
                error?.let { Text(it, color = UI.Danger, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val pts = xs.indices.mapNotNull { i ->
                    val x = xs[i].replace(',', '.').toFloatOrNull(); val y = ys[i].replace(',', '.').toFloatOrNull()
                    if (x != null && y != null) x to y else null
                }
                val err = ctl.plotPoints(pts.sortedBy { it.first }, connect, ctl.penColor or 0xFF000000.toInt())
                if (err == null) onDismiss() else error = err
            }, shape = RoundedCornerShape(10.dp)) { Text("Çiz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç", color = UI.TextSoft) } },
    )
}

/** Sayfaları ayrı deftere çıkarma: "3-7, 12" gibi aralık. Asıl defter değişmez. */
@Composable
fun ExtractDialog(ctl: EditorController, initialRange: String? = null, onDismiss: () -> Unit, onDone: (List<Int>, String) -> Unit) {
    var range by remember { mutableStateOf(initialRange ?: "${ctl.currentPage + 1}") }
    var name by remember { mutableStateOf(ctl.nb.name + " (bölüm)") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = UI.Surface,
        title = { Text("Sayfaları ayrı deftere çıkar") },
        text = {
            Column {
                Text("Defterde ${ctl.pageCount} sayfa var. Aralığı yazın: ör. 3-7, 12", fontSize = 13.sp, color = UI.TextSoft)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(range, { range = it; error = null }, label = { Text("Sayfalar") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(name, { name = it }, label = { Text("Yeni defterin adı") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Yazılarınız da birlikte kopyalanır. PDF sayfaları yeni defterde ayrı bir PDF olur.", fontSize = 12.sp, color = UI.TextFaint, modifier = Modifier.padding(top = 8.dp))
                error?.let { Text(it, color = UI.Danger, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val idx = PageRanges.parse(range, ctl.pageCount)
                if (idx == null || idx.isEmpty()) error = "Aralık geçersiz (1-${ctl.pageCount})" else onDone(idx, name.ifBlank { "Yeni defter" })
            }, shape = RoundedCornerShape(10.dp)) { Text("Çıkar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç", color = UI.TextSoft) } },
    )
}

/** Defteri Android yazdırma ekranına gönderir (yazıcı, sayfa aralığı ve kopya sayısı orada seçilir). */
fun printNotebook(context: Context, ctl: EditorController) {
    val pm = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: run { ctl.message = "Yazdırma bu cihazda yok"; return }
    val out = File(context.cacheDir, "yazdir.pdf")
    val pages = ctl.nb.pages.toList()
    val nb = ctl.nb
    val dir = ctl.store.dir(nb.id)
    ctl.message = "Yazdırma için hazırlanıyor…"
    val adapter = object : PrintDocumentAdapter() {
        @Volatile private var ready = false
        override fun onLayout(old: PrintAttributes?, new: PrintAttributes?, cancel: CancellationSignal?, cb: LayoutResultCallback, extras: Bundle?) {
            if (cancel?.isCanceled == true) { cb.onLayoutCancelled(); return }
            Thread {
                try {
                    if (!ready) {
                        Exporter.export(context, nb, dir, pages, out, { ctl.loadImageBlocking(it) }) { _, _ -> }
                        ready = true
                    }
                    val info = PrintDocumentInfo.Builder(nb.name.replace(Regex("[\\\\/:*?\"<>|]"), "_") + ".pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(pages.size).build()
                    android.os.Handler(android.os.Looper.getMainLooper()).post { cb.onLayoutFinished(info, true) }
                } catch (e: Throwable) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post { cb.onLayoutFailed(e.message) }
                }
            }.start()
        }

        override fun onWrite(pr: Array<out PageRange>?, dest: ParcelFileDescriptor, cancel: CancellationSignal?, cb: WriteResultCallback) {
            try {
                FileInputStream(out).use { inp -> FileOutputStream(dest.fileDescriptor).use { inp.copyTo(it) } }
                cb.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) { cb.onWriteFailed(e.message) }
        }
    }
    pm.print(nb.name, adapter, PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
}

/** Yanlış defteri: soruları konuya ve çözülme durumuna göre süzüp ilgili sayfaya gitme. */
@Composable
fun WrongFilterDialog(ctl: EditorController, onDismiss: () -> Unit) {
    val s = ctl.settings
    @Suppress("UNUSED_VARIABLE") val wv = ctl.wrongVersion
    val pages = ctl.nb.pages.toList()
    val tags = pages.map { s.wrongTag(it.id) }.filter { it.isNotEmpty() }.distinct().sorted()
    var tag by remember { mutableStateOf<String?>(null) }
    var onlyOpen by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = UI.Surface,
        title = { Text("Sorular") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                    Pill("Hepsi", tag == null && !onlyOpen) { tag = null; onlyOpen = false }
                    Pill("Çözülmeyenler", onlyOpen) { onlyOpen = !onlyOpen }
                }
                if (tags.isNotEmpty()) Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.take(6).forEach { t -> Pill(t, tag == t) { tag = if (tag == t) null else t } }
                }
                val list = pages.indices.filter { i ->
                    val id = pages[i].id
                    (tag == null || s.wrongTag(id) == tag) && (!onlyOpen || !s.wrongDone(id))
                }
                Text("${list.size} soru", color = UI.TextSoft, fontSize = 13.sp)
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(list) { i ->
                        val id = pages[i].id
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { ctl.goToPage(i); onDismiss() }.padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Soru ${i + 1}", fontWeight = FontWeight.SemiBold, color = UI.Primary, modifier = Modifier.width(80.dp))
                            Text(s.wrongTag(id).ifEmpty { "—" }, color = UI.Text, modifier = Modifier.weight(1f))
                            Text(if (s.wrongDone(id)) "✓ çözüldü" else "tekrar", color = if (s.wrongDone(id)) androidx.compose.ui.graphics.Color(0xFF16A34A) else UI.TextSoft, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } },
    )
}
