package com.foxings.foobarthingy

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface BeefwebApi {

    @GET("api/player")
    suspend fun getPlayerState(
        @Query("columns") columns: String = "%artist%,%title%,%album%,%length%"
    ): PlayerResponse

    @POST("api/player/play")
    suspend fun play(): Response<Unit>

    @POST("api/player/pause")
    suspend fun pause(): Response<Unit>

    @POST("api/player/pause/toggle")
    suspend fun togglePause(): Response<Unit>

    @POST("api/player/stop")
    suspend fun stop(): Response<Unit>

    @POST("api/player/next")
    suspend fun next(): Response<Unit>

    @POST("api/player/previous")
    suspend fun previous(): Response<Unit>

    @POST("api/player/volume/set")
    suspend fun setVolume(
        @Query("value") value: Double
    ): Response<Unit>

    @POST("api/player/seek")
    suspend fun seek(
        @Query("position") position: Double
    ): Response<Unit>
}