package org.example.authentication.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Request
import org.example.designsystem.ImageDecoding
import kotlin.math.max

/**
 * Keeps profile pictures in the app's private storage. A picked or photographed image only comes
 * with a temporary content URI, so it is copied (downsampled JPEG) to `files/profile_photos/<userId>.jpg`.
 */
internal class ProfilePhotoStore(private val context: Context) {
    private val dir get() = File(context.filesDir, "profile_photos")

    enum class Kind(val suffix: String, val maxSide: Int) {
        PROFILE("", 1024),
        COVER("_cover", 1600),
    }

    /** Saves the image at [uri] for [userId] and returns the file path, or null if it could not be read or written. */
    fun save(userId: String, uri: String, kind: Kind = Kind.PROFILE): String? = runCatching {
        val bitmap = ImageDecoding.decode(context.contentResolver, Uri.parse(uri), kind.maxSide) ?: return@runCatching null
        dir.mkdirs()
        val file = File(dir, "$userId${kind.suffix}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        file.absolutePath
    }.getOrNull()

    /** Path of the cached picture if the file exists. */
    fun existingPath(userId: String, kind: Kind): String? =
        File(dir, "$userId${kind.suffix}.jpg").takeIf { it.isFile }?.absolutePath

    /** Downloads the server copy at [url] into the cache. Blocking: call from Dispatchers.IO. Null on any failure. */
    fun download(userId: String, url: String, kind: Kind, http: OkHttpClient): String? = runCatching {
        http.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return@use null
            dir.mkdirs()
            val file = File(dir, "$userId${kind.suffix}.jpg")
            response.body.byteStream().use { input -> file.outputStream().use { input.copyTo(it) } }
            file.absolutePath
        }
    }.getOrNull()

    /** Removes everything this store ever wrote (legacy local-account cleanup). */
    fun deleteAll() {
        dir.deleteRecursively()
    }

    /** Deletes [userId]'s picture of the given [kind], or all of them when [kind] is null. */
    fun delete(userId: String, kind: Kind? = null) {
        (kind?.let { listOf(it) } ?: Kind.values().toList()).forEach { File(dir, "$userId${it.suffix}.jpg").delete() }
    }
}
