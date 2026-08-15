package com.editor.es.editor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.getComponent

@Composable
fun EditorPane(
    onEditorCreated: (CodeEditor) -> Unit,
    onEditorReleased: () -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { viewContext ->
            TextMateSetup.ensureInitialized(viewContext)
            CodeEditor(viewContext).apply {
                EditorTheme.apply(this, viewContext)
                textSizePx = 15f
                getComponent<EditorAutoCompletion>().setEnabledAnimation(true)
            }.also(onEditorCreated)
        },
        onRelease = { editor ->
            onEditorReleased()
            runCatching { editor.release() }
        }
    )
}
