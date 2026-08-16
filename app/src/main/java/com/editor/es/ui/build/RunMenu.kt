package com.editor.es.ui.build

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MenuBackground = Color(0xFF0A222B)
private val TextPrimary = Color(0xFFDDF5EA)
private val IconTint = Color(0xFF6E9184)

@Composable
fun RunMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onBuild: () -> Unit,
    onCleanBuild: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(MenuBackground)
    ) {
        Entry("Build", Icons.Outlined.PlayArrow, onBuild)
        Entry("Clean build", Icons.Outlined.CleaningServices, onCleanBuild)
    }
}

@Composable
private fun Entry(label: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = label, fontSize = 13.sp, color = TextPrimary) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = IconTint,
                modifier = Modifier.size(17.dp)
            )
        },
        onClick = onClick
    )
}
