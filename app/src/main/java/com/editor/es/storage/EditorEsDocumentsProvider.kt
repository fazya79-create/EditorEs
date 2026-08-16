package com.editor.es.storage

import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import com.editor.es.R
import java.io.File
import java.io.FileNotFoundException

class EditorEsDocumentsProvider : DocumentsProvider() {

    companion object {
        private const val AllMimeTypes = "*/*"

        private val DefaultRootProjection = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES
        )

        private val DefaultDocumentProjection = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
        )

        private fun docId(file: File): String = file.absolutePath

        private fun fileForDocId(baseDir: File, docId: String): File {
            val file = File(docId)
            if (!file.canonicalPath.startsWith(baseDir.canonicalPath)) {
                throw FileNotFoundException("Access denied")
            }
            if (!file.exists()) throw FileNotFoundException(docId)
            return file
        }

        private fun mimeType(file: File): String {
            if (file.isDirectory) return Document.MIME_TYPE_DIR
            val dot = file.name.lastIndexOf('.')
            if (dot >= 0) {
                val extension = file.name.substring(dot + 1).lowercase()
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)?.let { return it }
            }
            return "application/octet-stream"
        }
    }

    private val baseDir: File by lazy { context!!.filesDir }

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<String>?): Cursor {
        val result = MatrixCursor(projection ?: DefaultRootProjection)
        val row = result.newRow()
        row.add(Root.COLUMN_ROOT_ID, docId(baseDir))
        row.add(Root.COLUMN_DOCUMENT_ID, docId(baseDir))
        row.add(Root.COLUMN_SUMMARY, null)
        row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE or Root.FLAG_SUPPORTS_SEARCH or Root.FLAG_SUPPORTS_IS_CHILD)
        row.add(Root.COLUMN_TITLE, context!!.getString(R.string.app_name))
        row.add(Root.COLUMN_MIME_TYPES, AllMimeTypes)
        row.add(Root.COLUMN_AVAILABLE_BYTES, baseDir.freeSpace)
        row.add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
        return result
    }

    override fun queryDocument(documentId: String, projection: Array<String>?): Cursor {
        val result = MatrixCursor(projection ?: DefaultDocumentProjection)
        includeFile(result, documentId, null)
        return result
    }

    override fun queryChildDocuments(parentDocumentId: String, projection: Array<String>?, sortOrder: String?): Cursor {
        val result = MatrixCursor(projection ?: DefaultDocumentProjection)
        val parent = fileForDocId(baseDir, parentDocumentId)
        parent.listFiles()?.forEach { includeFile(result, null, it) }
        return result
    }

    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        val file = fileForDocId(baseDir, documentId)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode))
    }

    override fun openDocumentThumbnail(documentId: String, sizeHint: Point, signal: CancellationSignal?): AssetFileDescriptor {
        val file = fileForDocId(baseDir, documentId)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0, file.length())
    }

    override fun createDocument(parentDocumentId: String, mimeType: String?, displayName: String): String {
        val parent = fileForDocId(baseDir, parentDocumentId)
        var newFile = File(parent, displayName)
        var conflictId = 2
        while (newFile.exists()) {
            newFile = File(parent, "$displayName (${conflictId++})")
        }
        val succeeded = if (Document.MIME_TYPE_DIR == mimeType) {
            newFile.mkdir()
        } else {
            runCatching { newFile.createNewFile() }.getOrDefault(false)
        }
        if (!succeeded) throw FileNotFoundException("Failed to create $displayName")
        return docId(newFile)
    }

    override fun deleteDocument(documentId: String) {
        val file = fileForDocId(baseDir, documentId)
        if (!deleteTree(file)) throw FileNotFoundException("Failed to delete $documentId")
    }

    private fun deleteTree(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteTree(it) }
        }
        if (!file.delete()) {
            file.setWritable(true)
            file.setExecutable(true)
            return file.delete()
        }
        return true
    }

    override fun getDocumentType(documentId: String): String =
        mimeType(fileForDocId(baseDir, documentId))

    override fun querySearchDocuments(rootId: String, query: String, projection: Array<String>?): Cursor {
        val result = MatrixCursor(projection ?: DefaultDocumentProjection)
        val root = fileForDocId(baseDir, rootId)
        val pending = ArrayDeque<File>().apply { add(root) }
        val lowercaseQuery = query.lowercase()
        var matches = 0
        while (pending.isNotEmpty() && matches < 50) {
            val file = pending.removeFirst()
            if (!file.canonicalPath.startsWith(baseDir.canonicalPath)) continue
            if (file.isDirectory) {
                file.listFiles()?.forEach { pending.add(it) }
            } else if (file.name.lowercase().contains(lowercaseQuery)) {
                includeFile(result, null, file)
                matches++
            }
        }
        return result
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        val parent = runCatching { fileForDocId(baseDir, parentDocumentId) }.getOrNull() ?: return false
        val child = runCatching { fileForDocId(baseDir, documentId) }.getOrNull() ?: return false
        return child.canonicalPath.startsWith(parent.canonicalPath)
    }

    private fun includeFile(result: MatrixCursor, docId: String?, fileArg: File?) {
        var documentId = docId
        var file = fileArg
        if (documentId == null) {
            documentId = docId(file!!)
        } else {
            file = fileForDocId(baseDir, documentId)
        }
        var flags = 0
        if (file.isDirectory) {
            if (file.canWrite()) flags = flags or Document.FLAG_DIR_SUPPORTS_CREATE
        } else if (file.canWrite()) {
            flags = flags or Document.FLAG_SUPPORTS_WRITE
        }
        if (file.parentFile?.canWrite() == true) flags = flags or Document.FLAG_SUPPORTS_DELETE
        val type = mimeType(file)
        if (type.startsWith("image/")) flags = flags or Document.FLAG_SUPPORTS_THUMBNAIL
        val row = result.newRow()
        row.add(Document.COLUMN_DOCUMENT_ID, documentId)
        row.add(Document.COLUMN_DISPLAY_NAME, file.name)
        row.add(Document.COLUMN_SIZE, file.length())
        row.add(Document.COLUMN_MIME_TYPE, type)
        row.add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
        row.add(Document.COLUMN_FLAGS, flags)
        row.add(Document.COLUMN_ICON, R.mipmap.ic_launcher)
    }
}
