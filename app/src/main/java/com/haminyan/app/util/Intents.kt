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
}
