package com.editor.es.editor

import android.graphics.Typeface
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.schemes.SchemeEclipse
import io.github.rosemoe.sora.widget.schemes.SchemeGitHub
import io.github.rosemoe.sora.widget.schemes.SchemeNotepadXX
import io.github.rosemoe.sora.widget.schemes.SchemeVS2019
import org.eclipse.tm4e.core.registry.IThemeSource

object EditorTheme {

    const val THEME_TEXTMATE = "TEXTMATE"
    const val THEME_DRACULA = "DRACULA"
    const val THEME_DARCOLA = "DARCOLA"
    const val THEME_VS2019 = "VS2019"
    const val THEME_ECLIPSE = "ECLIPSE"
    const val THEME_GITHUB = "GITHUB"
    const val THEME_NOTEPADXX = "NOTEPADXX"

    private var draculaLoaded = false

    fun apply(editor: CodeEditor, context: Context? = null) {
        applyTextMate(editor)
        context ?: return
        runCatching {
            editor.typefaceText = Typeface.createFromAsset(context.assets, "fonts/JetBrainsMono-Regular.ttf")
        }
    }

    /**
     * WHY THIS EXISTS: C/C++/Kotlin/... highlighting uses TextMateLanguage, whose spans
     * carry token color ids >= 255. Only TextMateColorScheme can resolve those ids.
     * A plain built-in scheme (SchemeDarcula etc.) returns 0 (transparent black) for
     * them -> INVISIBLE TEXT on any background. Every theme entry point below either
     * uses a TextMateColorScheme, or overrides getColor(255+) to fall back to
     * TEXT_NORMAL so text is always visible.
     */

    /** Register dracula.json with the TM ThemeRegistry (once per process). */
    fun ensureDraculaThemeLoaded() {
        if (draculaLoaded) return
        runCatching {
            val registry = ThemeRegistry.getInstance()
            registry.setTheme("dark_plus")
            val source = IThemeSource.fromInputStream(
                FileProviderRegistry.getInstance().tryGetInputStream("textmate/dracula.json"),
                "textmate/dracula.json",
                null
            )
            registry.loadTheme(ThemeModel(source, "dracula").apply { isDark = true })
            draculaLoaded = true
        }
    }

    /** VS Code-style TextMate scheme driven by dark_plus.json */
    fun applyTextMate(editor: CodeEditor) {
        runCatching {
            val registry = ThemeRegistry.getInstance()
            registry.setTheme("dark_plus")
            editor.colorScheme = TextMateColorScheme.create(registry)
        }.onFailure {
            editor.colorScheme = fallbackScheme()
        }
    }

    /** Dracula: full per-token colors via textmate/dracula.json */
    fun applyDracula(editor: CodeEditor) {
        ensureDraculaThemeLoaded()
        val ok = runCatching {
            val registry = ThemeRegistry.getInstance()
            registry.setTheme("dracula")
            editor.colorScheme = TextMateColorScheme.create(registry)
        }.isSuccess
        if (!ok) {
            editor.colorScheme = object : SchemeDarcula() {
                override fun applyDefault() {
                    super.applyDefault()
                    setColor(WHOLE_BACKGROUND, 0xFF282A36.toInt())
                    setColor(LINE_NUMBER_BACKGROUND, 0xFF282A36.toInt())
                    setColor(CURRENT_LINE, 0xFF44475A.toInt())
                    setColor(TEXT_NORMAL, 0xFFF8F8F2.toInt())
                    setColor(KEYWORD, 0xFFFF79C6.toInt())
                    setColor(OPERATOR, 0xFFFF79C6.toInt())
                    setColor(FUNCTION_NAME, 0xFF50FA7B.toInt())
                    setColor(IDENTIFIER_VAR, 0xFF8BE9FD.toInt())
                    setColor(LITERAL, 0xFFF1FA8C.toInt())
                    setColor(COMMENT, 0xFF6272A4.toInt())
                    setColor(LINE_NUMBER, 0xFF6272A4.toInt())
                    setColor(LINE_NUMBER_CURRENT, 0xFFF8F8F2.toInt())
                    setColor(SELECTED_TEXT_BACKGROUND, 0xFF44475A.toInt())
                }
                override fun getColor(type: Int): Int =
                    if (type >= 255) super.getColor(TEXT_NORMAL) else super.getColor(type)
            }
        }
    }

    /** Android Studio Darcula chrome + dark_plus token colors */
    fun applyDarcula(editor: CodeEditor) = applyBuiltin(editor, SchemeDarcula())

    /** Visual Studio 2019 dark chrome + dark_plus token colors */
    fun applyVS2019(editor: CodeEditor) = applyBuiltin(editor, SchemeVS2019())

    /** Eclipse light chrome (tokens fall back to black — no light TM theme bundled) */
    fun applyEclipse(editor: CodeEditor) = applyLightBuiltin(editor, SchemeEclipse())

    /** GitHub dark chrome + dark_plus token colors */
    fun applyGitHub(editor: CodeEditor) = applyBuiltin(editor, SchemeGitHub())

    /** Notepad++ light chrome (tokens fall back to black) */
    fun applyNotepadXX(editor: CodeEditor) = applyLightBuiltin(editor, SchemeNotepadXX())

    /**
     * Dark built-in schemes: keep the built-in chrome (background, line numbers,
     * selection, completion window...) by answering ids < 255 from the builtin,
     * while token ids >= 255 resolve through the TM theme (dark_plus) so syntax
     * colors stay colorful AND visible.
     */
    private fun applyBuiltin(editor: CodeEditor, builtin: EditorColorScheme) {
        val ok = runCatching {
            builtin.applyDefault()
            val registry = ThemeRegistry.getInstance()
            registry.setTheme("dark_plus")
            editor.colorScheme = object : TextMateColorScheme(registry, registry.currentThemeModel) {
                override fun getColor(type: Int): Int =
                    if (type < 255) builtin.getColor(type) else super.getColor(type)
            }
        }.isSuccess
        if (!ok) editor.colorScheme = tokenSafe(builtin)
    }

    /**
     * Light built-in schemes: dark_plus tokens would be light-on-light, so keep the
     * builtin scheme entirely and just make TextMate token ids (255+) fall back to
     * the scheme's TEXT_NORMAL instead of invisible color 0.
     */
    private fun applyLightBuiltin(editor: CodeEditor, builtin: EditorColorScheme) {
        editor.colorScheme = tokenSafe(builtin)
    }

    /** Wraps a built-in scheme so unknown TM token ids render as TEXT_NORMAL, never 0. */
    private fun tokenSafe(builtin: EditorColorScheme): EditorColorScheme = when (builtin) {
        is SchemeDarcula -> object : SchemeDarcula() {
            override fun getColor(type: Int): Int =
                if (type >= 255) super.getColor(TEXT_NORMAL) else super.getColor(type)
        }
        is SchemeVS2019 -> object : SchemeVS2019() {
            override fun getColor(type: Int): Int =
                if (type >= 255) super.getColor(TEXT_NORMAL) else super.getColor(type)
        }
        is SchemeEclipse -> object : SchemeEclipse() {
            override fun getColor(type: Int): Int =
                if (type >= 255) super.getColor(TEXT_NORMAL) else super.getColor(type)
        }
        is SchemeGitHub -> object : SchemeGitHub() {
            override fun getColor(type: Int): Int =
                if (type >= 255) super.getColor(TEXT_NORMAL) else super.getColor(type)
        }
        is SchemeNotepadXX -> object : SchemeNotepadXX() {
            override fun getColor(type: Int): Int =
                if (type >= 255) super.getColor(TEXT_NORMAL) else super.getColor(type)
        }
        else -> builtin
    }

    /** Last-resort dark scheme (matches the app's original look). */
    private fun fallbackScheme(): EditorColorScheme = object : EditorColorScheme() {
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
        override fun getColor(type: Int): Int =
            if (type >= 255) super.getColor(TEXT_NORMAL) else super.getColor(type)
    }
}
