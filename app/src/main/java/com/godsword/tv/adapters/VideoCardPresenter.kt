package com.godsword.tv.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.godsword.tv.R
import com.godsword.tv.models.Video
import com.godsword.tv.utils.ThumbnailGenerator
import kotlinx.coroutines.*
import java.io.File

class VideoCardPresenter : Presenter() {
    
    companion object {
        private const val TAG = "VideoCardPresenter"
    }
    
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
        
        // Try to load thumbnail
        loadThumbnail(cardViewHolder, video)
    }
    
    private fun loadThumbnail(holder: VideoCardViewHolder, video: Video) {
        val context = holder.view.context
        
        when {
            // Case 1: Thumbnail file already exists
            video.thumbnailUrl.isNotEmpty() && File(video.thumbnailUrl).exists() -> {
                Log.d(TAG, "Loading existing thumbnail for: ${video.title}")
                Glide.with(context)
                    .load(File(video.thumbnailUrl))
                    .placeholder(createColoredPlaceholder(video.category))
                    .error(createColoredPlaceholder(video.category))
                    .into(holder.imageView)
            }
            
            // Case 2: No thumbnail - show colored placeholder and try to generate
            else -> {
                Log.d(TAG, "No thumbnail for: ${video.title}, using placeholder")
                
                // Show colored placeholder immediately
                holder.imageView.setImageDrawable(createColoredPlaceholder(video.category))
                
                // Try to generate thumbnail in background
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val thumbnailPath = ThumbnailGenerator.generateThumbnail(
                            context,
                            video.videoUrl
                        )
                        
                        if (thumbnailPath != null) {
                            // Switch to main thread to update UI
                            withContext(Dispatchers.Main) {
                                Glide.with(context)
                                    .load(File(thumbnailPath))
                                    .into(holder.imageView)
                                Log.d(TAG, "✓ Thumbnail generated and loaded: ${video.title}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "✗ Failed to generate thumbnail: ${e.message}")
                    }
                }
            }
        }
    }
    
    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardViewHolder = viewHolder as VideoCardViewHolder
        Glide.with(cardViewHolder.view.context).clear(cardViewHolder.imageView)
    }
    
    /**
     * Create colored gradient placeholder based on category
     */
    private fun createColoredPlaceholder(category: String): GradientDrawable {
        val color = getCategoryColor(category)
        val darkerColor = adjustColorBrightness(color, 0.7f)
        
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(color, darkerColor)
        ).apply {
            cornerRadius = 8f
        }
    }
    
    /**
     * Get color based on video category (Dark glossy theme)
     */
    private fun getCategoryColor(category: String): Int {
        return when (category.lowercase()) {
            "worship", "aaradhana", "आराधना" -> Color.parseColor("#4A4458") // Dark Purple
            "sermons", "sermon", "updesh", "उपदेश" -> Color.parseColor("#2D3E50") // Dark Blue
            "bible-stories", "stories", "kahaniya", "कहानियाँ" -> Color.parseColor("#5D4E37") // Dark Brown
            "kids", "bacho-ke-liye", "बच्चों-के-लिए" -> Color.parseColor("#2C3E2C") // Dark Green
            "testimonies", "gawahi", "गवाही" -> Color.parseColor("#4A2D2D") // Dark Red
            "songs", "geeton", "गीतों" -> Color.parseColor("#4A2E44") // Dark Pink
            "prayer", "prarthana", "प्रार्थना" -> Color.parseColor("#3D2E4A") // Dark Purple
            "teaching", "shiksha", "शिक्षा" -> Color.parseColor("#2D4A4A") // Dark Teal
            else -> Color.parseColor("#2D2D2D") // Default Dark Gray
        }
    }
    
    /**
     * Adjust color brightness for gradient effect
     */
    private fun adjustColorBrightness(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt()
        val g = (Color.green(color) * factor).toInt()
        val b = (Color.blue(color) * factor).toInt()
        return Color.argb(a, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }
    
    inner class VideoCardViewHolder(val view: View) : ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.video_thumbnail)
        val titleText: TextView = view.findViewById(R.id.video_title)
        val durationText: TextView = view.findViewById(R.id.video_duration)
    }
}