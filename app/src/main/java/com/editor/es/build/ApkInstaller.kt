package com.editor.es.build

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ApkInstaller {

    fun latestApk(projectDir: File): File? {
        val roots = listOf(
            File(projectDir, "app/build/outputs/apk/debug"),
            File(projectDir, "app/build/outputs/apk/release")
        )
        return roots
            .filter { it.isDirectory }
            .flatMap { it.listFiles { f -> f.extension == "apk" }?.toList() ?: emptyList() }
            .maxByOrNull { it.lastModified() }
    }

    fun install(context: Context, apk: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apk
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
