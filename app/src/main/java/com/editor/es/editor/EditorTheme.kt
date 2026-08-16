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
}
