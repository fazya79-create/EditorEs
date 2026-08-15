package com.editor.es.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector

object FileTypeIcons {

    fun resolve(fileName: String): ImageVector {
        val lower = fileName.lowercase()
        return when {
            lower == "cmakelists.txt" || lower.endsWith(".cmake") -> FileTypeCMake
            lower.endsWith(".mk") -> FileTypeMakefile
            lower.endsWith(".cpp") || lower.endsWith(".cc") || lower.endsWith(".cxx") ||
                lower.endsWith(".hpp") || lower.endsWith(".hh") || lower.endsWith(".hxx") ||
                lower.endsWith(".h") -> FileTypeCpp
            lower.endsWith(".c") -> FileTypeC
            lower.endsWith(".kt") || lower.endsWith(".kts") -> FileTypeKotlin
            lower.endsWith(".java") -> FileTypeJava
            lower.endsWith(".json") -> FileTypeJson
            lower.endsWith(".md") -> FileTypeMarkdown
            lower.endsWith(".xml") -> FileTypeXml
            else -> FileTypeDocument
        }
    }
}
