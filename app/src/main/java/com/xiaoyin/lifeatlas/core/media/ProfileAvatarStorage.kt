package com.xiaoyin.lifeatlas.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.File
import kotlin.math.max

class ProfileAvatarStorage(context: Context) {
    private val appContext = context.applicationContext
    private val avatarDirectory = File(appContext.filesDir, "profile").apply { mkdirs() }
    private val avatarFile = File(avatarDirectory, "avatar_image.jpg")

    fun importAvatar(sourceUri: String?, currentAvatarUri: String?): String? {
        if (sourceUri.isNullOrBlank()) {
            deleteOwnedAvatar(currentAvatarUri)
            return null
        }
        if (sourceUri == currentAvatarUri || sourceUri == Uri.fromFile(avatarFile).toString()) {
            return sourceUri
        }

        val temporaryFile = File(avatarDirectory, "avatar_image.tmp")
        runCatching { temporaryFile.delete() }
        val bitmap = appContext.contentResolver.openInputStream(Uri.parse(sourceUri))?.use(BitmapFactory::decodeStream)
            ?: error("无法读取所选头像")
        val scaledBitmap = bitmap.scaledToFit(maxAvatarDimension)
        temporaryFile.outputStream().use { output ->
            require(scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 84, output)) { "头像压缩失败" }
        }
        if (scaledBitmap !== bitmap) scaledBitmap.recycle()
        bitmap.recycle()
        require(temporaryFile.length() > 0L) { "所选头像文件为空" }

        if (avatarFile.exists() && !avatarFile.delete()) error("无法替换旧头像")
        if (!temporaryFile.renameTo(avatarFile)) {
            temporaryFile.copyTo(avatarFile, overwrite = true)
            temporaryFile.delete()
        }
        return Uri.fromFile(avatarFile).toString()
    }

    fun readAvatarBase64(avatarUri: String?): String? {
        if (avatarUri != Uri.fromFile(avatarFile).toString() || !avatarFile.isFile) return null
        return Base64.encodeToString(avatarFile.readBytes(), Base64.NO_WRAP)
    }

    fun importRemoteAvatar(avatarBase64: String?, currentAvatarUri: String?): String? {
        if (avatarBase64.isNullOrBlank()) return currentAvatarUri
        val bytes = Base64.decode(avatarBase64, Base64.DEFAULT)
        require(bytes.isNotEmpty() && bytes.size <= maxAvatarBytes) { "云端头像数据无效" }
        avatarDirectory.mkdirs()
        avatarFile.writeBytes(bytes)
        return Uri.fromFile(avatarFile).toString()
    }

    private fun deleteOwnedAvatar(uri: String?) {
        if (uri == Uri.fromFile(avatarFile).toString()) {
            avatarFile.delete()
        }
    }

    private fun Bitmap.scaledToFit(maxDimension: Int): Bitmap {
        val longestSide = max(width, height)
        if (longestSide <= maxDimension) return this
        val scale = maxDimension.toFloat() / longestSide
        return Bitmap.createScaledBitmap(
            this,
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    private companion object {
        const val maxAvatarDimension = 512
        const val maxAvatarBytes = 375_000
    }
}
