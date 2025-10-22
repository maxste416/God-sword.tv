package com.godsword.tv

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import com.godsword.tv.adapters.VideoCardPresenter
import com.godsword.tv.models.Video
import com.godsword.tv.usb.UsbVideoScanner
import com.godsword.tv.utils.VideoCacheManager

class MainActivity : FragmentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_STORAGE_PERMISSION = 100
    }
    
    private lateinit var usbScanner: UsbVideoScanner
    private lateinit var browseSupportFragment: BrowseSupportFragment
    private var isFromCache = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_main)
            Log.d(TAG, "God'sword.TV - Starting application")
            
            // Get cache info
            isFromCache = intent.getBooleanExtra("from_cache", false)
            
            if (checkStoragePermission()) {
                initializeApp()
            } else {
                requestStoragePermission()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in onCreate", e)
            Toast.makeText(this, "App failed to start: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun initializeApp() {
        try {
            Log.d(TAG, "Initializing app...")
            
            usbScanner = UsbVideoScanner(this)
            
            browseSupportFragment = BrowseSupportFragment().apply {
                headersState = BrowseSupportFragment.HEADERS_ENABLED
                isHeadersTransitionOnBackEnabled = true
                
                try {
                    brandColor = ContextCompat.getColor(this@MainActivity, R.color.primary_blue)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not set brand color", e)
                }
                
                title = "परमेश्वर का वचन"
            }
            
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_frame, browseSupportFragment)
                .commitNow()
            
            Log.d(TAG, "Fragment added successfully")
            loadUsbVideos(browseSupportFragment)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing app", e)
            Toast.makeText(this, "Initialization error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun loadUsbVideos(fragment: BrowseSupportFragment) {
        try {
            Log.d(TAG, "Loading videos from cache...")
            
            // Check if videos are already cached from LoadingActivity
            val usbVideos = if (VideoCache.cachedVideos.isNotEmpty()) {
                Log.d(TAG, "Using cached videos: ${VideoCache.cachedVideos.size}")
                VideoCache.cachedVideos
            } else {
                // Fallback: try to load from persistent cache
                Log.d(TAG, "Memory cache empty, trying persistent cache...")
                val persistedVideos = VideoCacheManager.loadVideos(this)
                if (persistedVideos != null && persistedVideos.isNotEmpty()) {
                    VideoCache.cachedVideos = persistedVideos
                    persistedVideos
                } else {
                    // Last resort: scan again
                    Log.d(TAG, "No cache available, scanning again...")
                    Toast.makeText(this, "पेनड्राइव स्कैन हो रहा है...", Toast.LENGTH_SHORT).show()
                    usbScanner.scanForVideos()
                }
            }
            
            Log.d(TAG, "Displaying ${usbVideos.size} videos")
            
            if (usbVideos.isEmpty()) {
                showNoUsbMessage(fragment)
                return
            }
            
            val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
            val videosByFolder = usbVideos.groupBy { it.category }
            
            Log.d(TAG, "Found ${videosByFolder.size} folders")
            
            videosByFolder.forEach { (folderName, videos) ->
                val listRowAdapter = ArrayObjectAdapter(VideoCardPresenter())
                videos.forEach { video ->
                    listRowAdapter.add(video)
                }
                
                val displayName = getHindiFolderName(folderName)
                val header = HeaderItem(displayName)
                rowsAdapter.add(ListRow(header, listRowAdapter))
            }
            
            fragment.adapter = rowsAdapter
            fragment.onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
                if (item is Video) {
                    playVideo(item)
                }
            }
            
            // Show appropriate message
            if (isFromCache) {
                Toast.makeText(
                    this,
                    "⚡ ${usbVideos.size} videos loaded from cache (instant load!)",
                    Toast.LENGTH_LONG
                ).show()
                Log.d(TAG, "✓ Loaded from cache - no scanning needed!")
            } else {
                Toast.makeText(
                    this,
                    "✓ ${usbVideos.size} videos scanned and cached",
                    Toast.LENGTH_LONG
                ).show()
                Log.d(TAG, "✓ Fresh scan completed and cached")
            }
            
            // Show cache info in logs
            Log.d(TAG, VideoCacheManager.getCacheInfo(this))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading videos", e)
            showErrorMessage(fragment, e.message ?: "Unknown error")
        }
    }
    
    private fun getHindiFolderName(folderName: String): String {
        return when (folderName.lowercase()) {
            "worship", "aaradhana", "आराधना" -> "🎵 Worship / आराधना"
            "sermons", "sermon", "updesh", "उपदेश" -> "📖 Sermons / उपदेश"
            "bible-stories", "stories", "kahaniya", "कहानियाँ" -> "📚 Bible Stories"
            "kids", "bacho-ke-liye", "बच्चों-के-लिए" -> "👶 Kids"
            "testimonies", "gawahi", "गवाही" -> "🙏 Testimonies"
            "songs", "geeton", "गीतों" -> "🎤 Songs / गीत"
            "prayer", "prarthana", "प्रार्थना" -> "🙏 Prayer"
            "teaching", "shiksha", "शिक्षा" -> "📘 Teaching"
            else -> "📁 $folderName"
        }
    }
    
    private fun playVideo(video: Video) {
        Log.d(TAG, "Playing: ${video.title}")
        
        // Get the current video's index and category
        val allVideos = VideoCache.cachedVideos
        val currentIndex = allVideos.indexOf(video)
        
        // Get videos from the same category for playlist
        val categoryVideos = allVideos.filter { it.category == video.category }
        val categoryIndex = categoryVideos.indexOf(video)
        
        Log.d(TAG, "Playlist: ${categoryVideos.size} videos in category '${video.category}'")
        Log.d(TAG, "Current position: ${categoryIndex + 1}/${categoryVideos.size}")
        
        val intent = Intent(this, PlaybackActivity::class.java).apply {
            putExtra("video_url", video.videoUrl)
            putExtra("video_title", video.title)
            putExtra("video_index", categoryIndex)
            putParcelableArrayListExtra("video_playlist", ArrayList(categoryVideos))
        }
        startActivity(intent)
    }
    
    private fun showNoUsbMessage(fragment: BrowseSupportFragment) {
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val listRowAdapter = ArrayObjectAdapter(VideoCardPresenter())
        
        val emptyVideo = Video(
            id = "empty",
            title = "📱 पेनड्राइव नहीं मिला",
            description = "USB Pendrive not detected",
            duration = "",
            thumbnailUrl = "",
            videoUrl = "",
            category = "Status"
        )
        
        listRowAdapter.add(emptyVideo)
        val header = HeaderItem("⚠️ सूचना / Notice")
        rowsAdapter.add(ListRow(header, listRowAdapter))
        fragment.adapter = rowsAdapter
        
        Toast.makeText(this, "कृपया पेनड्राइव लगाएं", Toast.LENGTH_LONG).show()
    }
    
    private fun showErrorMessage(fragment: BrowseSupportFragment, errorMsg: String) {
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val listRowAdapter = ArrayObjectAdapter(VideoCardPresenter())
        
        val errorVideo = Video(
            id = "error",
            title = "⚠️ त्रुटि / Error",
            description = errorMsg,
            duration = "",
            thumbnailUrl = "",
            videoUrl = "",
            category = "Error"
        )
        
        listRowAdapter.add(errorVideo)
        val header = HeaderItem("⚠️ Error")
        rowsAdapter.add(ListRow(header, listRowAdapter))
        fragment.adapter = rowsAdapter
        
        Toast.makeText(this, "Error: $errorMsg", Toast.LENGTH_LONG).show()
    }
    
    // Handle MENU button to refresh videos
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showRefreshDialog()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    
    private fun showRefreshDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Refresh Videos")
        builder.setMessage("Do you want to rescan the USB drive? This will clear the cache and scan for new videos.")
        builder.setPositiveButton("Yes, Rescan") { _, _ ->
            refreshVideos()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
    
    private fun refreshVideos() {
        Toast.makeText(this, "Clearing cache and rescanning...", Toast.LENGTH_SHORT).show()
        
        // Clear cache
        VideoCacheManager.clearCache(this)
        VideoCache.cachedVideos = emptyList()
        
        // Restart app to trigger rescan
        val intent = Intent(this, LoadingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun checkStoragePermission(): Boolean {
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
            else -> true
        }
    }
    
    private fun requestStoragePermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ - Request READ_MEDIA_VIDEO
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_VIDEO),
                    REQUEST_STORAGE_PERMISSION
                )
                Log.d(TAG, "Requesting READ_MEDIA_VIDEO permission for Android 13+")
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6-12 - Request READ_EXTERNAL_STORAGE
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_STORAGE_PERMISSION
                )
                Log.d(TAG, "Requesting READ_EXTERNAL_STORAGE permission")
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Storage permission granted")
                initializeApp()
            } else {
                Toast.makeText(this, "Storage permission required", Toast.LENGTH_LONG).show()
                initializeApp()
            }
        }
    }
}