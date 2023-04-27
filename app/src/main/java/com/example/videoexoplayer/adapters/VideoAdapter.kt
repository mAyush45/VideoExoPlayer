package com.example.videoexoplayer.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.videoexoplayer.R
import com.example.videoexoplayer.databinding.ListItemBinding
import com.example.videoexoplayer.models.Videos
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import okhttp3.OkHttpClient

class VideoAdapter(var context: Context, var videoList: List<Videos>) :
    RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private val recyclerViewHolders = mutableListOf<VideoViewHolder>()
    private var currentPlayingPosition = -1

    inner class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var binding = ListItemBinding.bind(view)
        var player: SimpleExoPlayer? = null

        init {
            recyclerViewHolders.add(this)
        }

        fun showProgressBar() {
            binding.progressBar.visibility = View.VISIBLE
            binding.playerView.useController = false
        }

        fun hideProgressBar() {
            binding.progressBar.visibility = View.GONE
            binding.playerView.useController = true
        }



    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        return VideoViewHolder(
            LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return videoList.size
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val item = videoList[position]
        holder.showProgressBar()
        holder.player = SimpleExoPlayer.Builder(holder.itemView.context).build()
        preparePlayer(holder, item.small.url, context, position)
    }

    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.playerView.player = null
        holder.player?.release()
        holder.player = null
        if (currentPlayingPosition == holder.adapterPosition) {
            currentPlayingPosition = -1
        }
    }


    private fun preparePlayer(
        holder: VideoViewHolder,
        url: String,
        context: Context,
        position: Int
    ) {
        val player = holder.player ?: return
        holder.binding.playerView.player = player

        val okHttpClient = OkHttpClient.Builder().build()
        val okHttpDataSourceFactory = OkHttpDataSourceFactory(okHttpClient)

        val mediaItem = MediaItem.fromUri(url)

        val mediaSource =
            ProgressiveMediaSource.Factory(okHttpDataSourceFactory).createMediaSource(mediaItem)
        player.setMediaSource(mediaSource)
        player.prepare()
        player.addListener(object : Player.Listener {
            override fun onIsLoadingChanged(isLoading: Boolean) {
                if (isLoading) {
                    holder.showProgressBar()
                    holder.binding.playerView.useController = false
                } else {
                    holder.hideProgressBar()
                    holder.binding.playerView.useController = true
                }
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    if (currentPlayingPosition >= 0) {
                        if (currentPlayingPosition < recyclerViewHolders.size) { // Check if currentPlayingPosition is less than the size of recyclerViewHolders
                            recyclerViewHolders[currentPlayingPosition].hideProgressBar()
                            recyclerViewHolders[currentPlayingPosition].binding.playerView.useController = true
                            if (playWhenReady) {
                                recyclerViewHolders[currentPlayingPosition].player?.play()
                            }
                        }
                    }
                    player.playWhenReady = true // Set playWhenReady to true when the state is ready
                } else if (playbackState == Player.STATE_ENDED) {
                    if (currentPlayingPosition < videoList.size - 1) {
                        currentPlayingPosition++
                        if (currentPlayingPosition < recyclerViewHolders.size) { // Check if currentPlayingPosition is less than the size of recyclerViewHolders
                            recyclerViewHolders[currentPlayingPosition].hideProgressBar()
                            recyclerViewHolders[currentPlayingPosition].player?.playWhenReady = true
                        }
                    } else {
                        // Handle the last video
                    }
                }
            }


            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                if (error is ExoPlaybackException) {
                    val cause = error.sourceException
                    if (cause is HttpDataSource.InvalidResponseCodeException) {
                        val responseCode = cause.responseCode
                        if (responseCode == 503) {
                            // Handle 503 error
                            Toast.makeText(
                                context,
                                "Server is currently unavailable. Please try again in sometime.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (responseCode == 403) {
                            // Handle 403 error
                            Toast.makeText(
                                context,
                                "Server is refusing to fulfill the request. Please try again in sometime.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // Handle other errors
                            val message = "Invalid response code: $responseCode"
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Handle other errors
                    Toast.makeText(
                        context,
                        "An error occurred while playing the video.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        })
    }






    fun releasePlayer() {
        for (holder in recyclerViewHolders) {
            holder.player?.release()
            holder.player = null
        }
        currentPlayingPosition = -1
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // The user has stopped scrolling
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                    val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
                    for (i in 0 until recyclerViewHolders.size) {
                        if (i >= firstVisiblePosition && i <= lastVisiblePosition) {
                            // The video is visible on the screen, so play it
                            recyclerViewHolders.getOrNull(i)?.player?.playWhenReady = true // Use getOrNull() to return null if the index is out of bounds
                            currentPlayingPosition = i
                        } else {
                            // The video is not visible on the screen, so pause it
                            recyclerViewHolders.getOrNull(i)?.player?.playWhenReady = false // Use getOrNull() to return null if the index is out of bounds
                        }
                    }
                } else {
                    // The user is scrolling, so pause the currently playing video
                    if (currentPlayingPosition >= 0 && currentPlayingPosition < recyclerViewHolders.size) { // Add a check to ensure that currentPlayingPosition is within the bounds of recyclerViewHolders
                        recyclerViewHolders[currentPlayingPosition].player?.playWhenReady = false
                        currentPlayingPosition = -1
                    }
                }
            }


        })
    }


    private fun pauseCurrentVideo() {
        if (currentPlayingPosition >= 0) {
            recyclerViewHolders[currentPlayingPosition].player?.playWhenReady = false
        }
    }


    companion object {
        private const val TAG = "VideoAdapter"
    }
}



