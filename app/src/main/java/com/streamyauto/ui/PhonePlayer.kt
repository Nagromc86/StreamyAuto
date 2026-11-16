package com.streamyauto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.streamyauto.playback.PlayerHolder

@Composable
fun PhonePlayer(item: PlayItem, onClose: () -> Unit) {
    val context = LocalContext.current
    val player = remember { PlayerHolder.get(context) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(item.url) {
        error = null
        PlayerHolder.playUrl(context, item.url, item.name, item.icon)
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(e: PlaybackException) {
                error = "Lecture impossible : ${e.errorCodeName}"
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                player.stop()
                onClose()
            }) { Text("← Fermer", color = Color.White) }
            Text(
                item.name,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { ctx -> PlayerView(ctx).apply { this.player = player } },
                onRelease = { view -> view.player = null },
                modifier = Modifier.fillMaxSize()
            )
            error?.let {
                Text(
                    it + "\nEssaie l'autre format de flux dans l'onglet Comptes.",
                    color = Color.White,
                    modifier = Modifier.background(Color(0xAA000000)).padding(16.dp)
                )
            }
        }
    }
}
