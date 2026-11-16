package com.streamyauto.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

const val CAR_FAVORITES_LIMIT = 12

@Composable
fun FavoritesTab(vm: MainViewModel, onBrowse: () -> Unit) {
    val favorites by vm.favorites.collectAsState()

    if (favorites.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Aucun favori pour l'instant.", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Ajoute des chaînes avec ☆ dans l'onglet Chaînes.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBrowse) { Text("Parcourir les chaînes") }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            text = "Les $CAR_FAVORITES_LIMIT premiers s'affichent dans Android Auto, dans cet ordre.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(favorites, key = { _, f -> f.id }) { index, fav ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.play(PlayItem(fav.name, fav.streamUrl, fav.icon)) }
                        .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (index < CAR_FAVORITES_LIMIT) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = fav.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { vm.moveFavorite(fav, -1) }, enabled = index > 0) { Text("▲") }
                    TextButton(
                        onClick = { vm.moveFavorite(fav, +1) },
                        enabled = index < favorites.lastIndex
                    ) { Text("▼") }
                    TextButton(onClick = { vm.removeFavorite(fav) }) { Text("✕") }
                }
                HorizontalDivider()
            }
        }
    }
}
