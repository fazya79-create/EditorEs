package com.editor.es.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val FileTypeCpp: ImageVector by lazy {
    ImageVector.Builder(
        name = "FileTypeCpp",
        defaultWidth = 32f,
        defaultHeight = 32f,
        viewportWidth = 32f,
        viewportHeight = 32f
    ).apply {
        path(fill = SolidColor(Color(0xFF0288D1)), pathFillType = NonZero) {
            moveTo(28f, 14f)
            verticalLineToRelative(-4f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(4f)
            horizontalLineToRelative(-6f)
            verticalLineToRelative(-4f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(4f)
            horizontalLineToRelative(-4f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(4f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(-4f)
            horizontalLineToRelative(6f)
            verticalLineToRelative(4f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(-4f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(-2f)
            close()
        }
        path(fill = SolidColor(Color(0xFF0288D1)), pathFillType = NonZero) {
            moveTo(13.563f, 22f)
            arcTo(5.57f, 5.57f, 0f, 0f, 1f, true, true)
            verticalLineToRelative(-2.873f)
            arcTo(5.57f, 5.57f, 0f, 0f, 1f, true, true)
            horizontalLineTo(18f)
            verticalLineTo(2f)
            horizontalLineToRelative(-4.437f)
            arcTo(11.563f, 11.563f, 0f, 0f, 0f, true, true)
            verticalLineToRelative(2.873f)
            arcTo(11.564f, 11.564f, 0f, 0f, 0f, true, true)
            horizontalLineTo(18f)
            verticalLineToRelative(-6f)
            close()
        }
    }.build()
}
