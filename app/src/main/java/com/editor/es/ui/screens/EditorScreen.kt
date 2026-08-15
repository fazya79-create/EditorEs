package com.editor.es.ui.screens

import androidx.compose.runtime.Composable
import com.editor.es.editor.SoraEditorScreen
import java.io.File

@Composable
fun EditorScreen(filePath: String, onBack: () -> Unit) {
    SoraEditorScreen(file = File(filePath), onBack = onBack)
}
