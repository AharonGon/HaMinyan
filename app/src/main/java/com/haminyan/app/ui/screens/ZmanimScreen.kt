package com.haminyan.app.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.GpsOff
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haminyan.app.data.HalachicTime
import com.haminyan.app.data.SpecialDay
import com.haminyan.app.data.ZmanimGroup
import com.haminyan.app.ui.components.EmptyState
import com.haminyan.app.ui.components.ErrorState
import com.haminyan.app.ui.components.LoadingState
import com.haminyan.app.ui.vm.ZmanimUiState
import com.haminyan.app.ui.vm.ZmanimViewModel
import com.haminyan.app.util.DayUtils
import com.haminyan.app.util.HebrewDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZmanimScreen(viewModel: ZmanimViewModel) {
    val state by viewModel.state.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        viewModel.onPermissionResult(result.values.any { it })
    }

    LaunchedEffect(Unit) {
        if (!state.hasLoadedOnce && !state.permissionDenied) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ZmanimHeader(state = state, onRefresh = viewModel::refresh)

        if (state.coarseOnly && !state.permissionDenied) {
            ZmanimCoarseBanner()
        }

        when {
            state.permissionDenied -> EmptyState(
                icon = Icons.Outlined.LocationOff,
                title = "נדרש מיקום לחישוב זמנים",
                subtitle = "זמני הלכה מחושבים לפי מיקומכם. אשרו גישה למיקום המכשיר",
                actionLabel = "אישור גישה למיקום",
                onAction = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    )
                },
            )

            state.loading && !state.hasLoadedOnce -> LoadingState()

            state.error != null && state.zmanim.isEmpty() -> ErrorState(
                message = state.error!!,
                onRetry = viewModel::refresh,
                technicalDetails = state.errorDetails,
            )

            else -> PullToRefreshBox(
                isRefreshing = state.loading,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                ZmanimContent(state = state)
            }
        }
    }
}

@Composable
private fun ZmanimHeader(state: ZmanimUiState, onRefresh: () -> Unit) {
    val dayName = DayUtils.DAY_NAMES[DayUtils.todayLetter()] ?: ""
    val gregorian = LocalDate.now().format(DateTimeFormatter.ofPattern("d.M.yyyy", Locale.getDefault()))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "זמני היום", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "יום $dayName • ${HebrewDate.today()} • $gregorian",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.locationLabel != null) {
                val accuracy = state.locationAccuracy
                val accuracyText = if (accuracy != null) " • ±${accuracy.roundToInt()} מ׳" else ""
                Text(
                    text = "מיקום: ${state.locationLabel}$accuracyText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Outlined.Refresh, contentDescription = "רענון")
        }
    }
}

@Composable
private fun ZmanimCoarseBanner() {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Icon(
                Icons.Outlined.GpsOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "מיקום מקורב",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = "לדיוק מירבי בזמנים, אפשרו מיקום מדויק בהגדרות",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            TextButton(onClick = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
                context.startActivity(intent)
            }) {
                Text("הגדרות")
            }
        }
    }
}

@Composable
private fun ZmanimContent(state: ZmanimUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.dayInfo?.let { info ->
            if (info.specialDays.isNotEmpty()) {
                item { SpecialDaysCard(days = info.specialDays) }
            }
            info.dafYomi?.let { daf ->
                item { DafYomiCard(daf = daf, link = info.dafYomiLink) }
            }
        }

        ZmanimGroup.entries.forEach { group ->
            val items = state.groupedZmanim[group].orEmpty()
            if (items.isEmpty()) return@forEach
            item {
                ZmanimGroupCard(
                    group = group,
                    zmanim = items,
                    nextKey = state.nextZmanKey,
                )
            }
        }

        item {
            Text(
                text = "זמנים לפי Hebcal • מחושבים לפי מיקומכם",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SpecialDaysCard(days: List<SpecialDay>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Celebration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "יום מיוחד",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            days.forEach { day ->
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        specialDayIcon(day.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp).padding(top = 2.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = day.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        day.memo?.takeIf { it.isNotBlank() }?.let { memo ->
                            Text(
                                text = translateMemo(memo),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DafYomiCard(daf: String, link: String?) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "דף יומי",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = daf,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            if (link != null) {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                }) {
                    Text("ספריא")
                }
            }
        }
    }
}

@Composable
private fun ZmanimGroupCard(
    group: ZmanimGroup,
    zmanim: List<HalachicTime>,
    nextKey: String?,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(
                    groupIcon(group),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            zmanim.forEach { zman ->
                val isNext = zman.key == nextKey
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isNext) {
                                Modifier.background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    RoundedCornerShape(8.dp),
                                )
                            } else Modifier
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        if (isNext) {
                            Icon(
                                Icons.Outlined.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = zman.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isNext) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = zman.time,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                        color = if (isNext) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

private fun groupIcon(group: ZmanimGroup): ImageVector = when (group) {
    ZmanimGroup.MORNING -> Icons.Outlined.WbTwilight
    ZmanimGroup.DAY -> Icons.Outlined.WbSunny
    ZmanimGroup.EVENING -> Icons.Outlined.Schedule
}

private fun specialDayIcon(category: String): ImageVector = when (category) {
    "roshchodesh" -> Icons.Outlined.Star
    "parashat" -> Icons.AutoMirrored.Outlined.MenuBook
    else -> Icons.Outlined.Celebration
}

/** תרגום קצר לתיאורי חגים באנגלית מ-Hebcal */
private fun translateMemo(memo: String): String = when {
    memo.contains("Fast", ignoreCase = true) && memo.contains("Temple", ignoreCase = true) ->
        "צום לזכר חורבן בית המקדש"
    memo.contains("Fast", ignoreCase = true) -> "יום צום"
    memo.contains("Passover", ignoreCase = true) -> "חג הפסח"
    memo.contains("Sukkot", ignoreCase = true) -> "חג הסוכות"
    memo.contains("Shavuot", ignoreCase = true) -> "חג השבועות"
    memo.contains("Rosh Hashana", ignoreCase = true) -> "ראש השנה"
    memo.contains("Yom Kippur", ignoreCase = true) -> "יום כיפור"
    else -> memo
}
