package com.haminyan.app.data

import com.google.gson.GsonBuilder
import com.haminyan.app.data.model.ClientBoxResponse
import com.haminyan.app.data.model.MosadMinyanResponse
import com.haminyan.app.data.model.NearestResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface NedarimApi {

    @GET("Manage.aspx?Action=GetClientBox&Parent=&Mosdot=")
    suspend fun searchMosad(@Query("ByName") name: String): ClientBoxResponse

    @GET("Zmanim/Manage.aspx?Action=GetMosadMinyan")
    suspend fun getMosadMinyan(@Query("MosadId") mosadId: String): MosadMinyanResponse

    @GET("Zmanim/Manage.aspx?Action=GetNearestMinyan")
    suspend fun getNearestMinyan(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Int,
    ): NearestResponse

    companion object {
        private const val BASE_URL = "https://www.matara.pro/nedarimplus/"

        fun create(): NedarimApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("Referer", "https://www.matara.pro/nedarimplus/")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("User-Agent", "HaMinyan-Android/1.0")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val gson = GsonBuilder().create()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(NedarimApi::class.java)
        }
    }
}
