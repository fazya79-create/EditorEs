package com.editor.es.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.editor.es.ui.theme.EditorEsPalette
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.getComponent
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val EditorBackground = Color(0xFF001219)
private val ErrorTint = Color(0xFFEF6767)

private sealed interface EditorSaveState {
    data object Idle : EditorSaveState
    data object Saving : EditorSaveState
    data object Saved : EditorSaveState
    data object Failed : EditorSaveState
}

@Composable
fun SoraEditorScreen(file: File, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var saveState by remember { mutableStateOf<EditorSaveState>(EditorSaveState.Idle) }
    var editorRef by remember { mutableStateOf<CodeEditor?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorBackground)
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    tint = EditorEsPalette.textPrimary
                )
            }
            Text(
                text = file.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = EditorEsPalette.textPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    val editor = editorRef ?: return@IconButton
                    if (saveState is EditorSaveState.Saving) return@IconButton
                    saveState = EditorSaveState.Saving
                    scope.launch {
                        val saved = withContext(Dispatchers.IO) {
                            runCatching {
                                file.writeText(editor.text.toString())
                            }.isSuccess
                        }
                        saveState = if (saved) EditorSaveState.Saved else EditorSaveState.Failed
                    }
                }
            ) {
                Icon(
                    imageVector = if (saveState is EditorSaveState.Saved) Icons.Outlined.Check else Icons.Outlined.Save,
                    contentDescription = null,
                    tint = when (saveState) {
                        EditorSaveState.Saved -> EditorEsPalette.mint
                        EditorSaveState.Failed -> ErrorTint
                        else -> EditorEsPalette.textPrimary
                    }
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    TextMateSetup.ensureInitialized(viewContext)
                    CodeEditor(viewContext).apply {
                        EditorTheme.apply(this, viewContext)
                        setEditorLanguage(EditorLanguageResolver.resolve(file.name))
                        textSizePx = 15f
                        getComponent<EditorAutoCompletion>().setEnabledAnimation(true)
                        runCatching { setText(file.readText()) }
                    }.also { editorRef = it }
                },
                onRelease = { editor ->
                    editorRef = null
                    runCatching { editor.release() }
                }
            )
        }
    }
    LaunchedEffect(saveState) {
        if (saveState is EditorSaveState.Saved || saveState is EditorSaveState.Failed) {
            delay(1600)
            saveState = EditorSaveState.Idle
        }
    }
}
