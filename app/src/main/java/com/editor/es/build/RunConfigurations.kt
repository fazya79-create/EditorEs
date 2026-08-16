package com.editor.es.build

import android.content.Context
import com.editor.es.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class RunMenuState(
    val configurePresets: List<ConfigurePreset>,
    val actions: List<BuildAction>,
    val selected: String?
)

class RunConfigurations(
    private val context: Context,
    private val projectDir: File,
    private val runner: BuildRunner
) {

    private val selectionKey = "run_preset_" + projectDir.absolutePath.hashCode()

    fun selectedPreset(): String? =
        AppSettings.string(selectionKey, "").takeIf { it.isNotEmpty() }

    fun selectPreset(name: String) {
        AppSettings.putString(selectionKey, name)
        writeClangdDatabase(name)
    }

    fun hasPresets(): Boolean = CmakePresets.hasAny(projectDir)

    fun bootstrap() {
        CmakePresets.bootstrap(
            projectDir = projectDir,
            abi = runner.abi(),
            apiLevel = runner.apiLevel(),
            buildType = runner.buildType(),
            ninjaPath = ToolchainPaths.guestNinja()
        )
        selectPreset(CmakePresets.defaultPresetName(runner.buildType()))
    }

    suspend fun load(): RunMenuState = withContext(Dispatchers.IO) {
        val configureNames = runner.listPresets(projectDir, "configure")
        val buildNames = runner.listPresets(projectDir, "build")
        val configureLabels = CmakePresets.labelsFrom(projectDir, "configurePresets")
        val buildLabels = CmakePresets.labelsFrom(projectDir, "buildPresets")

        val configures = configureNames.map { name ->
            ConfigurePreset(name = name, label = configureLabels[name] ?: name)
        }
        val active = selectedPreset()?.takeIf { picked -> configures.any { it.name == picked } }
            ?: configures.firstOrNull()?.name

        val actions = mutableListOf<BuildAction>()
        for (name in buildNames) {
            actions += BuildAction(
                name = name,
                label = buildLabels[name] ?: name,
                configurePreset = name,
                rawCommand = null
            )
        }
        actions += CmakePresets.vendorCommands(projectDir)

        RunMenuState(configurePresets = configures, actions = actions, selected = active)
    }

    fun saveBuildPreset(
        name: String,
        configurePreset: String,
        targets: List<String>,
        jobs: Int,
        verbose: Boolean,
        cleanFirst: Boolean
    ) {
        CmakePresets.addBuildPreset(
            projectDir = projectDir,
            name = sanitize(name),
            displayName = name,
            configurePreset = configurePreset,
            targets = targets,
            jobs = jobs,
            verbose = verbose,
            cleanFirst = cleanFirst
        )
    }

    fun saveRawCommand(name: String, command: String) {
        CmakePresets.addVendorCommand(projectDir, name, command)
    }

    fun remove(action: BuildAction) {
        CmakePresets.removeAction(projectDir, action)
    }

    fun isUserDefined(action: BuildAction): Boolean =
        CmakePresets.isUserDefined(projectDir, action)

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

    private fun sanitize(name: String): String =
        name.trim().lowercase().replace(Regex("[^a-z0-9._-]+"), "-").trim('-')
            .ifEmpty { "custom" }
}
