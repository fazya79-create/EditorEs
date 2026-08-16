package com.editor.es.ui.build

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.selection.SelectionContainer
import com.editor.es.ui.theme.SpringGreen

private val ConsoleBackground = Color(0xFF071A20)
private val ConsoleHeader = Color(0xFF0E2A33)
private val ConsoleBorder = Color(0x3302F5A1)
private val ConsoleForeground = Color(0xFFD4EFE2)
private val ErrorForeground = Color(0xFFFF8A80)
private val SuccessForeground = SpringGreen
private val ConsoleShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)

data class ConsoleLine(val text: String, val kind: ConsoleLineKind)

enum class ConsoleLineKind {
    Normal,
    Error,
    Success
}

enum class ConsoleTab {
    Build,
    Terminal
}

@Composable
fun BuildConsole(
    lines: List<ConsoleLine>,
    running: Boolean,
    tab: ConsoleTab,
    maximized: Boolean,
    onSelectTab: (ConsoleTab) -> Unit,
    onToggleMaximize: () -> Unit,
    onCopy: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    terminalContent: @Composable () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size, tab) {
        if (tab == ConsoleTab.Build && lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ConsoleShape)
            .background(ConsoleBackground)
            .border(1.dp, ConsoleBorder, ConsoleShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConsoleHeader)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabLabel(
                label = "BUILD",
                active = tab == ConsoleTab.Build,
                onClick = { onSelectTab(ConsoleTab.Build) }
            )
            Spacer(modifier = Modifier.width(4.dp))
            TabLabel(
                label = "TERMINAL",
                active = tab == ConsoleTab.Terminal,
                onClick = { onSelectTab(ConsoleTab.Terminal) }
            )
            Spacer(modifier = Modifier.weight(1f))
            if (running && tab == ConsoleTab.Build) {
                IconButton(onClick = onStop, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Stop,
                        contentDescription = "Stop build",
                        tint = ErrorForeground,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            if (tab == ConsoleTab.Build) {
                IconButton(onClick = onCopy, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy output",
                        tint = ConsoleForeground,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onClear, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteSweep,
                        contentDescription = "Clear console",
                        tint = ConsoleForeground,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            IconButton(onClick = onToggleMaximize, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = if (maximized) {
                        Icons.Outlined.CloseFullscreen
                    } else {
                        Icons.Outlined.OpenInFull
                    },
                    contentDescription = "Toggle size",
                    tint = ConsoleForeground,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close console",
                    tint = ConsoleForeground,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        if (running && tab == ConsoleTab.Build) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = SpringGreen,
                strokeCap = StrokeCap.Square
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (tab) {
                ConsoleTab.Terminal -> terminalContent()
                ConsoleTab.Build -> {
                    if (lines.isEmpty()) {
                        Text(
                            text = "no output yet",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ConsoleForeground.copy(alpha = 0.45f),
                            modifier = Modifier.padding(14.dp)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            items(lines) { line ->
                                SelectionContainer {
                                    Text(
                                        text = line.text,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = when (line.kind) {
                                            ConsoleLineKind.Error -> ErrorForeground
                                            ConsoleLineKind.Success -> SuccessForeground
                                            ConsoleLineKind.Normal -> ConsoleForeground
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
private fun TabLabel(label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = 1.1.sp,
            color = if (active) SpringGreen else ConsoleForeground.copy(alpha = 0.55f)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (active) SpringGreen else Color.Transparent)
        )
    }
}
