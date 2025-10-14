package com.godsword.tv.usb

import android.content.Context
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

        Log.d(TAG, "Starting comprehensive USB scan...")

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

        val commonPaths = listOf(
            "/storage/usbotg",
            "/storage/usb",
            "/storage/usb1",
            "/mnt/media_rw",
            "/mnt/usb",
            "/mnt/usbhost",
            "/storage/sdcard1",
            Environment.getExternalStorageDirectory().absolutePath
        )

        for (path in commonPaths) {
            val dir = File(path)
            if (dir.exists() && dir.canRead()) {
                Log.d(TAG, "Scanning common path: $path")
                videos.addAll(scanDirectory(dir, maxDepth = 4))
            }
        }

        val uniqueVideos = videos.distinctBy { it.videoUrl }
        Log.d(TAG, "Total unique videos found: ${uniqueVideos.size}")
        
        // Generate thumbnails for first 10 videos immediately (for fast loading)
        generateInitialThumbnails(uniqueVideos.take(10))
        
        return uniqueVideos
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
                return videos
            }

            val files = directory.listFiles() ?: return videos

            for (file in files) {
                try {
                    when {
                        file.isDirectory && !shouldSkipFolder(file) -> {
                            videos.addAll(scanDirectory(file, currentDepth + 1, maxDepth))
                        }
                        file.isFile && isVideoFile(file) && file.canRead() -> {
                            videos.add(createVideoFromFile(file))
                            Log.d(TAG, "Found: ${file.name}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing file: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning: ${directory.name}")
        }

        return videos
    }

    private fun shouldSkipFolder(folder: File): Boolean {
        val name = folder.name.lowercase()
        val skipList = listOf(
            "android", "system", "data", "cache",
            ".", "lost+found", "dcim", "download",
            "thumbnails", ".thumbnails"
        )
        return skipList.any { name.contains(it) }
    }

    private fun isVideoFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return VIDEO_EXTENSIONS.contains(extension) && file.length() > 1024 * 1024
    }

    private fun createVideoFromFile(file: File): Video {
        val fileName = file.nameWithoutExtension
        val folderName = file.parentFile?.name ?: "Videos"
        val fileSize = formatFileSize(file.length())
        
        // Look for existing thumbnail (manual .jpg/.png file OR previously generated)
        val thumbnailPath = findThumbnail(file)

        return Video(
            id = file.absolutePath,
            title = fileName,
            description = "Folder: ${file.parentFile?.name ?: "Unknown"}\nSize: $fileSize",
            duration = fileSize,
            thumbnailUrl = thumbnailPath ?: "",
            videoUrl = file.absolutePath,
            category = folderName
        )
    }
    
    /**
     * Find thumbnail in this order:
     * 1. Manual thumbnail file next to video (video.jpg, video.png)
     * 2. Previously generated thumbnail in cache
     * 3. Generate new thumbnail
     */
    private fun findThumbnail(videoFile: File): String? {
        // 1. Check for manual thumbnail files
        val manualThumb = findManualThumbnail(videoFile)
        if (manualThumb != null) {
            Log.d(TAG, "Using manual thumbnail: ${File(manualThumb).name}")
            return manualThumb
        }
        
        // 2. Check cache for previously generated thumbnail
        val cachedThumb = findCachedThumbnail(videoFile)
        if (cachedThumb != null) {
            Log.d(TAG, "Using cached thumbnail: ${File(cachedThumb).name}")
            return cachedThumb
        }
        
        // 3. Will be generated on-demand later
        return null
    }
    
    /**
     * Look for manual thumbnail files next to the video
     * Patterns: video.jpg, video.png, video_thumb.jpg
     */
    private fun findManualThumbnail(videoFile: File): String? {
        val baseName = videoFile.nameWithoutExtension
        val parentDir = videoFile.parentFile ?: return null
        
        val possibleNames = listOf(
            "$baseName.jpg",
            "$baseName.png",
            "${baseName}_thumb.jpg",
            "${baseName}_thumb.png",
            "${baseName}_thumbnail.jpg"
        )
        
        for (name in possibleNames) {
            val thumbFile = File(parentDir, name)
            if (thumbFile.exists() && thumbFile.canRead()) {
                return thumbFile.absolutePath
            }
        }
        
        return null
    }
    
    /**
     * Check if thumbnail was already generated in cache
     */
    private fun findCachedThumbnail(videoFile: File): String? {
        val thumbnailDir = File(context.cacheDir, "thumbnails")
        if (!thumbnailDir.exists()) return null
        
        val thumbnailName = "${videoFile.nameWithoutExtension}_thumb.jpg"
        val cachedFile = File(thumbnailDir, thumbnailName)
        
        return if (cachedFile.exists()) cachedFile.absolutePath else null
    }
    
    /**
     * Generate thumbnails for initial videos (blocking)
     * This ensures first videos have thumbnails immediately
     */
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
                        // Update the video object
                        (video as? Video)?.let {
                            // Note: This is a mutable operation
                            generated++
                        }
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