package com.editor.es.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector

object FileTypeIcons {

    private val byExactName = mapOf(
        "cmakelists.txt" to "cmake",
        "makefile" to "makefile",
        "dockerfile" to "console",
        ".gitignore" to "git",
        ".gitattributes" to "git",
        ".gitmodules" to "git",
        ".clangd" to "yaml",
        ".clang-format" to "yaml",
        "cmakepresets.json" to "cmake",
        "cmakeuserpresets.json" to "cmake",
        "build.ninja" to "console",
        "compile_commands.json" to "json",
        "license" to "document",
        "readme" to "markdown"
    )

    private val byExtension = mapOf(
        "mp3" to "audio",
        "wav" to "audio",
        "ogg" to "audio",
        "flac" to "audio",
        "m4a" to "audio",
        "aac" to "audio",
        "opus" to "audio",
        "mid" to "audio",
        "png" to "image",
        "jpg" to "image",
        "jpeg" to "image",
        "gif" to "image",
        "bmp" to "image",
        "webp" to "image",
        "ico" to "image",
        "tiff" to "image",
        "svg" to "svg",
        "mp4" to "video",
        "mkv" to "video",
        "avi" to "video",
        "mov" to "video",
        "webm" to "video",
        "flv" to "video",
        "zip" to "zip",
        "rar" to "zip",
        "7z" to "zip",
        "tar" to "zip",
        "gz" to "zip",
        "xz" to "zip",
        "bz2" to "zip",
        "zst" to "zip",
        "apk" to "zip",
        "aar" to "zip",
        "jar" to "zip",
        "pdf" to "pdf",
        "py" to "python",
        "pyi" to "python",
        "js" to "javascript",
        "mjs" to "javascript",
        "cjs" to "javascript",
        "ts" to "typescript",
        "tsx" to "typescript",
        "html" to "html",
        "htm" to "html",
        "css" to "css",
        "scss" to "css",
        "yaml" to "yaml",
        "yml" to "yaml",
        "toml" to "toml",
        "ini" to "toml",
        "cfg" to "toml",
        "conf" to "toml",
        "properties" to "toml",
        "sh" to "console",
        "bash" to "console",
        "zsh" to "console",
        "fish" to "console",
        "bat" to "console",
        "cmd" to "console",
        "ps1" to "console",
        "ttf" to "font",
        "otf" to "font",
        "woff" to "font",
        "woff2" to "font",
        "db" to "database",
        "sqlite" to "database",
        "sqlite3" to "database",
        "sql" to "database",
        "so" to "exe",
        "dll" to "exe",
        "dylib" to "exe",
        "exe" to "exe",
        "o" to "exe",
        "a" to "exe",
        "bin" to "exe",
        "elf" to "exe",
        "lock" to "lock",
        "log" to "log",
        "asm" to "assembly",
        "s" to "assembly",
        "rs" to "rust",
        "go" to "go",
        "lua" to "lua",
        "gradle" to "gradle",
        "pem" to "key",
        "key" to "key",
        "keystore" to "key",
        "jks" to "key",
        "crt" to "key",
        "csv" to "table",
        "tsv" to "table",
        "xlsx" to "table",
        "txt" to "document"
    )

    private val builtins = mapOf(
        "cmake" to FileTypeCMake,
        "makefile" to FileTypeMakefile,
        "cpp" to FileTypeCpp,
        "c" to FileTypeC,
        "kotlin" to FileTypeKotlin,
        "java" to FileTypeJava,
        "json" to FileTypeJson,
        "markdown" to FileTypeMarkdown,
        "xml" to FileTypeXml,
        "document" to FileTypeDocument
    )

    fun resolve(fileName: String): ImageVector {
        val lower = fileName.lowercase()
        val key = byExactName[lower]
            ?: byExactName[lower.substringBeforeLast('.', lower)]
            ?: nativeKey(lower)
            ?: byExtension[lower.substringAfterLast('.', "")]
            ?: "document"
        builtins[key]?.let { return it }
        return materialIcon(key) ?: FileTypeDocument
    }

    private fun nativeKey(lower: String): String? = when {
        lower.endsWith(".cmake") -> "cmake"
        lower.endsWith(".mk") -> "makefile"
        lower.endsWith(".cpp") || lower.endsWith(".cc") || lower.endsWith(".cxx") ||
            lower.endsWith(".c++") || lower.endsWith(".hpp") || lower.endsWith(".hh") ||
            lower.endsWith(".hxx") || lower.endsWith(".h++") || lower.endsWith(".h") -> "cpp"
        lower.endsWith(".c") -> "c"
        lower.endsWith(".kt") || lower.endsWith(".kts") -> "kotlin"
        lower.endsWith(".java") -> "java"
        lower.endsWith(".json") -> "json"
        lower.endsWith(".md") || lower.endsWith(".markdown") -> "markdown"
        lower.endsWith(".xml") -> "xml"
        else -> null
    }
}
