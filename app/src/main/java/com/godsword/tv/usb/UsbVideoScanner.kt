package com.godsword.tv.usb

import android.content.Context
import android.os.Environment
import android.util.Log
import com.godsword.tv.models.Video
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

        return Video(
            id = file.absolutePath,
            title = fileName,
            description = "Folder: ${file.parentFile?.name ?: "Unknown"}\nSize: $fileSize",
            duration = fileSize,
            thumbnailUrl = "",
            videoUrl = file.absolutePath,
            category = folderName
        )
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