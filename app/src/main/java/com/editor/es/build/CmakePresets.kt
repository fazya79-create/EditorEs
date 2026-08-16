package com.editor.es.build

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ConfigurePreset(
    val name: String,
    val label: String
)

data class BuildAction(
    val name: String,
    val label: String,
    val configurePreset: String?,
    val rawCommand: String?
) {
    val isRaw: Boolean get() = rawCommand != null
}

object CmakePresets {

    const val ProjectFileName = "CMakePresets.json"
    const val UserFileName = "CMakeUserPresets.json"
    const val VendorKey = "editor.es/EditorEs/1.0"

    private const val SchemaVersion = 6

    fun projectFile(projectDir: File): File = File(projectDir, ProjectFileName)

    fun userFile(projectDir: File): File = File(projectDir, UserFileName)

    fun hasAny(projectDir: File): Boolean =
        projectFile(projectDir).isFile || userFile(projectDir).isFile

    fun bootstrap(projectDir: File, abi: String, apiLevel: Int, buildType: String) {
        val base = JSONObject()
            .put("name", "android-base")
            .put("hidden", true)
            .put("generator", "Ninja")
            .put("binaryDir", "\${sourceDir}/build/\${presetName}")
            .put(
                "toolchainFile",
                "\$env{ANDROID_NDK_ROOT}/build/cmake/android.toolchain.cmake"
            )
            .put(
                "cacheVariables",
                JSONObject()
                    .put("ANDROID_ABI", abi)
                    .put("ANDROID_PLATFORM", "android-$apiLevel")
                    .put("CMAKE_EXPORT_COMPILE_COMMANDS", "ON")
            )

        val configures = JSONArray().put(base)
        val builds = JSONArray()
        val types = if (buildType == "Debug") listOf("Debug", "Release") else listOf(buildType, "Debug")
        for (type in types.distinct()) {
            val presetName = "android-${type.lowercase()}"
            configures.put(
                JSONObject()
                    .put("name", presetName)
                    .put("displayName", "Android $type")
                    .put("inherits", "android-base")
                    .put(
                        "cacheVariables",
                        JSONObject().put("CMAKE_BUILD_TYPE", type)
                    )
            )
            builds.put(
                JSONObject()
                    .put("name", presetName)
                    .put("displayName", "Build $type")
                    .put("configurePreset", presetName)
            )
        }

        val root = JSONObject()
            .put("version", SchemaVersion)
            .put("configurePresets", configures)
            .put("buildPresets", builds)

        projectFile(projectDir).writeText(root.toString(2) + "\n")
        ensureGitIgnore(projectDir)
    }

    fun defaultPresetName(buildType: String): String = "android-${buildType.lowercase()}"

    private fun ensureGitIgnore(projectDir: File) {
        val gitignore = File(projectDir, ".gitignore")
        val existing = if (gitignore.isFile) gitignore.readText() else ""
        if (existing.lineSequence().any { it.trim() == UserFileName }) return
        val prefix = if (existing.isEmpty() || existing.endsWith("\n")) "" else "\n"
        runCatching { gitignore.appendText("$prefix$UserFileName\n") }
    }

    fun parseListOutput(output: String): List<String> = output.lineSequence()
        .map { it.trim() }
        .filter { it.startsWith("\"") }
        .mapNotNull { line ->
            val start = line.indexOf('"')
            val end = line.indexOf('"', start + 1)
            if (start < 0 || end <= start) null else line.substring(start + 1, end)
        }
        .filter { it.isNotEmpty() }
        .distinct()
        .toList()

    fun labelsFrom(projectDir: File, section: String): Map<String, String> {
        val labels = mutableMapOf<String, String>()
        for (file in listOf(projectFile(projectDir), userFile(projectDir))) {
            if (!file.isFile) continue
            runCatching {
                val root = JSONObject(file.readText())
                val array = root.optJSONArray(section) ?: return@runCatching
                for (i in 0 until array.length()) {
                    val entry = array.optJSONObject(i) ?: continue
                    val name = entry.optString("name").takeIf { it.isNotEmpty() } ?: continue
                    val display = entry.optString("displayName").takeIf { it.isNotEmpty() }
                    if (display != null) labels[name] = display
                }
            }
        }
        return labels
    }

    fun vendorCommands(projectDir: File): List<BuildAction> {
        val actions = mutableListOf<BuildAction>()
        for (file in listOf(projectFile(projectDir), userFile(projectDir))) {
            if (!file.isFile) continue
            runCatching {
                val root = JSONObject(file.readText())
                val vendor = root.optJSONObject("vendor") ?: return@runCatching
                val ours = vendor.optJSONObject(VendorKey) ?: return@runCatching
                val commands = ours.optJSONArray("commands") ?: return@runCatching
                for (i in 0 until commands.length()) {
                    val entry = commands.optJSONObject(i) ?: continue
                    val name = entry.optString("name").takeIf { it.isNotEmpty() } ?: continue
                    val run = entry.optString("run").takeIf { it.isNotEmpty() } ?: continue
                    actions += BuildAction(
                        name = name,
                        label = name,
                        configurePreset = null,
                        rawCommand = run
                    )
                }
            }
        }
        return actions
    }

    fun binaryDirOf(projectDir: File, presetName: String): File =
        File(projectDir, "build/$presetName")

    fun addBuildPreset(
        projectDir: File,
        name: String,
        displayName: String,
        configurePreset: String,
        targets: List<String>,
        jobs: Int,
        verbose: Boolean,
        cleanFirst: Boolean
    ) {
        val root = readUserRoot(projectDir)
        val builds = root.optJSONArray("buildPresets") ?: JSONArray()
        removeByName(builds, name)
        val preset = JSONObject()
            .put("name", name)
            .put("displayName", displayName)
            .put("configurePreset", configurePreset)
        if (targets.isNotEmpty()) {
            preset.put("targets", JSONArray().apply { targets.forEach { put(it) } })
        }
        if (jobs > 0) preset.put("jobs", jobs)
        if (verbose) preset.put("verbose", true)
        if (cleanFirst) preset.put("cleanFirst", true)
        builds.put(preset)
        root.put("buildPresets", builds)
        writeUserRoot(projectDir, root)
    }

    fun addVendorCommand(projectDir: File, name: String, command: String) {
        val root = readUserRoot(projectDir)
        val vendor = root.optJSONObject("vendor") ?: JSONObject()
        val ours = vendor.optJSONObject(VendorKey) ?: JSONObject()
        val commands = ours.optJSONArray("commands") ?: JSONArray()
        removeByName(commands, name)
        commands.put(JSONObject().put("name", name).put("run", command))
        ours.put("commands", commands)
        vendor.put(VendorKey, ours)
        root.put("vendor", vendor)
        writeUserRoot(projectDir, root)
    }

    fun removeAction(projectDir: File, action: BuildAction) {
        val root = readUserRoot(projectDir)
        if (action.isRaw) {
            val vendor = root.optJSONObject("vendor") ?: return
            val ours = vendor.optJSONObject(VendorKey) ?: return
            val commands = ours.optJSONArray("commands") ?: return
            removeByName(commands, action.name)
            ours.put("commands", commands)
            vendor.put(VendorKey, ours)
            root.put("vendor", vendor)
        } else {
            val builds = root.optJSONArray("buildPresets") ?: return
            removeByName(builds, action.name)
            root.put("buildPresets", builds)
        }
        writeUserRoot(projectDir, root)
    }

    fun isUserDefined(projectDir: File, action: BuildAction): Boolean {
        val file = userFile(projectDir)
        if (!file.isFile) return false
        return runCatching {
            val root = JSONObject(file.readText())
            if (action.isRaw) {
                val commands = root.optJSONObject("vendor")
                    ?.optJSONObject(VendorKey)
                    ?.optJSONArray("commands")
                containsName(commands, action.name)
            } else {
                containsName(root.optJSONArray("buildPresets"), action.name)
            }
        }.getOrDefault(false)
    }

    private fun containsName(array: JSONArray?, name: String): Boolean {
        if (array == null) return false
        for (i in 0 until array.length()) {
            if (array.optJSONObject(i)?.optString("name") == name) return true
        }
        return false
    }

    private fun removeByName(array: JSONArray, name: String) {
        for (i in array.length() - 1 downTo 0) {
            if (array.optJSONObject(i)?.optString("name") == name) array.remove(i)
        }
    }

    private fun readUserRoot(projectDir: File): JSONObject {
        val file = userFile(projectDir)
        if (!file.isFile) return JSONObject().put("version", SchemaVersion)
        return runCatching { JSONObject(file.readText()) }
            .getOrElse { JSONObject().put("version", SchemaVersion) }
            .also { if (!it.has("version")) it.put("version", SchemaVersion) }
    }

    private fun writeUserRoot(projectDir: File, root: JSONObject) {
        userFile(projectDir).writeText(root.toString(2) + "\n")
        ensureGitIgnore(projectDir)
    }
}
