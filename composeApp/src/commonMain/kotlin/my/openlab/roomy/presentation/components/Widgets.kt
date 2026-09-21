package my.openlab.roomy.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Frosted panel used for every secondary surface on the screen. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Int = 24,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f))
                ),
                shape = RoundedCornerShape(corner.dp),
            )
            .padding(16.dp),
    ) {
        content()
    }
}

/** Pulsing dot with an expanding ring. Green while streaming, red once the feed breaks. */
@Composable
fun LiveDot(active: Boolean, color: Color, modifier: Modifier = Modifier) {
    val tint by animateColorAsState(color, tween(600), label = "dot")
    val transition = rememberInfiniteTransition(label = "live")
    val ring by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Restart),
        label = "ring",
    )
    Box(modifier = modifier.size(14.dp), contentAlignment = Alignment.Center) {
        if (active) {
            Box(
                Modifier.fillMaxSize().drawBehind {
                    drawCircle(
                        color = tint.copy(alpha = (1f - ring) * 0.55f),
                        radius = size.minDimension / 2f * (0.4f + ring * 0.6f),
                    )
                }
            )
        }
        Box(Modifier.size(7.dp).clip(CircleShape).background(tint))
    }
}

/** Horizontal meter with a light sweep running across the filled part. */
@Composable
fun MeterBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(900),
        label = "meter",
    )
    val transition = rememberInfiniteTransition(label = "sweep")
    val sweep by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Restart),
        label = "sweepPos",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.08f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .drawBehind {
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(color.copy(alpha = 0.55f), color)
                        )
                    )
                    val x = size.width * sweep
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.45f),
                                Color.Transparent,
                            ),
                            startX = x - size.width * 0.18f,
                            endX = x + size.width * 0.18f,
                        )
                    )
                }
        )
    }
}

/** Loading placeholder with a diagonal shimmer. */
@Composable
fun ShimmerBlock(modifier: Modifier = Modifier, corner: Int = 16) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val position by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Restart),
        label = "shimmerPos",
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner.dp))
            .drawBehind {
                drawRect(Color.White.copy(alpha = 0.06f))
                val travel = size.width * 2f
                val x = -size.width + travel * position
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.16f),
                            Color.Transparent,
                        ),
                        start = Offset(x, 0f),
                        end = Offset(x + size.width * 0.6f, size.height),
                    )
                )
            },
    )
}

/** Small pill used for sensor id and status text. */
@Composable
fun Pill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.32f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        leading?.invoke()
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.86f),
        )
    }
}

/** Scale-in entrance used to stagger the screen. */
@Composable
fun rememberEntrance(visible: Boolean, delayMillis: Int): Float {
    val value by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 650, delayMillis = delayMillis),
        label = "entrance$delayMillis",
    )
    return value
}

fun Modifier.entrance(progress: Float, rise: Float = 40f): Modifier =
    this
        .scale(0.94f + 0.06f * progress)
        .graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * rise
        }
