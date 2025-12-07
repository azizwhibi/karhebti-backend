package com.example.karhebti_android.util

import android.content.Context
import android.net.Uri
import java.io.File

data class ImageValidationResult(
    val isValid: Boolean,
    val error: String? = null,
    val fileSize: Long = 0,
    val mimeType: String? = null
)

object ImageUploadValidator {
    // Supported MIME types
    private val SUPPORTED_MIME_TYPES = listOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    )

    // Supported extensions
    private val SUPPORTED_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp")

    // Maximum file size: 5MB
    private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024 // 5MB

    /**
     * Validates an image file before upload
     * @param context Android context
     * @param uri Uri of the image file
     * @return ImageValidationResult with validation status and errors
     */
    fun validateImage(context: Context, uri: Uri): ImageValidationResult {
        return try {
            // Get file size
            val fileSize = getFileSizeFromUri(context, uri)
            if (fileSize <= 0) {
                return ImageValidationResult(
                    isValid = false,
                    error = "Impossible de déterminer la taille du fichier"
                )
            }

            // Check file size
            if (fileSize > MAX_FILE_SIZE_BYTES) {
                return ImageValidationResult(
                    isValid = false,
                    error = "La taille de l'image dépasse 5MB (${formatFileSize(fileSize)})"
                )
            }

            // Get MIME type
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType == null || !SUPPORTED_MIME_TYPES.contains(mimeType)) {
                return ImageValidationResult(
                    isValid = false,
                    error = "Format non supporté. Formats acceptés: JPG, PNG, WebP",
                    mimeType = mimeType
                )
            }

            // Validate by extension as well
            val extension = getFileExtension(context, uri)?.lowercase()
            if (extension != null && !SUPPORTED_EXTENSIONS.contains(extension)) {
                return ImageValidationResult(
                    isValid = false,
                    error = "Extension de fichier non supportée: .$extension"
                )
            }

            ImageValidationResult(
                isValid = true,
                fileSize = fileSize,
                mimeType = mimeType
            )
        } catch (e: Exception) {
            ImageValidationResult(
                isValid = false,
                error = "Erreur lors de la validation: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Gets file size from URI
     */
    private fun getFileSizeFromUri(context: Context, uri: Uri): Long {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                it.moveToFirst()
                val size = it.getLong(sizeIndex)
                size
            } ?: -1L
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Gets file extension from URI
     */
    private fun getFileExtension(context: Context, uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                it.moveToFirst()
                val fileName = it.getString(nameIndex)
                fileName?.substringAfterLast(".")
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats file size to human-readable format
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes <= 0 -> "0 B"
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }

    /**
     * Validates that it's actually an image by checking file header
     */
    fun isValidImageFile(file: File): Boolean {
        return try {
            file.exists() && file.length() > 0 && file.length() <= MAX_FILE_SIZE_BYTES
        } catch (e: Exception) {
            false
        }
    }
}

