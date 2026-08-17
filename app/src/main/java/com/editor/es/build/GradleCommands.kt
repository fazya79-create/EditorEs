package com.editor.es.build

import android.content.Context
import java.io.File

object GradleCommands {

    private const val ScriptDir = "gradle-scripts"

    private fun envPrelude(): String = buildString {
        appendLine("export ANDROID_HOME=${GradleToolchain.GuestSdkRoot}")
        appendLine("export ANDROID_SDK_ROOT=${GradleToolchain.GuestSdkRoot}")
        appendLine("export GRADLE_USER_HOME=/root/.gradle")
        appendLine("export PATH=${GradleToolchain.GuestGradleHome}/bin:\$PATH")
        appendLine("export JAVA_HOME=\$(dirname \$(dirname \$(readlink -f \$(command -v java))))")
    }

    fun build(projectDir: File, args: String): String = buildString {
        append("echo '=== gradle $args ==='; ")
        append(envPrelude().replace("\n", " "))
        append("cd ")
        append(projectDir.absolutePath)
        append(" && gradle --no-daemon $args; ")
        append("echo \"=== exit code: \$? ===\"")
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
