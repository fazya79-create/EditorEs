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
                if (!ToolchainPaths.isInstalled(context, ToolchainKind.Ndk)) {
                    onEvent(BuildEvent.Failed("Android NDK is not installed"))
                    return@withContext
                }

                val target = BuildSystemDetector.detect(projectDir)
                if (target == null) {
                    onEvent(
                        BuildEvent.Failed(
                            "No CMakeLists.txt or Android.mk found in ${projectDir.name}"
                        )
                    )
                    return@withContext
                }

                if (target.system == BuildSystem.CMake &&
                    !ToolchainPaths.isInstalled(context, ToolchainKind.CMake)
                ) {
                    onEvent(BuildEvent.Failed("CMake is not installed"))
                    return@withContext
                }

                prepareExecutables(target.system)

                val guestProject = "/project"
                val script = when (target.system) {
                    BuildSystem.CMake -> cmakeScript()
                    BuildSystem.NdkBuild -> ndkBuildScript(projectDir, target.makefile, guestProject)
                }

                val args = ProotConfig.commandArgs(
                    context = context,
                    script = script,
                    guestCwd = guestProject,
                    binds = listOf("${projectDir.absolutePath}:$guestProject"),
                    extraPath = listOf(ToolchainPaths.guestCMakeBin(), ToolchainPaths.guestNdkBin())
                )

                when (target.system) {
                    BuildSystem.CMake -> {
                        onEvent(BuildEvent.Line("> cmake + ninja (arm64-v8a, android-24)"))
                        onEvent(BuildEvent.Line("> ${ToolchainPaths.guestCMake()}"))
                    }
                    BuildSystem.NdkBuild -> {
                        val appMk = BuildSystemDetector.applicationMk(target.makefile)
                        onEvent(BuildEvent.Line("> ndk-build (${target.makefile.name})"))
                        onEvent(
                            BuildEvent.Line(
                                if (appMk != null) {
                                    "> using Application.mk"
                                } else {
                                    "> no Application.mk, defaulting to arm64-v8a / android-24"
                                }
                            )
                        )
                    }
                }

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

    private fun prepareExecutables(system: BuildSystem) {
        if (system == BuildSystem.CMake) {
            val cmake = ToolchainPaths.cmakeBinary(context)
            cmake.setExecutable(true, false)
            File(cmake.parentFile, "ninja").setExecutable(true, false)
        } else {
            ToolchainPaths.ndkBuildScript(context).setExecutable(true, false)
            ToolchainPaths.ndkMakeBinary(context).setExecutable(true, false)
            ToolchainPaths.ndkPythonBinary(context).setExecutable(true, false)
        }
    }

    private fun cmakeScript(): String {
        val cmake = ToolchainPaths.guestCMake()
        val toolchain = ToolchainPaths.guestNdkToolchainFile()
        val ninja = ToolchainPaths.guestNinja()
        return listOf(
            "set -e",
            "$cmake -S . -B build -G Ninja" +
                " -DCMAKE_MAKE_PROGRAM=$ninja" +
                " -DCMAKE_TOOLCHAIN_FILE=$toolchain" +
                " -DANDROID_ABI=arm64-v8a" +
                " -DANDROID_PLATFORM=android-24" +
                " -DCMAKE_BUILD_TYPE=Release",
            "$cmake --build build"
        ).joinToString(" && ")
    }

    private fun ndkBuildScript(projectDir: File, androidMk: File, guestRoot: String): String {
        val guestMk = BuildSystemDetector.guestRelativePath(projectDir, androidMk, guestRoot)
        val appMk = BuildSystemDetector.applicationMk(androidMk)
        val ndkRoot = ToolchainPaths.guestDir(ToolchainKind.Ndk)
        val make = ToolchainPaths.guestNdkMake()
        val python = ToolchainPaths.guestNdkPython()

        val parts = mutableListOf(
            "NDK_PROJECT_PATH=$guestRoot",
            "APP_BUILD_SCRIPT=$guestMk",
            "NDK_HOST_MAKE=$make",
            "NDK_HOST_PYTHON=$python"
        )
        if (appMk != null) {
            parts += "NDK_APPLICATION_MK=" +
                BuildSystemDetector.guestRelativePath(projectDir, appMk, guestRoot)
        } else {
            parts += "APP_ABI=arm64-v8a"
            parts += "APP_PLATFORM=android-24"
        }

        return "set -e && ${parts.joinToString(" ")} $ndkRoot/ndk-build"
    }
}
