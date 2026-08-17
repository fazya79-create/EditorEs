package com.editor.es.ui.terminal

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.editor.es.R
import com.editor.es.proot.InstallPhase
import com.editor.es.data.AppSettings
import com.editor.es.data.PreferenceSettings
import com.editor.es.proot.ProotConfig
import com.editor.es.proot.UbuntuInstaller
import com.editor.es.service.TermuxService
import com.editor.es.ui.theme.SpringGreen
import com.termux.terminal.TerminalColors
import com.termux.terminal.TerminalSession
import com.termux.terminal.TextStyle
import com.termux.view.TerminalView
import java.io.File
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TerminalBackground = Color(0xFF0A2129)
private val HeaderBackground = Color(0xFF0E2A33)
private val KeyBackground = Color(0xFF0F2830)
private val KeyBorder = Color(0x3302F5A1)
private val KeyForeground = Color(0xFFD9F3E6)
private val ArmedBackground = Color(0x2902F5A1)
private val KeyShape = RoundedCornerShape(10.dp)
private val CardShape = RoundedCornerShape(18.dp)
private val CardSurface = Color(0xFF0C242D)
internal val TerminalTextSize = 13
private const val MinTerminalTextSize = 8
private const val MaxTerminalTextSize = 24
private const val HoldDelayMs = 350L
private const val RepeatIntervalMs = 60L

internal enum class ModifierKey {
    Ctrl,
    Alt
}

private sealed class TerminalFlow {
    data object Terminal : TerminalFlow()
    data object AskInstall : TerminalFlow()
    data object Installing : TerminalFlow()
    data object AskNotification : TerminalFlow()
}

internal data class TerminalKey(
    val label: String,
    val payload: String? = null,
    val modifier: ModifierKey? = null,
    val repeatable: Boolean = false
)

internal val FirstRow = listOf(
    TerminalKey("ESC", "\u001b"),
    TerminalKey("/", "/"),
    TerminalKey("-", "-"),
    TerminalKey("HOME", "\u001b[H", repeatable = true),
    TerminalKey("▲", "\u001b[A", repeatable = true),
    TerminalKey("END", "\u001b[F", repeatable = true),
    TerminalKey("PGUP", "\u001b[5~", repeatable = true)
)

internal val SecondRow = listOf(
    TerminalKey("TAB", "\t"),
    TerminalKey("CTRL", modifier = ModifierKey.Ctrl),
    TerminalKey("ALT", modifier = ModifierKey.Alt),
    TerminalKey("◀", "\u001b[D", repeatable = true),
    TerminalKey("▼", "\u001b[B", repeatable = true),
    TerminalKey("▶", "\u001b[C", repeatable = true),
    TerminalKey("PGDN", "\u001b[6~", repeatable = true)
)

internal fun buildShellEnv(home: String): Array<String> = arrayOf(
    "TERM=xterm-256color",
    "HOME=$home",
    "PATH=/system/bin:/system/xbin:/vendor/bin",
    "LANG=C.UTF-8",
    "ENV=$home/.editor-es-shrc"
)

internal fun installShellProfile(context: Context) {
    val profile = File(context.filesDir, ".editor-es-shrc")
    val content = "clear() { printf '\\033[2J\\033[3J\\033[H'; }\n"
    if (!profile.exists() || profile.readText() != content) {
        profile.writeText(content)
    }
}

internal fun applyColorScheme() {
    runCatching {
        val colors = TerminalColors.COLOR_SCHEME.mDefaultColors
        colors[TextStyle.COLOR_INDEX_FOREGROUND] = 0xFFDDF5EA.toInt()
        colors[TextStyle.COLOR_INDEX_BACKGROUND] = 0xFF0A2129.toInt()
        colors[TextStyle.COLOR_INDEX_CURSOR] = 0xFF02F5A1.toInt()
    }
}

internal fun writeText(session: TerminalSession?, text: String) {
    if (session == null) return
    val bytes = text.toByteArray(Charsets.UTF_8)
    session.write(bytes, 0, bytes.size)
}

private fun createTerminalSession(
    context: Context,
    view: TerminalView,
    projectDir: File?,
    bootCommand: String?,
    isolation: ProotConfig.Isolation,
    onShellExited: () -> Unit
): Pair<TerminalSession, Int> {
    val client = EditorEsSessionClient(context, view, onShellExited)
    val tag = if (bootCommand != null) {
        "boot:${bootCommand.hashCode()}:${isolation.hashCode()}"
    } else {
        projectDir?.let { "project:${it.absolutePath}" }
    }
    if (tag != null) {
        TermuxService.taggedSession(tag)?.let { (id, existing) ->
            existing.updateTerminalSessionClient(client)
            return existing to id
        }
    } else {
        TermuxService.liveSession()?.let { existing ->
            existing.updateTerminalSessionClient(client)
            return existing to TermuxService.currentSessionId()
        }
    }
    val ubuntuReady = ProotConfig.isInstalled(context) && ProotConfig.isAvailable(context)
    val session = if (ubuntuReady) {
        ProotConfig.registerAndroidIds(context)
        ProotConfig.writeShellProfile(context)
        ProotConfig.prepareStorageMounts(context)
        val cwd = projectDir?.absolutePath ?: "/root"
        if (projectDir != null) {
            runCatching {
                File(ProotConfig.rootfsDir(context), cwd.trimStart('/')).mkdirs()
            }
        }
        TerminalSession(
            ProotConfig.prootBinary(context),
            context.filesDir.absolutePath,
            ProotConfig.prootArgs(context, cwd, bootCommand, isolation),
            ProotConfig.prootEnv(context),
            null,
            client
        )
    } else {
        val home = projectDir?.absolutePath ?: context.filesDir.absolutePath
        val args = if (bootCommand != null) {
            arrayOf("-c", "echo 'Ubuntu is required for this action'; exec /system/bin/sh")
        } else {
            emptyArray()
        }
        TerminalSession(
            "/system/bin/sh",
            home,
            args,
            buildShellEnv(context.filesDir.absolutePath),
            null,
            client
        )
    }
    val sessionId = if (tag != null) {
        TermuxService.registerTagged(context, tag, session)
    } else {
        TermuxService.registerSession(context, session)
    }
    return session to sessionId
}

@Composable
fun TerminalScreen(
    onBack: () -> Unit,
    projectDir: File? = null,
    initialCommand: String? = null,
    isolation: ProotConfig.Isolation = ProotConfig.Isolation.None
) {
    val context = LocalContext.current
    var ctrlArmed by remember { mutableStateOf(false) }
    var altArmed by remember { mutableStateOf(false) }
    var terminalTextSize by remember { mutableStateOf(TerminalTextSize) }
    val appliedTextSize = remember { intArrayOf(TerminalTextSize) }
    var terminalView by remember { mutableStateOf<TerminalView?>(null) }
    var sessionRef by remember { mutableStateOf<TerminalSession?>(null) }
    var previousSessionId by remember { mutableStateOf<Int?>(null) }
    val isFinishing = remember { mutableStateOf(false) }
    val localView = LocalView.current
    var flow by remember {
        mutableStateOf(
            if (ProotConfig.isInstalled(context)) TerminalFlow.Terminal else TerminalFlow.AskInstall
        )
    }

    remember {
        applyColorScheme()
        installShellProfile(context)
    }

    fun hideKeyboard() {
        val window = (localView.context as? Activity)?.window ?: return
        WindowCompat.getInsetsController(window, localView)
            .hide(WindowInsetsCompat.Type.ime())
        terminalView?.clearFocus()
    }

    val sessionTag = remember(projectDir, initialCommand, isolation) {
        if (initialCommand != null) {
            "boot:${initialCommand.hashCode()}:${isolation.hashCode()}"
        } else {
            projectDir?.let { "project:${it.absolutePath}" }
        }
    }

    fun releaseSession() {
        if (sessionTag != null) {
            TermuxService.unregisterTagged(context, sessionTag)
        } else {
            previousSessionId?.let { TermuxService.unregisterSession(context, it) }
        }
    }

    fun leaveTerminal() {
        if (!isFinishing.value) {
            isFinishing.value = true
            hideKeyboard()
            if (!AppSettings.bool(PreferenceSettings.KeepTerminalAlive, true)) {
                sessionRef?.finishIfRunning()
                releaseSession()
            }
            sessionRef = null
            previousSessionId = null
            onBack()
        }
    }

    fun terminateTerminal() {
        if (!isFinishing.value) {
            isFinishing.value = true
            hideKeyboard()
            sessionRef?.finishIfRunning()
            sessionRef = null
            releaseSession()
            previousSessionId = null
            onBack()
        }
    }

    DisposableEffect(Unit) {
        TermuxService.onExitRequested = { terminateTerminal() }
        onDispose { TermuxService.onExitRequested = null }
    }

    fun startSession(view: TerminalView) {
        val (session, sessionId) =
            createTerminalSession(context, view, projectDir, initialCommand, isolation) {
                terminateTerminal()
            }
        view.attachSession(session)
        sessionRef = session
        previousSessionId = sessionId
    }

    fun restartSession(view: TerminalView) {
        sessionRef?.finishIfRunning()
        sessionRef = null
        releaseSession()
        previousSessionId = null
        startSession(view)
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

    BackHandler {
        when (flow) {
            TerminalFlow.AskInstall -> leaveTerminal()
            TerminalFlow.Installing -> leaveTerminal()
            TerminalFlow.AskNotification -> flow = TerminalFlow.Terminal
            TerminalFlow.Terminal -> leaveTerminal()
        }
    }

    LaunchedEffect(flow, terminalView) {
        if (flow == TerminalFlow.Terminal && terminalView != null && sessionRef == null) {
            startSession(terminalView!!)
        }
    }

    if (flow == TerminalFlow.AskInstall) {
        InstallAskDialog(
            onOk = { flow = TerminalFlow.Installing },
            onCancel = { leaveTerminal() }
        )
    }

    if (flow == TerminalFlow.AskNotification) {
        NotificationPermissionGate(onDismiss = { flow = TerminalFlow.Terminal })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TerminalBackground)
                .statusBarsPadding()
                .imePadding()
                .navigationBarsPadding()
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderBackground)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { leaveTerminal() }) {
                Icon(
                    painter = painterResource(R.drawable.chevron_left),
                    contentDescription = stringResource(R.string.back),
                    tint = KeyForeground,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = projectDir?.name?.uppercase()
                    ?: stringResource(R.string.terminal).uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = SpringGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { writeText(terminalView?.currentSession, "\u001b[2J\u001b[3J\u001b[H") }) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = stringResource(R.string.clear_terminal),
                    tint = KeyForeground,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = { terminalView?.let { restartSession(it) } }
            ) {
                Icon(
                    painter = painterResource(R.drawable.restart),
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
                        setTextSize(terminalTextSize)
                        setTypeface(
                            runCatching {
                                Typeface.createFromAsset(ctx.assets, "fonts/JetBrainsMono-Regular.ttf")
                            }.getOrDefault(Typeface.MONOSPACE)
                        )
                        terminalView = this
                        requestFocus()
                    }
                },
                update = { view ->
                    if (appliedTextSize[0] != terminalTextSize) {
                        view.setTextSize(terminalTextSize)
                        appliedTextSize[0] = terminalTextSize
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

        if (flow == TerminalFlow.Installing) {
            InstallerScreen(
                onFinished = { flow = TerminalFlow.AskNotification },
                onFailed = { leaveTerminal() }
            )
        }
    }
}

@Composable
private fun InstallAskDialog(onOk: () -> Unit, onCancel: () -> Unit) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(CardSurface)
                .border(1.dp, KeyBorder, CardShape)
                .padding(22.dp)
        ) {
            Text(
                text = stringResource(R.string.ubuntu_install_title),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = SpringGreen
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.ubuntu_install_message),
                fontSize = 13.sp,
                color = KeyForeground,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(onClick = onOk) {
                    Text(stringResource(R.string.ok), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InstallerScreen(onFinished: () -> Unit, onFailed: () -> Unit) {
    val context = LocalContext.current
    val installer = remember { UbuntuInstaller(context) }
    var phase by remember { mutableStateOf<InstallPhase>(InstallPhase.Idle) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    LaunchedEffect(Unit) {
        installer.install { newPhase -> mainHandler.post { phase = newPhase } }
    }

    DisposableEffect(Unit) {
        onDispose {
            installer.cancel()
            mainHandler.removeCallbacksAndMessages(null)
        }
    }

    LaunchedEffect(phase) {
        when (phase) {
            InstallPhase.Done -> {
                delay(600)
                onFinished()
            }
            is InstallPhase.Failed -> {
                delay(1200)
                onFailed()
            }
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .clip(CardShape)
                .background(CardSurface)
                .border(1.dp, KeyBorder, CardShape)
                .padding(22.dp)
        ) {
            Text(
                text = stringResource(R.string.ubuntu_installing),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SpringGreen
            )
            Spacer(modifier = Modifier.height(16.dp))
            when (val current = phase) {
                InstallPhase.Idle -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = SpringGreen,
                        strokeCap = StrokeCap.Round
                    )
                }
                is InstallPhase.Downloading -> {
                    LinearProgressIndicator(
                        progress = { current.percent / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = SpringGreen,
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Downloading rootfs • ${current.percent}% • " +
                            "%.1f MB / %.1f MB".format(current.receivedMb, current.totalMb),
                        fontSize = 12.sp,
                        color = KeyForeground
                    )
                }
                is InstallPhase.Extracting -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = SpringGreen,
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "extracting: ${current.entry}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SpringGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${current.count} files",
                        fontSize = 12.sp,
                        color = KeyForeground
                    )
                }
                InstallPhase.Finalizing -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = SpringGreen,
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Configuring Ubuntu…",
                        fontSize = 12.sp,
                        color = KeyForeground
                    )
                }
                InstallPhase.Done -> {
                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = SpringGreen,
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Ubuntu installed ✓",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpringGreen
                    )
                }
                is InstallPhase.Failed -> {
                    Text(
                        text = current.message,
                        fontSize = 12.sp,
                        color = Color(0xFFFF6B6B),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationPermissionGate(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permissionGranted = true
        onDismiss()
    }

    LaunchedEffect(Unit) {
        delay(400)
        if (permissionGranted) onDismiss()
    }

    if (!permissionGranted) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(CardSurface)
                    .border(1.dp, KeyBorder, CardShape)
                    .padding(22.dp)
            ) {
                Text(
                    text = stringResource(R.string.notification_permission_title),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = SpringGreen
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.notification_permission_message),
                    fontSize = 13.sp,
                    color = KeyForeground,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    ) {
                        Text(stringResource(R.string.ok), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun TerminalKeyChip(
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
                coroutineScope {
                    awaitEachGesture {
                        awaitFirstDown()
                        pressed = true
                        onTap()
                        val repeatJob = if (key.repeatable) {
                            launch {
                                delay(HoldDelayMs)
                                while (true) {
                                    onTap()
                                    delay(RepeatIntervalMs)
                                }
                            }
                        } else {
                            null
                        }
                        try {
                            waitForUpOrCancellation()
                        } finally {
                            repeatJob?.cancel()
                            pressed = false
                        }
                    }
                }
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
