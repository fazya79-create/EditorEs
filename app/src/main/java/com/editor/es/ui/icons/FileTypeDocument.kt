package com.editor.es.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val FileTypeDocument: ImageVector by lazy {
    ImageVector.Builder(
        name = "FileTypeDocument",
        defaultWidth = 24f,
        defaultHeight = 24f,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color(0xFF42A5F5)), pathFillType = NonZero) {
            moveTo(8f, 16f)
            horizontalLineToRelative(8f)
            verticalLineToRelative(2f)
            horizontalLineTo(8f)
            close()
            moveToRelative(0f, -4f)
            horizontalLineToRelative(8f)
            verticalLineToRelative(2f)
            horizontalLineTo(8f)
            close()
            moveToRelative(6f, -10f)
            horizontalLineTo(6f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(16f)
            curveToRelative(0f, 1.1f, 0.89f, 2f, 1.99f, 2f)
            horizontalLineTo(18f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(8f)
            close()
            moveToRelative(4f, 18f)
            horizontalLineTo(6f)
            verticalLineTo(4f)
            horizontalLineToRelative(7f)
            verticalLineToRelative(5f)
            horizontalLineToRelative(5f)
            close()
        }
    }.build()
}
