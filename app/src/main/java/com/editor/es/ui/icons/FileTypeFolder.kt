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
        defaultWidth = 16.dp,
        defaultHeight = 16.dp,
        viewportWidth = 16f,
        viewportHeight = 16f
    ).apply {
        path(fill = SolidColor(Color(0xFF4CAF50)), pathFillType = NonZero) {
            moveToRelative(6.922f, 3.768f)
            lineToRelative(-0.644f, -0.536f)
            arcTo(1f, 1f, 0f, false, false, 5.638f, 3f)
            horizontalLineTo(2f)
            arcToRelative(1f, 1f, 0f, false, false, -1f, 1f)
            verticalLineToRelative(8f)
            arcToRelative(1f, 1f, 0f, false, false, 1f, 1f)
            horizontalLineToRelative(12f)
            arcToRelative(1f, 1f, 0f, false, false, 1f, -1f)
            verticalLineTo(5f)
            arcToRelative(1f, 1f, 0f, false, false, -1f, -1f)
            horizontalLineTo(7.562f)
            arcToRelative(1f, 1f, 0f, false, true, -0.64f, -0.232f)
        }
        path(fill = SolidColor(Color(0xFFC8E6C9)), pathFillType = NonZero) {
            moveTo(9.225f, 15f)
            arcToRelative(0.5f, 0.5f, 0f, false, true, -0.12f, -0.014f)
            arcToRelative(0.57f, 0.568f, 0f, false, true, -0.414f, -0.661f)
            lineToRelative(1.549f, -7.872f)
            arcToRelative(0.566f, 0.565f, 0f, false, true, 0.254f, -0.372f)
            arcToRelative(0.53f, 0.53f, 0f, false, true, 0.4f, -0.067f)
            arcToRelative(0.57f, 0.57f, 0f, false, true, 0.415f, 0.662f)
            lineToRelative(-1.552f, 7.872f)
            arcToRelative(0.56f, 0.56f, 0f, false, true, -0.253f, 0.371f)
            arcToRelative(0.53f, 0.53f, 0f, false, true, -0.28f, 0.081f)
            moveToRelative(3.105f, -1f)
            horizontalLineToRelative(-0.038f)
            arcToRelative(0.54f, 0.54f, 0f, false, true, -0.382f, -0.206f)
            arcToRelative(0.583f, 0.582f, 0f, false, true, 0.057f, -0.774f)
            lineToRelative(2.664f, -2.483f)
            lineToRelative(-2.653f, -2.312f)
            arcToRelative(0.583f, 0.582f, 0f, false, true, -0.08f, -0.772f)
            arcToRelative(0.54f, 0.54f, 0f, false, true, 0.377f, -0.218f)
            arcToRelative(0.53f, 0.53f, 0f, false, true, 0.406f, 0.129f)
            lineToRelative(3.126f, 2.727f)
            arcToRelative(0.579f, 0.578f, 0f, false, true, 0.002f, 0.862f)
            lineToRelative(-3.114f, 2.904f)
            arcToRelative(0.536f, 0.535f, 0f, false, true, -0.365f, 0.144f)
            close()
            moveToRelative(-4.661f, 0f)
            arcToRelative(0.536f, 0.535f, 0f, false, true, -0.365f, -0.146f)
            lineTo(4.186f, 10.95f)
            arcToRelative(0.58f, 0.58f, 0f, false, true, -0.005f, -0.846f)
            lineToRelative(0.01f, -0.01f)
            lineToRelative(3.128f, -2.726f)
            arcToRelative(0.516f, 0.515f, 0f, false, true, 0.4f, -0.13f)
            arcToRelative(0.54f, 0.54f, 0f, false, true, 0.38f, 0.218f)
            arcToRelative(0.583f, 0.582f, 0f, false, true, -0.08f, 0.773f)
            lineToRelative(-2.65f, 2.31f)
            lineToRelative(2.663f, 2.482f)
            arcToRelative(0.579f, 0.578f, 0f, false, true, 0.056f, 0.774f)
            arcToRelative(0.536f, 0.535f, 0f, false, true, -0.381f, 0.206f)
            close()
        }
    }.build()
}
