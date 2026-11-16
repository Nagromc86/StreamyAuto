package com.streamyauto.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.streamyauto.data.model.Account
import kotlinx.coroutines.launch

@Composable
fun AccountsTab(vm: MainViewModel) {
    val accounts by vm.accounts.collectAsState()
    var toDelete by remember { mutableStateOf<Account?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (accounts.isNotEmpty()) {
            Text("Comptes", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            accounts.forEach { account ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(account.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${account.username} · ${account.server} · .${account.streamFormat}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = { toDelete = account }) { Text("Supprimer") }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        AccountForm(vm)
    }

    toDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Supprimer « ${account.name} » ?") },
            text = { Text("Ses favoris seront aussi supprimés.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteAccount(account)
                    toDelete = null
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun AccountForm(vm: MainViewModel) {
    var name by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var format by remember { mutableStateOf("m3u8") }
    var status by remember { mutableStateOf<String?>(null) }
    var statusOk by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val valid = server.isNotBlank() && username.isNotBlank() && password.isNotBlank()
    fun current() = Account(
        name = name.ifBlank { server.substringAfter("://").substringBefore(":").substringBefore("/") },
        server = server.trim(),
        username = username.trim(),
        password = password,
        streamFormat = format
    )

    Text("Ajouter un compte Xtream Codes", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = name, onValueChange = { name = it },
        label = { Text("Nom (facultatif)") }, singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = server, onValueChange = { server = it },
        label = { Text("Serveur") }, placeholder = { Text("http://exemple.com:8080") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = username, onValueChange = { username = it },
        label = { Text("Nom d'utilisateur") }, singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = password, onValueChange = { password = it },
        label = { Text("Mot de passe") }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    Text("Format des flux", style = MaterialTheme.typography.bodyMedium)
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = format == "m3u8", onClick = { format = "m3u8" })
        Text("HLS (.m3u8)")
        Spacer(Modifier.width(16.dp))
        RadioButton(selected = format == "ts", onClick = { format = "ts" })
        Text("MPEG-TS (.ts)")
    }
    Text(
        "Si les chaînes ne se lancent pas, essaie l'autre format.",
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(12.dp))

    Row {
        OutlinedButton(
            enabled = valid && !busy,
            onClick = {
                busy = true
                status = null
                scope.launch {
                    val err = vm.checkAccount(current())
                    statusOk = err == null
                    status = err ?: "Connexion réussie ✓"
                    busy = false
                }
            }
        ) { Text("Tester") }
        Spacer(Modifier.width(12.dp))
        Button(
            enabled = valid && !busy,
            onClick = {
                vm.saveAccount(current()) {
                    name = ""; server = ""; username = ""; password = ""
                    statusOk = true
                    status = "Compte enregistré ✓"
                }
            }
        ) { Text("Enregistrer") }
    }
    status?.let {
        Spacer(Modifier.height(8.dp))
        Text(
            it,
            color = if (statusOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}
