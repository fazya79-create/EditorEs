package com.editor.es.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.editor.es.R
import com.editor.es.ui.components.EditorEsButton
import com.editor.es.ui.theme.EditorEsPalette

@Composable
fun ProjectCreatedDialog(path: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        DialogCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = EditorEsPalette.mint,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.project_created),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = EditorEsPalette.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = path,
                    fontSize = 12.sp,
                    color = EditorEsPalette.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(22.dp))
                EditorEsButton(
                    primary = true,
                    label = stringResource(R.string.ok),
                    icon = Icons.Outlined.Check,
                    onClick = onDismiss
                )
            }
        }
    }
}
