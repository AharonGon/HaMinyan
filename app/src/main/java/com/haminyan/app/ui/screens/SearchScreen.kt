package com.haminyan.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Synagogue
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.haminyan.app.data.model.MosadResult
import com.haminyan.app.ui.components.EmptyState
import com.haminyan.app.ui.vm.SearchViewModel

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onOpenMosad: (id: String, name: String) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val recents by viewModel.recentSearches.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "חיפוש בית כנסת",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("הקלידו שם בית כנסת או מוסד…") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "ניקוי")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
        )

        when {
            state.loading -> Column(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }

            state.error != null -> Text(
                text = state.error!!,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            )

            state.searched && state.results.isEmpty() -> EmptyState(
                icon = Icons.Outlined.SearchOff,
                title = "לא נמצאו תוצאות",
                subtitle = "נסו לחפש בשם אחר או בכתיב שונה",
            )

            state.results.isNotEmpty() -> ResultsList(
                results = state.results,
                onClick = { mosad ->
                    viewModel.onResultChosen()
                    onOpenMosad(mosad.mosadId, mosad.mosadName)
                },
            )

            else -> RecentSearches(
                recents = recents,
                onPick = viewModel::searchNow,
                onClear = viewModel::clearRecents,
            )
        }
    }
}

@Composable
private fun ResultsList(
    results: List<MosadResult>,
    onClick: (MosadResult) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(results, key = { it.mosadId }) { mosad ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(mosad) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Synagogue,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = mosad.mosadName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSearches(
    recents: List<String>,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (recents.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.Search,
            title = "חפשו בית כנסת",
            subtitle = "הקלידו לפחות 2 תווים כדי להתחיל בחיפוש",
        )
        return
    }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "חיפושים אחרונים",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClear) { Text("ניקוי") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(recents) { query ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(query) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(text = query, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
