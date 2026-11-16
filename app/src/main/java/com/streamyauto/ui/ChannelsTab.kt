package com.streamyauto.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ChannelsTab(vm: MainViewModel, onAddAccount: () -> Unit) {
    val accounts by vm.accounts.collectAsState()
    val favorites by vm.favorites.collectAsState()

    if (accounts.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Aucun compte Xtream configuré.", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAddAccount) { Text("Ajouter un compte") }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        // Sélecteur de compte (utile en multi-comptes)
        if (accounts.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp)
            ) {
                accounts.forEach { account ->
                    val selected = vm.selectedAccount?.id == account.id
                    if (selected) {
                        Button(onClick = {}, modifier = Modifier.padding(4.dp)) { Text(account.name) }
                    } else {
                        OutlinedButton(
                            onClick = { vm.selectAccount(account) },
                            modifier = Modifier.padding(4.dp)
                        ) { Text(account.name) }
                    }
                }
            }
        }

        val category = vm.openedCategory
        if (category == null) {
            CategoryList(vm)
        } else {
            BackHandler { vm.closeCategory() }
            ChannelList(vm, favorites = favorites)
        }
    }
}

@Composable
private fun LoadingOrError(vm: MainViewModel, content: @Composable () -> Unit) {
    val error = vm.error
    when {
        vm.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        error != null -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(error, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { vm.retry() }) { Text("Réessayer") }
        }
        else -> content()
    }
}

@Composable
private fun CategoryList(vm: MainViewModel) {
    LoadingOrError(vm) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(vm.categories, key = { it.category_id ?: "" }) { cat ->
                Text(
                    text = cat.category_name ?: "Sans nom",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.openCategory(cat) }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ChannelList(vm: MainViewModel, favorites: List<com.streamyauto.data.model.Favorite>) {
    var query by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.closeCategory() }) { Text("← Catégories") }
            Text(
                text = vm.openedCategory?.category_name ?: "",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Rechercher") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        LoadingOrError(vm) {
            val filtered = if (query.isBlank()) vm.channels
            else vm.channels.filter { it.name.contains(query.trim(), ignoreCase = true) }

            LazyColumn(Modifier.fillMaxSize()) {
                items(filtered, key = { "${it.accountId}:${it.streamId}" }) { ch ->
                    val fav = vm.isFavorite(ch, favorites)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.play(PlayItem(ch.name, ch.streamUrl, ch.icon)) }
                            .padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ch.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { vm.toggleFavorite(ch) }) {
                            Text(
                                if (fav) "★" else "☆",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (fav) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
