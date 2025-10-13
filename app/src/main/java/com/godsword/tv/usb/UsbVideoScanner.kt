package com.godsword.tv.usb

import android.content.Context
import android.util.Log
import com.godsword.tv.models.Video
import java.io.File

class UsbVideoScanner(private val context: Context) {
    
    companion object {
        private const val TAG = "UsbVideoScanner"
        
        // Supported video formats
        private val VIDEO_EXTENSIONS = listOf(
            "mp4", "mkv", "avi", "mov", "wmv", 
            "flv", "webm", "m4v", "3gp", "mpeg", "mpg"
        )
    }
    
    fun scanForVideos(): List<Video> {
        val videos = mutableListOf<Video>()
        
        Log.d(TAG, "Starting USB scan...")
        
        // Scan multiple possible USB mount points
        val usbPaths = listOf(
            "/storage/usb",
            "/storage/usbotg",
            "/mnt/usb",
            "/mnt/media_rw",
            "/mnt/usbhost",
            "/storage/sdcard1",
            "/mnt/usb_storage"
        )
        
        // Also check external storage directories
        val externalFiles = context.getExternalFilesDirs(null)
        
        for (file in externalFiles) {
            if (file != null && isRemovableStorage(file.absolutePath)) {
                Log.d(TAG, "Scanning external storage: ${file.absolutePath}")
                videos.addAll(scanDirectory(file))
            }
        }
        
        // Scan known USB paths
        for (path in usbPaths) {
            val directory = File(path)
            if (directory.exists() && directory.isDirectory && directory.canRead()) {
                Log.d(TAG, "Scanning USB path: $path")
                videos.addAll(scanDirectory(directory))
            }
        }
        
        Log.d(TAG, "Scan complete. Found ${videos.size} videos")
        return videos
    }
    
    private fun scanDirectory(directory: File): List<Video> {
        val videos = mutableListOf<Video>()
        
        try {
            val files = directory.listFiles() ?: return videos
            
            for (file in files) {
                when {
                    file.isDirectory && file.canRead() -> {
                        // Recursively scan subdirectories
                        videos.addAll(scanDirectory(file))
                    }
                    file.isFile && isVideoFile(file) && file.canRead() -> {
                        videos.add(createVideoFromFile(file))
                        Log.d(TAG, "Found video: ${file.name}")
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for directory: ${directory.absolutePath}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning directory: ${directory.absolutePath}", e)
        }
        
        return videos
    }
    
    private fun isVideoFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return VIDEO_EXTENSIONS.contains(extension)
    }
    
    private fun createVideoFromFile(file: File): Video {
        val fileName = file.nameWithoutExtension
        val folderName = file.parentFile?.name ?: "USB Drive"
        val fileSize = formatFileSize(file.length())
        
        return Video(
            id = file.absolutePath,
            title = fileName,
            description = "Path: ${file.absolutePath}\nSize: $fileSize",
            duration = fileSize,
            thumbnailUrl = "", // No thumbnail for local files
            videoUrl = file.absolutePath,
            category = folderName
        )
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
    
    private fun isRemovableStorage(path: String): Boolean {
        return !path.contains("/Android/data") && 
               (path.contains("usb") || 
                path.contains("sdcard1") || 
                path.contains("external") ||
                path.contains("removable"))
    }
    
    fun getUsbDrivePaths(): List<String> {
        val paths = mutableListOf<String>()
        
        val externalFiles = context.getExternalFilesDirs(null)
        for (file in externalFiles) {
            if (file != null && isRemovableStorage(file.absolutePath)) {
                paths.add(file.absolutePath)
            }
        }
        
        return paths
    }
}
