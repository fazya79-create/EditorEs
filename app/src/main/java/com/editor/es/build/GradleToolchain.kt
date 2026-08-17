package com.editor.es.build

import android.content.Context
import com.editor.es.proot.ProotConfig
import java.io.File

enum class GradleTask(val label: String, val arguments: String) {
    AssembleDebug("Build debug", ":app:assembleDebug"),
    AssembleRelease("Build release", ":app:assembleRelease"),
    Clean("Clean build", "clean :app:assembleDebug")
}

enum class GradlePiece(val label: String, val sizeHint: String) {
    Jdk("JDK 17", "180 MB"),
    Sdk("Android SDK tools", "149 MB"),
    Platform("SDK platform", "60 MB"),
    Gradle("Gradle ${AndroidToolchainVersions.GradleVersion}", "130 MB")
}

data class GradleRequirement(val missing: List<GradlePiece>) {
    val summary: String
        get() = missing.joinToString(", ") { it.label }
}

object AndroidToolchainVersions {
    const val GradleVersion = "8.14.3"
    const val SdkRelease = "36.0.2"
    const val SdkAsset = "android-sdk-aarch64-linux-gnu.tar.xz"
    const val SdkUrl =
        "https://github.com/HomuHomu833/android-sdk-custom/releases/download/" +
            "$SdkRelease/$SdkAsset"
    const val GradleUrl =
        "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip"
}

object GradleToolchain {

    private const val GuestOptRoot = "/opt"

    const val GuestSdkRoot = "$GuestOptRoot/android-sdk"
    const val GuestGradleHome = "$GuestOptRoot/gradle"

    private fun hostRoot(context: Context): File = ToolchainPaths.toolchainRoot(context)

    fun sdkDir(context: Context): File = File(hostRoot(context), "android-sdk")

    fun gradleDir(context: Context): File = File(hostRoot(context), "gradle")

    fun jdkMarker(context: Context): File = File(hostRoot(context), ".jdk17")

    fun platformDir(context: Context, sdk: Int): File =
        File(sdkDir(context), "platforms/android-$sdk")

    fun missing(context: Context, projectDir: File): List<GradlePiece> {
        val pieces = mutableListOf<GradlePiece>()
        if (!jdkMarker(context).isFile) pieces += GradlePiece.Jdk
        if (!File(sdkDir(context), "cmdline-tools/bin/sdkmanager").isFile) {
            pieces += GradlePiece.Sdk
        }
        val compileSdk = ProjectDetector.compileSdkOf(projectDir) ?: 36
        if (!platformDir(context, compileSdk).isDirectory) pieces += GradlePiece.Platform
        if (!File(gradleDir(context), "bin/gradle").isFile) pieces += GradlePiece.Gradle
        return pieces
    }

    fun nativeMissing(context: Context, projectDir: File): Boolean {
        if (!ProjectDetector.usesNative(projectDir)) return false
        return !ToolchainPaths.isInstalled(context, ToolchainKind.Ndk) ||
            !ToolchainPaths.isInstalled(context, ToolchainKind.CMake)
    }

    fun installScript(context: Context, projectDir: File): String {
        val compileSdk = ProjectDetector.compileSdkOf(projectDir) ?: 36
        return buildString {
            appendLine("set -e")
            appendLine("export DEBIAN_FRONTEND=noninteractive")
            appendLine()
            appendLine("if ! command -v java >/dev/null 2>&1; then")
            appendLine("  echo '==> installing JDK 17'")
            appendLine("  apt-get update -y")
            appendLine("  apt-get install -y openjdk-17-jdk-headless unzip curl xz-utils")
            appendLine("fi")
            appendLine("java -version")
            appendLine("touch $GuestOptRoot/.jdk17")
            appendLine()
            appendLine("if [ ! -x $GuestSdkRoot/cmdline-tools/bin/sdkmanager ]; then")
            appendLine("  echo '==> downloading Android SDK tools'")
            appendLine("  cd $GuestOptRoot")
            appendLine("  curl -fL --retry 3 -o sdk.tar.xz ${AndroidToolchainVersions.SdkUrl}")
            appendLine("  echo '==> extracting Android SDK tools'")
            appendLine("  tar -xJf sdk.tar.xz")
            appendLine("  rm -f sdk.tar.xz")
            appendLine("fi")
            appendLine()
            appendLine("export ANDROID_HOME=$GuestSdkRoot")
            appendLine("export ANDROID_SDK_ROOT=$GuestSdkRoot")
            appendLine("export PATH=\$ANDROID_HOME/cmdline-tools/bin:\$PATH")
            appendLine()
            appendLine("if [ ! -d $GuestSdkRoot/platforms/android-$compileSdk ]; then")
            appendLine("  echo '==> installing platform android-$compileSdk'")
            appendLine(
                "  sdkmanager --sdk_root=$GuestSdkRoot \"platforms;android-$compileSdk\" " +
                    "\"build-tools;36.1.0\""
            )
            appendLine("fi")
            appendLine()
            appendLine("if [ ! -x $GuestGradleHome/bin/gradle ]; then")
            appendLine("  echo '==> downloading Gradle ${AndroidToolchainVersions.GradleVersion}'")
            appendLine("  cd $GuestOptRoot")
            appendLine("  curl -fL --retry 3 -o gradle.zip ${AndroidToolchainVersions.GradleUrl}")
            appendLine("  unzip -q gradle.zip")
            appendLine("  rm -f gradle.zip")
            appendLine("  mv gradle-${AndroidToolchainVersions.GradleVersion} gradle")
            appendLine("fi")
            appendLine("$GuestGradleHome/bin/gradle --version")
            appendLine()
            appendLine("echo '==> android toolchain ready'")
        }
    }

    fun writeLocalProperties(context: Context, projectDir: File) {
        runCatching {
            val lines = mutableListOf("sdk.dir=$GuestSdkRoot")
            if (ToolchainPaths.isInstalled(context, ToolchainKind.Ndk)) {
                lines += "ndk.dir=${ToolchainPaths.guestDir(ToolchainKind.Ndk)}"
            }
            if (ToolchainPaths.isInstalled(context, ToolchainKind.CMake)) {
                lines += "cmake.dir=${ToolchainPaths.guestDir(ToolchainKind.CMake)}"
            }
            File(projectDir, "local.properties").writeText(lines.joinToString("\n") + "\n")
        }
    }

    fun prepareGuestDirs(context: Context) {
        runCatching {
            hostRoot(context).mkdirs()
            File(ProotConfig.rootfsDir(context), "opt").mkdirs()
        }
    }

}
