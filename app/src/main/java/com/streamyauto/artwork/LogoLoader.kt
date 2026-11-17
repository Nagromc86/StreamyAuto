package com.streamyauto.artwork

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Télécharge les logos des chaînes pour les tuiles Android Auto
 * (lecteur vidéo et mode audio).
 *
 * Chaque logo est centré dans un carré transparent (les logos sont souvent
 * larges) pour que l'hôte Android Auto ne le rogne pas, puis gardé en mémoire.
 */
object LogoLoader {

    private const val SIZE = 256
    private const val PADDING = 0.12f

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val cache = LruCache<String, Bitmap>(48)
    private val failed = mutableSetOf<String>()

    fun cached(url: String): Bitmap? = cache.get(url)

    fun hasFailed(url: String): Boolean = synchronized(failed) { url in failed }

    suspend fun load(url: String): Bitmap? {
        cache.get(url)?.let { return it }
        if (hasFailed(url)) return null
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val bytes = resp.body?.bytes() ?: return@use null
                    decodeSampled(bytes)?.let { toSquare(it) }
                }
            }.getOrNull()
        }
        if (bitmap != null) cache.put(url, bitmap) else synchronized(failed) { failed += url }
        return bitmap
    }

    private fun decodeSampled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= SIZE && bounds.outHeight / (sample * 2) >= SIZE) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private fun toSquare(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val inner = SIZE * (1 - 2 * PADDING)
        val scale = minOf(inner / src.width, inner / src.height)
        val w = src.width * scale
        val h = src.height * scale
        val left = (SIZE - w) / 2
        val top = (SIZE - h) / 2
        Canvas(out).drawBitmap(
            src, null, RectF(left, top, left + w, top + h),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        if (src != out) src.recycle()
        return out
    }
}
