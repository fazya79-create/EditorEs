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
    val deep: Color,
    val accent: Color,
    val mist: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val buttonPrimaryBackground: Color,
    val buttonPrimaryContent: Color,
    val buttonSecondaryBackground: Color,
    val buttonSecondaryBorder: Color,
    val buttonSecondaryContent: Color
)

val EditorEsPalette = EditorEsColors(
    deep = Color(0xFF022F40),
    accent = Color(0xFF38AECC),
    mist = Color(0xFF9FD8E6),
    textPrimary = Color(0xFFF4FBFD),
    textSecondary = Color(0xB3E6F6FA),
    buttonPrimaryBackground = Color(0xFF38AECC),
    buttonPrimaryContent = Color(0xFF022F40),
    buttonSecondaryBackground = Color(0x2638AECC),
    buttonSecondaryBorder = Color(0x4D38AECC),
    buttonSecondaryContent = Color(0xFFEAF7FB)
)

private val EditorEsColorScheme = darkColorScheme(
    primary = Color(0xFF38AECC),
    onPrimary = Color(0xFF022F40),
    secondary = Color(0xFF9FD8E6),
    background = Color(0xFF022F40),
    surface = Color(0xFF022F40),
    onBackground = Color(0xFFF4FBFD),
    onSurface = Color(0xFFF4FBFD)
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
