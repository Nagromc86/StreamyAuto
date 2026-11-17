package com.streamyauto.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.streamyauto.update.UpdateInfo
import com.streamyauto.update.UpdateManager
import kotlinx.coroutines.launch

@Composable
fun UpdateSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var available by remember { mutableStateOf<UpdateInfo?>(null) }
    var progress by remember { mutableStateOf<Float?>(null) }

    fun check() {
        busy = true
        message = null
        available = null
        scope.launch {
            try {
                val info = UpdateManager.findUpdate()
                available = info
                isError = false
                message = if (info == null) "StreamyAuto est à jour ✓"
                else "Nouvelle version disponible : build ${info.build}"
            } catch (e: Exception) {
                isError = true
                message = "Vérification impossible : ${e.message ?: e.javaClass.simpleName}"
            } finally {
                busy = false
            }
        }
    }

    fun downloadAndInstall(info: UpdateInfo) {
        if (!UpdateManager.canInstall(context)) {
            isError = true
            message = "Autorise StreamyAuto à installer des applications, puis appuie à nouveau."
            UpdateManager.openInstallPermissionSettings(context)
            return
        }
        busy = true
        progress = 0f
        scope.launch {
            try {
                val apk = UpdateManager.download(context, info) { progress = it }
                isError = false
                message = "Téléchargement terminé, installation…"
                UpdateManager.install(context, apk)
            } catch (e: Exception) {
                isError = true
                message = "Téléchargement impossible : ${e.message ?: e.javaClass.simpleName}"
            } finally {
                busy = false
                progress = null
            }
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Mises à jour", style = MaterialTheme.typography.titleMedium)
            Text(
                "Version installée : build ${UpdateManager.currentBuild}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))

            val info = available
            if (info == null) {
                OutlinedButton(onClick = { check() }, enabled = !busy) {
                    Text(if (busy) "Recherche…" else "Rechercher une mise à jour")
                }
            } else {
                Button(onClick = { downloadAndInstall(info) }, enabled = !busy) {
                    Text("Installer le build ${info.build}")
                }
            }

            progress?.let {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth())
            }
            message?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    color = if (isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
