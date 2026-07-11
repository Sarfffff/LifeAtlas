package com.xiaoyin.lifeatlas.core.media

import android.content.Context
import android.net.Uri
import java.io.File

class ProfileAvatarStorage(context: Context) {
    private val appContext = context.applicationContext
    private val avatarDirectory = File(appContext.filesDir, "profile").apply { mkdirs() }
    private val avatarFile = File(avatarDirectory, "avatar_image")

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
        appContext.contentResolver.openInputStream(Uri.parse(sourceUri))?.use { input ->
            temporaryFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("无法读取所选头像")
        require(temporaryFile.length() > 0L) { "所选头像文件为空" }

        if (avatarFile.exists() && !avatarFile.delete()) error("无法替换旧头像")
        if (!temporaryFile.renameTo(avatarFile)) {
            temporaryFile.copyTo(avatarFile, overwrite = true)
            temporaryFile.delete()
        }
        return Uri.fromFile(avatarFile).toString()
    }

    private fun deleteOwnedAvatar(uri: String?) {
        if (uri == Uri.fromFile(avatarFile).toString()) {
            avatarFile.delete()
        }
    }
}
