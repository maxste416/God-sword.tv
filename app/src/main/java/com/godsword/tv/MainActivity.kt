package com.godsword.tv

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import com.godsword.tv.adapters.VideoCardPresenter
import com.godsword.tv.models.Video
import com.godsword.tv.usb.UsbBrowserActivity
import com.godsword.tv.utils.VideoDataProvider

class MainActivity : FragmentActivity() {
    
    companion object {
        private const val REQUEST_STORAGE_PERMISSION = 100
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Request storage permissions
        requestStoragePermissions()
        
        val fragment = BrowseSupportFragment().apply {
            headersState = BrowseSupportFragment.HEADERS_ENABLED
            isHeadersTransitionOnBackEnabled = true
            brandColor = resources.getColor(R.color.primary_blue, null)
            title = "God'sword.TV"
        }
        
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_frame, fragment)
            .commit()
        
        setupRows(fragment)
    }
    
    private fun setupRows(fragment: BrowseSupportFragment) {
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        
        // Add USB Drive option as first row
        addUsbDriveRow(rowsAdapter)
        
        // Add online content categories
        VideoDataProvider.getCategories().forEach { category ->
            val listRowAdapter = ArrayObjectAdapter(VideoCardPresenter())
            category.videos.forEach { video ->
                listRowAdapter.add(video)
            }
            
            val header = HeaderItem(category.name)
            rowsAdapter.add(ListRow(header, listRowAdapter))
        }
        
        fragment.adapter = rowsAdapter
        fragment.onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            when (item) {
                is Video -> {
                    if (item.id == "usb_drive") {
                        // Open USB Browser
                        val intent = Intent(this, UsbBrowserActivity::class.java)
                        startActivity(intent)
                    } else {
                        // Open video details for online content
                        val intent = Intent(this, VideoDetailsActivity::class.java).apply {
                            putExtra("video_id", item.id)
                            putExtra("video_title", item.title)
                            putExtra("video_description", item.description)
                            putExtra("video_duration", item.duration)
                            putExtra("video_thumbnail", item.thumbnailUrl)
                            putExtra("video_url", item.videoUrl)
                        }
                        startActivity(intent)
                    }
                }
            }
        }
    }
    
    private fun addUsbDriveRow(rowsAdapter: ArrayObjectAdapter) {
        val listRowAdapter = ArrayObjectAdapter(VideoCardPresenter())
        
        val usbOption = Video(
            id = "usb_drive",
            title = "📁 USB Drive",
            description = "Browse videos from your USB drive",
            duration = "Local",
            thumbnailUrl = "",
            videoUrl = "",
            category = "Local Storage"
        )
        
        listRowAdapter.add(usbOption)
        val header = HeaderItem("📂 Local Storage")
        rowsAdapter.add(ListRow(header, listRowAdapter))
    }
    
    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
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
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Storage permission needed to access USB drive",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
