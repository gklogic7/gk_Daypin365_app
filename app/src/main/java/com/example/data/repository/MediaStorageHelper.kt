package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class CachedMediaResult(
    val uriString: String,
    val displayName: String
)

object MediaStorageHelper {
    private const val MEDIA_DIR_NAME = "reminder_media"

    suspend fun persistPickedMedia(
        context: Context,
        sourceUri: Uri,
        prefix: String
    ): CachedMediaResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        // Try taking persistable permission when supported (e.g., SAF OpenDocument)
        runCatching {
            resolver.takePersistableUriPermission(
                sourceUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        val displayName = resolveDisplayName(context, sourceUri) ?: "$prefix attachment"
        val mimeType = runCatching { resolver.getType(sourceUri) }.getOrNull()?.lowercase()
        val mediaDir = File(context.filesDir, MEDIA_DIR_NAME).apply { mkdirs() }
        val sanitizedBase = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_").takeLast(48)
        val sanitizedName = ensureAnimatedImageExtension(sanitizedBase, mimeType)
        val targetFile = File(mediaDir, "${prefix}_${System.currentTimeMillis()}_$sanitizedName")

        val copied = runCatching {
            resolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output, bufferSize = 16 * 1024)
                }
            }
            targetFile.exists() && targetFile.length() > 0L
        }.getOrDefault(false)

        if (copied) {
            CachedMediaResult(
                uriString = Uri.fromFile(targetFile).toString(),
                displayName = displayName
            )
        } else {
            runCatching { targetFile.delete() }
            CachedMediaResult(
                uriString = sourceUri.toString(),
                displayName = displayName
            )
        }
    }

    fun deleteLocalMediaIfManaged(context: Context, uriString: String?) {
        if (uriString.isNullOrBlank()) return
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return
        if (uri.scheme == "file") {
            val file = uri.path?.let { File(it) } ?: return
            val mediaRoot = File(context.filesDir, MEDIA_DIR_NAME)
            if (file.absolutePath.startsWith(mediaRoot.absolutePath) && file.exists()) {
                runCatching { file.delete() }
            }
        }
    }

    fun createTempCameraProofUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera_proofs").apply { mkdirs() }
        val file = File(dir, "proof_${System.currentTimeMillis()}.jpg")
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        }.getOrNull()
    }

    private fun ensureAnimatedImageExtension(baseName: String, mimeType: String?): String {
        val lower = baseName.lowercase()
        return when {
            mimeType == "image/gif" && !lower.endsWith(".gif") -> "$baseName.gif"
            mimeType == "image/webp" && !lower.endsWith(".webp") -> "$baseName.webp"
            else -> baseName
        }
    }
}
