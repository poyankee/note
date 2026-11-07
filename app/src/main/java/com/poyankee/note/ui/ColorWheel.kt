package com.poyankee.note.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poyankee.note.core.L
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Renk topu: daire üzerinde parmak/kalemle kaydırarak renk seçilir.
 * Açı = ton, merkeze uzaklık = doygunluk; alttaki kaydırıcı = parlaklık. Her değişiklikte [onChange] çağrılır.
 */
@Composable
fun ColorWheel(color: Int, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    val start = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(color, it) } }
    var h by remember { mutableFloatStateOf(start[0]) }
    var sat by remember { mutableFloatStateOf(start[1]) }
    var v by remember { mutableFloatStateOf(if (start[2] < 0.05f && start[1] < 0.05f) 0f else start[2]) }
    fun out() = android.graphics.Color.HSVToColor(floatArrayOf(h, sat, v))
    val hues = remember { (0..12).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it * 30f, 1f, 1f))) } }

    Column(modifier) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            fun pick(p: Offset, w: Float, hh: Float) {
                val cx = w / 2f; val cy = hh / 2f; val r = min(cx, cy)
                val dx = p.x - cx; val dy = p.y - cy
                var a = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
                if (a < 0) a += 360f
                h = a
                sat = (hypot(dx, dy) / r).coerceIn(0f, 1f)
                if (v < 0.15f) v = 1f // siyahken topta gezinince renk görünsün
                onChange(out())
            }
            Canvas(
                Modifier.fillMaxWidth().aspectRatio(1f)
                    .pointerInput(Unit) { detectTapGestures { pick(it, size.width.toFloat(), size.height.toFloat()) } }
                    .pointerInput(Unit) {
                        detectDragGestures(onDragStart = { pick(it, size.width.toFloat(), size.height.toFloat()) }) { ch, _ ->
                            ch.consume(); pick(ch.position, size.width.toFloat(), size.height.toFloat())
                        }
                    },
            ) {
                val r = min(size.width, size.height) / 2f
                val c = Offset(size.width / 2f, size.height / 2f)
                drawCircle(Brush.sweepGradient(hues, c), r, c)
                drawCircle(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0f)), c, r), r, c)
                if (v < 1f) drawCircle(Color.Black.copy(alpha = 1f - v), r, c)
                drawCircle(Color(0x22000000), r, c, style = Stroke(1.dp.toPx()))
                // seçili nokta
                val rad = Math.toRadians(h.toDouble())
                val p = Offset(c.x + (cos(rad) * sat * r).toFloat(), c.y + (sin(rad) * sat * r).toFloat())
                drawCircle(Color(out()), 14.dp.toPx(), p)
                drawCircle(Color.White, 14.dp.toPx(), p, style = Stroke(3.dp.toPx()))
                drawCircle(Color(0x55000000), 15.5.dp.toPx(), p, style = Stroke(1.dp.toPx()))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(Color(out())).border(1.dp, UI.Border, CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(L.t("Parlaklık"), fontSize = 12.sp, color = UI.TextSoft)
                val full = Color(android.graphics.Color.HSVToColor(floatArrayOf(h, sat, 1f)))
                Box(Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(Color.Black, full))))
                    Slider(v, { v = it; onChange(out()) }, colors = SliderDefaults.colors(thumbColor = Color(out()), activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent))
                }
            }
        }
        Text("#" + String.format("%06X", out() and 0xFFFFFF), color = UI.TextFaint, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}
