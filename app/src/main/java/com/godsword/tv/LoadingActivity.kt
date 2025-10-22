package com.godsword.tv

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.godsword.tv.models.Video
import com.godsword.tv.usb.UsbVideoScanner
import com.godsword.tv.utils.VideoCacheManager
import kotlinx.coroutines.*

class LoadingActivity : FragmentActivity() {
    
    companion object {
        private const val TAG = "LoadingActivity"
        private const val REQUEST_STORAGE_PERMISSION = 100
    }
    
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var progressText: TextView
    private lateinit var loadingDots: TextView
    
    private val handler = Handler(Looper.getMainLooper())
    private var dotCount = 0
    private var permissionGranted = false
    
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
        
        // Check permissions first, THEN start loading
        checkAndRequestPermissions()
    }
    
    /**
     * Check if storage permission is granted
     */
    private fun hasStoragePermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+) - Need READ_MEDIA_VIDEO
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6-12 - Need READ_EXTERNAL_STORAGE
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> true // No runtime permissions needed for older Android
        }
    }
    
    /**
     * Check permissions and request if needed
     */
    private fun checkAndRequestPermissions() {
        if (hasStoragePermission()) {
            Log.d(TAG, "✓ Storage permission already granted")
            permissionGranted = true
            startLoadingProcess()
        } else {
            Log.d(TAG, "⚠️ Storage permission NOT granted - requesting...")
            statusText.text = "📋 Requesting storage permission..."
            progressBar.progress = 5
            
            // Request permission
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_MEDIA_VIDEO),
                        REQUEST_STORAGE_PERMISSION
                    )
                    Log.d(TAG, "Requesting READ_MEDIA_VIDEO for Android 13+")
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        REQUEST_STORAGE_PERMISSION
                    )
                    Log.d(TAG, "Requesting READ_EXTERNAL_STORAGE")
                }
            }
        }
    }
    
    /**
     * Handle permission result
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✓ Storage permission GRANTED by user")
                permissionGranted = true
                
                statusText.text = "✓ Permission granted!"
                progressBar.progress = 10
                
                // Small delay to show permission success message
                handler.postDelayed({
                    startLoadingProcess()
                }, 500)
                
            } else {
                Log.e(TAG, "✗ Storage permission DENIED by user")
                
                statusText.text = "⚠️ Storage permission denied"
                progressText.text = "Permission required to access videos"
                Toast.makeText(
                    this,
                    "Storage permission is required to scan for videos. Please grant permission in Settings.",
                    Toast.LENGTH_LONG
                ).show()
                
                // Wait 3 seconds then proceed anyway (will show empty list)
                handler.postDelayed({
                    startMainActivity(0, fromCache = false)
                }, 3000)
            }
        }
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
        if (!permissionGranted) {
            Log.e(TAG, "Cannot start loading - permission not granted")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateStatus("🔍 Checking cache...")
                updateProgress(15)
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
                    updateProgress(25)
                    delay(300)
                    
                    val scanner = UsbVideoScanner(this@LoadingActivity)
                    
                    updateStatus("🎬 Scanning for videos...")
                    updateProgress(35)
                    
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
        var progress = 35
        
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
        updateStatus("✨ Processing videos...")
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