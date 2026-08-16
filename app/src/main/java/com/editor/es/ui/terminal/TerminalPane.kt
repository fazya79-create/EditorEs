package com.editor.es.ui.terminal

import android.content.Context
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.editor.es.proot.ProotConfig
import com.editor.es.service.TermuxService
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import java.io.File

private val PaneBackground = Color(0xFF0A2129)
private val KeyBarBackground = Color(0xFF0E2A33)

fun projectSessionTag(projectDir: File): String = "project:${projectDir.absolutePath}"

private fun createProjectSession(
    context: Context,
    view: TerminalView,
    projectDir: File,
    tag: String,
    onShellExited: () -> Unit
): Pair<TerminalSession, Int> {
    val client = EditorEsSessionClient(context, view, onShellExited)
    TermuxService.taggedSession(tag)?.let { (id, existing) ->
        existing.updateTerminalSessionClient(client)
        return existing to id
    }
    val ubuntuReady = ProotConfig.isInstalled(context) && ProotConfig.isAvailable(context)
    val session = if (ubuntuReady) {
        ProotConfig.registerAndroidIds(context)
        ProotConfig.writeShellProfile(context)
        ProotConfig.prepareStorageMounts(context)
        runCatching {
            File(ProotConfig.rootfsDir(context), projectDir.absolutePath.trimStart('/')).mkdirs()
        }
        TerminalSession(
            ProotConfig.prootBinary(context),
            context.filesDir.absolutePath,
            ProotConfig.prootArgs(context, projectDir.absolutePath),
            ProotConfig.prootEnv(context),
            null,
            client
        )
    } else {
        TerminalSession(
            "/system/bin/sh",
            projectDir.absolutePath,
            emptyArray(),
            buildShellEnv(projectDir.absolutePath),
            null,
            client
        )
    }
    val id = TermuxService.registerTagged(context, tag, session)
    return session to id
}

@Composable
fun TerminalPane(
    projectDir: File,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tag = remember(projectDir) { projectSessionTag(projectDir) }
    var ctrlArmed by remember { mutableStateOf(false) }
    var altArmed by remember { mutableStateOf(false) }
    var view by remember { mutableStateOf<TerminalView?>(null) }
    var session by remember { mutableStateOf<TerminalSession?>(null) }

    remember {
        applyColorScheme()
        installShellProfile(context)
    }

    LaunchedEffect(view, projectDir) {
        val current = view ?: return@LaunchedEffect
        if (session != null) return@LaunchedEffect
        val (created, _) = createProjectSession(context, current, projectDir, tag) {
            TermuxService.unregisterTagged(context, tag)
            session = null
        }
        current.attachSession(created)
        session = created
    }

    DisposableEffect(Unit) {
        onDispose { view?.clearFocus() }
    }

    fun sendKey(key: TerminalKey) {
        when (key.modifier) {
            ModifierKey.Ctrl -> ctrlArmed = !ctrlArmed
            ModifierKey.Alt -> altArmed = !altArmed
            null -> {
                val payload = key.payload ?: return
                val target = view?.currentSession
                if (ctrlArmed && payload.length == 1 && payload[0].isLetter()) {
                    val code = (payload.lowercase()[0] - 'a' + 1).toByte()
                    target?.write(byteArrayOf(code), 0, 1)
                } else {
                    writeText(target, payload)
                }
                ctrlArmed = false
                altArmed = false
            }
        }
    }

    Column(modifier = modifier.background(PaneBackground)) {
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
                                onZoom = { }
                            )
                        )
                        setTextSize(TerminalTextSize)
                        setTypeface(
                            runCatching {
                                Typeface.createFromAsset(
                                    ctx.assets,
                                    "fonts/JetBrainsMono-Regular.ttf"
                                )
                            }.getOrDefault(Typeface.MONOSPACE)
                        )
                        view = this
                    }
                }
            )
        }
        Column(modifier = Modifier.background(KeyBarBackground)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 3.dp, end = 3.dp, top = 3.dp),
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
                    .padding(3.dp),
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
