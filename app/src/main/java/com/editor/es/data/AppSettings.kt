package com.editor.es.data

import android.content.Context

class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences("editor_es_settings", Context.MODE_PRIVATE)

    var lspEnabled: Boolean
        get() = prefs.getBoolean(KeyLspEnabled, false)
        set(value) {
            prefs.edit().putBoolean(KeyLspEnabled, value).apply()
        }

    companion object {
        private const val KeyLspEnabled = "lsp_enabled"
    }
}
