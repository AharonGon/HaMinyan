package com.haminyan.app.data

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * OpenRouteService Matrix API - חישוב מרחק וזמן הליכה אמיתיים.
 * קואורדינטות נשלחות בסדר [lng, lat] כפי שנדרש ב-ORS.
 */
data class MatrixRequest(
    @SerializedName("locations") val locations: List<List<Double>>,
    @SerializedName("sources") val sources: List<Int>,
    @SerializedName("destinations") val destinations: List<Int>,
    @SerializedName("metrics") val metrics: List<String> = listOf("distance", "duration"),
    @SerializedName("units") val units: String = "m",
)

data class MatrixResponse(
    @SerializedName("distances") val distances: List<List<Double?>>? = null,
    @SerializedName("durations") val durations: List<List<Double?>>? = null,
)

interface RoutingApi {

    @POST("v2/matrix/foot-walking")
    suspend fun walkingMatrix(@Body body: MatrixRequest): MatrixResponse

    companion object {
        private const val BASE_URL = "https://api.openrouteservice.org/"

        fun create(apiKey: String): RoutingApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("Authorization", apiKey)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .build()
                    chain.proceed(request)
                }
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
                .build()
                .create(RoutingApi::class.java)
        }
    }
}
