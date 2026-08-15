package com.editor.es.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val FileTypeJson: ImageVector by lazy {
    ImageVector.Builder(
        name = "FileTypeJson",
        defaultWidth = 960f,
        defaultHeight = 960f,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color(0xFFF9A825)), pathFillType = NonZero) {
            moveTo(560f, -160f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(120f)
            quadToRelative(17f, 0f, 28.5f, -11.5f)
            reflectiveQuadTo(720f, -280f)
            verticalLineToRelative(-80f)
            quadToRelative(0f, -38f, 22f, -69f)
            reflectiveQuadToRelative(58f, -44f)
            verticalLineToRelative(-14f)
            quadToRelative(-36f, -13f, -58f, -44f)
            reflectiveQuadToRelative(-22f, -69f)
            verticalLineToRelative(-80f)
            quadToRelative(0f, -17f, -11.5f, -28.5f)
            reflectiveQuadTo(680f, -720f)
            horizontalLineTo(560f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(120f)
            quadToRelative(50f, 0f, 85f, 35f)
            reflectiveQuadToRelative(35f, 85f)
            verticalLineToRelative(80f)
            quadToRelative(0f, 17f, 11.5f, 28.5f)
            reflectiveQuadTo(840f, -560f)
            horizontalLineToRelative(40f)
            verticalLineToRelative(160f)
            horizontalLineToRelative(-40f)
            quadToRelative(-17f, 0f, -28.5f, 11.5f)
            reflectiveQuadTo(800f, -360f)
            verticalLineToRelative(80f)
            quadToRelative(0f, 50f, -35f, 85f)
            reflectiveQuadToRelative(-85f, 35f)
            close()
            moveToRelative(-280f, 0f)
            quadToRelative(-50f, 0f, -85f, -35f)
            reflectiveQuadToRelative(-35f, -85f)
            verticalLineToRelative(-80f)
            quadToRelative(0f, -17f, -11.5f, -28.5f)
            reflectiveQuadTo(120f, -400f)
            horizontalLineTo(80f)
            verticalLineToRelative(-160f)
            horizontalLineToRelative(40f)
            quadToRelative(17f, 0f, 28.5f, -11.5f)
            reflectiveQuadTo(160f, -600f)
            verticalLineToRelative(-80f)
            quadToRelative(0f, -50f, 35f, -85f)
            reflectiveQuadToRelative(85f, -35f)
            horizontalLineToRelative(120f)
            verticalLineToRelative(80f)
            horizontalLineTo(280f)
            quadToRelative(-17f, 0f, -28.5f, 11.5f)
            reflectiveQuadTo(240f, -680f)
            verticalLineToRelative(80f)
            quadToRelative(0f, 38f, -22f, 69f)
            reflectiveQuadToRelative(-58f, 44f)
            verticalLineToRelative(14f)
            quadToRelative(36f, 13f, 58f, 44f)
            reflectiveQuadToRelative(22f, 69f)
            verticalLineToRelative(80f)
            quadToRelative(0f, 17f, 11.5f, 28.5f)
            reflectiveQuadTo(280f, -240f)
            horizontalLineToRelative(120f)
            verticalLineToRelative(80f)
            close()
        }
    }.build()
}
