package my.openlab.roomy.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import my.openlab.roomy.domain.model.Comfort

/** Four colours per comfort band. Everything on screen is tinted from these. */
@Immutable
data class ComfortPalette(
    val deep: Color,
    val mid: Color,
    val accent: Color,
    val glow: Color,
)

val ColdPalette = ComfortPalette(
    deep = Color(0xFF04101F),
    mid = Color(0xFF10386B),
    accent = Color(0xFF4EC5F1),
    glow = Color(0xFF7FE9FF),
)

val ComfortablePalette = ComfortPalette(
    deep = Color(0xFF03120C),
    mid = Color(0xFF0C4F38),
    accent = Color(0xFF37D399),
    glow = Color(0xFF8CFFCB),
)

val HotPalette = ComfortPalette(
    deep = Color(0xFF1A0505),
    mid = Color(0xFF6B1B18),
    accent = Color(0xFFFF7043),
    glow = Color(0xFFFFC07A),
)

val NeutralPalette = ComfortPalette(
    deep = Color(0xFF0A0C12),
    mid = Color(0xFF262B3A),
    accent = Color(0xFF8A93AB),
    glow = Color(0xFFC3CADB),
)

fun Comfort.palette(): ComfortPalette = when (this) {
    Comfort.COLD -> ColdPalette
    Comfort.COMFORTABLE -> ComfortablePalette
    Comfort.HOT -> HotPalette
}

fun Comfort.label(): String = when (this) {
    Comfort.COLD -> "Dingin"
    Comfort.COMFORTABLE -> "Nyaman"
    Comfort.HOT -> "Panas"
}

private val RoomyColorScheme = darkColorScheme(
    primary = Color(0xFF37D399),
    onPrimary = Color(0xFF03120C),
    background = Color(0xFF05070C),
    onBackground = Color(0xFFEDF1F7),
    surface = Color(0xFF0C1018),
    onSurface = Color(0xFFEDF1F7),
    error = Color(0xFFFF6B6B),
)

@Composable
fun RoomyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RoomyColorScheme, content = content)
}
