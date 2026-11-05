package com.poyankee.note.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poyankee.note.editor.InstrumentKind
import com.poyankee.note.editor.Tool
import kotlin.math.cos
import kotlin.math.sin

/** Araç simgeleri: dış bağımlılık olmadan, 24x24 birimlik bir alana elle çizilir. */
@Composable
fun ToolGlyph(tool: Tool, tint: Color, accent: Color = tint, size: Dp = 26.dp) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 24f
        val st = Stroke(width = 1.8f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(x * u, y * u)
        fun path(vararg xy: Float, close: Boolean = false) = Path().apply {
            moveTo(xy[0] * u, xy[1] * u)
            var i = 2
            while (i + 1 < xy.size) { lineTo(xy[i] * u, xy[i + 1] * u); i += 2 }
            if (close) close()
        }
        when (tool) {
            Tool.PEN -> {
                drawPath(path(5f, 19f, 6f, 15f, 16f, 5f, 19f, 8f, 9f, 18f, close = true), tint, style = st)
                drawLine(accent, p(4f, 21f), p(10f, 21f), 2.4f * u, StrokeCap.Round)
            }
            Tool.HIGHLIGHTER -> {
                drawPath(path(7f, 15f, 15f, 4f, 20f, 8f, 12f, 19f, close = true), tint, style = st)
                drawPath(path(7f, 15f, 5f, 19f, 9f, 19f, 12f, 19f), tint, style = st)
                drawLine(accent.copy(alpha = 0.6f), p(3f, 21.5f), p(21f, 21.5f), 3f * u, StrokeCap.Round)
            }
            Tool.ERASER -> {
                drawPath(path(3f, 15f, 13f, 5f, 20f, 12f, 12f, 20f, 8f, 20f, close = true), tint, style = st)
                drawLine(tint, p(8f, 10f), p(15f, 17f), 1.8f * u)
                drawLine(tint, p(12f, 20.5f), p(21f, 20.5f), 1.8f * u, StrokeCap.Round)
            }
            Tool.LASSO -> {
                drawOval(tint, p(3f, 3f), Size(18f * u, 12f * u), style = Stroke(1.8f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * u, 2.5f * u))))
                drawPath(path(8f, 14f, 7f, 18f, 9f, 21f), tint, style = st)
            }
            Tool.TEXT -> {
                drawLine(tint, p(5f, 5f), p(19f, 5f), 2.2f * u, StrokeCap.Round)
                drawLine(tint, p(12f, 5f), p(12f, 20f), 2.2f * u, StrokeCap.Round)
                drawLine(tint, p(9f, 20f), p(15f, 20f), 1.8f * u, StrokeCap.Round)
            }
            Tool.LASER -> {
                drawLine(tint, p(4f, 20f), p(13f, 11f), 2.6f * u, StrokeCap.Round)
                drawCircle(Color(0xFFFF1744), 3.2f * u, p(16f, 8f))
                drawCircle(Color(0x55FF1744), 6.2f * u, p(16f, 8f))
            }
            Tool.TEMP -> {
                drawPath(path(5f, 19f, 6f, 15f, 16f, 5f, 19f, 8f, 9f, 18f, close = true), tint, style = st)
                drawArc(tint, -90f, 270f, false, p(14f, 14f), Size(8f * u, 8f * u), style = Stroke(1.5f * u))
                drawLine(tint, p(18f, 18f), p(18f, 15.5f), 1.4f * u)
            }
            Tool.SHAPE -> {
                drawCircle(tint, 5f * u, p(8f, 9f), style = st)
                drawRect(tint, p(11f, 11f), Size(10f * u, 10f * u), style = st)
            }
            Tool.LINE -> {
                drawLine(tint, p(4f, 20f), p(20f, 4f), 1.8f * u, StrokeCap.Round)
                drawCircle(tint, 2f * u, p(4f, 20f)); drawCircle(tint, 2f * u, p(20f, 4f))
            }
            Tool.ARROW -> {
                drawLine(tint, p(4f, 20f), p(20f, 4f), 1.8f * u, StrokeCap.Round)
                drawPath(path(12f, 4f, 20f, 4f, 20f, 12f), tint, style = st)
            }
            Tool.POLYGON -> {
                val pts = FloatArray(12)
                for (i in 0 until 6) {
                    val a = Math.PI / 3 * i - Math.PI / 2
                    pts[2 * i] = (12 + 9 * cos(a)).toFloat(); pts[2 * i + 1] = (12 + 9 * sin(a)).toFloat()
                }
                drawPath(path(*pts, close = true), tint, style = st)
            }
            Tool.COMPASS -> {
                drawLine(tint, p(12f, 3f), p(6f, 20f), 1.8f * u, StrokeCap.Round)
                drawLine(tint, p(12f, 3f), p(18f, 20f), 1.8f * u, StrokeCap.Round)
                drawCircle(tint, 1.8f * u, p(12f, 4f))
                drawArc(accent, 20f, 140f, false, p(3f, 9f), Size(18f * u, 18f * u), style = Stroke(1.4f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * u, 2f * u))))
            }
            Tool.SPOT -> {
                drawCircle(tint.copy(alpha = 0.18f), 10f * u, p(12f, 12f))
                drawCircle(tint, 5.5f * u, p(12f, 12f), style = st)
                for (k in 0 until 8) {
                    val a = Math.PI / 4 * k
                    drawLine(tint, p(12f + 7.5f * cos(a).toFloat(), 12f + 7.5f * sin(a).toFloat()), p(12f + 10.5f * cos(a).toFloat(), 12f + 10.5f * sin(a).toFloat()), 1.4f * u, StrokeCap.Round)
                }
            }
            Tool.SOLID -> {
                drawPath(path(4f, 9f, 14f, 9f, 14f, 20f, 4f, 20f, close = true), tint, style = st)
                drawPath(path(4f, 9f, 9f, 4f, 19f, 4f, 14f, 9f), tint, style = st)
                drawPath(path(19f, 4f, 19f, 15f, 14f, 20f), tint, style = st)
            }
            Tool.MARK -> {
                drawLine(tint, p(4f, 20f), p(20f, 20f), 1.8f * u, StrokeCap.Round)
                drawLine(tint, p(4f, 20f), p(4f, 4f), 1.8f * u, StrokeCap.Round)
                drawPath(path(4f, 13f, 11f, 13f, 11f, 20f), accent, style = st)
            }
        }
    }
}

@Composable
fun InstrumentGlyph(kind: InstrumentKind?, tint: Color, size: Dp = 26.dp) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 24f
        val st = Stroke(width = 1.6f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(x * u, y * u)
        when (kind) {
            InstrumentKind.PROTRACTOR -> {
                drawArc(tint, 180f, 180f, true, p(2f, 6f), Size(20f * u, 20f * u), style = st)
                for (k in 1..5) {
                    val a = Math.PI * k / 6
                    drawLine(tint, p(12f - 10f * cos(a).toFloat(), 16f - 10f * sin(a).toFloat()), p(12f - 7.5f * cos(a).toFloat(), 16f - 7.5f * sin(a).toFloat()), 1.2f * u)
                }
            }
            InstrumentKind.SET_SQUARE_45, InstrumentKind.SET_SQUARE_60 -> {
                drawPath(Path().apply { moveTo(4f * u, 20f * u); lineTo(20f * u, 20f * u); lineTo(4f * u, 4f * u); close() }, tint, style = st)
                drawPath(Path().apply { moveTo(7f * u, 17f * u); lineTo(12f * u, 17f * u); lineTo(7f * u, 12f * u); close() }, tint, style = st)
            }
            else -> {
                // cetvel
                drawRect(tint, p(2f, 8f), Size(20f * u, 8f * u), style = st)
                for (k in 0..6) drawLine(tint, p(4f + k * 2.7f, 8f), p(4f + k * 2.7f, if (k % 2 == 0) 12f else 10.5f), 1.2f * u)
            }
        }
    }
}

/** Kalınlık noktası. */
fun DrawScope.widthDot(color: Color, r: Float) = drawCircle(color, r, center)
