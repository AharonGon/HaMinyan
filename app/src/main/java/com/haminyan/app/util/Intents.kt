package com.haminyan.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object Intents {

    fun openNavigation(context: Context, lat: Double, lng: Double, label: String) {
        val encoded = Uri.encode(label)
        val geo = Uri.parse("geo:$lat,$lng?q=$lat,$lng($encoded)")
        val intent = Intent(Intent.ACTION_VIEW, geo)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            val web = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
            context.startActivity(Intent(Intent.ACTION_VIEW, web))
        }
    }

    fun shareText(context: Context, title: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    /** פתיחת Issue חדש בגיטהב עם פרטי השגיאה מולאו מראש */
    fun reportIssue(context: Context, technicalDetails: String) {
        val deviceInfo = "App: ${com.haminyan.app.BuildConfig.VERSION_NAME} (${com.haminyan.app.BuildConfig.VERSION_CODE})\n" +
            "Android: ${android.os.Build.VERSION.RELEASE} | ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        val body = "```\n$technicalDetails\n$deviceInfo\n```"
        val url = "https://github.com/${com.haminyan.app.BuildConfig.GITHUB_REPOSITORY}/issues/new" +
            "?title=${Uri.encode("דיווח תקלה מהאפליקציה")}" +
            "&body=${Uri.encode(body)}"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
