package com.godsword.tv

import android.content.Intent
import android.os.Bundle
import android.graphics.drawable.Drawable
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition

class VideoDetailsActivity : FragmentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
        
        val fragment = VideoDetailsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.details_fragment, fragment)
            .commit()
    }
}

class VideoDetailsFragment : DetailsSupportFragment() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val videoTitle = activity?.intent?.getStringExtra("video_title") ?: ""
        val videoDescription = activity?.intent?.getStringExtra("video_description") ?: ""
        val videoDuration = activity?.intent?.getStringExtra("video_duration") ?: ""
        val videoThumbnail = activity?.intent?.getStringExtra("video_thumbnail") ?: ""
        val videoUrl = activity?.intent?.getStringExtra("video_url") ?: ""
        
        val detailsOverview = DetailsOverviewRow(videoTitle).apply {
            setImageDrawable(resources.getDrawable(R.drawable.placeholder_thumbnail, null))
            
            // Load thumbnail if available
            if (videoThumbnail.isNotEmpty()) {
                Glide.with(requireContext())
                    .load(videoThumbnail)
                    .into(object : SimpleTarget<Drawable>() {
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable>?
                        ) {
                            setImageDrawable(resource)
                        }
                    })
            }
            
            // Add description
            addAction(Action(1, "Play"))
            addAction(Action(2, "Add to Favorites"))
        }
        
        val rowsAdapter = ArrayObjectAdapter(ClassPresenterSelector().apply {
            addClassPresenter(
                DetailsOverviewRow::class.java,
                DetailsOverviewRowPresenter(DetailsDescriptionPresenter())
            )
            addClassPresenter(ListRow::class.java, ListRowPresenter())
        })
        
        rowsAdapter.add(detailsOverview)
        adapter = rowsAdapter
        
        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            if (item is Action) {
                when (item.id) {
                    1L -> {
                        val intent = Intent(requireContext(), PlaybackActivity::class.java).apply {
                            putExtra("video_url", videoUrl)
                            putExtra("video_title", videoTitle)
                        }
                        startActivity(intent)
                    }
                    2L -> {
                        // TODO: Add to favorites logic
                    }
                }
            }
        }
    }
}

class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {
    override fun onBindDescription(vh: ViewHolder, item: Any) {
        val row = item as DetailsOverviewRow
        vh.title.text = row.item.toString()
        vh.subtitle.text = "Video Details"
        vh.body.text = "Select Play to watch this video"
    }
}