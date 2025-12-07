package com.example.karhebti_android.util

/**
 * Utility for handling image URLs and paths
 */
object ImageUrlHelper {
    // Base URL should match your backend (10.0.2.2 = host machine for emulator)
    private const val BASE_URL = "http://10.0.2.2:3000"

    /**
     * Convert relative image path to absolute URL
     * @param imagePath Relative path like "/uploads/cars/filename.jpg" or absolute URL
     * @return Full absolute URL
     */
    fun getFullImageUrl(imagePath: String?): String? {
        if (imagePath.isNullOrEmpty()) {
            return null
        }

        // If it's already an absolute URL, return as is
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return imagePath
        }

        // If it's a relative path, prepend base URL
        if (imagePath.startsWith("/")) {
            return "$BASE_URL$imagePath"
        }

        // Otherwise, assume it needs a leading slash
        return "$BASE_URL/$imagePath"
    }

    /**
     * Check if an image URL is valid and can be loaded
     */
    fun isValidImageUrl(imageUrl: String?): Boolean {
        return !imageUrl.isNullOrEmpty() &&
               (imageUrl.startsWith("http://") || imageUrl.startsWith("https://") || imageUrl.startsWith("/"))
    }
}
