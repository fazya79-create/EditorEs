package com.editor.es.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val FileTypeFolder: ImageVector by lazy {
    ImageVector.Builder(
        name = "FileTypeFolder",
        defaultWidth = 16f,
        defaultHeight = 16f,
        viewportWidth = 16f,
        viewportHeight = 16f
    ).apply {
        path(fill = SolidColor(Color(0xFF4CAF50)), pathFillType = NonZero) {
            moveToRelative(6.922f, 3.768f)
            lineToRelative(-0.644f, -0.536f)
            arcTo(1f, 1f, 0f, 0f, 0f, true, true)
            horizontalLineTo(2f)
            arcToRelative(1f, 1f, 0f, 0f, 0f, true, true)
            verticalLineToRelative(8f)
            arcToRelative(1f, 1f, 0f, 0f, 0f, true, true)
            horizontalLineToRelative(12f)
            arcToRelative(1f, 1f, 0f, 0f, 0f, true, true)
            verticalLineTo(5f)
            arcToRelative(1f, 1f, 0f, 0f, 0f, true, true)
            horizontalLineTo(7.562f)
            arcToRelative(1f, 1f, 0f, 0f, 1f, true, true)
        }
        path(fill = SolidColor(Color(0xFFC8E6C9)), pathFillType = NonZero) {
            moveTo(9.225f, 15f)
            arcToRelative(0.5f, 0.5f, 0f, 0f, 1f, true, true)
            arcToRelative(0.57f, 0.568f, 0f, 0f, 1f, true, true)
            lineToRelative(1.549f, -7.872f)
            arcToRelative(0.566f, 0.565f, 0f, 0f, 1f, true, true)
            arcToRelative(0.53f, 0.53f, 0f, 0f, 1f, true, true)
            arcToRelative(0.57f, 0.57f, 0f, 0f, 1f, true, true)
            lineToRelative(-1.552f, 7.872f)
            arcToRelative(0.56f, 0.56f, 0f, 0f, 1f, true, true)
            arcToRelative(0.53f, 0.53f, 0f, 0f, 1f, true, true)
            moveToRelative(3.105f, -1f)
            horizontalLineToRelative(-0.038f)
            arcToRelative(0.54f, 0.54f, 0f, 0f, 1f, true, true)
            arcToRelative(0.583f, 0.582f, 0f, 0f, 1f, true, true)
            lineToRelative(2.664f, -2.483f)
            lineToRelative(-2.653f, -2.312f)
            arcToRelative(0.583f, 0.582f, 0f, 0f, 1f, true, true)
            arcToRelative(0.54f, 0.54f, 0f, 0f, 1f, true, true)
            arcToRelative(0.53f, 0.53f, 0f, 0f, 1f, true, true)
            lineToRelative(3.126f, 2.727f)
            arcToRelative(0.579f, 0.578f, 0f, 0f, 1f, true, true)
            lineToRelative(-3.114f, 2.904f)
            arcToRelative(0.536f, 0.535f, 0f, 0f, 1f, true, true)
            close()
            moveToRelative(-4.661f, 0f)
            arcToRelative(0.536f, 0.535f, 0f, 0f, 1f, true, true)
            lineTo(4.186f, 10.95f)
            arcToRelative(0.58f, 0.58f, 0f, 0f, 1f, true, true)
            lineToRelative(0.01f, -0.01f)
            lineToRelative(3.128f, -2.726f)
            arcToRelative(0.516f, 0.515f, 0f, 0f, 1f, true, true)
            arcToRelative(0.54f, 0.54f, 0f, 0f, 1f, true, true)
            arcToRelative(0.583f, 0.582f, 0f, 0f, 1f, true, true)
            lineToRelative(-2.65f, 2.31f)
            lineToRelative(2.663f, 2.482f)
            arcToRelative(0.579f, 0.578f, 0f, 0f, 1f, true, true)
            arcToRelative(0.536f, 0.535f, 0f, 0f, 1f, true, true)
            close()
        }
    }.build()
}
