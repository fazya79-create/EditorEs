package com.editor.es

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.editor.es.ui.navigation.EditorEsNavHost
import com.editor.es.ui.theme.EditorEsTheme

private val RootBrush = Brush.linearGradient(
    0f to Color(0xFF001219),
    0.5f to Color(0xFF005F73),
    0.78f to Color(0xFF0A9396),
    0.94f to Color(0xFF94D2BD),
    1f to Color(0xFFEE9B00),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EditorEsTheme {
                EditorEsRoot()
            }
        }
    }
}

@Composable
private fun EditorEsRoot() {
    Box(modifier = Modifier.fillMaxSize().background(RootBrush)) {
        EditorEsNavHost()
    }
}
