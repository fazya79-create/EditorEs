package com.editor.es.build

import java.io.File

enum class ProjectType {
    Cmake,
    Gradle,
    Unknown
}

object ProjectDetector {

    private val gradleSettings = listOf(
        "settings.gradle.kts",
        "settings.gradle"
    )

    fun detect(projectDir: File): ProjectType {
        if (gradleSettings.any { File(projectDir, it).isFile }) return ProjectType.Gradle
        if (File(projectDir, "CMakeLists.txt").isFile) return ProjectType.Cmake
        return ProjectType.Unknown
    }

    fun compileSdkOf(projectDir: File): Int? {
        val candidates = listOf(
            File(projectDir, "app/build.gradle.kts"),
            File(projectDir, "app/build.gradle")
        )
        val script = candidates.firstOrNull { it.isFile } ?: return null
        val text = runCatching { script.readText() }.getOrNull() ?: return null
        return Regex("compileSdk\\s*=?\\s*(\\d+)").find(text)?.groupValues?.get(1)?.toIntOrNull()
    }

    fun usesNative(projectDir: File): Boolean =
        File(projectDir, "app/src/main/cpp/CMakeLists.txt").isFile ||
            runCatching {
                listOf(
                    File(projectDir, "app/build.gradle.kts"),
                    File(projectDir, "app/build.gradle")
                ).firstOrNull { it.isFile }?.readText()?.contains("externalNativeBuild") == true
            }.getOrDefault(false)
}
