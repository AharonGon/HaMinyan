package com.haminyan.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haminyan.app.data.model.MinyanItem
import com.haminyan.app.ui.components.ErrorState
import com.haminyan.app.ui.components.EmptyState
import com.haminyan.app.ui.components.LoadingState
import com.haminyan.app.ui.components.PrayerBadge
import com.haminyan.app.ui.vm.DetailViewModel
import com.haminyan.app.util.DayUtils
import com.haminyan.app.util.Intents
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    mosadName: String,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = mosadName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזרה")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val text = buildShareText(mosadName, state.daySchedule, state.selectedDay)
                        Intents.shareText(context, "זמני תפילות - $mosadName", text)
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = "שיתוף")
                    }
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorite) "הסרה ממועדפים" else "הוספה למועדפים",
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingState()
                state.error != null -> ErrorState(message = state.error!!, onRetry = viewModel::load)
                else -> {
                    DaySelector(
                        selected = state.selectedDay,
                        onSelect = viewModel::selectDay,
                    )
                    ScheduleList(
                        grouped = state.daySchedule,
                        isToday = state.selectedDay == DayUtils.todayLetter(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DaySelector(selected: Char, onSelect: (Char) -> Unit) {
    val today = DayUtils.todayLetter()
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        items(DayUtils.DAY_LETTERS) { day ->
            FilterChip(
                selected = selected == day,
                onClick = { onSelect(day) },
                label = {
                    Text(if (day == today) "היום" else DayUtils.DAY_NAMES[day] ?: day.toString())
                },
            )
        }
    }
}

@Composable
private fun ScheduleList(grouped: Map<String, List<MinyanItem>>, isToday: Boolean) {
    if (grouped.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.EventBusy,
            title = "אין מניינים ביום זה",
            subtitle = "בחרו יום אחר מהרשימה למעלה",
        )
        return
    }

    // המניין הקרוב ביותר שעדיין לא עבר (רק בתצוגת היום הנוכחי)
    val now = LocalTime.now()
    val nextItem: MinyanItem? = if (isToday) {
        grouped.values.flatten()
            .filter { DayUtils.parseTime(it.time)?.isAfter(now) == true }
            .minByOrNull { DayUtils.parseTime(it.time)!! }
    } else null

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        grouped.forEach { (type, list) ->
            item(key = "header-$type") {
                Text(
                    text = type,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                )
            }
            items(list, key = { "$type-${it.time}-${it.room}-${it.days}" }) { item ->
                ScheduleRow(item = item, highlighted = item === nextItem)
            }
        }
    }
}

@Composable
private fun ScheduleRow(item: MinyanItem, highlighted: Boolean) {
    val countdown = if (highlighted) DayUtils.minutesUntil(item.time) else null
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlighted) 2.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrayerBadge(type = item.type)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (!item.room.isNullOrBlank()) {
                    Text(text = item.room!!, style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    text = DayUtils.daysLabel(item.days),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!item.comment.isNullOrBlank()) {
                    Text(
                        text = item.comment!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = item.time ?: "",
                    style = MaterialTheme.typography.titleLarge,
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
    }
}

private fun buildShareText(
    name: String,
    grouped: Map<String, List<MinyanItem>>,
    day: Char,
): String = buildString {
    appendLine("זמני תפילות - $name")
    appendLine("יום ${DayUtils.DAY_NAMES[day] ?: day}")
    appendLine()
    grouped.forEach { (type, list) ->
        appendLine(type)
        list.forEach { item ->
            append("  ${item.time}")
            if (!item.room.isNullOrBlank()) append(" | ${item.room}")
            if (!item.comment.isNullOrBlank()) append(" (${item.comment})")
            appendLine()
        }
    }
    appendLine()
    appendLine("מקור: נדרים פלוס")
}
