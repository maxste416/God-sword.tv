package com.godsword.tv.usb

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import com.godsword.tv.models.Video
import com.godsword.tv.utils.ThumbnailGenerator
import java.io.File

class UsbVideoScanner(private val context: Context) {

    companion object {
        private const val TAG = "UsbVideoScanner"

        private val VIDEO_EXTENSIONS = listOf(
            "mp4", "mkv", "avi", "mov", "wmv",
            "flv", "webm", "m4v", "3gp", "mpeg",
            "mpg", "m2v", "vob", "ts", "m2ts"
        )
    }

    fun scanForVideos(): List<Video> {
        val videos = mutableListOf<Video>()

        Log.d(TAG, "Starting comprehensive video scan...")
        
        // EMULATOR TEST MODE: Check if running on emulator
        val isEmulator = isEmulator()
        if (isEmulator) {
            Log.d(TAG, "🧪 EMULATOR DETECTED - Including test paths")
        }

        // 1. Scan external storage directories
        val externalDirs = context.getExternalFilesDirs(null)
        Log.d(TAG, "Found ${externalDirs.size} external storage locations")

        for (dir in externalDirs) {
            if (dir != null) {
                Log.d(TAG, "Checking: ${dir.absolutePath}")
                val root = findStorageRoot(dir)
                if (root != null && root.exists()) {
                    Log.d(TAG, "Scanning root: ${root.absolutePath}")
                    videos.addAll(scanDirectory(root, maxDepth = 4))
                }
            }
        }

        // 2. Common USB mount points
        val commonPaths = mutableListOf(
            "/storage/usbotg",
            "/storage/usb",
            "/storage/usb1",
            "/mnt/media_rw",
            "/mnt/usb",
            "/mnt/usbhost",
            "/storage/sdcard1",
            Environment.getExternalStorageDirectory().absolutePath
        )
        
        // 3. EMULATOR: Add test video paths
        if (isEmulator) {
            commonPaths.add("/sdcard/TestVideos")
            commonPaths.add("/sdcard/Movies")
            commonPaths.add("/sdcard/Download")
            Log.d(TAG, "🧪 Added emulator test paths")
        }

        for (path in commonPaths) {
            val dir = File(path)
            if (dir.exists() && dir.canRead()) {
                Log.d(TAG, "Scanning path: $path")
                videos.addAll(scanDirectory(dir, maxDepth = 4))
            } else {
                Log.d(TAG, "Path not accessible: $path")
            }
        }

        val uniqueVideos = videos.distinctBy { it.videoUrl }
        Log.d(TAG, "✓ Total unique videos found: ${uniqueVideos.size}")
        
        if (uniqueVideos.isEmpty()) {
            Log.w(TAG, "⚠️ No videos found! Check:")
            Log.w(TAG, "  1. USB drive is connected (or test videos on emulator)")
            Log.w(TAG, "  2. Storage permissions granted")
            Log.w(TAG, "  3. Videos are in supported formats: ${VIDEO_EXTENSIONS.joinToString()}")
        }
        
        // Generate thumbnails for first 10 videos
        if (uniqueVideos.isNotEmpty()) {
            generateInitialThumbnails(uniqueVideos.take(10))
        }
        
        return uniqueVideos
    }
    
    /**
     * Detect if running on emulator
     */
    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT)
    }

    private fun findStorageRoot(directory: File): File? {
        var current = directory

        while (current.parentFile != null) {
            val parent = current.parentFile!!

            if (parent.absolutePath == "/storage" ||
                parent.absolutePath == "/mnt" ||
                parent.name == "emulated") {
                return current
            }

            current = parent
        }

        return current
    }

    private fun scanDirectory(
        directory: File,
        currentDepth: Int = 0,
        maxDepth: Int = 4
    ): List<Video> {
        val videos = mutableListOf<Video>()

        if (currentDepth > maxDepth) {
            return videos
        }

        try {
            if (!directory.canRead()) {
                Log.d(TAG, "Cannot read directory: ${directory.absolutePath}")
                return videos
            }

            val files = directory.listFiles() ?: return videos
            Log.d(TAG, "Found ${files.size} items in ${directory.name}")

            for (file in files) {
                try {
                    when {
                        file.isDirectory && !shouldSkipFolder(file) -> {
                            videos.addAll(scanDirectory(file, currentDepth + 1, maxDepth))
                        }
                        file.isFile && isVideoFile(file) && file.canRead() -> {
                            videos.add(createVideoFromFile(file))
                            Log.d(TAG, "✓ Found video: ${file.name} (${formatFileSize(file.length())})")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing: ${file.name}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning directory: ${directory.name}", e)
        }

        return videos
    }

    private fun shouldSkipFolder(folder: File): Boolean {
        val name = folder.name.lowercase()
        val skipList = listOf(
            "android", "system", "data", "cache",
            ".", "lost+found", "dcim",
            "thumbnails", ".thumbnails"
        )
        return skipList.any { name.contains(it) }
    }

    private fun isVideoFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        val isVideo = VIDEO_EXTENSIONS.contains(extension)
        val isLargeEnough = file.length() > 1024 * 1024 // > 1MB
        
        if (isVideo && !isLargeEnough) {
            Log.d(TAG, "Skipping small file: ${file.name} (${formatFileSize(file.length())})")
        }
        
        return isVideo && isLargeEnough
    }

    private fun createVideoFromFile(file: File): Video {
        val fileName = file.nameWithoutExtension
        val folderName = file.parentFile?.name ?: "Videos"
        val fileSize = formatFileSize(file.length())
        
        // Get actual video duration
        val duration = ThumbnailGenerator.getVideoDuration(file.absolutePath)
        
        val thumbnailPath = findThumbnail(file)

        return Video(
            id = file.absolutePath,
            title = fileName,
            description = "Folder: ${file.parentFile?.name ?: "Unknown"}\nSize: $fileSize",
            duration = duration, // Now shows actual duration like "5:32" or "1:23:45"
            thumbnailUrl = thumbnailPath ?: "",
            videoUrl = file.absolutePath,
            category = folderName
        )
    }
    
    private fun findThumbnail(videoFile: File): String? {
        val manualThumb = findManualThumbnail(videoFile)
        if (manualThumb != null) return manualThumb
        
        val cachedThumb = findCachedThumbnail(videoFile)
        if (cachedThumb != null) return cachedThumb
        
        return null
    }
    
    private fun findManualThumbnail(videoFile: File): String? {
        val baseName = videoFile.nameWithoutExtension
        val parentDir = videoFile.parentFile ?: return null
        
        val possibleNames = listOf(
            "$baseName.jpg",
            "$baseName.png",
            "${baseName}_thumb.jpg",
            "${baseName}_thumb.png"
        )
        
        for (name in possibleNames) {
            val thumbFile = File(parentDir, name)
            if (thumbFile.exists() && thumbFile.canRead()) {
                Log.d(TAG, "Found manual thumbnail: ${thumbFile.name}")
                return thumbFile.absolutePath
            }
        }
        
        return null
    }
    
    private fun findCachedThumbnail(videoFile: File): String? {
        val thumbnailDir = File(context.cacheDir, "thumbnails")
        if (!thumbnailDir.exists()) return null
        
        val thumbnailName = "${videoFile.nameWithoutExtension}_thumb.jpg"
        val cachedFile = File(thumbnailDir, thumbnailName)
        
        return if (cachedFile.exists()) cachedFile.absolutePath else null
    }
    
    private fun generateInitialThumbnails(videos: List<Video>) {
        Log.d(TAG, "Generating thumbnails for first ${videos.size} videos...")
        
        var generated = 0
        videos.forEach { video ->
            if (video.thumbnailUrl.isEmpty()) {
                try {
                    val thumbnailPath = ThumbnailGenerator.generateThumbnail(
                        context,
                        video.videoUrl
                    )
                    if (thumbnailPath != null) {
                        generated++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to generate thumbnail: ${e.message}")
                }
            }
        }
        
        Log.d(TAG, "Generated $generated thumbnails")
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    fun getUsbDrivePaths(): List<String> {
        val paths = mutableListOf<String>()
        val externalFiles = context.getExternalFilesDirs(null)
        for (file in externalFiles) {
            if (file != null) {
                paths.add(file.absolutePath)
            }
        }
        return paths
    }
}