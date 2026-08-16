package com.editor.es.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.editor.es.R
import com.editor.es.data.ProjectCreator
import com.editor.es.ui.components.EditorEsButton
import com.editor.es.ui.icons.FileTypeFolder
import com.editor.es.ui.theme.EditorEsPalette
import java.io.File

private val SelectedBackground = Color(0x330A9396)
private val ItemShape = RoundedCornerShape(12.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenProjectSheet(
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit
) {
    val projects = remember {
        ProjectCreator.baseDir()
            .listFiles { file -> file.isDirectory }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }
    var selected by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = EditorEsPalette.abyss,
        contentColor = EditorEsPalette.textPrimary
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = stringResource(R.string.open_project),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = EditorEsPalette.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ProjectCreator.baseDir().absolutePath,
                fontSize = 13.sp,
                color = EditorEsPalette.textSecondary
            )
            Spacer(modifier = Modifier.height(18.dp))
            if (projects.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_projects),
                    fontSize = 14.sp,
                    color = EditorEsPalette.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(projects, key = { it.absolutePath }) { project ->
                        ProjectRow(
                            project = project,
                            selected = selected == project.absolutePath,
                            onClick = { selected = project.absolutePath }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row {
                EditorEsButton(
                    label = stringResource(R.string.cancel),
                    icon = Icons.Outlined.Close,
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss
                )
                Spacer(modifier = Modifier.width(10.dp))
                EditorEsButton(
                    primary = true,
                    label = stringResource(R.string.ok),
                    icon = Icons.Outlined.Check,
                    modifier = Modifier.weight(1f),
                    enabled = selected != null,
                    onClick = { selected?.let(onOpen) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ProjectRow(
    project: File,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(ItemShape)
            .background(if (selected) SelectedBackground else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = FileTypeFolder,
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = project.name,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = EditorEsPalette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = EditorEsPalette.mint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
