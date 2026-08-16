package com.editor.es.build

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RunConfigurations(
    private val context: Context,
    private val projectDir: File,
    private val runner: BuildRunner
) {

    fun hasPresets(): Boolean = CmakePresets.hasAny(projectDir)

    fun bootstrap() {
        CmakePresets.bootstrap(
            projectDir = projectDir,
            abi = runner.abi(),
            apiLevel = runner.apiLevel(),
            buildType = runner.buildType(),
            ninjaPath = ToolchainPaths.guestNinja()
        )
        writeClangdDatabase(CmakePresets.defaultPresetName(runner.buildType()))
    }

    suspend fun activePreset(): String? = withContext(Dispatchers.IO) {
        val fromCmake = runner.listPresets(projectDir, "build").firstOrNull()
        val preset = fromCmake ?: CmakePresets.defaultPresetName(runner.buildType())
        writeClangdDatabase(preset)
        preset
    }

    private fun writeClangdDatabase(presetName: String) {
        val binaryDir = "build/$presetName"
        val file = File(projectDir, ".clangd")
        val body = "CompileFlags:\n  CompilationDatabase: $binaryDir\n"
        runCatching {
            if (!file.isFile) {
                file.writeText(body)
                return@runCatching
            }
            val existing = file.readText()
            if (!existing.contains("CompilationDatabase:")) {
                file.writeText(body + existing)
            } else {
                file.writeText(
                    existing.replace(
                        Regex("CompilationDatabase:.*"),
                        "CompilationDatabase: $binaryDir"
                    )
                )
            }
        }
    }
}
