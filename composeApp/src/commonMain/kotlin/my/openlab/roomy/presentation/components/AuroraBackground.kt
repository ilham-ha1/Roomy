package my.openlab.roomy.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import my.openlab.roomy.presentation.theme.ComfortPalette
import kotlin.math.cos
import kotlin.math.sin

private const val DRIFT_MILLIS = 14000
private const val BREATH_MILLIS = 6000

/**
 * Three slow-drifting colour blobs over a vertical gradient. Every colour animates when the
 * comfort band changes, so the whole screen shifts mood instead of snapping.
 */
@Composable
fun AuroraBackground(
    palette: ComfortPalette,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val deep by animateColorAsState(palette.deep, tween(900), label = "deep")
    val mid by animateColorAsState(palette.mid, tween(900), label = "mid")
    val accent by animateColorAsState(palette.accent, tween(900), label = "accent")
    val glow by animateColorAsState(palette.glow, tween(900), label = "glow")

    val transition = rememberInfiniteTransition(label = "aurora")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(DRIFT_MILLIS), RepeatMode.Restart),
        label = "drift",
    )
    val breath by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(BREATH_MILLIS), RepeatMode.Reverse),
        label = "breath",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to deep,
                    0.55f to mid.copy(alpha = 0.55f).compositeOver(deep),
                    1f to deep,
                )
            )

            val angle = drift * 2f * kotlin.math.PI.toFloat()
            val short = size.minDimension

            blob(
                center = Offset(
                    x = size.width * (0.22f + 0.10f * cos(angle)),
                    y = size.height * (0.24f + 0.06f * sin(angle)),
                ),
                radius = short * 0.62f * breath,
                color = accent,
                alpha = 0.30f,
            )
            blob(
                center = Offset(
                    x = size.width * (0.80f + 0.09f * cos(angle + 2.1f)),
                    y = size.height * (0.36f + 0.08f * sin(angle + 2.1f)),
                ),
                radius = short * 0.52f * (2f - breath),
                color = glow,
                alpha = 0.18f,
            )
            blob(
                center = Offset(
                    x = size.width * (0.50f + 0.14f * cos(angle + 4.2f)),
                    y = size.height * (0.86f + 0.05f * sin(angle + 4.2f)),
                ),
                radius = short * 0.70f * breath,
                color = mid,
                alpha = 0.42f,
            )
        }
        content()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.blob(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
