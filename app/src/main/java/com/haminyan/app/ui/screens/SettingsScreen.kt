package com.haminyan.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.haminyan.app.BuildConfig
import com.haminyan.app.MinyanApp
import com.haminyan.app.data.ThemeMode
import com.haminyan.app.ui.vm.UpdateUiState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    updateState: UpdateUiState,
    onCheckForUpdates: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as MinyanApp
    val scope = rememberCoroutineScope()
    val themeMode by app.prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val radiusKm by app.prefs.radiusKm.collectAsState(initial = 2)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "הגדרות", style = MaterialTheme.typography.headlineSmall)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "עדכוני אפליקציה", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "גרסה ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = onCheckForUpdates,
                        enabled = !updateState.checking,
                    ) {
                        if (updateState.checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("בדיקת עדכון")
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "מראה", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(12.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(
                        Triple(ThemeMode.SYSTEM, "אוטומטי", Icons.Outlined.BrightnessAuto),
                        Triple(ThemeMode.LIGHT, "בהיר", Icons.Outlined.LightMode),
                        Triple(ThemeMode.DARK, "כהה", Icons.Outlined.DarkMode),
                    )
                    options.forEachIndexed { index, (mode, label, icon) ->
                        SegmentedButton(
                            selected = themeMode == mode,
                            onClick = { scope.launch { app.prefs.setThemeMode(mode) } },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            icon = {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Explore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "רדיוס חיפוש ברירת מחדל",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "$radiusKm ק\"מ",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = radiusKm.toFloat(),
                    onValueChange = { value ->
                        scope.launch { app.prefs.setRadiusKm(value.roundToInt().coerceIn(1, 15)) }
                    },
                    valueRange = 1f..15f,
                    steps = 13,
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = "אודות", style = MaterialTheme.typography.titleMedium)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(
                    text = "המניין - איתור מנייני תפילה בקרבתכם",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "הנתונים מסופקים ממאגר המניינים של נדרים פלוס. ייתכנו שינויים בזמנים - מומלץ לוודא מול בית הכנסת.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = "גרסה ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
