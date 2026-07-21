package com.haminyan.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haminyan.app.data.model.NearbyMinyan
import com.haminyan.app.ui.components.EmptyState
import com.haminyan.app.ui.components.ErrorState
import com.haminyan.app.ui.components.LoadingState
import com.haminyan.app.ui.components.PrayerBadge
import com.haminyan.app.ui.vm.NearbyUiState
import com.haminyan.app.ui.vm.NearbyViewModel
import com.haminyan.app.util.DayUtils
import com.haminyan.app.util.DistanceFormat
import com.haminyan.app.util.HebrewDate
import com.haminyan.app.util.Intents
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RADIUS_OPTIONS = listOf(1, 2, 5, 10, 15)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyScreen(
    viewModel: NearbyViewModel,
    onOpenMosad: (id: String, name: String) -> Unit,
) {
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
        NearbyHeader(state = state, onRefresh = viewModel::refresh)

        when {
            state.permissionDenied -> EmptyState(
                icon = Icons.Outlined.LocationOff,
                title = "אין הרשאת מיקום",
                subtitle = "כדי למצוא מניינים בקרבתכם, אשרו גישה למיקום המכשיר",
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

            state.error != null && state.minyanim.isEmpty() ->
                ErrorState(message = state.error!!, onRetry = viewModel::refresh)

            else -> PullToRefreshBox(
                isRefreshing = state.loading,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                NearbyContent(
                    state = state,
                    onRadiusChange = viewModel::setRadius,
                    onTypeFilter = viewModel::setTypeFilter,
                    onOpenMosad = onOpenMosad,
                )
            }
        }
    }
}

@Composable
private fun NearbyHeader(state: NearbyUiState, onRefresh: () -> Unit) {
    val dayName = DayUtils.DAY_NAMES[DayUtils.todayLetter()] ?: ""
    val gregorian = LocalDate.now().format(DateTimeFormatter.ofPattern("d.M.yyyy", Locale.getDefault()))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "מניינים בקרבתי",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "יום $dayName • ${HebrewDate.today()} • $gregorian",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.lastUpdated != null) {
                Text(
                    text = "עודכן ב-${state.lastUpdated}",
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
private fun NearbyContent(
    state: NearbyUiState,
    onRadiusChange: (Int) -> Unit,
    onTypeFilter: (String?) -> Unit,
    onOpenMosad: (id: String, name: String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, bottom = 24.dp, top = 4.dp,
        ),
    ) {
        item {
            RadiusSelector(current = state.radiusKm, onSelect = onRadiusChange)
        }
        if (state.availableTypes.size > 1) {
            item {
                TypeFilterRow(
                    types = state.availableTypes,
                    selected = state.typeFilter,
                    onSelect = onTypeFilter,
                )
            }
        }
        if (state.filtered.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "לא נמצאו מניינים",
                    subtitle = "נסו להגדיל את רדיוס החיפוש",
                    modifier = Modifier.height(320.dp),
                )
            }
        } else {
            items(state.filtered, key = { "${it.mosadId}-${it.time}-${it.room()}" }) { minyan ->
                NearbyMinyanCard(minyan = minyan, onOpenMosad = onOpenMosad)
            }
        }
    }
}

private fun NearbyMinyan.room(): String = comment ?: ""

@Composable
private fun WalkingInfo(minyan: NearbyMinyan) {
    val walkDistance = DistanceFormat.meters(minyan.walkMeters)
    val walkDuration = DistanceFormat.walkDuration(minyan.walkSeconds)

    if (walkDistance != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.DirectionsWalk,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (walkDuration != null) "$walkDistance • $walkDuration" else walkDistance,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium,
            )
        }
    } else if (!minyan.distance.isNullOrBlank()) {
        // אין נתוני הליכה - נציג את המרחק האווירי מנדרים פלוס
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Icon(
                Icons.Outlined.Straighten,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${minyan.distance} (אווירי)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RadiusSelector(current: Int, onSelect: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.AutoMirrored.Outlined.DirectionsWalk,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(RADIUS_OPTIONS) { km ->
                FilterChip(
                    selected = current == km,
                    onClick = { onSelect(km) },
                    label = { Text("$km ק\"מ") },
                )
            }
        }
    }
}

@Composable
private fun TypeFilterRow(
    types: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("הכל") },
            )
        }
        items(types) { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(if (selected == type) null else type) },
                label = { Text(type) },
            )
        }
    }
}

@Composable
private fun NearbyMinyanCard(
    minyan: NearbyMinyan,
    onOpenMosad: (id: String, name: String) -> Unit,
) {
    val context = LocalContext.current
    val countdown = DayUtils.minutesUntil(minyan.time)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = minyan.mosadId != null) {
                onOpenMosad(minyan.mosadId!!, minyan.name ?: "")
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrayerBadge(type = minyan.type)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = minyan.name ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = minyan.type ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    WalkingInfo(minyan = minyan)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = minyan.time ?: "",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    if (countdown != null) {
                        Text(
                            text = DayUtils.countdownLabel(countdown),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }

            if (!minyan.comment.isNullOrBlank() || !minyan.address.isNullOrBlank()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                if (!minyan.comment.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = minyan.comment!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    if (!minyan.address.isNullOrBlank()) {
                        Icon(
                            Icons.Outlined.Place,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = minyan.address!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    if (minyan.lat != null && minyan.lng != null) {
                        TextButton(onClick = {
                            Intents.openNavigation(context, minyan.lat!!, minyan.lng!!, minyan.name ?: "")
                        }) {
                            Icon(
                                Icons.Outlined.Navigation,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("ניווט")
                        }
                    }
                }
            }
        }
    }
}
