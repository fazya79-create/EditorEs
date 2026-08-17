package com.editor.es.build

import android.content.Context
import java.io.File

object GradleCommands {

    private const val ScriptDir = "gradle-scripts"

    private fun envPrelude(): List<String> = listOf(
        "export ANDROID_HOME=${GradleToolchain.GuestSdkRoot}",
        "export ANDROID_SDK_ROOT=${GradleToolchain.GuestSdkRoot}",
        "export GRADLE_USER_HOME=/root/.gradle",
        "export PATH=${GradleToolchain.GuestGradleHome}/bin:\$PATH",
        "export JAVA_HOME=\$(dirname \$(dirname \$(readlink -f \$(command -v java))))"
    )

    fun build(projectDir: File, args: String): String {
        val steps = mutableListOf("echo '=== gradle $args ==='")
        steps += envPrelude()
        steps += "cd ${projectDir.absolutePath}"
        steps += "gradle --no-daemon $args"
        return steps.joinToString("; ") + "; echo \"=== exit code: \$? ===\""
    }

    fun install(context: Context, projectDir: File): String {
        val dir = File(
            com.editor.es.proot.ProotConfig.rootfsDir(context),
            "root/$ScriptDir"
        ).apply { mkdirs() }
        val script = File(dir, "install-android-toolchain.sh")
        runCatching {
            script.writeText(GradleToolchain.installScript(context, projectDir))
            script.setExecutable(true)
        }
        return "echo '=== installing Android build tools ==='; " +
            "bash /root/$ScriptDir/install-android-toolchain.sh; " +
            "echo \"=== exit code: \$? ===\""
    }
}
