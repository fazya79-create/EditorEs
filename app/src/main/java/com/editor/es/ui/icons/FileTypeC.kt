package com.editor.es.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val FileTypeC: ImageVector by lazy {
    ImageVector.Builder(
        name = "FileTypeC",
        defaultWidth = 32.dp,
        defaultHeight = 32.dp,
        viewportWidth = 32f,
        viewportHeight = 32f
    ).apply {
        path(fill = SolidColor(Color(0xFF0288D1)), pathFillType = NonZero) {
            moveTo(19.563f, 22f)
            arcTo(5.57f, 5.57f, 0f, false, true, 14f, 16.437f)
            verticalLineToRelative(-2.873f)
            arcTo(5.57f, 5.57f, 0f, false, true, 19.563f, 8f)
            horizontalLineTo(24f)
            verticalLineTo(2f)
            horizontalLineToRelative(-4.437f)
            arcTo(11.563f, 11.563f, 0f, false, false, 8f, 13.563f)
            verticalLineToRelative(2.873f)
            arcTo(11.564f, 11.564f, 0f, false, false, 19.563f, 28f)
            horizontalLineTo(24f)
            verticalLineToRelative(-6f)
            close()
        }
    }.build()
}
