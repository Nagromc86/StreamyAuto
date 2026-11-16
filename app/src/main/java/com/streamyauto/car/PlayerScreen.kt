package com.streamyauto.car

import android.content.ComponentName
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.streamyauto.R
import androidx.car.app.navigation.model.MessageInfo
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.streamyauto.data.model.Favorite
import com.streamyauto.playback.PlayerHolder
import com.streamyauto.playback.StreamyPlaybackService

/**
 * Écran de lecture Android Auto.
 *
 * La vidéo n'est attachée à la surface que si [ParkedGate] confirme que la
 * voiture est à l'arrêt. Sinon la lecture continue en audio seul et la surface
 * affiche le nom de la chaîne.
 */
class PlayerScreen(
    carContext: CarContext,
    private val favorites: List<Favorite>,
    startIndex: Int
) : Screen(carContext) {

    private var index = startIndex.coerceIn(0, (favorites.size - 1).coerceAtLeast(0))
    private val favorite: Favorite get() = favorites[index]

    private val player get() = PlayerHolder.get(carContext)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentSurface: Surface? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var videoAttached = false
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val gate = ParkedGate(carContext) { updateVideoBinding(); invalidate() }

    private val surfaceCallback = object : SurfaceCallback {
        override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
            currentSurface = surfaceContainer.surface
            surfaceWidth = surfaceContainer.width
            surfaceHeight = surfaceContainer.height
            videoAttached = false
            updateVideoBinding()
        }

        override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
            detachVideo()
            currentSurface = null
        }

        override fun onVisibleAreaChanged(visibleArea: Rect) {}
        override fun onStableAreaChanged(stableArea: Rect) {}
    }

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                // Connecte le service Media3 (notification, commandes volant)
                val token = SessionToken(
                    carContext,
                    ComponentName(carContext, StreamyPlaybackService::class.java)
                )
                controllerFuture = MediaController.Builder(carContext, token).buildAsync()
            }

            override fun onStart(owner: LifecycleOwner) {
                carContext.getCarService(AppManager::class.java)
                    .setSurfaceCallback(surfaceCallback)

                playCurrent()
                gate.start()
            }

            override fun onStop(owner: LifecycleOwner) {
                gate.stop()
                detachVideo()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                controllerFuture?.let { MediaController.releaseFuture(it) }
                controllerFuture = null
            }
        })
    }

    private fun playCurrent() {
        PlayerHolder.playUrl(carContext, favorite.streamUrl, favorite.name, favorite.icon)
    }

    private fun skip(delta: Int) {
        if (favorites.size < 2) return
        index = (index + delta + favorites.size) % favorites.size
        playCurrent()
        if (!videoAttached) mainHandler.postDelayed({ drawAudioOnlyScreen() }, 300)
        invalidate()
    }

    private fun icon(resId: Int): CarIcon =
        CarIcon.Builder(IconCompat.createWithResource(carContext, resId)).build()

    private fun updateVideoBinding() {
        val surface = currentSurface ?: return
        if (gate.state == ParkedGate.State.PARKED) {
            if (!videoAttached) {
                player.setVideoSurface(surface)
                videoAttached = true
            }
        } else {
            detachVideo()
            // Laisse le décodeur libérer la surface avant de dessiner dessus
            mainHandler.postDelayed({ drawAudioOnlyScreen() }, 300)
        }
    }

    private fun detachVideo() {
        if (videoAttached) {
            player.clearVideoSurface()
            videoAttached = false
        }
    }

    private fun drawAudioOnlyScreen() {
        val surface = currentSurface ?: return
        if (videoAttached || !surface.isValid) return
        try {
            val canvas = surface.lockCanvas(null)
            try {
                canvas.drawColor(Color.rgb(20, 24, 32))
                val w = if (surfaceWidth > 0) surfaceWidth else canvas.width
                val h = if (surfaceHeight > 0) surfaceHeight else canvas.height
                val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = h / 10f
                    textAlign = Paint.Align.CENTER
                }
                val sub = Paint(title).apply {
                    color = Color.LTGRAY
                    textSize = h / 22f
                }
                canvas.drawText(favorite.name, w / 2f, h / 2f, title)
                canvas.drawText(statusText(), w / 2f, h / 2f + h / 10f, sub)
            } finally {
                surface.unlockCanvasAndPost(canvas)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Dessin de l'écran audio impossible", e)
        }
    }

    private fun statusText(): String = when (gate.state) {
        ParkedGate.State.PARKED -> "Vidéo"
        ParkedGate.State.MOVING -> "Audio seul – vidéo disponible à l'arrêt"
        ParkedGate.State.UNKNOWN -> "Audio seul – vitesse du véhicule indisponible"
    }

    override fun onGetTemplate(): Template {
        // Barre de navigation : 4 actions max, une seule avec texte → icônes
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder().setIcon(icon(R.drawable.ic_car_previous))
                    .setOnClickListener { skip(-1) }.build()
            )
            .addAction(
                Action.Builder().setIcon(icon(R.drawable.ic_car_play_pause))
                    .setOnClickListener {
                        if (player.isPlaying) player.pause() else player.play()
                    }.build()
            )
            .addAction(
                Action.Builder().setIcon(icon(R.drawable.ic_car_next))
                    .setOnClickListener { skip(+1) }.build()
            )
            .addAction(
                Action.Builder().setIcon(icon(R.drawable.ic_car_close))
                    .setOnClickListener {
                        player.stop()
                        screenManager.pop()
                    }.build()
            )
            .build()

        val builder = NavigationTemplate.Builder().setActionStrip(actionStrip)
        // En mode vidéo, pas de bandeau : la vidéo occupe l'écran
        if (gate.state != ParkedGate.State.PARKED) {
            builder.setNavigationInfo(
                MessageInfo.Builder(favorite.name).setText(statusText()).build()
            )
        }
        return builder.build()
    }

    companion object {
        private const val TAG = "PlayerScreen"
    }
}
