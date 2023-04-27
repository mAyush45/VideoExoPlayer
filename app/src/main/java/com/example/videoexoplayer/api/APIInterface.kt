package com.example.videoexoplayer.api

import com.example.videoexoplayer.models.VideoModel
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface APIInterface {
    @GET("videos/")
    suspend fun getVideos(
        @Query("key") apiKey: String,
        @Query("q") query: String
    ): Response<VideoModel>


}
