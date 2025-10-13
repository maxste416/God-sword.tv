package com.godsword.tv.data

import android.content.Context
import android.net.Uri
import java.io.File

// Simple models your UI can use
data class VideoItem(
    val title: String,
    val uri: Uri,
    val thumbnailUri: Uri? = null,
    val description: String? = null,
    val durationMs: Long? = null
)

data class Category(
    val name: String,
    val videos: List<VideoItem>
)

/**
 * Very basic stub implementation that:
 * - Looks for a "FaithStream" folder on external storage roots (including USB)
 * - Treats each first-level subfolder as a category
 * - Adds all video files inside as VideoItem
 * Replace with your real USB scanner when ready.
 */
object VideoDataProvider {

    private val VIDEO_EXT = setOf("mp4", "mkv", "avi", "mov")

    fun loadFromUsb(context: Context): List<Category> {
        val roots = possibleRoots()
        val categories = mutableListOf<Category>()

        roots.forEach { root ->
            val faithStream = File(root, "FaithStream")
            if (faithStream.exists() && faithStream.isDirectory) {
                faithStream.listFiles()?.forEach { sub ->
                    if (sub.isDirectory) {
                        val videos = sub.listFiles()
                            ?.filter { it.isFile && it.extension.lowercase() in VIDEO_EXT }
                            ?.map { file ->
                                VideoItem(
                                    title = file.nameWithoutExtension,
                                    uri = Uri.fromFile(file)
                                )
                            }
                            ?: emptyList()

                        if (videos.isNotEmpty()) {
                            categories += Category(name = sub.name, videos = videos)
                        }
                    }
                }
            }
        }
        return categories
    }

    /**
     * Heuristic: common mount points on Android TV for USB/external storage.
     * Adjust as needed for your device(s).
     */
    private fun possibleRoots(): List<File> = listOf(
        File("/storage"),
        File("/mnt/media_rw")
    ).flatMap { parent ->
        parent.listFiles()?.toList().orEmpty()
    }.filter { it.isDirectory }
}
