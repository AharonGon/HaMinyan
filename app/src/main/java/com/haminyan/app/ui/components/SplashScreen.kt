package com.haminyan.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.haminyan.app.R

/** צבע רקע תואם ל-splash.png כדי שלא יופיעו פסים בצבע אחר */
private val SplashBackground = Color(0xFF3D8FD4)

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBackground),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.splash_screen),
            contentDescription = "המניין",
            modifier = Modifier.fillMaxSize(),
            // Fit = כל התמונה נראית (כולל הכיתוב), בלי חיתוך
            contentScale = ContentScale.Fit,
        )
    }
}
