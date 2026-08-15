package com.editor.es.ui.explorer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.editor.es.R
import com.editor.es.ui.icons.FileTypeFolder
import com.editor.es.ui.icons.FileTypeIcons
import com.editor.es.ui.theme.EditorEsPalette
import java.io.File

enum class NodeAction {
    NewFile,
    NewFolder,
    Rename,
    Delete
}

private val RowShape = RoundedCornerShape(12.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExplorerDrawerContent(
    projectDir: File,
    explorer: ExplorerState,
    onFileClick: (File) -> Unit,
    onMenuRequested: (File) -> Unit,
    onQuickAction: (NodeAction, File) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EditorEsPalette.abyss)
            .padding(horizontal = 14.dp)
            .padding(top = 8.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = FileTypeFolder,
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = projectDir.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EditorEsPalette.textPrimary
                )
                Text(
                    text = projectDir.absolutePath,
                    fontSize = 10.sp,
                    color = EditorEsPalette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row {
            QuickActionButton(
                label = stringResource(R.string.new_file),
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.NoteAdd,
                        contentDescription = null,
                        tint = EditorEsPalette.mint,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.weight(1f),
                onClick = { onQuickAction(NodeAction.NewFile, projectDir) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            QuickActionButton(
                label = stringResource(R.string.new_folder),
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.CreateNewFolder,
                        contentDescription = null,
                        tint = EditorEsPalette.mint,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.weight(1f),
                onClick = { onQuickAction(NodeAction.NewFolder, projectDir) }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        val rows = explorer.rows()
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(rows, key = { _, row -> row.file.absolutePath + ":" + row.file.lastModified() }) { _, row ->
                TreeRowItem(
                    row = row,
                    expanded = row.file.absolutePath in explorer.expanded,
                    onClick = {
                        if (row.isDirectory) explorer.toggle(row.file) else onFileClick(row.file)
                    },
                    onMenu = { onMenuRequested(row.file) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TreeRowItem(
    row: TreeRow,
    expanded: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = spring(stiffness = 400f),
        label = "chevron"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (row.depth * 16).dp)
            .clip(RowShape)
            .background(EditorEsPalette.buttonSecondaryBackground.copy(alpha = 0.35f))
            .combinedClickable(onClick = onClick, onLongClick = onMenu)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (row.isDirectory) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = EditorEsPalette.textSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = chevronRotation }
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = FileTypeFolder,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(20.dp))
            Icon(
                imageVector = FileTypeIcons.resolve(row.file.name),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = row.file.name,
            fontSize = 14.sp,
            fontWeight = if (row.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
            color = EditorEsPalette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onMenu, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = null,
                tint = EditorEsPalette.textSecondary,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickActionButton(
    label: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RowShape)
            .background(EditorEsPalette.buttonSecondaryBackground)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = EditorEsPalette.buttonSecondaryContent
        )
    }
}
