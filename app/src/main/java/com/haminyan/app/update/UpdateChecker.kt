package com.haminyan.app.update

import com.google.gson.annotations.SerializedName
import com.haminyan.app.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String = "",
    @SerializedName("name") val name: String? = null,
    @SerializedName("body") val body: String? = null,
    @SerializedName("html_url") val htmlUrl: String = "",
    @SerializedName("assets") val assets: List<GitHubAsset> = emptyList(),
)

data class GitHubAsset(
    @SerializedName("name") val name: String = "",
    @SerializedName("browser_download_url") val downloadUrl: String = "",
)

data class UpdateInfo(
    val versionName: String,
    val notes: String?,
    val downloadUrl: String,
)

private interface GitHubReleasesApi {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun latestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): GitHubRelease
}

class UpdateChecker(
    private val repository: String,
) {
    private val api = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("Accept", "application/vnd.github+json")
                            .header("X-GitHub-Api-Version", "2022-11-28")
                            .header("User-Agent", "HaMinyan-Android/${BuildConfig.VERSION_NAME}")
                            .build()
                    )
                }
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GitHubReleasesApi::class.java)

    /**
     * ה-versionCode נקרא משם ה-APK (code12.apk) או מהערת Release
     * בפורמט versionCode: 12. כך ההשוואה נשארת מספרית.
     */
    suspend fun check(): UpdateInfo? {
        val (owner, repo) = repository.split("/", limit = 2)
            .takeIf { it.size == 2 } ?: error("Invalid GitHub repository")
        val release = api.latestRelease(owner, repo)
        val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            ?: return null
        val remoteCode = Regex("""code(\d+)(?:-release)?\.apk$""", RegexOption.IGNORE_CASE)
            .find(apk.name)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: Regex("""versionCode\s*:\s*(\d+)""", RegexOption.IGNORE_CASE)
                .find(release.body.orEmpty())?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return null

        if (remoteCode <= BuildConfig.VERSION_CODE) return null
        return UpdateInfo(
            versionName = release.tagName.removePrefix("v"),
            notes = release.body?.trim()?.takeIf { it.isNotEmpty() },
            downloadUrl = apk.downloadUrl.ifBlank { release.htmlUrl },
        )
    }
}
