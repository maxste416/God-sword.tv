package com.godsword.tv

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import com.godsword.tv.adapters.VideoCardPresenter
import com.godsword.tv.models.Video
import com.godsword.tv.usb.UsbVideoScanner

class MainActivity : FragmentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_STORAGE_PERMISSION = 100
    }
    
    private lateinit var usbScanner: UsbVideoScanner
    private lateinit var browseSupportFragment: BrowseSupportFragment
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_main)
            Log.d(TAG, "God'sword.TV - Starting application")
            
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
                
                // FIXED: Use ContextCompat for API level compatibility
                try {
                    brandColor = ContextCompat.getColor(this@MainActivity, R.color.primary_blue)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not set brand color", e)
                }
                
                title = "God'sword.TV - हिंदी क्रिश्चियन वीडियो"
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
                // Fallback: scan again if cache is empty
                Log.d(TAG, "Cache empty, scanning again...")
                Toast.makeText(this, "पेनड्राइव स्कैन हो रहा है...", Toast.LENGTH_SHORT).show()
                usbScanner.scanForVideos()
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
            
            // Show summary
            val fromLoading = intent.getBooleanExtra("from_loading", false)
            if (fromLoading) {
                Toast.makeText(
                    this,
                    "✓ ${usbVideos.size} videos loaded successfully",
                    Toast.LENGTH_LONG
                ).show()
            }
            
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
        val intent = Intent(this, PlaybackActivity::class.java).apply {
            putExtra("video_url", video.videoUrl)
            putExtra("video_title", video.title)
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
    
    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION
            )
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