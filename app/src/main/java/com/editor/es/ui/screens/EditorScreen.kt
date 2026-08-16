package com.editor.es.ui.screens

import android.graphics.Rect
import android.os.Build
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.InsertDriveFile
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.editor.es.R
import com.editor.es.data.FileOps
import com.editor.es.data.ProjectCreator
import com.editor.es.editor.EditorLanguageResolver
import com.editor.es.editor.EditorPane
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
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentListener
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ErrorTint = Color(0xFFEF6767)
private val SidebarBackground = Color(0xFF252526)
private val TitleBarBackground = Color(0xFF3C3C3C)
private val EditorBackground = Color(0xFF1E1E1E)
private val TabBarBackground = Color(0xFF252526)
private val TabActiveForeground = Color.White
private val TabInactiveForeground = Color(0xFF969696)
private val DirtyDot = Color(0xFFE8E8E8)
private val AccentBlue = Color(0xFF007ACC)
private val DrawerWidth = 300.dp
private val DrawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
private val HamburgerBrush = Brush.linearGradient(
    colors = listOf(EditorEsPalette.mint, EditorEsPalette.amber)
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
    val view = LocalView.current
    val density = LocalDensity.current
    val drawerWidthPx = remember(density) { with(density) { DrawerWidth.toPx() } }
    var edgeDragActive by remember { mutableStateOf(false) }

    DisposableEffect(view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val stripWidthPx = (40f * density.density).toInt()
            val layoutListener = View.OnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                v.systemGestureExclusionRects = listOf(Rect(0, 0, stripWidthPx, v.height))
            }
            view.addOnLayoutChangeListener(layoutListener)
            onDispose {
                view.removeOnLayoutChangeListener(layoutListener)
                view.systemGestureExclusionRects = emptyList()
            }
        } else {
            onDispose {}
        }
    }

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

    val dirtyMarker = remember {
        DirtyMarker {
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
            saveState = if (saved) SaveState.Saved else SaveState.Failed
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
                IconButton(onClick = { saveActive() }) {
                    Icon(
                        imageVector = if (saveState is SaveState.Saved) Icons.Outlined.Check else Icons.Outlined.Save,
                        contentDescription = stringResource(R.string.save),
                        tint = when (saveState) {
                            SaveState.Saved -> EditorEsPalette.mint
                            SaveState.Failed -> ErrorTint
                            else -> Color(0xFFCCCCCC)
                        },
                        modifier = Modifier.size(20.dp)
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
                if (drawerProgress < 0.05f || edgeDragActive) {
                    EdgeGestureStrip(
                        onDragStart = { edgeDragActive = true },
                        onDrag = { delta ->
                            val next = (drawerAnim.value + delta / drawerWidthPx).coerceIn(0f, 1f)
                            scope.launch { drawerAnim.snapTo(next) }
                        },
                        onDragEnd = {
                            edgeDragActive = false
                            settleDrawer()
                        }
                    )
                }
            }
            SymbolBar(editor = editorRef)
        }

        if (drawerProgress > 0f || edgeDragActive) {
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
                .background(if (active) AccentBlue else Color.Transparent)
        )
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.close),
                tint = if (active) Color(0xFFCCCCCC) else TabInactiveForeground,
                modifier = Modifier
                    .size(15.dp)
                    .clickable(onClick = onClose)
            )
            Spacer(modifier = Modifier.width(7.dp))
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
            if (tab.dirty) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(DirtyDot)
                )
            }
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
private fun EdgeGestureStrip(
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                val touchSlop = viewConfiguration.touchSlop
                while (true) {
                    awaitPointerEventScope {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startX = down.position.x
                        val startY = down.position.y
                        var dragging = false
                        while (true) {
                            val change = awaitPointerEvent().changes
                                .firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val dx = change.position.x - startX
                            val dy = change.position.y - startY
                            if (!dragging && abs(dx) > touchSlop && abs(dx) > abs(dy)) {
                                dragging = true
                                onDragStart()
                            }
                            if (dragging) {
                                change.consume()
                                onDrag(change.position.x - change.previousPosition.x)
                            }
                        }
                        if (dragging) onDragEnd()
                    }
                }
            }
    )
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
