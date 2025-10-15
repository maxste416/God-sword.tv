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
            
            // Extract thumbnail from video with smart frame selection
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(videoPath)
                
                // Get video duration
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durationMs = durationStr?.toLongOrNull() ?: 0L
                
                if (durationMs == 0L) {
                    Log.e(TAG, "Could not determine video duration")
                    return null
                }
                
                // Try multiple positions to find a good frame (not black)
                val positions = listOf(0.3, 0.5, 0.2, 0.7, 0.1) // 30%, 50%, 20%, 70%, 10%
                var bitmap: Bitmap? = null
                
                for (position in positions) {
                    val timeMs = (durationMs * position).toLong()
                    val timeUs = timeMs * 1000 // Convert to microseconds
                    
                    val frame = retriever.getFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                    
                    if (frame != null && !isFrameTooDark(frame)) {
                        bitmap = frame
                        Log.d(TAG, "Found good frame at ${(position * 100).toInt()}% (${timeMs}ms)")
                        break
                    } else {
                        frame?.recycle()
                        Log.d(TAG, "Frame at ${(position * 100).toInt()}% is too dark, trying next...")
                    }
                }
                
                // If all frames are dark, just use the middle frame
                if (bitmap == null) {
                    Log.d(TAG, "All frames were dark, using middle frame anyway")
                    val middleMs = (durationMs * 0.5).toLong()
                    bitmap = retriever.getFrameAtTime(
                        middleMs * 1000,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                }
                
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
                    Log.e(TAG, "✗ Failed to extract any frame from: ${videoFile.name}")
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
     * Check if a frame is too dark (mostly black pixels)
     * Returns true if more than 70% of pixels are very dark
     */
    private fun isFrameTooDark(bitmap: Bitmap): Boolean {
        try {
            // Sample every 10th pixel to check brightness (for performance)
            var darkPixels = 0
            var totalSamples = 0
            val sampleRate = 10
            
            for (x in 0 until bitmap.width step sampleRate) {
                for (y in 0 until bitmap.height step sampleRate) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = android.graphics.Color.red(pixel)
                    val g = android.graphics.Color.green(pixel)
                    val b = android.graphics.Color.blue(pixel)
                    
                    // Calculate brightness (0-255)
                    val brightness = (r + g + b) / 3
                    
                    if (brightness < 40) { // Very dark threshold
                        darkPixels++
                    }
                    totalSamples++
                }
            }
            
            val darkPercentage = (darkPixels.toFloat() / totalSamples) * 100
            return darkPercentage > 70 // More than 70% dark pixels
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking frame brightness", e)
            return false // Assume it's fine if we can't check
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
    
    /**
     * Get video duration in readable format (MM:SS or HH:MM:SS)
     */
    fun getVideoDuration(videoPath: String): String {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            
            retriever.release()
            
            formatDuration(durationMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting video duration: ${e.message}")
            "00:00"
        }
    }
    
    /**
     * Format duration from milliseconds to HH:MM:SS or MM:SS
     */
    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000).toInt()
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%d:%02d", minutes, secs)
        }
    }
}