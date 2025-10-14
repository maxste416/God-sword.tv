package com.godsword.tv.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object ThumbnailGenerator {
    
    private const val TAG = "ThumbnailGenerator"
    
    /**
     * Generate and save thumbnail from video file
     * Returns the path to the saved thumbnail or null if failed
     */
    fun generateThumbnail(context: Context, videoPath: String): String? {
        try {
            val videoFile = File(videoPath)
            if (!videoFile.exists()) {
                Log.e(TAG, "Video file not found: $videoPath")
                return null
            }
            
            // Create thumbnails directory in app's cache
            val thumbnailDir = File(context.cacheDir, "thumbnails")
            if (!thumbnailDir.exists()) {
                thumbnailDir.mkdirs()
                Log.d(TAG, "Created thumbnail directory: ${thumbnailDir.absolutePath}")
            }
            
            // Generate unique thumbnail filename
            val thumbnailName = "${videoFile.nameWithoutExtension}_thumb.jpg"
            val thumbnailFile = File(thumbnailDir, thumbnailName)
            
            // Check if thumbnail already exists
            if (thumbnailFile.exists()) {
                Log.d(TAG, "Thumbnail already exists: ${thumbnailFile.absolutePath}")
                return thumbnailFile.absolutePath
            }
            
            // Extract thumbnail from video
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(videoPath)
                
                // Get frame at 1 second (or 1000000 microseconds)
                val bitmap = retriever.getFrameAtTime(
                    1000000, // 1 second into the video
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                
                if (bitmap != null) {
                    // Scale down bitmap to save space (16:9 aspect ratio)
                    val scaledBitmap = Bitmap.createScaledBitmap(
                        bitmap,
                        480,  // width
                        270,  // height (16:9 ratio)
                        true
                    )
                    
                    // Save to file as JPEG
                    FileOutputStream(thumbnailFile).use { out ->
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    
                    bitmap.recycle()
                    scaledBitmap.recycle()
                    
                    Log.d(TAG, "✓ Thumbnail created: ${thumbnailFile.name}")
                    return thumbnailFile.absolutePath
                } else {
                    Log.e(TAG, "✗ Failed to extract frame from: ${videoFile.name}")
                    return null
                }
            } finally {
                retriever.release()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error generating thumbnail for ${File(videoPath).name}: ${e.message}")
            return null
        }
    }
    
    /**
     * Clear all cached thumbnails to free up space
     */
    fun clearThumbnailCache(context: Context) {
        try {
            val thumbnailDir = File(context.cacheDir, "thumbnails")
            if (thumbnailDir.exists()) {
                val deletedCount = thumbnailDir.listFiles()?.count { it.delete() } ?: 0
                Log.d(TAG, "Cleared $deletedCount thumbnails from cache")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing thumbnail cache", e)
        }
    }
    
    /**
     * Get cache size in MB
     */
    fun getCacheSize(context: Context): String {
        try {
            val thumbnailDir = File(context.cacheDir, "thumbnails")
            if (!thumbnailDir.exists()) return "0 MB"
            
            val totalSize = thumbnailDir.listFiles()?.sumOf { it.length() } ?: 0
            val sizeMB = totalSize / (1024.0 * 1024.0)
            return String.format("%.2f MB", sizeMB)
        } catch (e: Exception) {
            return "Unknown"
        }
    }
}