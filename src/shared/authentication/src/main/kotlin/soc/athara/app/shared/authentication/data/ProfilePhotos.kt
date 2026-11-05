package soc.athara.app.shared.authentication.data

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
import soc.athara.app.shared.designsystem.ImageDecoding
import kotlin.math.max





internal class ProfilePhotoStore(private val context: Context) {
    private val dir get() = File(context.filesDir, "profile_photos")

    enum class Kind(val suffix: String, val maxSide: Int) {
        PROFILE("", 1024),
        COVER("_cover", 1600),
    }

    
    fun save(userId: String, uri: String, kind: Kind = Kind.PROFILE): String? = runCatching {
        val bitmap = ImageDecoding.decode(context.contentResolver, Uri.parse(uri), kind.maxSide) ?: return@runCatching null
        dir.mkdirs()
        val file = File(dir, "$userId${kind.suffix}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        file.absolutePath
    }.getOrNull()

    
    fun existingPath(userId: String, kind: Kind): String? =
        File(dir, "$userId${kind.suffix}.jpg").takeIf { it.isFile }?.absolutePath

    
    fun download(userId: String, url: String, kind: Kind, http: OkHttpClient): String? = runCatching {
        http.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return@use null
            dir.mkdirs()
            val file = File(dir, "$userId${kind.suffix}.jpg")
            response.body.byteStream().use { input -> file.outputStream().use { input.copyTo(it) } }
            file.absolutePath
        }
    }.getOrNull()

    
    fun deleteAll() {
        dir.deleteRecursively()
    }

    
    fun delete(userId: String, kind: Kind? = null) {
        (kind?.let { listOf(it) } ?: Kind.values().toList()).forEach { File(dir, "$userId${it.suffix}.jpg").delete() }
    }
}