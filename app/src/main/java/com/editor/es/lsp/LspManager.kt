package com.editor.es.lsp

import android.content.Context
import com.editor.es.build.ToolchainKind
import com.editor.es.build.ToolchainPaths
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.languageServerDefinition
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.LspProject
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LspManager(private val context: Context, private val projectDir: File) {

    private var project: LspProject? = null
    private val attached = mutableMapOf<String, LspEditor>()

    fun supports(fileName: String): Boolean {
        val lower = fileName.lowercase()
        return CppExtensions.any { lower.endsWith(it) }
    }

    fun tooLarge(file: File): Boolean = file.length() > MaxFileBytes

    private fun ensureProject(): LspProject {
        project?.let { return it }
        val created = LspProject(projectDir.absolutePath)
        for (ext in CppExtensions) {
            created.addServerDefinition(
                languageServerDefinition {
                    name("clangd")
                    ext(ext.removePrefix("."))
                    connection {
                        custom { ClangdConnection(context, projectDir) }
                    }
                }
            )
        }
        project = created
        return created
    }

    suspend fun attach(
        editor: CodeEditor,
        file: File,
        onStatus: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        if (!ClangdConnection.isAvailable(context)) {
            onStatus("clangd not installed, reinstall the NDK")
            return@withContext false
        }
        if (!supports(file.name)) return@withContext false
        if (tooLarge(file)) {
            onStatus("${file.name} is too large for LSP")
            return@withContext false
        }
        writeFallbackFlags()
        val path = file.absolutePath
        val lspProject = ensureProject()
        val lspEditor = lspProject.getOrCreateEditor(path)
        withContext(Dispatchers.Main) {
            lspEditor.wrapperLanguage =
                com.editor.es.editor.EditorLanguageResolver.resolve(file.name)
            lspEditor.editor = editor
            lspEditor.isEnableHover = true
            lspEditor.isEnableSignatureHelp = true
            lspEditor.isEnableInlayHint = false
        }
        val connected = runCatching { lspEditor.connect(false) }.getOrDefault(false)
        if (connected) {
            attached[path] = lspEditor
            onStatus("clangd connected")
        } else {
            onStatus("clangd failed to start")
            runCatching { lspEditor.dispose() }
        }
        connected
    }

    suspend fun detach(path: String) = withContext(Dispatchers.IO) {
        val lspEditor = attached.remove(path) ?: return@withContext
        runCatching { lspEditor.dispose() }
    }

    suspend fun notifySaved(path: String) = withContext(Dispatchers.IO) {
        val lspEditor = attached[path] ?: return@withContext
        runCatching { lspEditor.saveDocument() }
    }

    suspend fun shutdown() = withContext(Dispatchers.IO) {
        attached.clear()
        val current = project ?: return@withContext
        project = null
        runCatching { current.closeAllEditors() }
        runCatching { current.dispose() }
    }

    private fun writeFallbackFlags() {
        if (File(projectDir, "compile_flags.txt").isFile) return
        if (File(projectDir, "build/compile_commands.json").isFile) return
        if (File(projectDir, "compile_commands.json").isFile) return
        val sysroot = "${ToolchainPaths.guestDir(ToolchainKind.Ndk)}/toolchains/llvm/prebuilt/linux-arm64/sysroot"
        runCatching {
            File(projectDir, "compile_flags.txt").writeText(
                listOf(
                    "-xc++",
                    "-std=c++17",
                    "--target=aarch64-none-linux-android24",
                    "--sysroot=$sysroot",
                    "-I.",
                    "-Iinclude",
                    "-DANDROID",
                    "-D__ANDROID_API__=24"
                ).joinToString("\n") + "\n"
            )
        }
    }

    companion object {
        private const val MaxFileBytes = 300L * 1024L

        private val CppExtensions =
            listOf(".cpp", ".cc", ".cxx", ".c", ".h", ".hpp", ".hh", ".hxx")
    }
}
