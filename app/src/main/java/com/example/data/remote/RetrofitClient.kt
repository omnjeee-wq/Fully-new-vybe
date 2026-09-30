package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    const val DEFAULT_BASE_URL = "https://vybe.vyoma-apps.workers.dev/"

    @Volatile
    private var customBaseUrl: String? = null

    fun setBaseUrl(url: String?) {
        if (url.isNullOrBlank() || url == DEFAULT_BASE_URL) {
            customBaseUrl = null
        } else {
            val formatted = if (url.endsWith("/")) url else "$url/"
            if (formatted.toHttpUrlOrNull() != null) {
                customBaseUrl = formatted
            }
        }
    }

    fun getBaseUrl(): String {
        return customBaseUrl ?: DEFAULT_BASE_URL
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(LenientStringAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val activeBase = customBaseUrl
                val newRequest = if (activeBase != null) {
                    val defaultUrl = DEFAULT_BASE_URL
                    val originalUrlString = original.url.toString()
                    if (originalUrlString.startsWith(defaultUrl)) {
                        val replaced = originalUrlString.replaceFirst(defaultUrl, activeBase)
                        replaced.toHttpUrlOrNull()?.let { newHttpUrl ->
                            original.newBuilder().url(newHttpUrl).build()
                        } ?: original
                    } else {
                        original
                    }
                } else {
                    original
                }

                val finalRequest = newRequest.newBuilder()
                    .header("User-Agent", "VYBE-Android/1.0")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(finalRequest)
            }
            .addInterceptor(logging)
            .build()
    }

    val apiService: JioSaavnApiService by lazy {
        Retrofit.Builder()
            .baseUrl(DEFAULT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(JioSaavnApiService::class.java)
    }
}
