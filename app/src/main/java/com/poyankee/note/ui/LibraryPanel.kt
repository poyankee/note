package com.poyankee.note.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poyankee.note.core.Item
import com.poyankee.note.core.Library
import com.poyankee.note.data.UserLibrary
import com.poyankee.note.render.PageRenderer

private val previewRenderer = PageRenderer(null)

/** Hazır çizimin küçük önizlemesi (beyaz kart üzerinde). */
@Composable
fun ItemsPreview(items: List<Item>, modifier: Modifier) {
    val b = remember(items) { Library.bounds(items) }
    Canvas(modifier.clip(RoundedCornerShape(10.dp)).background(androidx.compose.ui.graphics.Color.White)) {
        val bx = b ?: return@Canvas
        val pad = 8.dp.toPx()
        val s = minOf((size.width - 2 * pad) / bx.width.coerceAtLeast(1f), (size.height - 2 * pad) / bx.height.coerceAtLeast(1f))
        drawIntoCanvas { c ->
            val nc = c.nativeCanvas
            nc.save()
            nc.translate(size.width / 2f, size.height / 2f)
            nc.scale(s, s)
            nc.translate(-bx.cx, -(bx.top + bx.bottom) / 2f)
            previewRenderer.drawItems(nc, items)
            nc.restore()
        }
    }
}

/**
 * Hazır kütüphane paneli: kategoriler, arama, kesir modelleri ve kullanıcının kendi çizimleri.
 * [onInsert] seçilen çizimi sayfaya ekler.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryPanel(onInsert: (List<Item>) -> Unit, onClose: () -> Unit) {
    val user = UserLibrary.get(LocalContext.current)
    val mine = "Benim çizimlerim"
    val fractions = "Kesir modelleri"
    val cats = Library.categories.take(5) + fractions + Library.categories.drop(5) + mine
    var cat by remember { mutableStateOf(cats.first()) }
    var query by remember { mutableStateOf("") }
    var num by remember { mutableIntStateOf(3) }
    var den by remember { mutableIntStateOf(4) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Kütüphane", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = UI.Text, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Kapat", tint = UI.TextSoft) }
        }
        Row(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(40.dp).clip(RoundedCornerShape(20.dp)).background(UI.SurfaceAlt).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = UI.TextFaint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("Ara (ör. üçgen, venn)", color = UI.TextFaint, fontSize = 14.sp)
                BasicTextField(query, { query = it }, singleLine = true, textStyle = TextStyle(fontSize = 14.sp, color = UI.Text), modifier = Modifier.fillMaxWidth())
            }
        }
        if (query.isBlank()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                cats.forEach { c -> Pill(c, c == cat) { cat = c } }
            }
        } else Spacer(Modifier.height(10.dp))

        val tr = java.util.Locale.forLanguageTag("tr")
        val q = query.trim().lowercase(tr)
        LazyVerticalGrid(
            GridCells.Fixed(2), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            when {
                q.isNotEmpty() -> {
                    val found = Library.entries.filter { it.title.lowercase(tr).contains(q) || it.category.lowercase(tr).contains(q) }
                    items(found, key = { it.id }) { e -> Tile(e.title, remember(e.id) { e.build() }) { onInsert(e.build()) } }
                    val u = user.entries.filter { it.name.lowercase(tr).contains(q) }
                    items(u, key = { it.id }) { e -> Tile(e.name, e.items) { onInsert(e.items) } }
                }
                cat == fractions -> {
                    item(span = { GridItemSpan(2) }) {
                        Column {
                            Text("Pay ve paydayı seçin, sonra modele dokunun.", fontSize = 12.sp, color = UI.TextSoft)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Stepper("Pay", num, 0, 24) { num = it }
                                Spacer(Modifier.width(16.dp))
                                Stepper("Payda", den, 1, 24) { den = it }
                            }
                        }
                    }
                    item { Tile("Pasta modeli", Library.fractionPie(num, den)) { onInsert(Library.fractionPie(num, den)) } }
                    item { Tile("Çubuk modeli", Library.fractionBar(num, den)) { onInsert(Library.fractionBar(num, den)) } }
                    item { Tile("Sayı doğrusunda", Library.fractionOnLine(num, den)) { onInsert(Library.fractionOnLine(num, den)) } }
                    item {
                        val eq = Library.fractionBar(num * 2, den * 2)
                        Tile("Denk kesir (×2)", eq) { onInsert(Library.fractionBar(num, den) + Library.place(eq, 0f, 110f, 999f, 999f)) }
                    }
                }
                cat == mine -> {
                    if (user.entries.isEmpty()) item(span = { GridItemSpan(2) }) {
                        Text(
                            "Sayfada bir çizimi seçim aracıyla seçin, alttaki çubuktan \"Kütüphaneye kaydet\"e dokunun. Burada saklanır ve tek dokunuşla tekrar eklenir.",
                            fontSize = 13.sp, color = UI.TextSoft, modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(user.entries, key = { it.id }) { e ->
                        var menu by remember { mutableStateOf(false) }
                        Box {
                            Tile(e.name, e.items, onLong = { menu = true }) { onInsert(e.items) }
                            DropdownMenu(menu, { menu = false }) {
                                DropdownMenuItem(text = { Text("Sil", color = UI.Danger) }, onClick = { menu = false; user.delete(e) })
                            }
                        }
                    }
                }
                else -> {
                    val list = Library.entries.filter { it.category == cat }
                    items(list, key = { it.id }) { e -> Tile(e.title, remember(e.id) { e.build() }) { onInsert(e.build()) } }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Tile(title: String, items: List<Item>, onLong: (() -> Unit)? = null, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, UI.Border, RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLong).padding(6.dp),
    ) {
        ItemsPreview(items, Modifier.fillMaxWidth().aspectRatio(1.3f))
        Text(
            title, fontSize = 12.sp, color = UI.Text, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

@Composable
private fun Stepper(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = UI.TextSoft)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(UI.SurfaceAlt).clickable { if (value > min) onChange(value - 1) }, contentAlignment = Alignment.Center) { Text("−", fontSize = 18.sp, color = UI.Text) }
            Text("$value", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = UI.Text, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(UI.SurfaceAlt).clickable { if (value < max) onChange(value + 1) }, contentAlignment = Alignment.Center) { Text("+", fontSize = 18.sp, color = UI.Text) }
        }
    }
}
