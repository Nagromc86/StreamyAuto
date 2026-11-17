package com.streamyauto.artwork

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.streamyauto.StreamyApp
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileNotFoundException

/**
 * Sert le logo d'un favori à Android Auto en mode audio.
 *
 * Android Auto ne télécharge pas les images web des éléments média : il lit
 * des URI content://. L'URI ne contient que l'identifiant du favori
 * (content://<app>.artwork/fav/<id>) ; l'adresse du logo est relue en base,
 * donc ce fournisseur ne peut servir que les logos des favoris.
 */
class ArtworkProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = "image/png"

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw SecurityException("Lecture seule")
        val id = uri.pathSegments.takeIf { it.size == 2 && it[0] == "fav" }
            ?.get(1)?.toLongOrNull()
            ?: throw FileNotFoundException(uri.toString())
        val app = context?.applicationContext as? StreamyApp
            ?: throw FileNotFoundException(uri.toString())

        val file = runBlocking {
            val url = app.repository.favoriteById(id)?.icon?.takeIf { it.isNotBlank() }
                ?: return@runBlocking null
            val cached = cacheFile(app, url)
            if (cached.exists()) return@runBlocking cached
            val bitmap = LogoLoader.load(url) ?: return@runBlocking null
            cached.parentFile?.mkdirs()
            val tmp = File(cached.parentFile, cached.name + ".tmp")
            tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            tmp.renameTo(cached)
            cached
        } ?: throw FileNotFoundException(uri.toString())

        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private fun cacheFile(context: Context, url: String) =
        File(File(context.cacheDir, "artwork"), "${url.hashCode().toUInt()}.png")

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?
    ) = 0

    companion object {
        fun favoriteUri(context: Context, favoriteId: Long): Uri =
            Uri.parse("content://${context.packageName}.artwork/fav/$favoriteId")
    }
}
