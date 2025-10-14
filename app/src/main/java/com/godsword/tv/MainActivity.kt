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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        Log.d(TAG, "God'sword.TV - हिंदी वीडियो प्लेयर")
        
        // Request storage permissions first
        if (checkStoragePermission()) {
            initializeApp()
        } else {
            requestStoragePermission()
        }
    }
    
    private fun initializeApp() {
        usbScanner = UsbVideoScanner(this)
        
        val fragment = BrowseSupportFragment().apply {
            headersState = BrowseSupportFragment.HEADERS_ENABLED
            isHeadersTransitionOnBackEnabled = true
            brandColor = resources.getColor(R.color.primary_blue, null)
            title = "God'sword.TV - हिंदी क्रिश्चियन वीडियो" // Hindi Christian Videos
        }
        
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_frame, fragment)
            .commit()
        
        loadUsbVideos(fragment)
    }
    
    private fun loadUsbVideos(fragment: BrowseSupportFragment) {
        try {
            Log.d(TAG, "Scanning USB drive for videos...")
            Toast.makeText(this, "पेनड्राइव स्कैन हो रहा है... (Scanning USB)", Toast.LENGTH_SHORT).show()
            
            val usbVideos = usbScanner.scanForVideos()
            
            Log.d(TAG, "Found ${usbVideos.size} videos")
            
            if (usbVideos.isEmpty()) {
                showNoUsbMessage(fragment)
                return
            }
            
            val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
            
            // Group videos by folder (each folder = category)
            val videosByFolder = usbVideos.groupBy { it.category }
            
            Log.d(TAG, "Found ${videosByFolder.size} folders")
            
            videosByFolder.forEach { (folderName, videos) ->
                val listRowAdapter = ArrayObjectAdapter(VideoCardPresenter())
                videos.forEach { video ->
                    listRowAdapter.add(video)
                }
                
                // Add Hindi labels for common folder names
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
            
            Toast.makeText(
                this,
                "${usbVideos.size} वीडियो मिले (${usbVideos.size} videos found)",
                Toast.LENGTH_LONG
            ).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading videos", e)
            showErrorMessage(fragment, e.message ?: "Unknown error")
        }
    }
    
    private fun getHindiFolderName(folderName: String): String {
        return when (folderName.lowercase()) {
            "worship", "aaradhana", "आराधना" -> "🎵 Worship / आराधना"
            "sermons", "sermon", "updesh", "उपदेश" -> "📖 Sermons / उपदेश"
            "bible-stories", "stories", "kahaniya", "कहानियाँ" -> "📚 Bible Stories / बाइबिल कहानियाँ"
            "kids", "bacho-ke-liye", "बच्चों-के-लिए" -> "👶 Kids / बच्चों के लिए"
            "testimonies", "gawahi", "गवाही" -> "🙏 Testimonies / गवाही"
            "songs", "geeton", "गीतों" -> "🎤 Songs / गीत"
            "prayer", "prarthana", "प्रार्थना" -> "🙏 Prayer / प्रार्थना"
            "teaching", "shiksha", "शिक्षा" -> "📘 Teaching / शिक्षा"
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
            description = "USB Pendrive not detected\nकृपया पेनड्राइव लगाएं और ऐप दोबारा खोलें",
            duration = "",
            thumbnailUrl = "",
            videoUrl = "",
            category = "Status"
        )
        
        listRowAdapter.add(emptyVideo)
        val header = HeaderItem("⚠️ सूचना / Notice")
        rowsAdapter.add(ListRow(header, listRowAdapter))
        
        fragment.adapter = rowsAdapter
        
        Toast.makeText(
            this,
            "कृपया वीडियो वाली पेनड्राइव लगाएं\nPlease insert USB pendrive with videos",
            Toast.LENGTH_LONG
        ).show()
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
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
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
                Toast.makeText(
                    this,
                    "Storage permission required\nवीडियो देखने के लिए अनुमति दें",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "App resumed")
    }
}
