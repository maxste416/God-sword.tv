package com.godsword.tv

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File

class PlaybackActivity : FragmentActivity() {
    
    companion object {
        private const val TAG = "PlaybackActivity"
    }
    
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playback)
        
        playerView = findViewById(R.id.player_view)
        
        val videoUrl = intent.getStringExtra("video_url")
        val videoTitle = intent.getStringExtra("video_title") ?: "Unknown"
        
        if (videoUrl == null) {
            Toast.makeText(this, "Error: No video URL provided", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        Log.d(TAG, "Playing: $videoTitle from $videoUrl")
        
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
                            Player.STATE_READY -> Log.d(TAG, "Playback ready")
                            Player.STATE_ENDED -> Log.d(TAG, "Playback ended")
                            Player.STATE_BUFFERING -> Log.d(TAG, "Buffering...")
                        }
                    }
                    
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e(TAG, "Playback error: ${error.message}", error)
                        Toast.makeText(
                            this@PlaybackActivity,
                            "Playback error: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing player", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun isLocalFile(path: String): Boolean {
        return path.startsWith("/") && File(path).exists()
    }
    
    override fun onDestroy() {
        super.onDestroy()
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