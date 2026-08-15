package com.editor.es.editor

import android.content.Context
import android.graphics.Typeface
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

object EditorTheme {

    fun apply(editor: io.github.rosemoe.sora.widget.CodeEditor, context: Context) {
        runCatching {
            editor.colorScheme = TextMateColorScheme.create()
            editor.typefaceText = Typeface.createFromAsset(context.assets, "fonts/JetBrainsMono-Regular.ttf")
        }.onFailure {
            editor.colorScheme = object : EditorColorScheme() {
                override fun applyDefault() {
                    super.applyDefault()
                    setColor(WHOLE_BACKGROUND, 0xFF001219.toInt())
                    setColor(LINE_NUMBER_BACKGROUND, 0xFF001219.toInt())
                    setColor(TEXT_NORMAL, 0xFFE8F4F2.toInt())
                    setColor(LINE_NUMBER, 0xFF5C7A80.toInt())
                    setColor(LINE_NUMBER_CURRENT, 0xFFEE9B00.toInt())
                    setColor(LINE_DIVIDER, 0x330A9396)
                    setColor(SELECTED_TEXT_BACKGROUND, 0x660A9396)
                    setColor(SELECTION_HANDLE, 0xFFEE9B00.toInt())
                    setColor(CURRENT_LINE, 0x220A9396)
                    setColor(CURSOR_COLOR, 0xFFEE9B00.toInt())
                }
            }
        }
    }
}
