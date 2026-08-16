package com.editor.es.build

import java.io.File

enum class BuildSystem {
    CMake,
    NdkBuild
}

data class BuildTarget(val system: BuildSystem, val makefile: File)

object BuildSystemDetector {

    fun detect(projectDir: File): BuildTarget? {
        androidMk(projectDir)?.let { return BuildTarget(BuildSystem.NdkBuild, it) }
        val cmakeLists = File(projectDir, "CMakeLists.txt")
        if (cmakeLists.isFile) return BuildTarget(BuildSystem.CMake, cmakeLists)
        return null
    }

    fun androidMk(projectDir: File): File? {
        val candidates = listOf(
            File(projectDir, "jni/Android.mk"),
            File(projectDir, "Android.mk"),
            File(projectDir, "src/main/jni/Android.mk"),
            File(projectDir, "app/jni/Android.mk")
        )
        return candidates.firstOrNull { it.isFile }
    }

    fun applicationMk(androidMk: File): File? =
        File(androidMk.parentFile, "Application.mk").takeIf { it.isFile }

    fun guestRelativePath(projectDir: File, file: File, guestRoot: String): String {
        val root = projectDir.absolutePath.trimEnd('/')
        val path = file.absolutePath
        if (!path.startsWith(root)) return guestRoot
        val relative = path.removePrefix(root).trimStart('/')
        return if (relative.isEmpty()) guestRoot else "$guestRoot/$relative"
    }
}
