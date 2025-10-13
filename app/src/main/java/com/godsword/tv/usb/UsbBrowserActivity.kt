package com.godsword.tv.usb

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import com.godsword.tv.PlaybackActivity
import com.godsword.tv.R
import com.godsword.tv.adapters.VideoCardPresenter
import com.godsword.tv.models.Video

class UsbBrowserActivity : FragmentActivity() {
    
    companion object {
        private const val TAG = "UsbBrowserActivity"
    }
    
    private lateinit var usbScanner: UsbVideoScanner
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        Log.d(TAG, "UsbBrowserActivity started")
        
        usbScanner = UsbVideoScanner(this)
        
        val fragment = BrowseSupportFragment().apply {
            headersState = BrowseSupportFragment.HEADERS_ENABLED
            isHeadersTransitionOnBackEnabled = true
            
            // Try to get color from resources, fallback to default blue
            brandColor = try {
                ContextCompat.getColor(this@UsbBrowserActivity, R.color.primary_blue)
            } catch (e: Exception) {
                Color.parseColor("#4A90E2") // Default blue
            }
            
            title = "USB Drive - God'sword.TV"
        }
        
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_frame, fragment)
            .commit()
        
        loadUsbVideos(fragment)
    }
    
    private fun loadUsbVideos(fragment: BrowseSupportFragment) {
        try {
            Log.d(TAG, "Starting USB video scan...")
            val usbVideos = usbScanner.scanForVideos()
            
            Log.d(TAG, "Found ${usbVideos.size} videos")
            
            if (usbVideos.isEmpty()) {
                showNoUsbMessage(fragment)
                return
            }
            
            val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
            
            // Group videos by folder
            val videosByFolder = usbVideos.groupBy { it.category }
            
            Log.d(TAG, "Grouped into ${videosByFolder.size} folders")
            
            videosByFolder.forEach { (folderName, videos) ->
                val listRowAdapter = ArrayObjectAdapter(VideoCardPresenter())
                videos.forEach { video ->
                    listRowAdapter.add(video)
                }
                
                val header = HeaderItem(folderName)
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
                "Found ${usbVideos.size} videos",
                Toast.LENGTH_SHORT
            ).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading USB videos", e)
            showErrorMessage(fragment, e.message ?: "Unknown error")
        }
    }
    
    private fun playVideo(video: Video) {
        try {
            Log.d(TAG, "Playing video: ${video.title} from ${video.videoUrl}")
            
            val intent = Intent(this, PlaybackActivity::class.java).apply {
                putExtra("video_url", video.videoUrl)
                putExtra("video_title", video.title)
            }
            startActivity(intent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting playback", e)
            Toast.makeText(
                this,
                "Error playing video: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun showNoUsbMessage(fragment: BrowseSupportFragment) {
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val listRowAdapter = ArrayObjectAdapter(VideoCardPresenter())
        
        val emptyVideo = Video(
            id = "empty",
            title = "📁 No USB Drive Detected",
            description = "Please insert a USB drive with video files and try again",
            duration = "",
            thumbnailUrl = "",
            videoUrl = "",
            category = "USB Status"
        )
        
        listRowAdapter.add(emptyVideo)
        val header = HeaderItem("📂 USB Status")
        rowsAdapter.add(ListRow(header, listRowAdapter))
        
        fragment.adapter = rowsAdapter
        
        Toast.makeText(
            this,
            "No USB drive detected. Please insert a USB drive.",
            Toast.LENGTH_LONG
        ).show()
    }
    
    private fun showErrorMessage(fragment: BrowseSupportFragment, errorMsg: String) {
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val listRowAdapter = ArrayObjectAdapter(VideoCardPresenter())
        
        val errorVideo = Video(
            id = "error",
            title = "⚠️ Error Scanning USB",
            description = "Error: $errorMsg",
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
    
    override fun onResume() {
        super.onResume()
        // Optionally refresh USB scan when returning to this activity
        Log.d(TAG, "Activity resumed")
    }
}