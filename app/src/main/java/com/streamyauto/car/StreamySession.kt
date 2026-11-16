package com.streamyauto.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class StreamySession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = FavoritesScreen(carContext)
}
