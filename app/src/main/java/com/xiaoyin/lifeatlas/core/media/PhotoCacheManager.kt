package com.xiaoyin.lifeatlas.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.InputStream

class PhotoCacheManager(context: Context) {
    private val appContext = context.applicationContext
    private val thumbnailDir = File(appContext.filesDir, "photo_thumbnails").apply {
        mkdirs()
    }
    private val compressedDir = File(appContext.filesDir, "photo_compressed").apply {
        mkdirs()
    }

    fun createThumbnail(originalUri: String): String? {
        return runCatching {
            val sourceUri = Uri.parse(originalUri)
            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            }

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = bounds.calculateSampleSize(maxDimension = 1200)
            }
            val bitmap = appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return@runCatching null

            val scaledBitmap = bitmap.scaleToMaxDimension(maxDimension = 720)
            val outputFile = File(thumbnailDir, "${System.currentTimeMillis()}_${sourceUri.hashCode()}.jpg")
            outputFile.outputStream().use { output ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 82, output)
            }
            if (scaledBitmap !== bitmap) {
                bitmap.recycle()
                scaledBitmap.recycle()
            }
            Uri.fromFile(outputFile).toString()
        }.getOrNull()
    }

    fun deleteCachedPhoto(path: String?) {
        if (path.isNullOrBlank()) return

        runCatching {
            val candidate = path.toFile()
            val allowedRoots = listOf(thumbnailDir.canonicalFile, compressedDir.canonicalFile)
            val target = candidate.canonicalFile
            if (allowedRoots.any { target.startsWith(it) } && target.isFile) {
                target.delete()
            }
        }
    }

    fun restoreBackupMedia(kind: String, entryName: String, inputStream: InputStream): String? {
        return runCatching {
            val targetDir = when (kind) {
                "thumbnail" -> thumbnailDir
                "compressed" -> compressedDir
                else -> return@runCatching null
            }
            val fileName = entryName.substringAfterLast('/').sanitizeFileName()
            if (fileName.isBlank()) return@runCatching null

            val outputFile = File(targetDir, "${System.currentTimeMillis()}_$fileName")
            outputFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            Uri.fromFile(outputFile).toString()
        }.getOrNull()
    }

    private fun String.toFile(): File {
        return if (startsWith("file:")) {
            File(requireNotNull(Uri.parse(this).path))
        } else {
            File(this)
        }
    }
}

private fun String.sanitizeFileName(): String {
    return replace(Regex("[^A-Za-z0-9._-]"), "_")
}

private fun BitmapFactory.Options.calculateSampleSize(maxDimension: Int): Int {
    var sampleSize = 1
    val halfWidth = outWidth / 2
    val halfHeight = outHeight / 2
    while (halfWidth / sampleSize >= maxDimension || halfHeight / sampleSize >= maxDimension) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun Bitmap.scaleToMaxDimension(maxDimension: Int): Bitmap {
    val maxSide = maxOf(width, height)
    if (maxSide <= maxDimension) return this

    val scale = maxDimension.toFloat() / maxSide.toFloat()
    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}
