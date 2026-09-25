package my.openlab.roomy.presentation.slice

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.openlab.roomy.domain.model.RoomField
import my.openlab.roomy.domain.model.SliceAxis
import my.openlab.roomy.domain.model.TemperatureReading
import my.openlab.roomy.presentation.components.AuroraBackground
import my.openlab.roomy.presentation.components.GlassCard
import my.openlab.roomy.presentation.format
import my.openlab.roomy.presentation.theme.ColdPalette
import my.openlab.roomy.presentation.theme.ComfortablePalette
import my.openlab.roomy.presentation.theme.HotPalette
import my.openlab.roomy.presentation.theme.NeutralPalette
import my.openlab.roomy.presentation.theme.palette
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Room size in metres. Only used for drawing proportions and labels; the field is normalised. */
private const val ROOM_WIDTH = 5f
private const val ROOM_DEPTH = 4f
private const val ROOM_HEIGHT = 2.8f

private const val CELLS = 32

/** Colour scale spans a little past the comfort thresholds so both ends stay readable. */
private const val SCALE_MIN = TemperatureReading.COLD_BELOW - 2
private const val SCALE_MAX = TemperatureReading.HOT_ABOVE + 2

private fun SliceAxis.label() = when (this) {
    SliceAxis.X -> "Lebar"
    SliceAxis.Y -> "Kedalaman"
    SliceAxis.Z -> "Tinggi"
}

private fun SliceAxis.length() = when (this) {
    SliceAxis.X -> ROOM_WIDTH
    SliceAxis.Y -> ROOM_DEPTH
    SliceAxis.Z -> ROOM_HEIGHT
}

/** Real-world size of the slice's (u, v) axes, matching [RoomField.celsiusOnSlice]. */
private fun SliceAxis.planeSize() = when (this) {
    SliceAxis.X -> Size(ROOM_DEPTH, ROOM_HEIGHT)
    SliceAxis.Y -> Size(ROOM_WIDTH, ROOM_HEIGHT)
    SliceAxis.Z -> Size(ROOM_WIDTH, ROOM_DEPTH)
}

/** Slice (u, v) back to normalised room (x, y, z). */
private fun SliceAxis.toRoom(position: Float, u: Float, v: Float): Triple<Float, Float, Float> = when (this) {
    SliceAxis.X -> Triple(position, u, v)
    SliceAxis.Y -> Triple(u, position, v)
    SliceAxis.Z -> Triple(u, v, position)
}

private fun heatColor(celsius: Double): Color {
    val t = ((celsius - SCALE_MIN) / (SCALE_MAX - SCALE_MIN)).toFloat().coerceIn(0f, 1f)
    return if (t < 0.5f) {
        lerp(ColdPalette.accent, ComfortablePalette.accent, t * 2)
    } else {
        lerp(ComfortablePalette.accent, HotPalette.accent, (t - 0.5f) * 2)
    }
}

@Composable
fun RoomSliceScreen(
    reading: TemperatureReading?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = reading?.comfort?.palette() ?: NeutralPalette
    val field = remember(reading?.celsius) { RoomField.demoAround(reading?.celsius ?: 24.0) }

    var axis by rememberSaveable { mutableStateOf(SliceAxis.Z) }
    var position by rememberSaveable { mutableFloatStateOf(0.5f) }
    val grid = remember(field, axis, position) { field.slice(axis, position.toDouble(), CELLS) }

    AuroraBackground(palette = palette, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TopBar(onBack = onBack, accent = palette.accent)

            GlassCard(Modifier.fillMaxWidth()) {
                Column {
                    Caption("Geser untuk memutar ruangan")
                    RoomCube(
                        field = field,
                        grid = grid,
                        axis = axis,
                        position = position,
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SliceAxis.entries.forEach { entry ->
                    AxisChip(
                        text = entry.label(),
                        selected = entry == axis,
                        accent = palette.accent,
                        onClick = { axis = entry },
                    )
                }
            }

            Column {
                Caption("${axis.label()} ${(position * axis.length()).toDouble().format(1)} m")
                Slider(
                    value = position,
                    onValueChange = { position = it },
                    colors = SliderDefaults.colors(
                        thumbColor = palette.glow,
                        activeTrackColor = palette.accent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                    ),
                )
            }

            GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Caption("Ketuk atau geser untuk membaca suhu")
                    SliceHeatmap(field = field, grid = grid, axis = axis, position = position)
                    Legend()
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * Orthographic 3D view: room wireframe, the slice drawn as heatmap quads in place, and the sensors.
 * Drag horizontally to spin, vertically to tilt.
 */
@Composable
private fun RoomCube(
    field: RoomField,
    grid: Array<DoubleArray>,
    axis: SliceAxis,
    position: Float,
    modifier: Modifier = Modifier,
) {
    var yaw by remember { mutableFloatStateOf(-0.6f) }
    var pitch by remember { mutableFloatStateOf(0.45f) }
    val path = remember { Path() }

    Canvas(
        modifier.pointerInput(Unit) {
            detectDragGestures { change, drag ->
                change.consume()
                yaw += drag.x * 0.01f
                pitch = (pitch + drag.y * 0.01f).coerceIn(-1.3f, 1.3f)
            }
        },
    ) {
        val diagonal = sqrt(ROOM_WIDTH * ROOM_WIDTH + ROOM_DEPTH * ROOM_DEPTH + ROOM_HEIGHT * ROOM_HEIGHT)
        val scale = size.minDimension / diagonal * 0.95f
        val cy = cos(yaw); val sy = sin(yaw)
        val cp = cos(pitch); val sp = sin(pitch)

        // Normalised room point -> screen. World: X = width, Y = height (up), Z = depth.
        fun project(x: Float, y: Float, z: Float): Offset {
            val wx = (x - 0.5f) * ROOM_WIDTH
            val wy = (z - 0.5f) * ROOM_HEIGHT
            val wz = (y - 0.5f) * ROOM_DEPTH
            val rx = wx * cy + wz * sy
            val rz = -wx * sy + wz * cy
            val ry = wy * cp - rz * sp
            return Offset(center.x + rx * scale, center.y - ry * scale)
        }

        fun planePoint(u: Float, v: Float): Offset {
            val (x, y, z) = axis.toRoom(position, u, v)
            return project(x, y, z)
        }

        // Slice plane, one quad per cell.
        val n = grid.size
        for (row in 0 until n) {
            val vTop = 1f - row.toFloat() / n
            val vBottom = 1f - (row + 1f) / n
            for (col in 0 until n) {
                val uLeft = col.toFloat() / n
                val uRight = (col + 1f) / n
                path.reset()
                planePoint(uLeft, vBottom).let { path.moveTo(it.x, it.y) }
                planePoint(uRight, vBottom).let { path.lineTo(it.x, it.y) }
                planePoint(uRight, vTop).let { path.lineTo(it.x, it.y) }
                planePoint(uLeft, vTop).let { path.lineTo(it.x, it.y) }
                path.close()
                drawPath(path, heatColor(grid[row][col]).copy(alpha = 0.78f))
            }
        }

        // Room wireframe: corner i has bits (x, y, z); an edge flips exactly one bit.
        fun corner(i: Int) = project((i shr 2 and 1).toFloat(), (i shr 1 and 1).toFloat(), (i and 1).toFloat())
        for (i in 0 until 8) {
            for (bit in 0 until 3) {
                val j = i or (1 shl bit)
                if (j != i) drawLine(Color.White.copy(alpha = 0.4f), corner(i), corner(j), 1.5.dp.toPx())
            }
        }

        // Sensors on top.
        for (s in field.sensors) {
            val p = project(s.x.toFloat(), s.y.toFloat(), s.z.toFloat())
            drawCircle(heatColor(s.celsius), radius = 5.dp.toPx(), center = p)
            drawCircle(Color.White, radius = 5.dp.toPx(), center = p, style = Stroke(1.5.dp.toPx()))
        }
    }
}

/** Flat view of the current slice. Tap or drag to probe a point. */
@Composable
private fun SliceHeatmap(
    field: RoomField,
    grid: Array<DoubleArray>,
    axis: SliceAxis,
    position: Float,
) {
    // Normalised (u, v) of the probe, v measured from the bottom.
    var probe by remember { mutableStateOf<Offset?>(null) }
    val plane = axis.planeSize()

    Box(Modifier.fillMaxWidth().aspectRatio(plane.width / plane.height)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectTapGestures { probe = Offset(it.x / size.width, 1f - it.y / size.height) }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        probe = Offset(
                            (change.position.x / size.width).coerceIn(0f, 1f),
                            (1f - change.position.y / size.height).coerceIn(0f, 1f),
                        )
                    }
                },
        ) {
            val n = grid.size
            val cw = size.width / n
            val ch = size.height / n
            for (row in 0 until n) {
                for (col in 0 until n) {
                    // +1px overlap hides hairline seams between cells.
                    drawRect(
                        color = heatColor(grid[row][col]),
                        topLeft = Offset(col * cw, row * ch),
                        size = Size(cw + 1f, ch + 1f),
                    )
                }
            }
            probe?.let {
                val p = Offset(it.x * size.width, (1f - it.y) * size.height)
                drawLine(Color.White.copy(alpha = 0.6f), Offset(p.x, 0f), Offset(p.x, size.height), 1.dp.toPx())
                drawLine(Color.White.copy(alpha = 0.6f), Offset(0f, p.y), Offset(size.width, p.y), 1.dp.toPx())
                drawCircle(Color.White, radius = 6.dp.toPx(), center = p, style = Stroke(2.dp.toPx()))
            }
        }

        probe?.let {
            val celsius = field.celsiusOnSlice(axis, position.toDouble(), it.x.toDouble(), it.y.toDouble())
            Text(
                text = "${celsius.format(1)}°C  ·  ${(it.x * plane.width).toDouble().format(1)} m, " +
                    "${(it.y * plane.height).toDouble().format(1)} m",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun Legend() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(List(12) { heatColor(SCALE_MIN + it * (SCALE_MAX - SCALE_MIN) / 11) })),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Caption("${SCALE_MIN.format(0)}°C")
            Caption("${SCALE_MAX.format(0)}°C")
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali", tint = accent)
        }
        Text(
            text = "Irisan 3D ruangan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun AxisChip(text: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
        modifier = Modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = if (selected) 0.35f else 0.1f))
            .border(1.dp, accent.copy(alpha = if (selected) 0.8f else 0.25f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun Caption(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
}
