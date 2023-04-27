package com.example.videoexoplayer

import android.os.Bundle
import android.util.Log.e
import androidx.appcompat.app.AppCompatActivity
import com.example.videoexoplayer.adapters.VideoAdapter
import com.example.videoexoplayer.api.APIUtilities
import com.example.videoexoplayer.databinding.ActivityMainBinding
import com.example.videoexoplayer.util.Constants.Companion.API_KEY
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: VideoAdapter

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = VideoAdapter(this, emptyList())

        fetchVideos()
    }

    private fun fetchVideos() {
        coroutineScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    APIUtilities.instance.getVideos(API_KEY, "yellow+flowers")
                }

                if (response.isSuccessful) {
                    val videos = response.body()?.hits?.map { it.videos } ?: emptyList()
                    adapter.videoList = videos
                    adapter.notifyDataSetChanged()
                    binding.viewPager2.adapter = adapter

                } else {
                    e("MainActivity", "Failed to get videos: ${response.code()}")
                }
            } catch (e: Exception) {
                e("MainActivity", "Failed to get videos: ${e.message}")
            }
        }
    }


    override fun onStop() {
        super.onStop()
        adapter.releasePlayer()
    }


    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        adapter.releasePlayer()
    }
}
