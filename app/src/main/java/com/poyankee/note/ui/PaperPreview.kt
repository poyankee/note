package com.poyankee.note.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poyankee.note.core.Background
import com.poyankee.note.core.Templates
import com.poyankee.note.render.PageRenderer

/**
 * Kağıt deseninin küçük önizlemesi (sayfa panelindeki şablon kutuları ve kapaklar için).
 * Desen gerçek sayfa ölçüsünde hesaplanır, sonra küçültülerek çizilir; böylece aralık ayarı da görünür.
 */
@Composable
fun PaperPreview(paper: Background.Paper, modifier: Modifier, pageW: Float = 595f, pageH: Float = 842f, zoom: Float = 2.2f) {
    // Önizlemede sayfanın sol üst köşesi büyütülerek gösterilir (desen okunabilsin diye)
    val w = pageW / zoom; val h = pageH / zoom
    val geo = remember(paper.template, paper.spacing) { Templates.geometry(paper.template, pageW, pageH, paper.spacing) }
    Canvas(modifier) {
        val sx = size.width / w; val sy = size.height / h
        drawRect(Color(paper.color))
        val op = paper.opacity
        for (l in geo.lines) {
            if (minOf(l.x1, l.x2) > w || minOf(l.y1, l.y2) > h) continue
            drawLine(
                Color(PageRenderer.lineColor(l.kind, paper.dark)).copy(alpha = op),
                Offset(l.x1 * sx, l.y1 * sy), Offset(l.x2 * sx, l.y2 * sy),
                strokeWidth = (PageRenderer.lineWidth(l.kind) * sx).coerceAtLeast(0.8f),
            )
        }
        var i = 0
        val dc = Color(PageRenderer.dotColor(paper.dark)).copy(alpha = op)
        while (i + 1 < geo.dots.size) {
            val x = geo.dots[i]; val y = geo.dots[i + 1]
            if (x <= w && y <= h) drawCircle(dc, (geo.dotRadius * sx).coerceAtLeast(1.2f), Offset(x * sx, y * sy))
            i += 2
        }
    }
}

/** "Sayfa" düğmesi için küçük kareli kağıt simgesi. */
@Composable
fun PaperGlyph(tint: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 24f
        drawRoundRect(tint, Offset(4f * u, 3f * u), androidx.compose.ui.geometry.Size(16f * u, 18f * u),
            androidx.compose.ui.geometry.CornerRadius(2.5f * u), style = Stroke(1.8f * u))
        for (k in 1..3) {
            drawLine(tint.copy(alpha = 0.6f), Offset(4f * u, (3f + k * 4.5f) * u), Offset(20f * u, (3f + k * 4.5f) * u), 1.1f * u, StrokeCap.Round)
            drawLine(tint.copy(alpha = 0.6f), Offset((4f + k * 4f) * u, 3f * u), Offset((4f + k * 4f) * u, 21f * u), 1.1f * u, StrokeCap.Round)
        }
    }
}
