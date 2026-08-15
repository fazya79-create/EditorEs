package com.editor.es.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val FileTypeXml: ImageVector by lazy {
    ImageVector.Builder(
        name = "FileTypeXml",
        defaultWidth = 24f,
        defaultHeight = 24f,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color(0xFF8BC34A)), pathFillType = NonZero) {
            moveTo(13f, 9f)
            horizontalLineToRelative(5.5f)
            lineTo(13f, 3.5f)
            close()
            moveTo(6f, 2f)
            horizontalLineToRelative(8f)
            lineToRelative(6f, 6f)
            verticalLineToRelative(12f)
            arcToRelative(2f, 2f, 0f, 0f, 1f, true, true)
            horizontalLineTo(6f)
            arcToRelative(2f, 2f, 0f, 0f, 1f, true, true)
            verticalLineTo(4f)
            curveToRelative(0f, -1.11f, 0.89f, -2f, 2f, -2f)
            moveToRelative(0.12f, 13.5f)
            lineToRelative(3.74f, 3.74f)
            lineToRelative(1.42f, -1.41f)
            lineToRelative(-2.33f, -2.33f)
            lineToRelative(2.33f, -2.33f)
            lineToRelative(-1.42f, -1.41f)
            close()
            moveToRelative(11.16f, 0f)
            lineToRelative(-3.74f, -3.74f)
            lineToRelative(-1.42f, 1.41f)
            lineToRelative(2.33f, 2.33f)
            lineToRelative(-2.33f, 2.33f)
            lineToRelative(1.42f, 1.41f)
            close()
        }
    }.build()
}
