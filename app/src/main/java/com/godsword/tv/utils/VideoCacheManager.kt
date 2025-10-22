package com.godsword.tv.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.godsword.tv.models.Video
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages persistent video cache to avoid re-scanning on every app launch
 */
object VideoCacheManager {
    
    private const val TAG = "VideoCacheManager"
    private const val PREFS_NAME = "video_cache_prefs"
    private const val KEY_CACHED_VIDEOS = "cached_videos"
    private const val KEY_LAST_SCAN_TIME = "last_scan_time"
    private const val KEY_SCAN_COUNT = "scan_count"
    
    // Cache expires after 24 hours (in case USB content changes)
    private const val CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000L
    
    private var memoryCache: List<Video>? = null
    
    /**
     * Save videos to persistent storage
     */
    fun saveVideos(context: Context, videos: List<Video>) {
        try {
            val prefs = getPrefs(context)
            val jsonArray = JSONArray()
            
            videos.forEach { video ->
                val jsonObject = JSONObject().apply {
                    put("id", video.id)
                    put("title", video.title)
                    put("description", video.description)
                    put("duration", video.duration)
                    put("thumbnailUrl", video.thumbnailUrl)
                    put("videoUrl", video.videoUrl)
                    put("category", video.category)
                    put("isFavorite", video.isFavorite)
                }
                jsonArray.put(jsonObject)
            }
            
            prefs.edit().apply {
                putString(KEY_CACHED_VIDEOS, jsonArray.toString())
                putLong(KEY_LAST_SCAN_TIME, System.currentTimeMillis())
                putInt(KEY_SCAN_COUNT, prefs.getInt(KEY_SCAN_COUNT, 0) + 1)
                apply()
            }
            
            memoryCache = videos
            
            Log.d(TAG, "✓ Saved ${videos.size} videos to persistent cache")
            Log.d(TAG, "  Total scans this session: ${prefs.getInt(KEY_SCAN_COUNT, 0)}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving videos to cache", e)
        }
    }
    
    /**
     * Load videos from persistent storage
     * Returns null if cache is empty or expired
     */
    fun loadVideos(context: Context, ignoreExpiry: Boolean = false): List<Video>? {
        try {
            // First check memory cache
            if (memoryCache != null) {
                Log.d(TAG, "✓ Using memory cache (${memoryCache!!.size} videos)")
                return memoryCache
            }
            
            val prefs = getPrefs(context)
            val cachedJson = prefs.getString(KEY_CACHED_VIDEOS, null) ?: return null
            val lastScanTime = prefs.getLong(KEY_LAST_SCAN_TIME, 0)
            val scanCount = prefs.getInt(KEY_SCAN_COUNT, 0)
            
            // Check if cache is expired (unless we're ignoring expiry)
            if (!ignoreExpiry && isCacheExpired(lastScanTime)) {
                Log.d(TAG, "⚠️ Cache expired (${getTimeSinceLastScan(lastScanTime)})")
                return null
            }
            
            // Parse JSON
            val jsonArray = JSONArray(cachedJson)
            val videos = mutableListOf<Video>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val video = Video(
                    id = jsonObject.getString("id"),
                    title = jsonObject.getString("title"),
                    description = jsonObject.getString("description"),
                    duration = jsonObject.getString("duration"),
                    thumbnailUrl = jsonObject.getString("thumbnailUrl"),
                    videoUrl = jsonObject.getString("videoUrl"),
                    category = jsonObject.getString("category"),
                    isFavorite = jsonObject.optBoolean("isFavorite", false)
                )
                videos.add(video)
            }
            
            memoryCache = videos
            
            Log.d(TAG, "✓ Loaded ${videos.size} videos from persistent cache")
            Log.d(TAG, "  Last scan: ${getTimeSinceLastScan(lastScanTime)}")
            Log.d(TAG, "  Total scans: $scanCount")
            
            return videos
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading videos from cache", e)
            return null
        }
    }
    
    /**
     * Check if we have valid cached videos
     */
    fun hasCachedVideos(context: Context): Boolean {
        val prefs = getPrefs(context)
        val hasCache = prefs.contains(KEY_CACHED_VIDEOS)
        val lastScanTime = prefs.getLong(KEY_LAST_SCAN_TIME, 0)
        
        return hasCache && !isCacheExpired(lastScanTime)
    }
    
    /**
     * Clear the video cache (force rescan on next launch)
     */
    fun clearCache(context: Context) {
        try {
            getPrefs(context).edit().clear().apply()
            memoryCache = null
            Log.d(TAG, "✓ Cache cleared - will rescan on next launch")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
        }
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheInfo(context: Context): String {
        val prefs = getPrefs(context)
        val videoCount = loadVideos(context)?.size ?: 0
        val lastScanTime = prefs.getLong(KEY_LAST_SCAN_TIME, 0)
        val scanCount = prefs.getInt(KEY_SCAN_COUNT, 0)
        val cacheAge = getTimeSinceLastScan(lastScanTime)
        
        return """
            Videos: $videoCount
            Last Scan: $cacheAge
            Total Scans: $scanCount
            Cache Valid: ${!isCacheExpired(lastScanTime)}
        """.trimIndent()
    }
    
    /**
     * Check if cache is expired
     */
    private fun isCacheExpired(lastScanTime: Long): Boolean {
        if (lastScanTime == 0L) return true
        val age = System.currentTimeMillis() - lastScanTime
        return age > CACHE_EXPIRY_MS
    }
    
    /**
     * Get human-readable time since last scan
     */
    private fun getTimeSinceLastScan(lastScanTime: Long): String {
        if (lastScanTime == 0L) return "Never"
        
        val ageMs = System.currentTimeMillis() - lastScanTime
        val ageMinutes = ageMs / (60 * 1000)
        val ageHours = ageMinutes / 60
        
        return when {
            ageMinutes < 1 -> "Just now"
            ageMinutes < 60 -> "$ageMinutes minutes ago"
            ageHours < 24 -> "$ageHours hours ago"
            else -> "${ageHours / 24} days ago"
        }
    }
    
    /**
     * Get SharedPreferences instance
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}