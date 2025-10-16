package com.godsword.tv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.godsword.tv.models.Video
import java.io.File

class PlaybackActivity : FragmentActivity() {
    
    companion object {
        private const val TAG = "PlaybackActivity"
        private const val AUTO_PLAY_COUNTDOWN = 5 // seconds
    }
    
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    
    // Auto-play components
    private lateinit var autoPlayOverlay: FrameLayout
    private lateinit var countdownText: TextView
    private lateinit var nextVideoTitle: TextView
    private lateinit var countdownProgress: ProgressBar
    
    // Video playlist data
    private var currentVideoIndex = 0
    private var videoPlaylist: ArrayList<Video> = arrayListOf()
    private var currentVideo: Video? = null
    
    // Auto-play timer
    private val autoPlayHandler = Handler(Looper.getMainLooper())
    private var autoPlayCountdown = AUTO_PLAY_COUNTDOWN
    private var isAutoPlayScheduled = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playback)
        
        playerView = findViewById(R.id.player_view)
        autoPlayOverlay = findViewById(R.id.autoplay_overlay)
        countdownText = findViewById(R.id.countdown_text)
        nextVideoTitle = findViewById(R.id.next_video_title)
        countdownProgress = findViewById(R.id.countdown_progress)
        
        // Get video data from intent
        val videoUrl = intent.getStringExtra("video_url")
        val videoTitle = intent.getStringExtra("video_title") ?: "Unknown"
        currentVideoIndex = intent.getIntExtra("video_index", 0)
        
        // Get playlist if available
        @Suppress("DEPRECATION")
        videoPlaylist = intent.getParcelableArrayListExtra("video_playlist") ?: arrayListOf()
        
        if (videoUrl == null) {
            Toast.makeText(this, "Error: No video URL provided", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        Log.d(TAG, "Playing: $videoTitle (${currentVideoIndex + 1}/${videoPlaylist.size})")
        
        initializePlayer(videoUrl, videoTitle)
    }
    
    private fun initializePlayer(videoUrl: String, videoTitle: String) {
        try {
            player = ExoPlayer.Builder(this).build().also { exoPlayer ->
                playerView.player = exoPlayer
                
                // Check if it's a local file or URL
                val mediaItem = if (isLocalFile(videoUrl)) {
                    val file = File(videoUrl)
                    if (!file.exists()) {
                        throw Exception("File not found: $videoUrl")
                    }
                    if (!file.canRead()) {
                        throw Exception("Cannot read file: $videoUrl")
                    }
                    Log.d(TAG, "Playing local file: ${file.absolutePath}")
                    MediaItem.fromUri(Uri.fromFile(file))
                } else {
                    Log.d(TAG, "Playing URL: $videoUrl")
                    MediaItem.fromUri(videoUrl)
                }
                
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                
                // Add listener for playback events
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                Log.d(TAG, "Playback ready")
                                hideAutoPlayOverlay()
                            }
                            Player.STATE_ENDED -> {
                                Log.d(TAG, "Playback ended")
                                handleVideoEnd()
                            }
                            Player.STATE_BUFFERING -> {
                                Log.d(TAG, "Buffering...")
                            }
                        }
                    }
                    
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e(TAG, "Playback error: ${error.message}", error)
                        Toast.makeText(
                            this@PlaybackActivity,
                            "Playback error: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        
                        // Try next video if available
                        if (hasNextVideo()) {
                            Toast.makeText(
                                this@PlaybackActivity,
                                "Trying next video...",
                                Toast.LENGTH_SHORT
                            ).show()
                            playNextVideo()
                        }
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing player", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    /**
     * Handle video end - start auto-play countdown or finish
     */
    private fun handleVideoEnd() {
        if (hasNextVideo()) {
            startAutoPlayCountdown()
        } else {
            Log.d(TAG, "Playlist finished - no more videos")
            Toast.makeText(this, "✓ Playlist finished", Toast.LENGTH_LONG).show()
            
            // Wait 2 seconds then close
            Handler(Looper.getMainLooper()).postDelayed({
                finish()
            }, 2000)
        }
    }
    
    /**
     * Check if there's a next video in the playlist
     */
    private fun hasNextVideo(): Boolean {
        return videoPlaylist.isNotEmpty() && currentVideoIndex < videoPlaylist.size - 1
    }
    
    /**
     * Get the next video from playlist
     */
    private fun getNextVideo(): Video? {
        return if (hasNextVideo()) {
            videoPlaylist[currentVideoIndex + 1]
        } else {
            null
        }
    }
    
    /**
     * Start auto-play countdown timer
     */
    private fun startAutoPlayCountdown() {
        val nextVideo = getNextVideo() ?: return
        
        isAutoPlayScheduled = true
        autoPlayCountdown = AUTO_PLAY_COUNTDOWN
        
        // Show overlay with next video info
        showAutoPlayOverlay(nextVideo)
        
        // Start countdown
        runCountdown()
    }
    
    /**
     * Run the countdown recursively
     */
    private fun runCountdown() {
        if (!isAutoPlayScheduled) return
        
        if (autoPlayCountdown > 0) {
            // Update UI
            countdownText.text = autoPlayCountdown.toString()
            
            val progress = ((AUTO_PLAY_COUNTDOWN - autoPlayCountdown).toFloat() / AUTO_PLAY_COUNTDOWN * 100).toInt()
            countdownProgress.progress = progress
            
            // Schedule next countdown tick
            autoPlayHandler.postDelayed({
                autoPlayCountdown--
                runCountdown()
            }, 1000)
            
        } else {
            // Countdown finished - play next video
            playNextVideo()
        }
    }
    
    /**
     * Cancel auto-play countdown
     */
    private fun cancelAutoPlay() {
        if (isAutoPlayScheduled) {
            isAutoPlayScheduled = false
            autoPlayHandler.removeCallbacksAndMessages(null)
            hideAutoPlayOverlay()
            
            Log.d(TAG, "Auto-play cancelled by user")
            Toast.makeText(this, "Auto-play cancelled", Toast.LENGTH_SHORT).show()
            
            // Close activity after 2 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                finish()
            }, 2000)
        }
    }
    
    /**
     * Play the next video in playlist
     */
    private fun playNextVideo() {
        val nextVideo = getNextVideo()
        
        if (nextVideo != null) {
            currentVideoIndex++
            currentVideo = nextVideo
            
            Log.d(TAG, "Auto-playing next video: ${nextVideo.title} (${currentVideoIndex + 1}/${videoPlaylist.size})")
            
            hideAutoPlayOverlay()
            
            // Release current player
            player?.release()
            
            // Initialize new player with next video
            initializePlayer(nextVideo.videoUrl, nextVideo.title)
            
            // Show toast
            Toast.makeText(
                this,
                "▶️ Now playing: ${nextVideo.title}",
                Toast.LENGTH_SHORT
            ).show()
            
        } else {
            Log.d(TAG, "No next video available")
            finish()
        }
    }
    
    /**
     * Show auto-play overlay
     */
    private fun showAutoPlayOverlay(nextVideo: Video) {
        nextVideoTitle.text = "Next: ${nextVideo.title}"
        autoPlayOverlay.visibility = View.VISIBLE
        
        // Request focus for back button handling
        autoPlayOverlay.requestFocus()
    }
    
    /**
     * Hide auto-play overlay
     */
    private fun hideAutoPlayOverlay() {
        autoPlayOverlay.visibility = View.GONE
        isAutoPlayScheduled = false
        autoPlayHandler.removeCallbacksAndMessages(null)
    }
    
    /**
     * Handle remote control key events
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Cancel auto-play on BACK button
        if (keyCode == KeyEvent.KEYCODE_BACK && isAutoPlayScheduled) {
            cancelAutoPlay()
            return true
        }
        
        // Play next video immediately on DPAD_RIGHT during countdown
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && isAutoPlayScheduled) {
            Log.d(TAG, "Skip countdown - playing next video now")
            isAutoPlayScheduled = false
            autoPlayHandler.removeCallbacksAndMessages(null)
            playNextVideo()
            return true
        }
        
        return super.onKeyDown(keyCode, event)
    }
    
    private fun isLocalFile(path: String): Boolean {
        return path.startsWith("/") && File(path).exists()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        autoPlayHandler.removeCallbacksAndMessages(null)
        player?.release()
        Log.d(TAG, "Player released")
    }
    
    override fun onPause() {
        super.onPause()
        player?.pause()
    }
    
    override fun onResume() {
        super.onResume()
        player?.play()
    }
}