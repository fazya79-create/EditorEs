package com.editor.es.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.editor.es.R
import com.editor.es.data.AndroidProjectCreator
import com.editor.es.data.AndroidProjectRequest
import com.editor.es.data.ProjectCreator
import com.editor.es.data.ProjectLanguage
import com.editor.es.ui.components.EditorEsButton
import com.editor.es.ui.theme.EditorEsPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ErrorColor = Color(0xFFEF6767)
private val TabInactive = Color(0xFF0E2B35)
private val FieldShape = RoundedCornerShape(14.dp)
private const val MinSdkFloor = 21
private const val SdkCeiling = 36

private enum class CreateTab(val label: String) {
    CMake("CMake"),
    Android("Android")
}

@Composable
fun CreateProjectDialog(onClose: () -> Unit, onCreated: (String) -> Unit) {
    var tab by remember { mutableStateOf(CreateTab.CMake) }
    var creating by remember { mutableStateOf(false) }

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
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (entry in CreateTab.entries) {
                    TabChip(
                        label = entry.label,
                        selected = entry == tab,
                        onClick = { if (!creating) tab = entry }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            when (tab) {
                CreateTab.CMake -> CMakeForm(
                    creating = creating,
                    onCreatingChange = { creating = it },
                    onCreated = onCreated
                )
                CreateTab.Android -> AndroidForm(
                    creating = creating,
                    onCreatingChange = { creating = it },
                    onCreated = onCreated
                )
            }
        }
    }
}

@Composable
private fun CMakeForm(
    creating: Boolean,
    onCreatingChange: (Boolean) -> Unit,
    onCreated: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var libraryName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column {
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
                onCreatingChange(true)
                val result = withContext(Dispatchers.IO) {
                    ProjectCreator.create(folderName = folderName, libraryName = libraryName)
                }
                onCreatingChange(false)
                result.fold(onSuccess = onCreated, onFailure = { error = it.message })
            }
        }
    }
}

@Composable
private fun AndroidForm(
    creating: Boolean,
    onCreatingChange: (Boolean) -> Unit,
    onCreated: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var appName by remember { mutableStateOf("app") }
    var packageName by remember { mutableStateOf("com.example.app") }
    var packageEdited by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf(ProjectLanguage.Kotlin) }
    var withNative by remember { mutableStateOf(false) }
    var compileSdk by remember { mutableStateOf(SdkCeiling) }
    var minSdk by remember { mutableStateOf(24) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .heightIn(max = 380.dp)
            .verticalScroll(rememberScrollState())
    ) {
        FieldLabel(text = "Native code")
        Spacer(modifier = Modifier.height(8.dp))
        DropdownField(
            value = if (withNative) "With CMake / NDK" else "No native code",
            options = listOf("No native code", "With CMake / NDK"),
            enabled = !creating,
            onSelect = { withNative = it == "With CMake / NDK" }
        )
        Spacer(modifier = Modifier.height(14.dp))

        FieldLabel(text = "App name")
        Spacer(modifier = Modifier.height(8.dp))
        ProjectTextField(value = appName, isError = false) { value ->
            appName = value
            error = null
            if (!packageEdited) {
                packageName = AndroidProjectCreator.derivePackage(value)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        FieldLabel(text = "Package name")
        Spacer(modifier = Modifier.height(8.dp))
        ProjectTextField(value = packageName, isError = error != null) { value ->
            packageName = value
            packageEdited = true
            error = null
        }
        Spacer(modifier = Modifier.height(14.dp))

        FieldLabel(text = "Language")
        Spacer(modifier = Modifier.height(8.dp))
        DropdownField(
            value = language.label,
            options = ProjectLanguage.entries.map { it.label },
            enabled = !creating,
            onSelect = { picked ->
                language = ProjectLanguage.entries.first { it.label == picked }
            }
        )
        Spacer(modifier = Modifier.height(14.dp))

        FieldLabel(text = "Target SDK")
        Spacer(modifier = Modifier.height(8.dp))
        DropdownField(
            value = compileSdk.toString(),
            options = (SdkCeiling downTo MinSdkFloor).map { it.toString() },
            enabled = !creating,
            onSelect = { picked ->
                compileSdk = picked.toInt()
                if (minSdk > compileSdk) minSdk = compileSdk
            }
        )
        Spacer(modifier = Modifier.height(14.dp))

        FieldLabel(text = "Minimum SDK")
        Spacer(modifier = Modifier.height(8.dp))
        DropdownField(
            value = minSdk.toString(),
            options = (MinSdkFloor..compileSdk).map { it.toString() },
            enabled = !creating,
            onSelect = { picked -> minSdk = picked.toInt() }
        )
        Spacer(modifier = Modifier.height(14.dp))

        FieldLabel(text = stringResource(R.string.folder_name))
        Spacer(modifier = Modifier.height(8.dp))
        ProjectTextField(value = folderName, isError = error != null) {
            folderName = it
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
                onCreatingChange(true)
                val result = withContext(Dispatchers.IO) {
                    AndroidProjectCreator.create(
                        AndroidProjectRequest(
                            folderName = folderName,
                            appName = appName,
                            packageName = packageName,
                            language = language,
                            compileSdk = compileSdk,
                            minSdk = minSdk,
                            withNative = withNative
                        )
                    )
                }
                onCreatingChange(false)
                result.fold(onSuccess = onCreated, onFailure = { error = it.message })
            }
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) EditorEsPalette.teal else TabInactive)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color(0xFF07191E) else EditorEsPalette.textSecondary
        )
    }
}

@Composable
private fun DropdownField(
    value: String,
    options: List<String>,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FieldShape)
                .background(EditorEsPalette.buttonSecondaryBackground)
                .clickable(enabled = enabled) { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                fontSize = 14.sp,
                color = EditorEsPalette.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(R.drawable.chevron_down),
                contentDescription = null,
                tint = EditorEsPalette.textSecondary
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF0A222B))
        ) {
            for (option in options) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            fontSize = 13.sp,
                            color = EditorEsPalette.textPrimary
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
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
