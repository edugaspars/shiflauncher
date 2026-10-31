package io.github.edugaspars.shiflauncher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Tipos de gesto ilustrados por uma mini animação. */
enum class Gesture { SWIPE_UP, SWIPE_UP_HOLD, SIDE_BOTTOM, BACK_EDGE, DOUBLE_TAP, SWIPE_DOWN, LONG_PRESS, SWIPE_UP_DRAWER }

/** Celular desenhado com um "dedo" que repete o gesto. */
@Composable
fun GestureDemo(gesture: Gesture, modifier: Modifier = Modifier) {
    val fg = LocalPalette.current.fg
    val accent = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val t by rememberInfiniteTransition(label = "demo").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    Canvas(modifier.size(40.dp, 64.dp)) {
        val w = size.width; val h = size.height
        val r = 6.dp.toPx()
        drawRoundRect(fg.copy(alpha = 0.35f), size = size, cornerRadius = CornerRadius(r), style = Stroke(1.5.dp.toPx()))
        // Linha de gestos
        drawLine(fg.copy(alpha = 0.35f), Offset(w * 0.35f, h - 4.dp.toPx()), Offset(w * 0.65f, h - 4.dp.toPx()), 1.5.dp.toPx())

        // Progresso do movimento (0..1) com pausa no fim.
        val p = (t / 0.7f).coerceIn(0f, 1f)
        val visible = t < 0.85f
        val dot = 5.dp.toPx()
        fun finger(x: Float, y: Float, alpha: Float = 1f, big: Boolean = false) {
            if (!visible) return
            drawCircle(accent.copy(alpha = 0.9f * alpha), radius = if (big) dot * 1.4f else dot, center = Offset(x, y))
        }
        fun trail(from: Offset, to: Offset) {
            if (!visible) return
            drawLine(accent.copy(alpha = 0.3f), from, Offset(from.x + (to.x - from.x) * p, from.y + (to.y - from.y) * p), dot)
        }
        when (gesture) {
            Gesture.SWIPE_UP, Gesture.SWIPE_UP_DRAWER -> {
                val from = Offset(w / 2, h - 6.dp.toPx())
                val to = Offset(w / 2, if (gesture == Gesture.SWIPE_UP) h * 0.45f else h * 0.25f)
                trail(from, to); finger(from.x, from.y + (to.y - from.y) * p)
            }
            Gesture.SWIPE_UP_HOLD -> {
                val q = (t / 0.4f).coerceIn(0f, 1f)
                val y = h - 6.dp.toPx() - (h * 0.4f) * q
                if (visible) drawLine(accent.copy(alpha = 0.3f), Offset(w / 2, h - 6.dp.toPx()), Offset(w / 2, y), dot)
                finger(w / 2, y, big = t > 0.4f && t < 0.85f)
            }
            Gesture.SIDE_BOTTOM -> {
                val y = h - 6.dp.toPx()
                val from = Offset(w * 0.2f, y); val to = Offset(w * 0.8f, y)
                trail(from, to); finger(from.x + (to.x - from.x) * p, y)
            }
            Gesture.BACK_EDGE -> {
                val y = h * 0.45f
                val from = Offset(1.dp.toPx(), y); val to = Offset(w * 0.45f, y)
                trail(from, to); finger(from.x + (to.x - from.x) * p, y)
            }
            Gesture.SWIPE_DOWN -> {
                val from = Offset(w / 2, h * 0.3f); val to = Offset(w / 2, h * 0.7f)
                trail(from, to); finger(w / 2, from.y + (to.y - from.y) * p)
            }
            Gesture.DOUBLE_TAP -> {
                val tap = (t < 0.15f) || (t in 0.3f..0.45f)
                if (tap) drawCircle(accent.copy(alpha = 0.9f), dot * 1.3f, Offset(w / 2, h / 2))
                else if (t < 0.6f) drawCircle(accent.copy(alpha = 0.25f), dot, Offset(w / 2, h / 2))
            }
            Gesture.LONG_PRESS -> {
                val grow = (t / 0.6f).coerceIn(0f, 1f)
                if (visible) {
                    drawCircle(accent.copy(alpha = 0.2f), dot * (1f + 1.5f * grow), Offset(w / 2, h / 2))
                    drawCircle(accent.copy(alpha = 0.9f), dot, Offset(w / 2, h / 2))
                }
            }
        }
    }
}
