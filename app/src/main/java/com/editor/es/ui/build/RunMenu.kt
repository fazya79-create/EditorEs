package com.editor.es.ui.build

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.editor.es.build.BuildAction
import com.editor.es.build.ConfigurePreset

private val MenuBackground = Color(0xFF0A222B)
private val Accent = Color(0xFF02F5A1)
private val TextPrimary = Color(0xFFDDF5EA)
private val TextSecondary = Color(0xFF6E9184)
private val DividerColor = Color(0xFF11333F)

@Composable
fun RunMenu(
    expanded: Boolean,
    configurePresets: List<ConfigurePreset>,
    actions: List<BuildAction>,
    selected: String?,
    canDelete: (BuildAction) -> Boolean,
    onDismiss: () -> Unit,
    onSelectPreset: (String) -> Unit,
    onBuild: (BuildAction) -> Unit,
    onRebuild: (BuildAction) -> Unit,
    onReconfigure: () -> Unit,
    onDelete: (BuildAction) -> Unit,
    onNewCommand: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(MenuBackground)
    ) {
        if (configurePresets.isNotEmpty()) {
            SectionLabel("Configuration")
            for (preset in configurePresets) {
                val active = preset.name == selected
                DropdownMenuItem(
                    text = {
                        Text(
                            text = preset.label,
                            fontSize = 13.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) Accent else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        if (active) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Accent,
                                modifier = Modifier.size(17.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.width(17.dp))
                        }
                    },
                    onClick = { onSelectPreset(preset.name) }
                )
            }
            HorizontalDivider(color = DividerColor)
        }

        SectionLabel("Run")
        for (action in actions) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = action.label,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (action.isRaw) {
                            Icons.Outlined.Terminal
                        } else {
                            Icons.Outlined.PlayArrow
                        },
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!action.isRaw) {
                            IconButton(
                                onClick = { onRebuild(action) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Rebuild",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (canDelete(action)) {
                            IconButton(
                                onClick = { onDelete(action) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                },
                onClick = { onBuild(action) }
            )
        }

        HorizontalDivider(color = DividerColor)
        DropdownMenuItem(
            text = { Text("New command", fontSize = 13.sp, color = Accent) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(17.dp)
                )
            },
            onClick = onNewCommand
        )
        DropdownMenuItem(
            text = { Text("Clean reconfigure", fontSize = 13.sp, color = TextSecondary) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            },
            onClick = onReconfigure
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, top = 8.dp, bottom = 2.dp)
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = TextSecondary
        )
    }
}
