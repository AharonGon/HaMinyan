package com.haminyan.app.data

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface HebcalApi {

    @GET("zmanim")
    suspend fun zmanim(
        @Query("cfg") cfg: String = "json",
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("date") date: String,
        @Query("tzid") tzid: String,
    ): ZmanimResponse

    @GET("hebcal")
    suspend fun calendar(
        @Query("v") version: String = "1",
        @Query("cfg") cfg: String = "json",
        @Query("start") start: String,
        @Query("end") end: String,
        @Query("F") dafYomi: String = "on",
        @Query("maj") major: String = "on",
        @Query("min") minor: String = "on",
        @Query("mod") modern: String = "on",
        @Query("nx") roshChodesh: String = "on",
        @Query("mf") fast: String = "on",
        @Query("ss") specialShabbat: String = "on",
        @Query("s") parsha: String = "on",
        @Query("c") candles: String = "on",
        @Query("il") israel: String = "1",
    ): CalendarResponse

    companion object {
        private const val BASE_URL = "https://www.hebcal.com/"

        fun create(): HebcalApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "HaMinyan-Android/1.3")
                        .build()
                    chain.proceed(request)
                }
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
                .build()
                .create(HebcalApi::class.java)
        }
    }
}
