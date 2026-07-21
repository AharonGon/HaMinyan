package com.haminyan.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.haminyan.app.ui.vm.UpdateUiState

@Composable
fun UpdateResultDialog(
    state: UpdateUiState,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    when {
        state.update != null -> AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(Icons.Outlined.SystemUpdate, contentDescription = null)
            },
            title = {
                Text("גרסה ${state.update.versionName} זמינה")
            },
            text = {
                Text(
                    state.update.notes
                        ?: "זמינה גרסה חדשה של המניין. לחצו על הורדה כדי להתקין אותה."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(state.update.downloadUrl))
                    )
                    onDismiss()
                }) {
                    Text("הורדת העדכון")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("אחר כך")
                }
            },
        )

        state.checkedAndCurrent -> AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(Icons.Outlined.SystemUpdate, contentDescription = null)
            },
            title = {
                Text("האפליקציה מעודכנת")
            },
            text = {
                Text("מותקנת אצלכם הגרסה החדשה ביותר.")
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("אישור")
                }
            },
        )

        state.error != null -> AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("בדיקת העדכון נכשלה")
            },
            text = {
                Text(state.error)
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("אישור")
                }
            },
        )
    }
}
