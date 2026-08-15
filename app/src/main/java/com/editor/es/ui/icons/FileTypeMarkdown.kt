package com.editor.es.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val FileTypeMarkdown: ImageVector by lazy {
    ImageVector.Builder(
        name = "FileTypeMarkdown",
        defaultWidth = 32.dp,
        defaultHeight = 32.dp,
        viewportWidth = 32f,
        viewportHeight = 32f
    ).apply {
        path(fill = SolidColor(Color(0xFF42A5F5)), pathFillType = NonZero) {
            moveToRelative(14f, 10f)
            lineToRelative(-4f, 3.5f)
            lineTo(6f, 10f)
            horizontalLineTo(4f)
            verticalLineToRelative(12f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(-6f)
            lineToRelative(2f, 2f)
            lineToRelative(2f, -2f)
            verticalLineToRelative(6f)
            horizontalLineToRelative(4f)
            verticalLineTo(10f)
            close()
            moveToRelative(12f, 6f)
            verticalLineToRelative(-6f)
            horizontalLineToRelative(-4f)
            verticalLineToRelative(6f)
            horizontalLineToRelative(-4f)
            lineToRelative(6f, 8f)
            lineToRelative(6f, -8f)
            close()
        }
    }.build()
}
