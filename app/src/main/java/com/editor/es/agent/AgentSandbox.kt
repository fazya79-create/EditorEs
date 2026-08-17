package com.editor.es.agent

import android.content.Context
import com.editor.es.proot.ProotConfig
import java.io.File

data class AgentLaunch(
    val guestCwd: String,
    val guestHome: String,
    val isolation: ProotConfig.Isolation,
    val bootCommand: String
)

object AgentSandbox {

    private const val HomeRoot = "agent-home"

    fun guestHomeFor(projectDir: File): String =
        "/$HomeRoot/" + projectDir.name.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun hostHomeFor(context: Context, projectDir: File): File =
        File(ProotConfig.rootfsDir(context), guestHomeFor(projectDir).trimStart('/'))

    fun prepare(context: Context, projectDir: File) {
        runCatching {
            hostHomeFor(context, projectDir).mkdirs()
            File(ProotConfig.rootfsDir(context), projectDir.absolutePath.trimStart('/')).mkdirs()
        }
    }

    fun launch(context: Context, spec: AgentSpec, projectDir: File): AgentLaunch {
        prepare(context, projectDir)
        val guestHome = guestHomeFor(projectDir)
        return AgentLaunch(
            guestCwd = projectDir.absolutePath,
            guestHome = guestHome,
            isolation = ProotConfig.Isolation.Project(
                hostPath = projectDir.absolutePath,
                guestHome = guestHome
            ),
            bootCommand = spec.runCommand
        )
    }
}
