package my.openlab.roomy.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.openlab.roomy.presentation.theme.ComfortPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val START_ANGLE = 140f
private const val SWEEP_ANGLE = 260f
private const val TICK_COUNT = 44

/**
 * Circular gauge: static track, spring-animated progress arc, travelling head dot,
 * and a glow ring that breathes. [content] sits in the middle.
 */
@Composable
fun TemperatureGauge(
    progress: Float,
    palette: ComfortPalette,
    modifier: Modifier = Modifier,
    diameter: Dp = 300.dp,
    content: @Composable () -> Unit,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessVeryLow,
        ),
        label = "progress",
    )
    val accent by animateColorAsState(palette.accent, tween(900), label = "accent")
    val glow by animateColorAsState(palette.glow, tween(900), label = "glow")

    val transition = rememberInfiniteTransition(label = "gauge")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.055f
            val inset = stroke * 1.6f
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val radius = arcSize.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glow.copy(alpha = 0.22f * pulse), Color.Transparent),
                    center = center,
                    radius = radius * 1.45f,
                ),
                radius = radius * 1.45f,
                center = center,
            )

            drawTicks(center, radius, stroke, glow, animatedProgress)

            drawArc(
                color = Color.White.copy(alpha = 0.06f),
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_ANGLE,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            drawArc(
                brush = Brush.linearGradient(listOf(glow, accent, glow)),
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_ANGLE * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            val headAngle = (START_ANGLE + SWEEP_ANGLE * animatedProgress) * PI.toFloat() / 180f
            val head = Offset(
                x = center.x + radius * cos(headAngle),
                y = center.y + radius * sin(headAngle),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glow.copy(alpha = 0.8f), Color.Transparent),
                    center = head,
                    radius = stroke * 2.6f,
                ),
                radius = stroke * 2.6f,
                center = head,
            )
            drawCircle(color = Color.White, radius = stroke * 0.42f, center = head)
        }
        content()
    }
}

private fun DrawScope.drawTicks(
    center: Offset,
    radius: Float,
    stroke: Float,
    color: Color,
    progress: Float,
) {
    val inner = radius - stroke * 2.4f
    val outer = radius - stroke * 1.2f
    repeat(TICK_COUNT) { index ->
        val fraction = index / (TICK_COUNT - 1f)
        val angle = (START_ANGLE + SWEEP_ANGLE * fraction) * PI.toFloat() / 180f
        val lit = fraction <= progress
        drawLine(
            color = if (lit) color.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.10f),
            start = Offset(center.x + inner * cos(angle), center.y + inner * sin(angle)),
            end = Offset(center.x + outer * cos(angle), center.y + outer * sin(angle)),
            strokeWidth = stroke * 0.16f,
            cap = StrokeCap.Round,
        )
    }
}
