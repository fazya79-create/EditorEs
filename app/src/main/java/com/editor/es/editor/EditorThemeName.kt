package com.editor.es.editor

import io.github.rosemoe.sora.widget.CodeEditor

/**
 * Editor color themes. Names are persisted via AppSettings (key "editor_theme").
 * Extension-name style, avoids clashing with the existing `EditorTheme` object.
 */
enum class EditorThemeName(
    val displayName: String,
    val description: String
) {
    TEXTMATE("TextMate (VS Code)", "Grammar-aware dark_plus highlighting"),
    DRACULA("Dracula", "Purple / pink / cyan palette on #282a36"),
    DARCOLA("Darcula", "Android Studio classic dark"),
    VS2019("VS 2019 Dark", "Visual Studio 2019 dark"),
    ECLIPSE("Eclipse", "Classic light IDE"),
    GITHUB("GitHub Dark", "GitHub's official dark"),
    NOTEPADXX("Notepad++", "Classic Notepad++ styling");

    fun apply(editor: CodeEditor) {
        when (this) {
            TEXTMATE -> EditorTheme.applyTextMate(editor)
            DRACULA -> EditorTheme.applyDracula(editor)
            DARCOLA -> EditorTheme.applyDarcula(editor)
            VS2019 -> EditorTheme.applyVS2019(editor)
            ECLIPSE -> EditorTheme.applyEclipse(editor)
            GITHUB -> EditorTheme.applyGitHub(editor)
            NOTEPADXX -> EditorTheme.applyNotepadXX(editor)
        }
    }

    companion object {
        const val PREF_KEY = "editor_theme"
        val DEFAULT = TEXTMATE

        fun fromName(name: String?): EditorThemeName =
            entries.firstOrNull { it.name == name } ?: DEFAULT

        fun current(): EditorThemeName = fromName(
            com.editor.es.data.AppSettings.string(PREF_KEY, DEFAULT.name)
        )

        fun save(theme: EditorThemeName) {
            com.editor.es.data.AppSettings.putString(PREF_KEY, theme.name)
        }
    }
}
