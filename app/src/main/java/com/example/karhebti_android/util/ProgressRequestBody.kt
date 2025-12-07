package com.example.karhebti_android.util

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Progress tracking wrapper for RequestBody
 */
class ProgressRequestBody(
    private val requestBody: RequestBody,
    private val progressCallback: (progress: Int) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = requestBody.contentType()

    override fun contentLength(): Long = requestBody.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val contentLength = contentLength()
        val buffer = okio.Buffer()
        requestBody.writeTo(buffer)

        var uploaded = 0L
        val bufferSize = 2048L

        buffer.inputStream().use { input ->
            val data = ByteArray(bufferSize.toInt())
            var read: Int
            while (input.read(data).also { read = it } != -1) {
                uploaded += read
                sink.write(data, 0, read)
                val progress = (uploaded * 100 / contentLength).toInt()
                progressCallback(progress)
            }
        }
    }
}

/**
 * Image upload progress state
 */
data class ImageUploadProgress(
    val progress: Int = 0,  // 0-100
    val isUploading: Boolean = false,
    val totalBytes: Long = 0,
    val uploadedBytes: Long = 0,
    val error: String? = null
) {
    val formattedProgress: String
        get() = "$progress%"

    val formattedSize: String
        get() {
            val total = ImageUploadValidator.formatFileSize(totalBytes)
            val uploaded = ImageUploadValidator.formatFileSize(uploadedBytes)
            return "$uploaded / $total"
        }
}

