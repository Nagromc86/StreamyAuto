package com.streamyauto.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun StreamyRoot(vm: MainViewModel = viewModel()) {
    val playing = vm.playing
    if (playing != null) {
        BackHandler { vm.stopPlaying() }
        PhonePlayer(item = playing, onClose = { vm.stopPlaying() })
        return
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Favoris", "Chaînes", "Comptes")

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            text = "StreamyAuto",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
            }
        }
        when (tab) {
            0 -> FavoritesTab(vm, onBrowse = { tab = 1 })
            1 -> ChannelsTab(vm, onAddAccount = { tab = 2 })
            else -> AccountsTab(vm)
        }
    }
}
