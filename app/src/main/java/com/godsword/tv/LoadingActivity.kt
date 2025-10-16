package com.godsword.tv

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.godsword.tv.models.Video
import com.godsword.tv.usb.UsbVideoScanner
import com.godsword.tv.utils.VideoCacheManager
import kotlinx.coroutines.*

class LoadingActivity : FragmentActivity() {
    
    companion object {
        private const val TAG = "LoadingActivity"
    }
    
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var progressText: TextView
    private lateinit var loadingDots: TextView
    
    private val handler = Handler(Looper.getMainLooper())
    private var dotCount = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)
        
        // Initialize views
        progressBar = findViewById(R.id.loading_progress)
        statusText = findViewById(R.id.loading_status)
        progressText = findViewById(R.id.progress_text)
        loadingDots = findViewById(R.id.loading_dots)
        
        // Start animated dots
        startDotsAnimation()
        
        // Start loading process
        startLoadingProcess()
    }
    
    private fun startDotsAnimation() {
        handler.post(object : Runnable {
            override fun run() {
                dotCount = (dotCount + 1) % 4
                val dots = "●".repeat(dotCount) + "○".repeat(3 - dotCount)
                loadingDots.text = dots
                handler.postDelayed(this, 500)
            }
        })
    }
    
    private fun startLoadingProcess() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateStatus("🔍 Checking cache...")
                updateProgress(10)
                delay(300)
                
                // Check if we have cached videos
                val cachedVideos = VideoCacheManager.loadVideos(this@LoadingActivity)
                
                if (cachedVideos != null && cachedVideos.isNotEmpty()) {
                    // ✓ CACHE HIT - Use cached videos (FAST!)
                    Log.d(TAG, "✓ Using cached videos - skipping scan!")
                    
                    updateStatus("✓ Loading cached videos...")
                    updateProgress(50)
                    updateProgressText("${cachedVideos.size} videos in cache")
                    delay(500)
                    
                    updateStatus("✓ Ready!")
                    updateProgress(100)
                    delay(300)
                    
                    // Save to memory cache and proceed
                    VideoCache.cachedVideos = cachedVideos
                    
                    withContext(Dispatchers.Main) {
                        startMainActivity(cachedVideos.size, fromCache = true)
                    }
                    
                } else {
                    // ✗ CACHE MISS - Need to scan
                    Log.d(TAG, "✗ No cache found - performing full scan...")
                    
                    updateStatus("📂 Scanning USB drive...")
                    updateProgress(20)
                    delay(300)
                    
                    val scanner = UsbVideoScanner(this@LoadingActivity)
                    
                    updateStatus("🎬 Scanning for videos...")
                    updateProgress(30)
                    
                    // Scan with progress updates
                    val videos = scanWithProgress(scanner)
                    
                    // Save to persistent cache
                    updateStatus("💾 Saving to cache...")
                    updateProgress(95)
                    VideoCacheManager.saveVideos(this@LoadingActivity, videos)
                    
                    updateStatus("✓ Scan complete!")
                    updateProgress(100)
                    delay(500)
                    
                    // Save to memory cache and proceed
                    VideoCache.cachedVideos = videos
                    
                    withContext(Dispatchers.Main) {
                        startMainActivity(videos.size, fromCache = false)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during loading", e)
                withContext(Dispatchers.Main) {
                    updateStatus("❌ Error: ${e.message}")
                    delay(2000)
                    startMainActivity(0, fromCache = false)
                }
            }
        }
    }
    
    private suspend fun scanWithProgress(scanner: UsbVideoScanner): List<Video> {
        val videos = mutableListOf<Video>()
        var progress = 30
        
        // Get all storage locations
        val externalDirs = getExternalFilesDirs(null)
        val totalLocations = externalDirs.size + 5 // USB paths
        
        updateStatus("📁 Scanning storage locations...")
        
        // Simulate progressive scanning
        for (i in externalDirs.indices) {
            delay(100)
            progress += (50 / totalLocations)
            updateProgress(progress)
            updateStatus("📂 Scanning location ${i + 1}/${totalLocations}...")
        }
        
        // Perform actual scan
        updateStatus("🎥 Loading videos...")
        val foundVideos = scanner.scanForVideos()
        videos.addAll(foundVideos)
        
        // Update progress based on videos found
        updateProgress(85)
        updateStatus("✨ Generating thumbnails...")
        updateProgressText("${videos.size} videos found")
        
        delay(500)
        
        return videos
    }
    
    private suspend fun updateStatus(message: String) {
        withContext(Dispatchers.Main) {
            statusText.text = message
            Log.d(TAG, message)
        }
    }
    
    private suspend fun updateProgress(progress: Int) {
        withContext(Dispatchers.Main) {
            // Smooth animation
            ObjectAnimator.ofInt(progressBar, "progress", progress).apply {
                duration = 300
                interpolator = LinearInterpolator()
                start()
            }
        }
    }
    
    private suspend fun updateProgressText(text: String) {
        withContext(Dispatchers.Main) {
            progressText.text = text
        }
    }
    
    private fun startMainActivity(videoCount: Int, fromCache: Boolean) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("video_count", videoCount)
            putExtra("from_cache", fromCache)
        }
        startActivity(intent)
        finish()
        
        // Smooth transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}

// Singleton to cache videos in memory (for current session)
object VideoCache {
    var cachedVideos: List<Video> = emptyList()
}