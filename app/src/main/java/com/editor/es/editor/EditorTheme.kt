package com.editor.es.editor

import android.content.Context
import android.graphics.Typeface
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula
import io.github.rosemoe.sora.widget.schemes.SchemeEclipse
import io.github.rosemoe.sora.widget.schemes.SchemeGitHub
import io.github.rosemoe.sora.widget.schemes.SchemeNotepadXX
import io.github.rosemoe.sora.widget.schemes.SchemeVS2019

object EditorTheme {

    fun apply(editor: CodeEditor, context: Context) {
        runCatching {
            editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            editor.typefaceText = Typeface.createFromAsset(context.assets, "fonts/JetBrainsMono-Regular.ttf")
        }.onFailure {
            editor.colorScheme = object : EditorColorScheme() {
                override fun applyDefault() {
                    super.applyDefault()
                    setColor(WHOLE_BACKGROUND, 0xFF0A2129.toInt())
                    setColor(LINE_NUMBER_BACKGROUND, 0xFF0A2129.toInt())
                    setColor(TEXT_NORMAL, 0xFFDDF5EA.toInt())
                    setColor(LINE_NUMBER, 0xFF567A6B.toInt())
                    setColor(LINE_NUMBER_CURRENT, 0xFFB7E9D3.toInt())
                    setColor(LINE_DIVIDER, 0x330E3A31)
                    setColor(SELECTED_TEXT_BACKGROUND, 0x66027A52)
                    setColor(SELECTION_HANDLE, 0xFF02F5A1.toInt())
                    setColor(CURRENT_LINE, 0x2202F5A1)
                }
            }
        }
    }

    /** VS Code-style TextMate scheme driven by dark_plus.json */
    fun applyTextMate(editor: CodeEditor) {
        runCatching {
            editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
        }
    }

    /** Dracula palette built on top of Darcula (upstream scheme) */
    fun applyDracula(editor: CodeEditor) {
        editor.colorScheme = object : SchemeDarcula() {
            override fun applyDefault() {
                super.applyDefault()
                // Dracula palette: bg #282a36, text #f8f8f2, comment #6272a4
                // cyan #8be9fd, green #50fa7b, orange #ffb86c, pink #ff79c6,
                // purple #bd93f9, red #ff5555, yellow #f1fa8c
                setColor(WHOLE_BACKGROUND, 0xFF282A36.toInt())
                setColor(LINE_NUMBER_BACKGROUND, 0xFF282A36.toInt())
                setColor(CURRENT_LINE, 0xFF44475A.toInt())
                setColor(TEXT_NORMAL, 0xFFF8F8F2.toInt())
                setColor(COMMENT, 0xFF6272A4.toInt())
                setColor(KEYWORD, 0xFFFF79C6.toInt())          // pink
                setColor(OPERATOR, 0xFFFF79C6.toInt())          // pink
                setColor(FUNCTION_NAME, 0xFF50FA7B.toInt())     // green
                setColor(IDENTIFIER_NAME, 0xFFF8F8F2.toInt())
                setColor(IDENTIFIER_VAR, 0xFF8BE9FD.toInt())    // cyan
                setColor(LITERAL, 0xFFF1FA8C.toInt())           // yellow
                setColor(NUMBER_LITERAL, 0xFFBD93F9.toInt())    // purple
                setColor(ANNOTATION, 0xFF8BE9FD.toInt())
                setColor(LINE_NUMBER, 0xFF6272A4.toInt())
                setColor(LINE_NUMBER_CURRENT, 0xFFF8F8F2.toInt())
                setColor(LINE_DIVIDER, 0x336272A4)
                setColor(SELECTED_TEXT_BACKGROUND, 0xFF44475A.toInt())
                setColor(SELECTION_HANDLE, 0xFFBD93F9.toInt())
                setColor(SELECTION_INSERT, 0xFFF8F8F2.toInt())
                setColor(MATCHED_TEXT_BACKGROUND, 0xFF44475A.toInt())
                setColor(BLOCK_LINE, 0xFF44475A.toInt())
                setColor(BLOCK_LINE_CURRENT, 0xFF6272A4.toInt())
                setColor(SCROLL_BAR_THUMB, 0xFF6272A4.toInt())
                setColor(COMPLETION_WND_BACKGROUND, 0xFF282A36.toInt())
                setColor(COMPLETION_WND_CORNER, 0xFF44475A.toInt())
            }
        }
    }

    /** Android Studio Darcula (upstream built-in) */
    fun applyDarcula(editor: CodeEditor) {
        editor.colorScheme = SchemeDarcula()
    }

    /** Visual Studio 2019 dark (upstream built-in) */
    fun applyVS2019(editor: CodeEditor) {
        editor.colorScheme = SchemeVS2019()
    }

    /** Eclipse (upstream built-in, light) */
    fun applyEclipse(editor: CodeEditor) {
        editor.colorScheme = SchemeEclipse()
    }

    /** GitHub dark (upstream built-in) */
    fun applyGitHub(editor: CodeEditor) {
        editor.colorScheme = SchemeGitHub()
    }

    /** Notepad++ (upstream built-in) */
    fun applyNotepadXX(editor: CodeEditor) {
        editor.colorScheme = SchemeNotepadXX()
    }
}
