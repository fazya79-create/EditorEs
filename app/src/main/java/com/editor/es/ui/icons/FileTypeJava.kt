package com.editor.es.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val FileTypeJava: ImageVector by lazy {
    ImageVector.Builder(
        name = "FileTypeJava",
        defaultWidth = 32f,
        defaultHeight = 32f,
        viewportWidth = 32f,
        viewportHeight = 32f
    ).apply {
        path(fill = SolidColor(Color(0xFFF44336)), pathFillType = NonZero) {
            moveTo(4f, 26f)
            horizontalLineToRelative(24f)
            verticalLineToRelative(2f)
            horizontalLineTo(4f)
            close()
            moveTo(28f, 4f)
            horizontalLineTo(7f)
            arcToRelative(1f, 1f, 0f, 0f, 0f, true, true)
            verticalLineToRelative(13f)
            arcToRelative(4f, 4f, 0f, 0f, 0f, true, true)
            horizontalLineToRelative(10f)
            arcToRelative(4f, 4f, 0f, 0f, 0f, true, true)
            verticalLineToRelative(-4f)
            horizontalLineToRelative(4f)
            arcToRelative(2f, 2f, 0f, 0f, 0f, true, true)
            verticalLineTo(6f)
            arcToRelative(2f, 2f, 0f, 0f, 0f, true, true)
            moveToRelative(0f, 8f)
            horizontalLineToRelative(-4f)
            verticalLineTo(6f)
            horizontalLineToRelative(4f)
            close()
        }
    }.build()
}
