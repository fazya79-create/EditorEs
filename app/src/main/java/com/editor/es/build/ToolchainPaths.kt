package com.editor.es.build

import android.content.Context
import java.io.File

enum class ToolchainKind {
    Ndk,
    CMake
}

data class ToolchainRelease(
    val tag: String,
    val assetName: String,
    val downloadUrl: String,
    val sizeBytes: Long
) {
    val sizeMb: Double get() = sizeBytes / (1024.0 * 1024.0)
}

object ToolchainPaths {

    const val NdkReleasesApi =
        "https://api.github.com/repos/HomuHomu833/android-ndk-custom/releases?per_page=20"
    const val CMakeReleasesApi =
        "https://api.github.com/repos/HomuHomu833/cmake-custom/releases?per_page=20"

    const val AssetSuffix = "-aarch64-linux-gnu.tar.xz"

    private const val OptDirName = "opt"
    private const val NdkDirName = "ndk"
    private const val CMakeDirName = "cmake"
    private const val MarkerName = ".version"

    fun optDir(context: Context): File =
        File(context.filesDir, "${com.editor.es.proot.ProotConfig.RootfsName}/$OptDirName")

    fun hostDir(context: Context, kind: ToolchainKind): File = when (kind) {
        ToolchainKind.Ndk -> File(optDir(context), NdkDirName)
        ToolchainKind.CMake -> File(optDir(context), CMakeDirName)
    }

    fun guestDir(kind: ToolchainKind): String = when (kind) {
        ToolchainKind.Ndk -> "/$OptDirName/$NdkDirName"
        ToolchainKind.CMake -> "/$OptDirName/$CMakeDirName"
    }

    fun markerFile(context: Context, kind: ToolchainKind): File =
        File(hostDir(context, kind), MarkerName)

    fun installedVersion(context: Context, kind: ToolchainKind): String? {
        val marker = markerFile(context, kind)
        if (!marker.isFile) return null
        return runCatching { marker.readText().trim() }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    fun isInstalled(context: Context, kind: ToolchainKind): Boolean {
        if (installedVersion(context, kind) == null) return false
        return when (kind) {
            ToolchainKind.Ndk -> ndkToolchainFile(context).isFile
            ToolchainKind.CMake -> cmakeBinary(context).isFile
        }
    }

    fun ndkToolchainFile(context: Context): File =
        File(hostDir(context, ToolchainKind.Ndk), "build/cmake/android.toolchain.cmake")

    fun cmakeBinary(context: Context): File =
        File(hostDir(context, ToolchainKind.CMake), "bin/cmake")

    fun guestNdkToolchainFile(): String =
        "${guestDir(ToolchainKind.Ndk)}/build/cmake/android.toolchain.cmake"

    fun guestCMakeBin(): String = "${guestDir(ToolchainKind.CMake)}/bin"

    fun guestNinja(): String = "${guestDir(ToolchainKind.CMake)}/bin/ninja"

    fun downloadCache(context: Context, kind: ToolchainKind): File =
        File(context.cacheDir, if (kind == ToolchainKind.Ndk) "ndk.tar.xz" else "cmake.tar.xz")
}
