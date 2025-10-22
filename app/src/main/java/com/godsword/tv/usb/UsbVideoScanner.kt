package com.godsword.tv.usb

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.util.Log
import com.godsword.tv.models.Video
import com.godsword.tv.utils.ThumbnailGenerator
import java.io.File
import java.lang.reflect.Method

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

        Log.d(TAG, "=== Starting Video Scan ===")
        Log.d(TAG, "Android Version: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
        Log.d(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        
        val isEmulator = isEmulator()
        if (isEmulator) {
            Log.d(TAG, "🧪 EMULATOR DETECTED")
        }

        // 1. Get all available storage volumes
        val storageVolumes = getAllStorageVolumes()
        Log.d(TAG, "Found ${storageVolumes.size} storage volumes:")
        storageVolumes.forEachIndexed { index, path ->
            val dir = File(path)
            val accessible = dir.exists() && dir.canRead()
            val fileCount = if (accessible) dir.listFiles()?.size ?: 0 else 0
            Log.d(TAG, "  [$index] $path (accessible: $accessible, files: $fileCount)")
            
            if (accessible) {
                Log.d(TAG, "      Scanning: $path")
                videos.addAll(scanDirectory(dir, maxDepth = 5))
            }
        }

        // 2. Scan app-specific external directories
        val externalDirs = context.getExternalFilesDirs(null)
        Log.d(TAG, "\nScanning ${externalDirs.size} app-specific directories:")
        externalDirs.filterNotNull().forEachIndexed { index, dir ->
            Log.d(TAG, "  [$index] ${dir.absolutePath}")
            val root = findStorageRoot(dir)
            if (root != null && root.exists() && root.canRead()) {
                Log.d(TAG, "      Root: ${root.absolutePath}")
                videos.addAll(scanDirectory(root, maxDepth = 5))
            }
        }

        // 3. Common paths
        val commonPaths = getCommonVideoPaths(isEmulator)
        Log.d(TAG, "\nChecking ${commonPaths.size} common video paths:")
        commonPaths.forEachIndexed { index, path ->
            val dir = File(path)
            val accessible = dir.exists() && dir.canRead()
            Log.d(TAG, "  [$index] $path (accessible: $accessible)")
            
            if (accessible) {
                videos.addAll(scanDirectory(dir, maxDepth = 4))
            }
        }

        val uniqueVideos = videos.distinctBy { it.videoUrl }
        
        Log.d(TAG, "\n=== Scan Complete ===")
        Log.d(TAG, "✓ Total videos found: ${uniqueVideos.size}")
        
        if (uniqueVideos.isEmpty()) {
            Log.w(TAG, "\n⚠️ NO VIDEOS FOUND!")
            Log.w(TAG, "Troubleshooting:")
            Log.w(TAG, "  1. Check USB drive is connected")
            Log.w(TAG, "  2. Check storage permissions granted")
            Log.w(TAG, "  3. Check video files exist (formats: ${VIDEO_EXTENSIONS.take(5).joinToString()})")
            Log.w(TAG, "  4. For emulator: Add test videos to /sdcard/Movies or /sdcard/Download")
        } else {
            Log.d(TAG, "Categories found:")
            uniqueVideos.groupBy { it.category }.forEach { (category, vids) ->
                Log.d(TAG, "  - $category: ${vids.size} videos")
            }
        }
        
        // Generate thumbnails for first 10 videos
        if (uniqueVideos.isNotEmpty()) {
            generateInitialThumbnails(uniqueVideos.take(10))
        }
        
        return uniqueVideos
    }
    
    /**
     * Get all available storage volumes using StorageManager
     */
    private fun getAllStorageVolumes(): List<String> {
        val volumes = mutableListOf<String>()
        
        try {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            
            // Use reflection to access storage volumes (works on most devices)
            val storageVolumeClass = Class.forName("android.os.storage.StorageVolume")
            val getVolumeListMethod: Method = storageManager.javaClass.getMethod("getVolumeList")
            val volumeList = getVolumeListMethod.invoke(storageManager) as Array<*>
            
            for (volume in volumeList) {
                try {
                    val getPathMethod: Method = storageVolumeClass.getMethod("getPath")
                    val path = getPathMethod.invoke(volume) as String
                    volumes.add(path)
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting volume path", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing StorageManager", e)
        }
        
        return volumes
    }
    
    /**
     * Get common paths where videos might be located
     */
    private fun getCommonVideoPaths(isEmulator: Boolean): List<String> {
        val paths = mutableListOf<String>()
        
        // Standard Android paths
        paths.add(Environment.getExternalStorageDirectory().absolutePath)
        paths.add("${Environment.getExternalStorageDirectory()}/Movies")
        paths.add("${Environment.getExternalStorageDirectory()}/Download")
        paths.add("${Environment.getExternalStorageDirectory()}/DCIM")
        
        // USB mount points (various Android devices)
        paths.addAll(listOf(
            "/storage/usbotg",
            "/storage/usb",
            "/storage/usb1",
            "/storage/usb2",
            "/storage/usbdisk",
            "/storage/usbdisk1",
            "/mnt/media_rw",
            "/mnt/usb",
            "/mnt/usbhost",
            "/mnt/usb_storage",
            "/mnt/usbstorage",
            "/mnt/usb1",
            "/storage/sdcard1",
            "/storage/extSdCard"
        ))
        
        // Emulator-specific paths
        if (isEmulator) {
            paths.addAll(listOf(
                "/sdcard/TestVideos",
                "/sdcard/Videos",
                "/sdcard/Movies",
                "/sdcard/Download"
            ))
        }
        
        // Check /storage directory for any mounted devices
        try {
            val storageDir = File("/storage")
            if (storageDir.exists() && storageDir.canRead()) {
                storageDir.listFiles()?.forEach { file ->
                    if (file.isDirectory && file.name != "emulated" && file.name != "self") {
                        paths.add(file.absolutePath)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking /storage directory", e)
        }
        
        return paths.distinct()
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
        maxDepth: Int = 5
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
                            Log.d(TAG, "    ✓ ${file.name} (${formatFileSize(file.length())})")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing: ${file.name}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning: ${directory.name}", e)
        }

        return videos
    }

    private fun shouldSkipFolder(folder: File): Boolean {
        val name = folder.name.lowercase()
        val skipList = listOf(
            "android", "system", "data", "cache",
            ".", "lost+found",
            "thumbnails", ".thumbnails", ".trash"
        )
        return skipList.any { name.contains(it) }
    }

    private fun isVideoFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        val isVideo = VIDEO_EXTENSIONS.contains(extension)
        val isLargeEnough = file.length() > 1024 * 1024 // > 1MB
        
        return isVideo && isLargeEnough
    }

    private fun createVideoFromFile(file: File): Video {
        val fileName = file.nameWithoutExtension
        val folderName = file.parentFile?.name ?: "Videos"
        val fileSize = formatFileSize(file.length())
        
        val duration = ThumbnailGenerator.getVideoDuration(file.absolutePath)
        val thumbnailPath = findThumbnail(file)

        return Video(
            id = file.absolutePath,
            title = fileName,
            description = "Folder: ${file.parentFile?.name ?: "Unknown"}\nSize: $fileSize",
            duration = duration,
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
        Log.d(TAG, "\nGenerating thumbnails for first ${videos.size} videos...")
        
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
        
        Log.d(TAG, "Generated $generated new thumbnails")
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
        return getAllStorageVolumes()
    }
}