package com.editor.es.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class EditorEsColors(
    val abyss: Color,
    val ocean: Color,
    val teal: Color,
    val mint: Color,
    val amber: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val buttonPrimaryBackground: Color,
    val buttonPrimaryContent: Color,
    val buttonSecondaryBackground: Color,
    val buttonSecondaryBorder: Color,
    val buttonSecondaryContent: Color
)

val EditorEsPalette = EditorEsColors(
    abyss = Color(0xFF001219),
    ocean = Color(0xFF005F73),
    teal = Color(0xFF0A9396),
    mint = Color(0xFF94D2BD),
    amber = Color(0xFFEE9B00),
    textPrimary = Color(0xFFF3FAF8),
    textSecondary = Color(0xB8DCEBE6),
    buttonPrimaryBackground = Color(0xFFEE9B00),
    buttonPrimaryContent = Color(0xFF001219),
    buttonSecondaryBackground = Color(0x240A9396),
    buttonSecondaryBorder = Color(0x550A9396),
    buttonSecondaryContent = Color(0xFFE4F2EE)
)

private val EditorEsColorScheme = darkColorScheme(
    primary = Color(0xFFEE9B00),
    onPrimary = Color(0xFF001219),
    secondary = Color(0xFF94D2BD),
    tertiary = Color(0xFF0A9396),
    background = Color(0xFF001219),
    surface = Color(0xFF001219),
    onBackground = Color(0xFFF3FAF8),
    onSurface = Color(0xFFF3FAF8)
)

private val EditorEsShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun EditorEsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EditorEsColorScheme,
        shapes = EditorEsShapes,
        content = content
    )
}
