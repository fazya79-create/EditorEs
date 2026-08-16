package com.editor.es

import android.app.Application
import com.editor.es.build.ToolchainPaths
import com.editor.es.service.TermuxService

class EditorEsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        TermuxService.createChannel(this)
        runCatching { ToolchainPaths.migrateLegacyLayout(this) }
    }
}
