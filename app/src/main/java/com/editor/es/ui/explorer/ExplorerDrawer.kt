package com.editor.es.ui.explorer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.editor.es.R
import com.editor.es.ui.icons.FileTypeFolder
import com.editor.es.ui.icons.FileTypeIcons
import java.io.File

private val SidebarBackground = Color(0xFF252526)
private val SectionForeground = Color(0xFFBBBBBB)
private val ItemForeground = Color(0xFFCCCCCC)
private val MutedForeground = Color(0xFF8B949E)
private val ActiveRowBackground = Color(0xFF37373D)
private val GuideColor = Color(0xFF3C3C3C)
private val ChevronColor = Color(0xFFE8E8E8)

enum class NodeAction {
    NewFile,
    NewFolder,
    Rename,
    Delete
}

@Composable
fun ExplorerDrawerContent(
    projectDir: File,
    explorer: ExplorerState,
    activeFilePath: String?,
    onFileClick: (File) -> Unit,
    onMenuRequested: (File) -> Unit,
    onQuickAction: (NodeAction, File) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(SidebarBackground)
            .padding(top = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.explorer_title).uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp,
                color = SectionForeground,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onQuickAction(NodeAction.NewFile, projectDir) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.NoteAdd,
                    contentDescription = stringResource(R.string.new_file),
                    tint = SectionForeground,
                    modifier = Modifier.size(17.dp)
                )
            }
            IconButton(
                onClick = { onQuickAction(NodeAction.NewFolder, projectDir) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CreateNewFolder,
                    contentDescription = stringResource(R.string.new_folder),
                    tint = SectionForeground,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = FileTypeFolder,
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = projectDir.name.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ItemForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = projectDir.absolutePath,
                    fontSize = 10.sp,
                    color = MutedForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        val rows = explorer.rows()
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(rows, key = { it.file.absolutePath + ":" + it.file.lastModified() }) { row ->
                TreeRowItem(
                    row = row,
                    expanded = row.file.absolutePath in explorer.expanded,
                    isActive = row.file.absolutePath == activeFilePath,
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
    isActive: Boolean,
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
            .height(34.dp)
            .background(if (isActive) ActiveRowBackground else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onMenu)
            .padding(start = 8.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(row.depth) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(GuideColor)
                )
            }
        }
        if (row.isDirectory) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = ChevronColor,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = chevronRotation }
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = FileTypeFolder,
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(17.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(18.dp))
            Icon(
                imageVector = FileTypeIcons.resolve(row.file.name),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = row.file.name,
            fontSize = 13.sp,
            color = if (isActive) Color.White else ItemForeground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onMenu, modifier = Modifier.size(26.dp)) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = null,
                tint = MutedForeground,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
