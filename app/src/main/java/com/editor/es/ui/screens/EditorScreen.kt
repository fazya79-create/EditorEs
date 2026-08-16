package com.editor.es.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.editor.es.R
import com.editor.es.build.BuildEvent
import com.editor.es.build.BuildRunner
import com.editor.es.data.AppSettings
import com.editor.es.data.PreferenceSettings
import com.editor.es.lsp.LspManager
import com.editor.es.build.ToolchainKind
import com.editor.es.build.ToolchainPaths
import com.editor.es.data.FileOps
import com.editor.es.data.ProjectCreator
import com.editor.es.editor.EditorConfigurator
import com.editor.es.editor.EditorLanguageResolver
import com.editor.es.editor.EditorPane
import com.editor.es.ui.build.BuildConsole
import com.editor.es.ui.build.ConsoleLine
import com.editor.es.ui.build.ConsoleLineKind
import com.editor.es.ui.build.ToolchainInstallDialog
import com.editor.es.ui.dialogs.ConfirmDialog
import com.editor.es.ui.dialogs.NameInputDialog
import com.editor.es.ui.dialogs.UnsavedChangesDialog
import com.editor.es.ui.editor.SymbolBar
import com.editor.es.ui.explorer.ExplorerDrawerContent
import com.editor.es.ui.explorer.ExplorerState
import com.editor.es.ui.explorer.NodeAction
import com.editor.es.ui.explorer.NodeActionSheet
import com.editor.es.ui.icons.FileTypeIcons
import com.editor.es.ui.theme.EditorEsPalette
import com.editor.es.ui.theme.SpringGreen
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentListener
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ErrorTint = Color(0xFFEF6767)
private val DisabledTint = Color(0xFF3F5F58)
private val SidebarBackground = Color(0xFF0A222B)
private val TitleBarBackground = Color(0xFF0E2A33)
private val EditorBackground = Color(0xFF0A2129)
private val TabBarBackground = Color(0xFF0A222B)
private val TabActiveForeground = Color(0xFFF2FFFA)
private val TabInactiveForeground = Color(0xFF7FA898)
private val DirtyDot = SpringGreen
private val AccentGreen = SpringGreen
private val LspStatusColor = Color(0xFF6FD9AE)
private val LspStatusBackground = Color(0xFF08202A)
private val DrawerWidth = 220.dp
private val ConsoleHeight = 260.dp
private val DrawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
private val HamburgerBrush = Brush.linearGradient(
    colors = listOf(SpringGreen, SpringGreen.copy(alpha = 0.55f))
)

private data class TabItem(
    val path: String,
    val name: String,
    val dirty: Boolean,
    val text: String
)

private sealed interface ExplorerDialog {
    data class Menu(val target: File) : ExplorerDialog
    data class Input(val initial: String, val parent: File, val kind: NodeAction) : ExplorerDialog
    data class Delete(val target: File) : ExplorerDialog
    data class UnsavedClose(val path: String, val name: String) : ExplorerDialog
}

private sealed interface SaveState {
    data object Idle : SaveState
    data object Saving : SaveState
    data object Saved : SaveState
    data object Failed : SaveState
}

private class DirtyMarker(private val onDirty: () -> Unit) : ContentListener {
    var enabled = true
    override fun beforeReplace(content: Content) {}
    override fun afterInsert(content: Content, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, insertedContent: CharSequence) {
        if (enabled) onDirty()
    }
    override fun afterDelete(content: Content, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, deletedContent: CharSequence) {
        if (enabled) onDirty()
    }
}

@Composable
fun EditorScreen(projectPath: String) {
    val scope = rememberCoroutineScope()
    val drawerAnim = remember { Animatable(0f) }
    val density = LocalDensity.current
    val drawerWidthPx = remember(density) { with(density) { DrawerWidth.toPx() } }

    val projectDir = remember { File(projectPath) }
    val explorer = remember { ExplorerState(projectDir) }

    val tabs = remember {
        val initial = File(projectDir, ProjectCreator.MAIN_SOURCE_NAME)
        if (initial.exists()) {
            mutableStateListOf(TabItem(initial.absolutePath, initial.name, false, runCatching { initial.readText() }.getOrDefault("")))
        } else {
            mutableStateListOf<TabItem>()
        }
    }
    var activePath by remember { mutableStateOf(tabs.firstOrNull()?.path) }
    var editorRef by remember { mutableStateOf<CodeEditor?>(null) }
    var saveState by remember { mutableStateOf<SaveState>(SaveState.Idle) }
    var dialog by remember { mutableStateOf<ExplorerDialog?>(null) }

    val context = LocalContext.current
    val lspManager = remember(projectDir) { LspManager(context, projectDir) }
    val lspScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    var lspEnabled by remember { mutableStateOf(AppSettings.bool(PreferenceSettings.LspEnabled, false)) }
    var lspStatus by remember { mutableStateOf<String?>(null) }
    val buildRunner = remember { BuildRunner(context) }
    val consoleLines = remember { mutableStateListOf<ConsoleLine>() }
    var consoleVisible by remember { mutableStateOf(false) }
    var building by remember { mutableStateOf(false) }
    var showToolchainDialog by remember { mutableStateOf(false) }
    var historyRevision by remember { mutableStateOf(0) }

    val dirtyMarker = remember {
        DirtyMarker {
            historyRevision++
            val current = activePath
            if (current != null) {
                val index = tabs.indexOfFirst { it.path == current }
                if (index >= 0 && !tabs[index].dirty) {
                    tabs[index] = tabs[index].copy(dirty = true)
                }
            }
        }
    }

    fun detachListener(editor: CodeEditor) {
        runCatching { editor.text.removeContentListener(dirtyMarker) }
    }

    fun loadTabIntoEditor(editor: CodeEditor, tab: TabItem) {
        dirtyMarker.enabled = false
        detachListener(editor)
        editor.setText(tab.text)
        editor.setEditorLanguage(EditorLanguageResolver.resolve(tab.name))
        editor.text.addContentListener(dirtyMarker)
        dirtyMarker.enabled = true
        if (lspEnabled) {
            scope.launch {
                lspManager.attach(editor, File(tab.path)) { lspStatus = it }
            }
        }
    }

    fun captureActiveText() {
        val editor = editorRef ?: return
        val current = activePath ?: return
        val index = tabs.indexOfFirst { it.path == current }
        if (index >= 0) {
            tabs[index] = tabs[index].copy(text = editor.text.toString())
        }
    }

    fun openFile(file: File) {
        val path = file.absolutePath
        if (tabs.none { it.path == path }) {
            tabs.add(TabItem(path, file.name, false, runCatching { file.readText() }.getOrDefault("")))
        }
        activePath = path
        val tab = tabs.first { it.path == path }
        editorRef?.let { loadTabIntoEditor(it, tab) }
    }

    fun switchTab(path: String) {
        if (path == activePath) return
        captureActiveText()
        activePath = path
        val tab = tabs.firstOrNull { it.path == path } ?: return
        editorRef?.let { loadTabIntoEditor(it, tab) }
    }

    fun removeTab(path: String) {
        val index = tabs.indexOfFirst { it.path == path }
        if (index < 0) return
        scope.launch { lspManager.detach(path) }
        if (path == activePath) {
            val next = tabs.getOrNull(index - 1)?.path ?: tabs.getOrNull(index + 1)?.path
            tabs.removeAt(index)
            activePath = next
            val nextTab = next?.let { p -> tabs.firstOrNull { it.path == p } }
            if (nextTab != null) {
                editorRef?.let { loadTabIntoEditor(it, nextTab) }
            }
        } else {
            tabs.removeAt(index)
        }
    }

    fun saveActive() {
        val editor = editorRef ?: return
        val current = activePath ?: return
        if (saveState is SaveState.Saving) return
        saveState = SaveState.Saving
        scope.launch {
            val text = editor.text.toString()
            val saved = withContext(Dispatchers.IO) {
                runCatching { File(current).writeText(text) }.isSuccess
            }
            val index = tabs.indexOfFirst { it.path == current }
            if (saved && index >= 0) {
                tabs[index] = tabs[index].copy(dirty = false, text = text)
            }
            if (saved) lspManager.notifySaved(current)
            saveState = if (saved) SaveState.Saved else SaveState.Failed
        }
    }

    suspend fun saveAllDirty(): Int {
        captureActiveText()
        val pending = tabs.filter { it.dirty }
        if (pending.isEmpty()) return 0
        val failures = withContext(Dispatchers.IO) {
            pending.count { tab ->
                runCatching { File(tab.path).writeText(tab.text) }.isFailure
            }
        }
        for (tab in pending) {
            val index = tabs.indexOfFirst { it.path == tab.path }
            if (index >= 0) tabs[index] = tabs[index].copy(dirty = false)
        }
        if (failures == 0) saveState = SaveState.Saved
        return pending.size
    }

    fun appendConsole(text: String, kind: ConsoleLineKind = ConsoleLineKind.Normal) {
        consoleLines.add(ConsoleLine(text, kind))
        val limit = AppSettings.int(PreferenceSettings.ConsoleMaxLines, 2000)
        if (consoleLines.size > limit) consoleLines.removeRange(0, consoleLines.size - limit)
    }

    fun startBuild() {
        if (building) return
        if (AppSettings.bool(PreferenceSettings.ConsoleAutoOpen, true)) consoleVisible = true
        building = true
        scope.launch {
            val savedCount =
                if (AppSettings.bool(PreferenceSettings.AutoSaveOnBuild, true)) saveAllDirty() else 0
            if (savedCount > 0) {
                appendConsole(
                    if (savedCount == 1) "> saved 1 file" else "> saved $savedCount files"
                )
            }
            appendConsole("> building ${projectDir.name}")
            buildRunner.run(projectDir) { event ->
                when (event) {
                    is BuildEvent.Line -> appendConsole(event.text)
                    is BuildEvent.Finished -> {
                        building = false
                        if (event.exitCode == 0) {
                            appendConsole("> build succeeded", ConsoleLineKind.Success)
                        } else {
                            appendConsole("> build failed with exit code ${event.exitCode}", ConsoleLineKind.Error)
                        }
                    }
                    is BuildEvent.Failed -> {
                        building = false
                        appendConsole("> ${event.message}", ConsoleLineKind.Error)
                    }
                }
            }
        }
    }

    fun requestBuild() {
        val ready = ToolchainPaths.isInstalled(context, ToolchainKind.CMake) &&
            ToolchainPaths.isInstalled(context, ToolchainKind.Ndk)
        if (ready) startBuild() else showToolchainDialog = true
    }

    fun undo() {
        val editor = editorRef ?: return
        if (editor.canUndo()) {
            editor.undo()
            historyRevision++
        }
    }

    fun redo() {
        val editor = editorRef ?: return
        if (editor.canRedo()) {
            editor.redo()
            historyRevision++
        }
    }

    fun openDrawer() {
        scope.launch { drawerAnim.animateTo(1f, spring(stiffness = 320f, dampingRatio = 0.8f)) }
    }

    fun closeDrawer() {
        scope.launch { drawerAnim.animateTo(0f, spring(stiffness = 320f, dampingRatio = 0.8f)) }
    }

    fun settleDrawer() {
        val target = if (drawerAnim.value > 0.5f) 1f else 0f
        scope.launch { drawerAnim.animateTo(target, spring(stiffness = 320f, dampingRatio = 0.8f)) }
    }

    val drawerProgress = drawerAnim.value

    BackHandler(enabled = drawerProgress > 0f) {
        closeDrawer()
    }

    BackHandler(enabled = drawerProgress == 0f && consoleVisible) {
        consoleVisible = false
    }

    LaunchedEffect(editorRef) {
        editorRef?.let { EditorConfigurator.apply(it) }
    }

    DisposableEffect(Unit) {
        onDispose {
            buildRunner.stop()
            lspScope.launch { lspManager.shutdown() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EditorBackground)
                .systemBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TitleBarBackground)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { openDrawer() }) {
                    HamburgerIcon(modifier = Modifier.size(22.dp))
                }
                Text(
                    text = projectDir.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TabActiveForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                val canUndo = remember(historyRevision, editorRef) {
                    editorRef?.canUndo() == true
                }
                val canRedo = remember(historyRevision, editorRef) {
                    editorRef?.canRedo() == true
                }

                IconButton(onClick = { undo() }, enabled = canUndo) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Undo,
                        contentDescription = stringResource(R.string.undo),
                        tint = if (canUndo) Color(0xFFE4F5EC) else DisabledTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = { redo() }, enabled = canRedo) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Redo,
                        contentDescription = stringResource(R.string.redo),
                        tint = if (canRedo) Color(0xFFE4F5EC) else DisabledTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = { saveActive() }) {
                    Icon(
                        imageVector = if (saveState is SaveState.Saved) Icons.Outlined.Check else Icons.Outlined.Save,
                        contentDescription = stringResource(R.string.save),
                        tint = when (saveState) {
                            SaveState.Saved -> EditorEsPalette.mint
                            SaveState.Failed -> ErrorTint
                            else -> Color(0xFFE4F5EC)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = { requestBuild() }, enabled = !building) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = stringResource(R.string.run_build),
                        tint = if (building) DisabledTint else AccentGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                if (tabs.isEmpty()) {
                    EmptyEditorState()
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TabBarBackground),
                            content = {
                                items(tabs, key = { it.path }) { tab ->
                                    TabChip(
                                        tab = tab,
                                        active = tab.path == activePath,
                                        onClick = { switchTab(tab.path) },
                                        onClose = {
                                            if (tab.dirty) {
                                                dialog = ExplorerDialog.UnsavedClose(tab.path, tab.name)
                                            } else {
                                                removeTab(tab.path)
                                            }
                                        }
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            EditorPane(
                                onEditorCreated = { editor ->
                                    editorRef = editor
                                    val tab = activePath?.let { p -> tabs.firstOrNull { it.path == p } }
                                    if (tab != null) loadTabIntoEditor(editor, tab)
                                },
                                onEditorReleased = { editorRef = null }
                            )
                        }
                    }
                }
            }
            if (consoleVisible) {
                BuildConsole(
                    lines = consoleLines,
                    running = building,
                    onStop = {
                        buildRunner.stop()
                        building = false
                        appendConsole("> build stopped", ConsoleLineKind.Error)
                    },
                    onClear = { consoleLines.clear() },
                    onClose = { consoleVisible = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ConsoleHeight)
                )
            }

            lspStatus?.let { status ->
                Text(
                    text = status,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = LspStatusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LspStatusBackground)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            SymbolBar(editor = editorRef)
        }

        if (drawerProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f * drawerProgress))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { closeDrawer() }
            )
            Column(
                modifier = Modifier
                    .width(DrawerWidth)
                    .fillMaxHeight()
                    .offset { IntOffset(((drawerProgress - 1f) * drawerWidthPx).roundToInt(), 0) }
                    .clip(DrawerShape)
                    .background(SidebarBackground)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = { settleDrawer() },
                            onDragCancel = { settleDrawer() }
                        ) { change, drag ->
                            change.consume()
                            val next = (drawerAnim.value + drag / drawerWidthPx).coerceIn(0f, 1f)
                            scope.launch { drawerAnim.snapTo(next) }
                        }
                    }
                    .systemBarsPadding()
            ) {
                ExplorerDrawerContent(
                    projectDir = projectDir,
                    explorer = explorer,
                    activeFilePath = activePath,
                    onFileClick = { file ->
                        closeDrawer()
                        openFile(file)
                    },
                    onMenuRequested = { dialog = ExplorerDialog.Menu(it) },
                    onQuickAction = { action, parent ->
                        dialog = ExplorerDialog.Input(initial = "", parent = parent, kind = action)
                    },
                    lspEnabled = lspEnabled,
                    onLspToggle = { enabled ->
                        lspEnabled = enabled
                        AppSettings.putBool(PreferenceSettings.LspEnabled, enabled)
                        scope.launch {
                            if (enabled) {
                                val editor = editorRef
                                val current = activePath
                                if (editor != null && current != null) {
                                    lspManager.attach(editor, File(current)) { lspStatus = it }
                                }
                            } else {
                                lspManager.shutdown()
                                lspStatus = null
                                val editor = editorRef
                                val tab = activePath?.let { p -> tabs.firstOrNull { it.path == p } }
                                if (editor != null && tab != null) {
                                    editor.setEditorLanguage(EditorLanguageResolver.resolve(tab.name))
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    LaunchedEffect(saveState) {
        if (saveState is SaveState.Saved || saveState is SaveState.Failed) {
            delay(1600)
            saveState = SaveState.Idle
        }
    }

    LaunchedEffect(lspStatus) {
        if (lspStatus != null) {
            delay(2600)
            lspStatus = null
        }
    }

    if (showToolchainDialog) {
        ToolchainInstallDialog(
            onDismiss = { showToolchainDialog = false },
            onReady = {
                showToolchainDialog = false
                startBuild()
            }
        )
    }

    when (val current = dialog) {
        is ExplorerDialog.Menu -> NodeActionSheet(
            target = current.target,
            onDismiss = { dialog = null },
            onAction = { action ->
                val target = current.target
                dialog = when (action) {
                    NodeAction.NewFile -> ExplorerDialog.Input("", target, action)
                    NodeAction.NewFolder -> ExplorerDialog.Input("", target, action)
                    NodeAction.Rename -> ExplorerDialog.Input(target.name, target, action)
                    NodeAction.Delete -> ExplorerDialog.Delete(target)
                }
            }
        )
        is ExplorerDialog.Input -> NameInputDialog(
            title = when (current.kind) {
                NodeAction.NewFile -> stringResource(R.string.new_file)
                NodeAction.NewFolder -> stringResource(R.string.new_folder)
                else -> stringResource(R.string.rename)
            },
            initialValue = current.initial,
            onDismiss = { dialog = null },
            onSubmit = { name ->
                val result = when (current.kind) {
                    NodeAction.NewFile -> FileOps.createFile(current.parent, name)
                    NodeAction.NewFolder -> FileOps.createFolder(current.parent, name)
                    NodeAction.Rename -> FileOps.rename(current.parent, name)
                    else -> Result.failure(IllegalStateException())
                }
                result.fold(
                    onSuccess = { file ->
                        explorer.refresh()
                        when (current.kind) {
                            NodeAction.NewFile -> {
                                explorer.expand(file.parentFile)
                                openFile(file)
                            }
                            NodeAction.NewFolder -> explorer.expand(file)
                            NodeAction.Rename -> {
                                val oldPath = current.parent.absolutePath
                                val index = tabs.indexOfFirst { it.path == oldPath }
                                if (index >= 0) {
                                    tabs[index] = tabs[index].copy(path = file.absolutePath, name = file.name)
                                    if (activePath == oldPath) {
                                        activePath = file.absolutePath
                                        editorRef?.let { loadTabIntoEditor(it, tabs[index]) }
                                    }
                                }
                            }
                            else -> Unit
                        }
                    },
                    onFailure = { return@NameInputDialog it.message }
                )
                null
            }
        )
        is ExplorerDialog.Delete -> ConfirmDialog(
            title = stringResource(R.string.delete),
            message = if (current.target.isDirectory) {
                stringResource(R.string.delete_folder_message, current.target.name)
            } else {
                stringResource(R.string.delete_file_message, current.target.name)
            },
            confirmLabel = stringResource(R.string.delete),
            onDismiss = { dialog = null },
            onConfirm = {
                val target = current.target
                FileOps.delete(target)
                explorer.refresh()
                val prefix = target.absolutePath
                tabs.filter { it.path == prefix || it.path.startsWith(prefix + File.separator) }
                    .forEach { removeTab(it.path) }
            }
        )
        is ExplorerDialog.UnsavedClose -> UnsavedChangesDialog(
            fileName = current.name,
            onDismiss = { dialog = null },
            onSave = {
                val path = current.path
                val editor = editorRef
                val index = tabs.indexOfFirst { it.path == path }
                if (editor != null && index >= 0 && path == activePath) {
                    scope.launch {
                        val text = editor.text.toString()
                        withContext(Dispatchers.IO) { runCatching { File(path).writeText(text) } }
                        removeTab(path)
                        dialog = null
                    }
                } else {
                    removeTab(path)
                    dialog = null
                }
            },
            onDontSave = {
                removeTab(current.path)
                dialog = null
            }
        )
        null -> Unit
    }
}

@Composable
private fun TabChip(
    tab: TabItem,
    active: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .height(36.dp)
            .background(if (active) EditorBackground else TabBarBackground)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(if (active) AccentGreen else Color.Transparent)
        )
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (tab.dirty) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(DirtyDot)
                )
                Spacer(modifier = Modifier.width(7.dp))
            }
            Icon(
                imageVector = FileTypeIcons.resolve(tab.name),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = tab.name,
                fontSize = 13.sp,
                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                color = if (active) TabActiveForeground else TabInactiveForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(7.dp))
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.close),
                tint = if (active) Color(0xFFE4F5EC) else TabInactiveForeground,
                modifier = Modifier
                    .size(15.dp)
                    .clickable(onClick = onClose)
            )
        }
    }
}

@Composable
private fun HamburgerIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.height * 0.09f
        listOf(0.28f, 0.5f, 0.72f).forEach { fraction ->
            drawLine(
                brush = HamburgerBrush,
                start = Offset(size.width * 0.08f, size.height * fraction),
                end = Offset(size.width * 0.92f, size.height * fraction),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun EmptyEditorState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.InsertDriveFile,
                contentDescription = null,
                tint = TabInactiveForeground,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.no_open_files),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TabActiveForeground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.open_from_explorer),
                fontSize = 13.sp,
                color = TabInactiveForeground
            )
        }
    }
}
