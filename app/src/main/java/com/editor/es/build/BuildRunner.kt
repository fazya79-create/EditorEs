package com.editor.es.build

import android.content.Context
import com.editor.es.proot.ProotConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

sealed interface BuildEvent {
    data class Line(val text: String) : BuildEvent
    data class Finished(val exitCode: Int) : BuildEvent
    data class Failed(val message: String) : BuildEvent
}

class BuildRunner(private val context: Context) {

    @Volatile
    private var process: Process? = null

    val isRunning: Boolean get() = process?.isAlive == true

    fun stop() {
        process?.destroy()
        process = null
    }

    suspend fun run(projectDir: File, onEvent: (BuildEvent) -> Unit): Unit =
        withContext(Dispatchers.IO) {
            try {
                if (!ProotConfig.isInstalled(context)) {
                    onEvent(BuildEvent.Failed("Ubuntu environment is not installed"))
                    return@withContext
                }
                if (!ToolchainPaths.isInstalled(context, ToolchainKind.CMake)) {
                    onEvent(BuildEvent.Failed("CMake is not installed"))
                    return@withContext
                }
                if (!ToolchainPaths.isInstalled(context, ToolchainKind.Ndk)) {
                    onEvent(BuildEvent.Failed("Android NDK is not installed"))
                    return@withContext
                }
                if (!File(projectDir, "CMakeLists.txt").isFile) {
                    onEvent(BuildEvent.Failed("CMakeLists.txt not found in ${projectDir.name}"))
                    return@withContext
                }

                val guestProject = "/project"
                val script = buildScript()
                val args = ProotConfig.commandArgs(
                    context = context,
                    script = script,
                    guestCwd = guestProject,
                    binds = listOf("${projectDir.absolutePath}:$guestProject"),
                    extraPath = listOf(ToolchainPaths.guestCMakeBin())
                )

                onEvent(BuildEvent.Line("> cmake configure + build (arm64-v8a, android-24)"))

                val builder = ProcessBuilder(args)
                builder.redirectErrorStream(true)
                builder.environment().putAll(ProotConfig.prootEnvMap(context))
                val started = builder.start()
                process = started

                BufferedReader(InputStreamReader(started.inputStream)).use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        onEvent(BuildEvent.Line(line))
                    }
                }
                val exit = started.waitFor()
                process = null
                onEvent(BuildEvent.Finished(exit))
            } catch (e: Exception) {
                process = null
                onEvent(BuildEvent.Failed(e.message ?: "Build failed"))
            }
        }

    private fun buildScript(): String {
        val toolchain = ToolchainPaths.guestNdkToolchainFile()
        val ninja = ToolchainPaths.guestNinja()
        return listOf(
            "set -e",
            "cmake -S . -B build -G Ninja" +
                " -DCMAKE_MAKE_PROGRAM=$ninja" +
                " -DCMAKE_TOOLCHAIN_FILE=$toolchain" +
                " -DANDROID_ABI=arm64-v8a" +
                " -DANDROID_PLATFORM=android-24" +
                " -DCMAKE_BUILD_TYPE=Release",
            "cmake --build build"
        ).joinToString(" && ")
    }
}
