package com.editor.es.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val FileTypeCMake: ImageVector by lazy {
    ImageVector.Builder(
        name = "FileTypeCMake",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color(0xFF1E88E5)), pathFillType = NonZero) {
            moveTo(11.94f, 2.984f)
            lineTo(2.928f, 21.017f)
            lineToRelative(9.875f, -8.47f)
            close()
        }
        path(fill = SolidColor(Color(0xFFE53935)), pathFillType = NonZero) {
            moveToRelative(11.958f, 2.982f)
            lineToRelative(0.002f, 0.29f)
            lineToRelative(1.312f, 14.499f)
            lineToRelative(-0.002f, 0.006f)
            lineToRelative(0.023f, 0.26f)
            lineToRelative(7.363f, 2.978f)
            horizontalLineToRelative(0.415f)
            lineToRelative(-0.158f, -0.31f)
            lineToRelative(-0.114f, -0.228f)
            horizontalLineToRelative(-0.001f)
            lineToRelative(-8.84f, -17.494f)
            close()
        }
        path(fill = SolidColor(Color(0xFF7CB342)), pathFillType = NonZero) {
            moveToRelative(8.558f, 16.13f)
            lineToRelative(-5.627f, 4.884f)
            horizontalLineToRelative(17.743f)
            verticalLineToRelative(-0.016f)
            close()
        }
    }.build()
}
