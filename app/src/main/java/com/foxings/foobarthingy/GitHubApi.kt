package com.foxings.foobarthingy

import retrofit2.http.GET

interface GitHubApi {
    @GET("repos/raisvosfox/Foobar2kontroller/releases/latest")
    suspend fun getLatestRelease(): GitHubRelease
}

data class GitHubRelease(
    val tag_name: String
)
