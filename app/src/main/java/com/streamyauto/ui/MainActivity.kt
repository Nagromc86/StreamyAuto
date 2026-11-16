package com.streamyauto.ui

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.streamyauto.playback.StreamyPlaybackService

class MainActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotificationPermission()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StreamyRoot()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Connexion au service Media3 : notification et lecture en arrière-plan
        val token = SessionToken(this, ComponentName(this, StreamyPlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
    }

    override fun onStop() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        super.onStop()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
