package com.poyankee.note.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poyankee.note.data.Todo
import com.poyankee.note.data.TodoList

private val todoColors = listOf(Color.Transparent, Color(0xFFE5484D), Color(0xFFF59E0B), Color(0xFF22A06B))

/** Bağlanabilecek yer: açık defter ve sayfası. */
class TodoLink(val nbId: String, val nbName: String, val page: Int)

/**
 * Yapılacaklar paneli (ana sayfada ve defterin içinde soldan açılır).
 * [link] verilirse yeni görevler o deftere/sayfaya bağlanabilir; [onOpen] bağlı deftere gider.
 */
@Composable
fun TodoPanel(todos: TodoList, link: TodoLink?, onOpen: (String, Int) -> Unit, onClose: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var attach by remember { mutableStateOf(false) }
    fun add() {
        if (text.isBlank()) return
        if (attach && link != null) todos.add(text, link.nbId, link.nbName, link.page) else todos.add(text)
        text = ""
    }
    Surface(Modifier.width(340.dp).fillMaxHeight(), color = UI.Surface, shadowElevation = 16.dp) {
        Column(Modifier.fillMaxHeight()) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Yapılacaklar", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = UI.Text, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Kapat", tint = UI.TextSoft) }
            }
            // yeni görev
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(UI.SurfaceAlt).padding(start = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
                    if (text.isEmpty()) Text("Yeni görev yazın", color = UI.TextFaint, fontSize = 15.sp)
                    BasicTextField(
                        text, { text = it }, singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, color = UI.Text),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { add() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                IconButton(onClick = { add() }) { Icon(Icons.Rounded.Add, "Ekle", tint = UI.Primary) }
            }
            if (link != null) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(8.dp)).clickable { attach = !attach }.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)).background(if (attach) UI.Primary else Color.Transparent)
                            .border(1.5.dp, if (attach) UI.Primary else UI.TextFaint, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center,
                    ) { if (attach) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                    Spacer(Modifier.width(8.dp))
                    Text("Bu sayfaya bağla (${link.nbName}, s. ${link.page + 1})", fontSize = 13.sp, color = UI.TextSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(6.dp))
            val list = todos.items
            if (list.isEmpty()) {
                Text(
                    "Ders hazırlığı, okunacak sayfalar, kontrol edilecek ödevler… Görevlerinizi buraya yazın. Bir göreve dokununca tamamlanır.",
                    fontSize = 13.sp, color = UI.TextFaint, modifier = Modifier.padding(20.dp),
                )
            }
            LazyColumn(Modifier.weight(1f)) {
                items(list, key = { it.id }) { t -> TodoRow(todos, t, link, onOpen) }
            }
            if (list.any { it.done }) {
                HorizontalDivider(color = UI.Border)
                TextButton(onClick = { todos.clearDone() }, modifier = Modifier.padding(8.dp)) { Text("Tamamlananları temizle", color = UI.TextSoft) }
            }
        }
    }
}

@Composable
private fun TodoRow(todos: TodoList, t: Todo, link: TodoLink?, onOpen: (String, Int) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable { todos.toggle(t) }.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(if (t.done) UI.Primary else Color.Transparent)
                .border(2.dp, if (t.done) UI.Primary else if (t.color != 0) todoColors[t.color] else UI.TextFaint, CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (t.done) Icon(Icons.Rounded.Check, "Tamamlandı", tint = Color.White, modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                t.text, fontSize = 15.sp, color = if (t.done) UI.TextFaint else UI.Text,
                textDecoration = if (t.done) TextDecoration.LineThrough else null,
            )
            if (t.nbId.isNotEmpty()) {
                Text(
                    "→ ${t.nbName}, s. ${t.page + 1}", fontSize = 12.sp, color = UI.Primary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onOpen(t.nbId, t.page) }.padding(vertical = 2.dp),
                )
            }
        }
        if (t.color != 0) Box(Modifier.padding(horizontal = 4.dp).size(8.dp).clip(CircleShape).background(todoColors[t.color]))
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Seçenekler", tint = UI.TextFaint) }
            DropdownMenu(menu, { menu = false }) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    todoColors.forEachIndexed { i, c ->
                        Box(
                            Modifier.size(28.dp).clip(CircleShape).background(if (i == 0) UI.SurfaceAlt else c)
                                .border(if (t.color == i) 2.5.dp else 0.dp, UI.Text, CircleShape)
                                .clickable { todos.update(t.copy(color = i)); menu = false },
                        )
                    }
                }
                DropdownMenuItem(text = { Text("Yukarı taşı") }, onClick = { todos.move(t, -1); menu = false })
                DropdownMenuItem(text = { Text("Aşağı taşı") }, onClick = { todos.move(t, 1); menu = false })
                if (link != null) DropdownMenuItem(
                    text = { Text("Bu sayfaya bağla") },
                    onClick = { todos.update(t.copy(nbId = link.nbId, nbName = link.nbName, page = link.page)); menu = false },
                )
                if (t.nbId.isNotEmpty()) DropdownMenuItem(text = { Text("Bağlantıyı kaldır") }, onClick = { todos.update(t.copy(nbId = "", nbName = "", page = 0)); menu = false })
                DropdownMenuItem(text = { Text("Sil", color = UI.Danger) }, onClick = { todos.delete(t); menu = false })
            }
        }
    }
}
