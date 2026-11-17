package com.streamyauto.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.google.gson.JsonParser
import com.streamyauto.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

data class UpdateInfo(val build: Int, val apkUrl: String)

/**
 * Mises à jour depuis les Releases GitHub du dépôt (tags « build-N »).
 * N est le numéro de build GitHub Actions, utilisé aussi comme versionCode.
 */
object UpdateManager {

    private const val RELEASES_URL =
        "https://api.github.com/repos/Nagromc86/StreamyAuto/releases?per_page=30"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val currentBuild: Int get() = BuildConfig.VERSION_CODE

    /** Dernière Release plus récente que la version installée, ou null. */
    suspend fun findUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(RELEASES_URL)
            .header("Accept", "application/vnd.github+json")
            .build()
        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("GitHub a répondu ${response.code}")
            response.body?.string() ?: error("Réponse vide")
        }
        JsonParser.parseString(body).asJsonArray
            .mapNotNull { element ->
                val release = element.asJsonObject
                val build = release.get("tag_name")?.asString
                    ?.removePrefix("build-")?.toIntOrNull() ?: return@mapNotNull null
                val apk = release.getAsJsonArray("assets")
                    ?.map { it.asJsonObject }
                    ?.firstOrNull { it.get("name")?.asString?.endsWith(".apk") == true }
                    ?.get("browser_download_url")?.asString ?: return@mapNotNull null
                UpdateInfo(build, apk)
            }
            .maxByOrNull { it.build }
            ?.takeIf { it.build > currentBuild }
    }

    /** Télécharge l'APK dans le cache, avec la progression de 0 à 1. */
    suspend fun download(
        context: Context,
        info: UpdateInfo,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "StreamyAuto-${info.build}.apk")
        client.newCall(Request.Builder().url(info.apkUrl).build()).execute().use { response ->
            if (!response.isSuccessful) error("Téléchargement refusé (${response.code})")
            val body = response.body ?: error("Fichier vide")
            val total = body.contentLength()
            body.byteStream().use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) {
                            val p = done.toFloat() / total
                            withContext(Dispatchers.Main) { onProgress(p) }
                        }
                    }
                }
            }
        }
        file
    }

    /** Android 8+ : l'utilisateur doit autoriser StreamyAuto à installer des apps. */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(context: Context) {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Ouvre l'installateur d'Android sur l'APK téléchargée. */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
