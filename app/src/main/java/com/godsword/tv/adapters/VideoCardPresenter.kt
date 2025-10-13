package com.godsword.tv.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.godsword.tv.R
import com.godsword.tv.models.Video

class VideoCardPresenter : Presenter() {
    
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.video_card, parent, false)
        return VideoCardViewHolder(view)
    }
    
    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val video = item as Video
        val cardViewHolder = viewHolder as VideoCardViewHolder
        
        cardViewHolder.titleText.text = video.title
        cardViewHolder.durationText.text = video.duration
        
        // Load thumbnail if URL exists, otherwise use placeholder
        if (video.thumbnailUrl.isNotEmpty()) {
            Glide.with(cardViewHolder.view.context)
                .load(video.thumbnailUrl)
                .placeholder(R.drawable.placeholder_thumbnail)
                .into(cardViewHolder.imageView)
        } else {
            // For local files without thumbnails, show colored placeholder
            cardViewHolder.imageView.setBackgroundColor(Color.parseColor("#4A90E2"))
        }
    }
    
    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardViewHolder = viewHolder as VideoCardViewHolder
        Glide.with(cardViewHolder.view.context).clear(cardViewHolder.imageView)
    }
    
    inner class VideoCardViewHolder(val view: View) : ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.video_thumbnail)
        val titleText: TextView = view.findViewById(R.id.video_title)
        val durationText: TextView = view.findViewById(R.id.video_duration)
    }
}
