package com.godsword.tv.ui

import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class VideoDetailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_DESC = "extra_desc"
        const val EXTRA_THUMB = "extra_thumb"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_CATEGORY = "extra_category"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Minimal UI (replace with your Leanback DetailsFragment)
        val tv = TextView(this).apply {
            val title = intent.getStringExtra(EXTRA_TITLE)
            val uri = intent.getStringExtra(EXTRA_URI)?.let { Uri.parse(it) }
            val category = intent.getStringExtra(EXTRA_CATEGORY)
            text = "Details\n\nTitle: $title\nCategory: $category\nUri: $uri"
            textSize = 22f
            setPadding(32, 64, 32, 32)
        }
        setContentView(tv)
    }
}
