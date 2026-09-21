package my.openlab.roomy.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Smoothed trend line over the last readings, with a filled gradient underneath and a
 * pulsing head dot. Redraws on every new sample, so the line crawls left as data arrives.
 */
@Composable
fun Sparkline(
    values: List<Double>,
    color: Color,
    glow: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "spark")
    val pulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "sparkPulse",
    )

    Canvas(modifier.fillMaxSize()) {
        if (values.size < 2) return@Canvas

        val min = values.min()
        val max = values.max()
        val span = (max - min).takeIf { it > 0.4 } ?: 0.4
        // Keep the head dot and its glow inside the canvas instead of clipping at the edge.
        val inset = 8.dp.toPx()
        val stepX = (size.width - inset * 2) / (values.size - 1)

        val points = values.mapIndexed { index, value ->
            Offset(
                x = inset + index * stepX,
                y = size.height - ((value - min) / span).toFloat() * size.height * 0.78f -
                    size.height * 0.12f,
            )
        }

        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val previous = points[i - 1]
                val current = points[i]
                val midX = (previous.x + current.x) / 2f
                cubicTo(midX, previous.y, midX, current.y, current.x, current.y)
            }
        }

        val fill = Path().apply {
            addPath(line)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }

        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                listOf(color.copy(alpha = 0.28f), Color.Transparent)
            ),
        )
        drawPath(
            path = line,
            brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.35f), glow)),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
        )

        val head = points.last()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glow.copy(alpha = 0.7f * pulse), Color.Transparent),
                center = head,
                radius = 7.dp.toPx() * (0.7f + 0.3f * pulse),
            ),
            radius = 7.dp.toPx() * (0.7f + 0.3f * pulse),
            center = head,
        )
        drawCircle(color = Color.White, radius = 3.dp.toPx() / 2f, center = head)
    }
}
