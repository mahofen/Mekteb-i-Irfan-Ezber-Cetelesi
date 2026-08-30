package com.example.data.supabase

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class SupabaseClient private constructor(private val context: Context) {
  private val config = SupabaseConfig.getInstance(context)

  private val moshi: Moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  private val headerInterceptor = Interceptor { chain ->
    val original = chain.request()
    val requestBuilder = original.newBuilder()
      .header("apikey", config.anonKey)
      .header("Content-Type", "application/json")

    // If we have an active auth token, attach it; otherwise use anon key as Bearer
    val bearerToken = config.authToken?.takeIf { it.isNotBlank() } ?: config.anonKey
    requestBuilder.header("Authorization", "Bearer $bearerToken")

    chain.proceed(requestBuilder.build())
  }

  private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BASIC
  }

  private val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .writeTimeout(20, TimeUnit.SECONDS)
      .addInterceptor(headerInterceptor)
      .addInterceptor(loggingInterceptor)
      .build()
  }

  private fun getBaseUrl(): String {
    var url = config.projectUrl.trim()
    if (!url.endsWith("/")) {
      url += "/"
    }
    return url
  }

  val authApi: SupabaseAuthApi by lazy {
    Retrofit.Builder()
      .baseUrl(getBaseUrl())
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(SupabaseAuthApi::class.java)
  }

  val restApi: SupabaseRestApi by lazy {
    Retrofit.Builder()
      .baseUrl(getBaseUrl())
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(SupabaseRestApi::class.java)
  }

  companion object {
    @Volatile
    private var INSTANCE: SupabaseClient? = null

    fun getInstance(context: Context): SupabaseClient {
      return INSTANCE ?: synchronized(this) {
        val instance = SupabaseClient(context.applicationContext)
        INSTANCE = instance
        instance
      }
    }
  }
}
