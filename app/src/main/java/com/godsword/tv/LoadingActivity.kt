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
        
        // Start scanning in background
        startVideoScanning()
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
    
    private fun startVideoScanning() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateStatus("🔍 Initializing scanner...")
                delay(500)
                
                val scanner = UsbVideoScanner(this@LoadingActivity)
                
                updateStatus("📂 Checking USB drive...")
                updateProgress(10)
                delay(300)
                
                updateStatus("🎬 Scanning for videos...")
                updateProgress(20)
                
                // Scan with progress updates
                val videos = scanWithProgress(scanner)
                
                updateStatus("✓ Scan complete!")
                updateProgress(100)
                delay(500)
                
                // Save videos and proceed to main activity
                VideoCache.cachedVideos = videos
                
                withContext(Dispatchers.Main) {
                    startMainActivity(videos.size)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during scanning", e)
                withContext(Dispatchers.Main) {
                    updateStatus("❌ Error: ${e.message}")
                    delay(2000)
                    startMainActivity(0)
                }
            }
        }
    }
    
    private suspend fun scanWithProgress(scanner: UsbVideoScanner): List<Video> {
        val videos = mutableListOf<Video>()
        var progress = 20
        
        // Get all storage locations
        val externalDirs = getExternalFilesDirs(null)
        val totalLocations = externalDirs.size + 5 // USB paths
        
        updateStatus("📁 Scanning storage locations...")
        
        // Simulate progressive scanning
        for (i in externalDirs.indices) {
            delay(100)
            progress += (60 / totalLocations)
            updateProgress(progress)
            updateStatus("📂 Scanning location ${i + 1}/${totalLocations}...")
        }
        
        // Perform actual scan
        updateStatus("🎥 Loading videos...")
        val foundVideos = scanner.scanForVideos()
        videos.addAll(foundVideos)
        
        // Update progress based on videos found
        updateProgress(90)
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
    
    private fun startMainActivity(videoCount: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("video_count", videoCount)
            putExtra("from_loading", true)
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

// Singleton to cache videos
object VideoCache {
    var cachedVideos: List<Video> = emptyList()
}