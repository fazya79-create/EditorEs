package com.editor.es.editor

import android.content.Context
import android.graphics.Typeface
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

object EditorTheme {

    fun apply(editor: CodeEditor, context: Context) {
        runCatching {
            editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            editor.typefaceText = Typeface.createFromAsset(context.assets, "fonts/JetBrainsMono-Regular.ttf")
        }.onFailure {
            editor.colorScheme = object : EditorColorScheme() {
                override fun applyDefault() {
                    super.applyDefault()
                    setColor(WHOLE_BACKGROUND, 0xFF1E1E1E.toInt())
                    setColor(LINE_NUMBER_BACKGROUND, 0xFF1E1E1E.toInt())
                    setColor(TEXT_NORMAL, 0xFFD4D4D4.toInt())
                    setColor(LINE_NUMBER, 0xFF858585.toInt())
                    setColor(LINE_NUMBER_CURRENT, 0xFFC6C6C6.toInt())
                    setColor(LINE_DIVIDER, 0x335C6370)
                    setColor(SELECTED_TEXT_BACKGROUND, 0x66094771)
                    setColor(SELECTION_HANDLE, 0xFF007ACC.toInt())
                    setColor(CURRENT_LINE, 0x22282828)
                }
            }
        }
    }
}
