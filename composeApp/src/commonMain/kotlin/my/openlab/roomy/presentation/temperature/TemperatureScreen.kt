package my.openlab.roomy.presentation.temperature

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import my.openlab.roomy.domain.model.TemperatureReading
import my.openlab.roomy.presentation.components.AuroraBackground
import my.openlab.roomy.presentation.components.GlassCard
import my.openlab.roomy.presentation.components.LiveDot
import my.openlab.roomy.presentation.components.MeterBar
import my.openlab.roomy.presentation.components.Pill
import my.openlab.roomy.presentation.components.ShimmerBlock
import my.openlab.roomy.presentation.components.Sparkline
import my.openlab.roomy.presentation.components.TemperatureGauge
import my.openlab.roomy.presentation.components.entrance
import my.openlab.roomy.presentation.components.rememberEntrance
import my.openlab.roomy.presentation.format
import my.openlab.roomy.presentation.pad2
import my.openlab.roomy.presentation.theme.NeutralPalette
import my.openlab.roomy.presentation.theme.label
import my.openlab.roomy.presentation.theme.palette
import kotlin.time.Instant

/** Gauge spans this range. Anything outside just pins to an end. */
private const val SCALE_MIN = 5f
private const val SCALE_MAX = 40f

@Composable
fun TemperatureScreen(
    state: TemperatureUiState,
    onRetry: () -> Unit,
    onOpenSlice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reading = state.reading
    val palette = reading?.comfort?.palette() ?: NeutralPalette

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    AuroraBackground(palette = palette, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header(
                sensorId = reading?.sensorId ?: "—",
                streaming = reading != null && state.errorMessage == null,
                accent = palette.accent,
                onOpenSlice = onOpenSlice,
                modifier = Modifier.entrance(rememberEntrance(appeared, 0)),
            )

            Spacer(Modifier.height(8.dp))

            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Crossfade(
                    targetState = reading != null,
                    animationSpec = tween(500),
                    label = "gauge",
                ) { hasReading ->
                    if (hasReading && reading != null) {
                        TemperatureGauge(
                            progress = (reading.celsius.toFloat() - SCALE_MIN) /
                                (SCALE_MAX - SCALE_MIN),
                            palette = palette,
                            modifier = Modifier.entrance(
                                progress = rememberEntrance(appeared, 120),
                                rise = 70f,
                            ),
                        ) {
                            GaugeCenter(reading, palette.accent, palette.glow)
                        }
                    } else {
                        ShimmerBlock(Modifier.size(300.dp), corner = 150)
                    }
                }
            }

            AnimatedVisibility(
                visible = state.errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                ErrorBanner(state.errorMessage.orEmpty())
            }

            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(
                visible = state.history.size >= 2,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                TrendCard(
                    history = state.history,
                    accent = palette.accent,
                    glow = palette.glow,
                    modifier = Modifier.entrance(rememberEntrance(appeared, 200), rise = 60f),
                )
            }

            Spacer(Modifier.height(12.dp))

            StatRow(
                reading = reading,
                accent = palette.accent,
                modifier = Modifier.entrance(rememberEntrance(appeared, 260), rise = 60f),
            )

            Spacer(Modifier.height(20.dp))

            Footer(
                recordedAt = reading?.recordedAt,
                loading = state.isLoading,
                accent = palette.accent,
                onRetry = onRetry,
                modifier = Modifier.entrance(rememberEntrance(appeared, 380), rise = 50f),
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Header(
    sensorId: String,
    streaming: Boolean,
    accent: Color,
    onOpenSlice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "Roomy",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = if (streaming) "Memantau langsung" else "Menunggu sensor",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.55f),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(
                text = sensorId,
                color = accent,
                leading = {
                    LiveDot(
                        active = streaming,
                        color = if (streaming) accent else Color(0xFFFF6B6B),
                    )
                },
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f))
                    .clickable(onClick = onOpenSlice),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.ViewInAr, contentDescription = "Irisan 3D", tint = accent, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun GaugeCenter(reading: TemperatureReading, accent: Color, glow: Color) {
    val animatedCelsius by animateFloatAsState(
        targetValue = reading.celsius.toFloat(),
        animationSpec = tween(durationMillis = 900),
        label = "celsius",
    )
    val labelColor by animateColorAsState(glow, tween(900), label = "labelColor")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = animatedCelsius.format(1),
                fontSize = 76.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
            )
            Text(
                text = "°C",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = accent,
                modifier = Modifier.padding(top = 14.dp, start = 4.dp),
            )
        }
        Text(
            text = reading.comfort.label().uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = labelColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TrendCard(
    history: List<Double>,
    accent: Color,
    glow: Color,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Tren ${history.size} pembacaan",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f),
                )
                Text(
                    text = "${history.min().format(1)}° – ${history.max().format(1)}°",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(64.dp)) {
                Sparkline(values = history, color = accent, glow = glow)
            }
        }
    }
}

@Composable
private fun StatRow(
    reading: TemperatureReading?,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GlassCard(Modifier.weight(1f)) {
            Column {
                StatHeader(Icons.Rounded.WaterDrop, "Kelembapan", accent)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = reading?.humidityPercent?.let { "${it.format(0)}%" } ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Spacer(Modifier.height(10.dp))
                MeterBar(
                    fraction = ((reading?.humidityPercent ?: 0.0) / 100.0).toFloat(),
                    color = accent,
                )
            }
        }
        GlassCard(Modifier.weight(1f)) {
            Column {
                StatHeader(Icons.Rounded.Thermostat, "Fahrenheit", accent)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = reading?.let { "${it.fahrenheit.format(1)}°F" } ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Spacer(Modifier.height(10.dp))
                MeterBar(
                    fraction = (((reading?.celsius ?: 0.0) - SCALE_MIN) /
                        (SCALE_MAX - SCALE_MIN)).toFloat(),
                    color = accent,
                )
            }
        }
    }
}

@Composable
private fun StatHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    accent: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun ErrorBanner(message: String) {
    GlassCard(Modifier.fillMaxWidth(), corner = 16) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Rounded.Sensors,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun Footer(
    recordedAt: Instant?,
    loading: Boolean,
    accent: Color,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = recordedAt?.let { "Diperbarui ${it.clockTime()}" } ?: "Belum ada data",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f))
                .clickable(enabled = !loading, onClick = onRetry),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Muat ulang",
                tint = accent,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = if (loading) angle else 0f },
            )
        }
    }
}

private fun Instant.clockTime(): String {
    val time = toLocalDateTime(TimeZone.currentSystemDefault()).time
    return "${time.hour.pad2()}:${time.minute.pad2()}:${time.second.pad2()}"
}
