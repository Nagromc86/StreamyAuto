package com.streamyauto.playback

import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.streamyauto.StreamyApp
import com.streamyauto.artwork.ArtworkProvider
import com.streamyauto.data.model.Favorite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Service Media3 partagé :
 * - notification et lecture en arrière-plan sur le téléphone ;
 * - mode audio Android Auto (app média) : un onglet « Favoris » avec les
 *   12 premiers favoris en grille avec le logo de la chaîne, précédent/suivant
 *   entre favoris, commandes au volant.
 */
class StreamyPlaybackService : MediaLibraryService() {

    private var mediaSession: MediaLibrarySession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val repo get() = (application as StreamyApp).repository

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaLibrarySession.Builder(
            this,
            PlayerHolder.get(this),
            LibrarySessionCallback()
        ).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaSession

    override fun onDestroy() {
        scope.cancel()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    /** Exécute [block] dans une coroutine et expose le résultat en ListenableFuture. */
    private fun <T> future(block: suspend () -> T): ListenableFuture<T> {
        val result = SettableFuture.create<T>()
        scope.launch {
            try {
                result.set(block())
            } catch (e: Exception) {
                result.setException(e)
            }
        }
        return result
    }

    private suspend fun favoriteItems(): List<MediaItem> = repo.topFavorites().map(::toMediaItem)

    private fun toMediaItem(fav: Favorite): MediaItem =
        MediaItem.Builder()
            .setMediaId("$FAV_PREFIX${fav.id}")
            .setUri(fav.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(fav.name)
                    .setArtist("StreamyAuto")
                    .apply {
                        if (!fav.icon.isNullOrBlank()) {
                            setArtworkUri(ArtworkProvider.favoriteUri(this@StreamyPlaybackService, fav.id))
                        }
                    }
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()

    private fun folder(id: String, title: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setExtras(gridStyle())
                    .build()
            )
            .build()

    /** Demande à Android Auto d'afficher les éléments en tuiles plutôt qu'en liste. */
    private fun gridStyle() = Bundle().apply {
        putInt(
            MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
        )
        putInt(
            MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
        )
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> =
            SettableFuture.create<LibraryResult<MediaItem>>().apply {
                val rootParams = LibraryParams.Builder()
                    .setExtras(Bundle(params?.extras ?: Bundle()).apply { putAll(gridStyle()) })
                    .setOffline(params?.isOffline ?: false)
                    .setRecent(params?.isRecent ?: false)
                    .setSuggested(params?.isSuggested ?: false)
                    .build()
                set(LibraryResult.ofItem(folder(ROOT_ID, "StreamyAuto"), rootParams))
            }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = future {
            val children = when (parentId) {
                ROOT_ID -> listOf(folder(FAVORITES_ID, "Favoris"))
                FAVORITES_ID -> favoriteItems()
                else -> emptyList()
            }
            LibraryResult.ofItemList(ImmutableList.copyOf(children), params)
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> = future {
            val item = favoriteItems().firstOrNull { it.mediaId == mediaId }
            if (item != null) LibraryResult.ofItem(item, null)
            else LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
        }

        /** Android Auto n'envoie que l'identifiant : on retrouve l'URL du flux. */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> = future {
            val all = favoriteItems()
            mediaItems.mapNotNull { req ->
                all.firstOrNull { it.mediaId == req.mediaId }
                    ?: req.takeIf { it.localConfiguration != null }
            }.toMutableList()
        }

        /** Un favori choisi dans la voiture → toute la liste, pour précédent/suivant. */
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = future {
            val all = favoriteItems()
            val index = if (mediaItems.size == 1) {
                all.indexOfFirst { it.mediaId == mediaItems[0].mediaId }
            } else -1

            if (index >= 0) {
                MediaSession.MediaItemsWithStartPosition(all, index, C.TIME_UNSET)
            } else {
                val resolved = mediaItems.mapNotNull { req ->
                    all.firstOrNull { it.mediaId == req.mediaId }
                        ?: req.takeIf { it.localConfiguration != null }
                }
                MediaSession.MediaItemsWithStartPosition(
                    resolved,
                    startIndex.coerceIn(0, (resolved.size - 1).coerceAtLeast(0)),
                    startPositionMs
                )
            }
        }
    }

    companion object {
        private const val ROOT_ID = "root"
        private const val FAVORITES_ID = "favorites"
        private const val FAV_PREFIX = "fav:"
    }
}
