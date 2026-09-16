package com.foxings.foobarthingy


import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object BeefwebClient {

    fun artworkUrl(playlistId: String, index: Int): String {
        return "${BASE_URL}api/artwork/$playlistId/$index"
    }

    // Use the local IP you found earlier with ipconfig — must end in a slash
    private const val BASE_URL = "http://192.168.1.103:8880/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val api: BeefwebApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BeefwebApi::class.java)
    }
}