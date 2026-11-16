package com.streamyauto.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Lecteur unique du processus, partagé par le service Media3, le téléphone et
 * l'écran Android Auto. Évite de dépendre du démarrage du service
 * (dans le document d'origine, StreamyPlaybackService.instance pouvait être null
 * quand Android Auto ouvrait l'écran de lecture).
 */
object PlayerHolder {

    @Volatile
    private var player: ExoPlayer? = null

    fun get(context: Context): ExoPlayer =
        player ?: synchronized(this) {
            player ?: ExoPlayer.Builder(context.applicationContext).build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }.also { player = it }
        }

    fun playUrl(context: Context, url: String, title: String, iconUrl: String? = null) {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist("StreamyAuto")
            .apply { if (!iconUrl.isNullOrBlank()) setArtworkUri(Uri.parse(iconUrl)) }
            .build()
        val item = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(metadata)
            .build()
        get(context).apply {
            setMediaItem(item)
            prepare()
            play()
        }
    }
}
