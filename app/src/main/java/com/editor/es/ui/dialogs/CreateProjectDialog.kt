package com.editor.es.ui.dialogs

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.editor.es.R
import com.editor.es.data.ProjectCreator
import com.editor.es.ui.components.EditorEsButton
import com.editor.es.ui.theme.EditorEsPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ErrorColor = Color(0xFFEF6767)

@Composable
fun CreateProjectDialog(onClose: () -> Unit, onCreated: (String) -> Unit) {
    var folderName by remember { mutableStateOf("") }
    var libraryName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = { if (!creating) onClose() }) {
        DialogCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.create_project),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = EditorEsPalette.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { if (!creating) onClose() }) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = stringResource(R.string.close),
                        tint = EditorEsPalette.textSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            FieldLabel(text = stringResource(R.string.folder_name))
            Spacer(modifier = Modifier.height(8.dp))
            ProjectTextField(value = folderName, isError = error != null) {
                folderName = it
                error = null
            }
            Spacer(modifier = Modifier.height(14.dp))
            FieldLabel(text = stringResource(R.string.library_name))
            Spacer(modifier = Modifier.height(8.dp))
            ProjectTextField(value = libraryName, isError = error != null) {
                libraryName = it
                error = null
            }
            error?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = message, fontSize = 13.sp, color = ErrorColor)
            }
            Spacer(modifier = Modifier.height(22.dp))
            EditorEsButton(
                primary = true,
                enabled = !creating,
                label = stringResource(if (creating) R.string.creating else R.string.create),
                iconRes = R.drawable.add
            ) {
                if (creating) return@EditorEsButton
                error = null
                scope.launch {
                    creating = true
                    val result = withContext(Dispatchers.IO) {
                        ProjectCreator.create(
                            folderName = folderName,
                            libraryName = libraryName
                        )
                    }
                    creating = false
                    result.fold(onSuccess = onCreated, onFailure = { error = it.message })
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = EditorEsPalette.textSecondary
    )
}

@Composable
private fun ProjectTextField(
    value: String,
    isError: Boolean,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = isError,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = EditorEsPalette.buttonSecondaryBackground,
            unfocusedContainerColor = EditorEsPalette.buttonSecondaryBackground,
            focusedBorderColor = EditorEsPalette.teal,
            unfocusedBorderColor = EditorEsPalette.buttonSecondaryBorder,
            errorBorderColor = ErrorColor,
            cursorColor = EditorEsPalette.amber,
            errorCursorColor = ErrorColor,
            focusedTextColor = EditorEsPalette.textPrimary,
            unfocusedTextColor = EditorEsPalette.textPrimary
        )
    )
}
