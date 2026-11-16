package com.streamyauto.car

import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.Speed
import androidx.core.content.ContextCompat
import kotlin.math.abs

/**
 * Barrière d'arrêt : autorise la vidéo uniquement quand la voiture transmet
 * une vitesse nulle depuis [STABLE_MS] millisecondes.
 *
 * Fail-safe : si la vitesse est inconnue (voiture qui ne la transmet pas,
 * permission refusée, erreur), l'état reste [State.UNKNOWN] et la vidéo
 * reste désactivée. Seul le son est joué.
 */
class ParkedGate(
    private val carContext: CarContext,
    private val onStateChanged: (State) -> Unit
) {

    enum class State { UNKNOWN, MOVING, PARKED }

    var state: State = State.UNKNOWN
        private set

    private val handler = Handler(Looper.getMainLooper())
    private var stoppedSince: Long = 0L
    private var registered = false
    private var permissionRequested = false

    private val confirmParked = Runnable {
        if (stoppedSince != 0L && SystemClock.elapsedRealtime() - stoppedSince >= STABLE_MS) {
            setState(State.PARKED)
        }
    }

    private val speedListener = OnCarDataAvailableListener<Speed> { speed ->
        val value = readSpeed(speed)
        when {
            value == null -> {
                stoppedSince = 0L
                handler.removeCallbacks(confirmParked)
                setState(State.UNKNOWN)
            }
            abs(value) < STOPPED_THRESHOLD_MS -> {
                if (stoppedSince == 0L) {
                    stoppedSince = SystemClock.elapsedRealtime()
                    handler.removeCallbacks(confirmParked)
                    handler.postDelayed(confirmParked, STABLE_MS)
                }
            }
            else -> {
                // La voiture bouge : coupure vidéo immédiate
                stoppedSince = 0L
                handler.removeCallbacks(confirmParked)
                setState(State.MOVING)
            }
        }
    }

    fun start() {
        if (registered) return
        if (!hasSpeedPermission() && !permissionRequested) {
            permissionRequested = true
            try {
                carContext.requestPermissions(SPEED_PERMISSIONS) { granted, _ ->
                    if (granted.isNotEmpty()) register()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Demande de permission vitesse impossible", e)
            }
        }
        register()
    }

    fun stop() {
        handler.removeCallbacks(confirmParked)
        stoppedSince = 0L
        if (registered) {
            try {
                carInfo()?.removeSpeedListener(speedListener)
            } catch (e: Exception) {
                Log.w(TAG, "removeSpeedListener", e)
            }
            registered = false
        }
        setState(State.UNKNOWN)
    }

    private fun register() {
        if (registered) return
        try {
            carInfo()?.addSpeedListener(ContextCompat.getMainExecutor(carContext), speedListener)
            registered = true
        } catch (e: Exception) {
            // SecurityException, API non supportée… → vidéo désactivée (fail-safe)
            Log.w(TAG, "Vitesse indisponible : vidéo désactivée", e)
            setState(State.UNKNOWN)
        }
    }

    private fun carInfo() =
        carContext.getCarService(CarHardwareManager::class.java).carInfo

    private fun hasSpeedPermission(): Boolean = SPEED_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(carContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun readSpeed(speed: Speed): Float? {
        val raw = speed.rawSpeedMetersPerSecond
        if (raw.status == CarValue.STATUS_SUCCESS && raw.value != null) return raw.value
        val display = speed.displaySpeedMetersPerSecond
        if (display.status == CarValue.STATUS_SUCCESS && display.value != null) return display.value
        return null
    }

    private fun setState(newState: State) {
        if (newState == state) return
        state = newState
        onStateChanged(newState)
    }

    companion object {
        private const val TAG = "ParkedGate"
        private const val STABLE_MS = 3_000L
        private const val STOPPED_THRESHOLD_MS = 0.1f // m/s
        private val SPEED_PERMISSIONS = listOf(
            "com.google.android.gms.permission.CAR_SPEED",
            "android.car.permission.CAR_SPEED"
        )
    }
}
