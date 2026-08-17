package com.editor.es.agent

import android.content.Context
import com.editor.es.data.ProjectCreator
import com.editor.es.proot.ProotConfig
import java.io.File

data class AgentLaunch(
    val guestCwd: String,
    val guestHome: String,
    val isolation: ProotConfig.Isolation,
    val bootCommand: String
)

object AgentSandbox {

    private const val GuestHome = "/agent-home"

    fun workspaceDir(): File = ProjectCreator.baseDir()

    fun prepare(context: Context, projectDir: File?) {
        val rootfs = ProotConfig.rootfsDir(context)
        runCatching {
            File(rootfs, GuestHome.trimStart('/')).mkdirs()
            File(rootfs, workspaceDir().absolutePath.trimStart('/')).mkdirs()
            projectDir?.let { File(rootfs, it.absolutePath.trimStart('/')).mkdirs() }
        }
    }

    fun launch(context: Context, spec: AgentSpec, projectDir: File?): AgentLaunch {
        prepare(context, projectDir)
        val workspace = workspaceDir()
        val cwd = projectDir?.takeIf { it.absolutePath.startsWith(workspace.absolutePath) }
            ?: workspace
        return AgentLaunch(
            guestCwd = cwd.absolutePath,
            guestHome = GuestHome,
            isolation = ProotConfig.Isolation.Workspace(
                hostPath = workspace.absolutePath,
                guestHome = GuestHome
            ),
            bootCommand = spec.runCommand
        )
    }
}
