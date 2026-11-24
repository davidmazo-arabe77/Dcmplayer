package com.neon.musica.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ---- Paleta neón: morado + azul ----
val NeonPurple = Color(0xFFB026FF)
val NeonBlue = Color(0xFF00D4FF)
val DeepBg = Color(0xFF0A0614)
val SurfaceDark = Color(0xFF150B2A)
val TextPrimary = Color(0xFFF2EAFF)
val TextSecondary = Color(0xFF9D8FBF)

val NeonBrush: Brush = Brush.linearGradient(listOf(NeonPurple, NeonBlue))

@Composable
fun NeonTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = NeonPurple,
        secondary = NeonBlue,
        background = DeepBg,
        surface = SurfaceDark,
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onBackground = TextPrimary,
        onSurface = TextPrimary
    )
    MaterialTheme(colorScheme = colors, content = content)
}
