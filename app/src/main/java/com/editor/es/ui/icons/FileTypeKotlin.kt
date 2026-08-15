package com.editor.es.ui.icons

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val FileTypeKotlin: ImageVector by lazy {
    ImageVector.Builder(
        name = "FileTypeKotlin",
        defaultWidth = 24f,
        defaultHeight = 24f,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = Brush.linearGradient(colors = listOf(Color(0xFF7C4DFF), Color(0xFFD500F9), Color(0xFFEF5350)), start = Offset(2.8468f, 21.3788f), end = Offset(21.1225f, 2.8994f)), pathFillType = NonZero) {
            moveTo(2.975f, 2.976f)
            verticalLineToRelative(18.048f)
            horizontalLineToRelative(18.05f)
            verticalLineToRelative(-0.03f)
            lineToRelative(-4.478f, -4.511f)
            lineToRelative(-4.48f, -4.515f)
            lineToRelative(4.48f, -4.515f)
            lineToRelative(4.443f, -4.477f)
            close()
        }
    }.build()
}
