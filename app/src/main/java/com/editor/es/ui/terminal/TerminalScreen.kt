package com.editor.es.ui.terminal

import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.editor.es.R
import com.editor.es.ui.theme.SpringGreen
import com.termux.terminal.TerminalColors
import com.termux.terminal.TerminalSession
import com.termux.terminal.TextStyle
import com.termux.view.TerminalView
import kotlinx.coroutines.delay

private val TerminalBackground = Color(0xFF0A2129)
private val HeaderBackground = Color(0xFF0E2A33)
private val KeyBackground = Color(0xFF0F2830)
private val KeyBorder = Color(0x3302F5A1)
private val KeyForeground = Color(0xFFD9F3E6)
private val ArmedBackground = Color(0x2902F5A1)
private val KeyShape = RoundedCornerShape(10.dp)
internal val TerminalTextSize = 13
private const val MinTerminalTextSize = 8
private const val MaxTerminalTextSize = 24
private const val HoldDelayMs = 350L
private const val RepeatIntervalMs = 60L

private enum class ModifierKey {
    Ctrl,
    Alt
}

private data class TerminalKey(
    val label: String,
    val payload: String? = null,
    val modifier: ModifierKey? = null,
    val repeatable: Boolean = false
)

private val FirstRow = listOf(
    TerminalKey("ESC", "\u001b"),
    TerminalKey("/", "/"),
    TerminalKey("-", "-"),
    TerminalKey("HOME", "\u001b[H", repeatable = true),
    TerminalKey("▲", "\u001b[A", repeatable = true),
    TerminalKey("END", "\u001b[F", repeatable = true),
    TerminalKey("PGUP", "\u001b[5~", repeatable = true)
)

private val SecondRow = listOf(
    TerminalKey("TAB", "\t"),
    TerminalKey("CTRL", modifier = ModifierKey.Ctrl),
    TerminalKey("ALT", modifier = ModifierKey.Alt),
    TerminalKey("◀", "\u001b[D", repeatable = true),
    TerminalKey("▼", "\u001b[B", repeatable = true),
    TerminalKey("▶", "\u001b[C", repeatable = true),
    TerminalKey("PGDN", "\u001b[6~", repeatable = true)
)

private fun buildEnv(home: String): Array<String> = arrayOf(
    "TERM=xterm-256color",
    "HOME=$home",
    "PATH=/system/bin:/system/xbin:/vendor/bin",
    "LANG=C.UTF-8"
)

private fun applyColorScheme() {
    runCatching {
        val colors = TerminalColors.COLOR_SCHEME.mDefaultColors
        colors[TextStyle.COLOR_INDEX_FOREGROUND] = 0xFFDDF5EA.toInt()
        colors[TextStyle.COLOR_INDEX_BACKGROUND] = 0xFF0A2129.toInt()
        colors[TextStyle.COLOR_INDEX_CURSOR] = 0xFF02F5A1.toInt()
    }
}

private fun writeText(session: TerminalSession?, text: String) {
    if (session == null) return
    val bytes = text.toByteArray(Charsets.UTF_8)
    session.write(bytes, 0, bytes.size)
}

@Composable
fun TerminalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var ctrlArmed by remember { mutableStateOf(false) }
    var altArmed by remember { mutableStateOf(false) }
    var terminalTextSize by remember { mutableStateOf(TerminalTextSize) }
    var appliedTextSize by remember { mutableStateOf(TerminalTextSize) }
    var terminalView by remember { mutableStateOf<TerminalView?>(null) }
    var sessionClient by remember { mutableStateOf<EditorEsSessionClient?>(null) }
    var sessionRef by remember { mutableStateOf<TerminalSession?>(null) }

    remember { applyColorScheme() }

    BackHandler { onBack() }

    DisposableEffect(Unit) {
        onDispose {
            sessionRef?.finishIfRunning()
        }
    }

    fun sendKey(key: TerminalKey) {
        when (key.modifier) {
            ModifierKey.Ctrl -> ctrlArmed = !ctrlArmed
            ModifierKey.Alt -> altArmed = !altArmed
            null -> {
                val payload = key.payload ?: return
                val session = terminalView?.currentSession
                if (ctrlArmed && payload.length == 1 && payload[0].isLetter()) {
                    val code = (payload.lowercase()[0] - 'a' + 1).toByte()
                    session?.write(byteArrayOf(code), 0, 1)
                } else {
                    writeText(session, payload)
                }
                ctrlArmed = false
                altArmed = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .systemBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderBackground)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = KeyForeground,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = stringResource(R.string.terminal).uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = SpringGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { writeText(terminalView?.currentSession, "\u001b[2J\u001b[H") }) {
                Icon(
                    imageVector = Icons.Outlined.DeleteSweep,
                    contentDescription = stringResource(R.string.clear_terminal),
                    tint = KeyForeground,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = {
                    val view = terminalView ?: return@IconButton
                    val client = sessionClient ?: return@IconButton
                    sessionRef?.finishIfRunning()
                    val session = TerminalSession(
                        "/system/bin/sh",
                        context.filesDir.absolutePath,
                        emptyArray(),
                        buildEnv(context.filesDir.absolutePath),
                        null,
                        client
                    )
                    view.attachSession(session)
                    sessionRef = session
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = stringResource(R.string.restart_terminal),
                    tint = KeyForeground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    TerminalView(ctx, null).apply {
                        isFocusable = true
                        isFocusableInTouchMode = true
                        setTerminalViewClient(
                            EditorEsViewClient(
                                context = ctx,
                                view = this,
                                ctrlArmed = { ctrlArmed },
                                altArmed = { altArmed },
                                shiftArmed = { false },
                                onKeyConsumed = {
                                    ctrlArmed = false
                                    altArmed = false
                                },
                                onZoom = { zoomIn ->
                                    val delta = if (zoomIn) 1 else -1
                                    terminalTextSize = (terminalTextSize + delta)
                                        .coerceIn(MinTerminalTextSize, MaxTerminalTextSize)
                                }
                            )
                        )
                        val client = EditorEsSessionClient(ctx, this)
                        sessionClient = client
                        setTextSize(terminalTextSize)
                        setTypeface(
                            runCatching {
                                Typeface.createFromAsset(ctx.assets, "fonts/JetBrainsMono-Regular.ttf")
                            }.getOrDefault(Typeface.MONOSPACE)
                        )
                        val session = TerminalSession(
                            "/system/bin/sh",
                            ctx.filesDir.absolutePath,
                            emptyArray(),
                            buildEnv(ctx.filesDir.absolutePath),
                            null,
                            client
                        )
                        attachSession(session)
                        sessionRef = session
                        terminalView = this
                        requestFocus()
                    }
                },
                update = { view ->
                    if (appliedTextSize != terminalTextSize) {
                        view.setTextSize(terminalTextSize)
                        appliedTextSize = terminalTextSize
                    }
                }
            )
        }
        Column(modifier = Modifier.background(HeaderBackground)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 4.dp),
                content = {
                    FirstRow.forEach { key ->
                        TerminalKeyChip(
                            key = key,
                            armed = (key.modifier == ModifierKey.Ctrl && ctrlArmed) ||
                                (key.modifier == ModifierKey.Alt && altArmed),
                            onTap = { sendKey(key) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                content = {
                    SecondRow.forEach { key ->
                        TerminalKeyChip(
                            key = key,
                            armed = (key.modifier == ModifierKey.Ctrl && ctrlArmed) ||
                                (key.modifier == ModifierKey.Alt && altArmed),
                            onTap = { sendKey(key) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun TerminalKeyChip(
    key: TerminalKey,
    armed: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .height(30.dp)
            .graphicsLayer {
                val scale = if (pressed) 0.9f else 1f
                scaleX = scale
                scaleY = scale
            }
            .clip(KeyShape)
            .background(if (armed) ArmedBackground else KeyBackground)
            .border(1.dp, if (armed) SpringGreen else KeyBorder, KeyShape)
            .pointerInput(key) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onTap()
                        try {
                            if (key.repeatable) {
                                delay(HoldDelayMs)
                                while (true) {
                                    onTap()
                                    delay(RepeatIntervalMs)
                                }
                            } else {
                                tryAwaitRelease()
                            }
                        } finally {
                            pressed = false
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key.label,
            fontSize = 9.5.sp,
            fontWeight = if (armed) FontWeight.Bold else FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
            color = if (armed) SpringGreen else KeyForeground,
            maxLines = 1
        )
    }
}
