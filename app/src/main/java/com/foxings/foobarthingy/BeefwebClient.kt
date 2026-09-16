package com.foxings.foobarthingy


import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object BeefwebClient {
    private var baseUrl: String = ""
    private var _api: BeefwebApi? = null

    val api: BeefwebApi
        get() = _api ?: throw IllegalStateException("BeefwebClient not initialized. Call initialize() first.")

    fun isInitialized() = _api != null

    fun getBaseUrl() = baseUrl

    fun artworkUrl(playlistId: String, index: Int): String {
        return "${baseUrl}api/artwork/$playlistId/$index"
    }

    fun initialize(url: String) {
        val formattedUrl = if (url.startsWith("http")) {
            if (url.endsWith("/")) url else "$url/"
        } else {
            val base = "http://$url"
            if (base.endsWith("/")) base else "$base/"
        }
        
        baseUrl = formattedUrl

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        _api = Retrofit.Builder()
            .baseUrl(formattedUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BeefwebApi::class.java)
    }
}