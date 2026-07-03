package com.xiaoyin.lifeatlas.data.export

import com.xiaoyin.lifeatlas.data.dao.MemoryRecordDao
import com.xiaoyin.lifeatlas.data.dao.PhotoDao
import com.xiaoyin.lifeatlas.data.dao.TagDao
import com.xiaoyin.lifeatlas.core.database.AppDatabase
import com.xiaoyin.lifeatlas.core.media.PhotoCacheManager
import com.xiaoyin.lifeatlas.data.entity.MemoryRecordEntity
import com.xiaoyin.lifeatlas.data.entity.MemoryTagCrossRefEntity
import com.xiaoyin.lifeatlas.data.entity.PhotoEntity
import com.xiaoyin.lifeatlas.data.entity.TagEntity
import androidx.room.withTransaction
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class LifeAtlasExportService(
    private val database: AppDatabase,
    private val memoryRecordDao: MemoryRecordDao,
    private val photoDao: PhotoDao,
    private val tagDao: TagDao,
    private val photoCacheManager: PhotoCacheManager
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun exportJson(): String {
        val export = LifeAtlasExport(
            schemaVersion = 1,
            app = "LifeAtlas",
            exportedAt = System.currentTimeMillis(),
            records = memoryRecordDao.getAll().map {
                ExportRecord(
                    id = it.id,
                    title = it.title,
                    content = it.content,
                    recordTime = it.recordTime,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    locationName = it.locationName,
                    mood = it.mood,
                    importance = it.importance,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            photos = photoDao.getAll().map {
                ExportPhoto(
                    id = it.id,
                    recordId = it.recordId,
                    originalUri = it.originalUri,
                    thumbnailPath = it.thumbnailPath,
                    compressedPath = it.compressedPath,
                    takenAt = it.takenAt,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    createdAt = it.createdAt
                )
            },
            tags = tagDao.getAllTags().map {
                ExportTag(
                    id = it.id,
                    name = it.name,
                    color = it.color,
                    createdAt = it.createdAt
                )
            },
            recordTags = tagDao.getAllCrossRefs().map {
                ExportRecordTag(
                    recordId = it.recordId,
                    tagId = it.tagId
                )
            }
        )

        return json.encodeToString(export)
    }

    suspend fun hasLocalRecords(): Boolean {
        return memoryRecordDao.count() > 0
    }

    suspend fun hasOnlyStarterRecords(): Boolean {
        val records = memoryRecordDao.getAll()
        if (records.isEmpty() || records.size > 2) return false
        return records.all { record ->
            record.title.contains("房本") ||
                record.title.contains("上海生活")
        }
    }

    suspend fun exportBackupZip(outputStream: OutputStream): LifeAtlasBackupResult {
        val exportedAt = System.currentTimeMillis()
        val exportJson = exportJson()
        val mediaFiles = collectBackupMediaFiles()
        val manifest = LifeAtlasBackupManifest(
            schemaVersion = 1,
            app = "LifeAtlas",
            exportedAt = exportedAt,
            jsonEntry = BackupEntries.dataJson,
            mediaFiles = mediaFiles
        )

        ZipOutputStream(outputStream.buffered()).use { zip ->
            zip.putTextEntry(BackupEntries.dataJson, exportJson)
            zip.putTextEntry(BackupEntries.manifestJson, json.encodeToString(manifest))
            mediaFiles.forEach { media ->
                val file = media.sourcePath.toFile()
                if (file.isFile) {
                    zip.putNextEntry(ZipEntry(media.entryName))
                    file.inputStream().use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }

        return LifeAtlasBackupResult(
            recordCount = memoryRecordDao.getAll().size,
            photoCount = photoDao.getAll().size,
            mediaFileCount = mediaFiles.size
        )
    }

    suspend fun importJson(jsonText: String): LifeAtlasImportResult {
        val export = decodeAndValidate(jsonText)
        return importExportData(export, restoredMediaPaths = emptyMap())
    }

    suspend fun importBackupZip(inputStream: InputStream): LifeAtlasBackupImportResult {
        val backup = readBackupZip(inputStream)
        val manifest = decodeAndValidateManifest(backup.manifestJson)
        val export = decodeAndValidate(backup.exportJson)
        require(manifest.jsonEntry == BackupEntries.dataJson) { "备份包数据入口不受支持：${manifest.jsonEntry}" }

        val restoredMediaPaths = restoreBackupMediaFiles(manifest, backup.mediaEntries)
        val result = runCatching {
            importExportData(export, restoredMediaPaths)
        }.onFailure {
            restoredMediaPaths.values.forEach(photoCacheManager::deleteCachedPhoto)
        }.getOrThrow()
        return LifeAtlasBackupImportResult(
            recordCount = result.recordCount,
            photoCount = result.photoCount,
            tagCount = result.tagCount,
            restoredMediaFileCount = restoredMediaPaths.size
        )
    }

    private suspend fun importExportData(
        export: LifeAtlasExport,
        restoredMediaPaths: Map<MediaRestoreKey, String>
    ): LifeAtlasImportResult {
        val oldCachePaths = export.records
            .flatMap { record -> photoDao.getByRecordId(record.id) }
            .flatMap { photo -> listOfNotNull(photo.thumbnailPath, photo.compressedPath) }

        database.withTransaction {
            memoryRecordDao.insertAll(
                export.records.map {
                    MemoryRecordEntity(
                        id = it.id,
                        title = it.title,
                        content = it.content,
                        recordTime = it.recordTime,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        locationName = it.locationName,
                        mood = it.mood,
                        importance = it.importance,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt
                    )
                }
            )

            tagDao.insertTags(
                export.tags.map {
                    TagEntity(
                        id = it.id,
                        name = it.name,
                        color = it.color,
                        createdAt = it.createdAt
                    )
                }
            )

            export.records.forEach { record ->
                photoDao.deleteByRecordId(record.id)
                tagDao.clearTagsForRecord(record.id)
            }

            photoDao.insertAll(
                export.photos.map {
                    PhotoEntity(
                        id = it.id,
                        recordId = it.recordId,
                        originalUri = it.originalUri,
                        thumbnailPath = restoredMediaPaths[MediaRestoreKey(it.id, "thumbnail")],
                        compressedPath = restoredMediaPaths[MediaRestoreKey(it.id, "compressed")],
                        takenAt = it.takenAt,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        createdAt = it.createdAt
                    )
                }
            )

            tagDao.insertCrossRefs(
                export.recordTags.map {
                    MemoryTagCrossRefEntity(
                        recordId = it.recordId,
                        tagId = it.tagId
                    )
                }
            )
        }

        oldCachePaths.forEach(photoCacheManager::deleteCachedPhoto)

        return LifeAtlasImportResult(
            recordCount = export.records.size,
            photoCount = export.photos.size,
            tagCount = export.tags.size
        )
    }

    fun previewJson(jsonText: String): LifeAtlasImportPreview {
        val export = decodeAndValidate(jsonText)
        return LifeAtlasImportPreview(
            schemaVersion = export.schemaVersion,
            exportedAt = export.exportedAt,
            recordCount = export.records.size,
            photoCount = export.photos.size,
            tagCount = export.tags.size,
            recordTagCount = export.recordTags.size,
            mediaFileCount = null,
            backupKind = BackupKind.Json
        )
    }

    fun previewBackupZip(inputStream: InputStream): LifeAtlasImportPreview {
        val backup = readBackupZip(inputStream)
        val manifest = decodeAndValidateManifest(backup.manifestJson)
        val export = decodeAndValidate(backup.exportJson)
        require(manifest.jsonEntry == BackupEntries.dataJson) { "备份包数据入口不受支持：${manifest.jsonEntry}" }

        return LifeAtlasImportPreview(
            schemaVersion = export.schemaVersion,
            exportedAt = manifest.exportedAt,
            recordCount = export.records.size,
            photoCount = export.photos.size,
            tagCount = export.tags.size,
            recordTagCount = export.recordTags.size,
            mediaFileCount = backup.mediaEntries.size,
            backupKind = BackupKind.Zip
        )
    }

    private fun decodeAndValidate(jsonText: String): LifeAtlasExport {
        val export = json.decodeFromString<LifeAtlasExport>(jsonText)
        require(export.app == "LifeAtlas") { "不是岁迹导出的备份文件" }
        require(export.schemaVersion == 1) { "暂不支持该备份版本：${export.schemaVersion}" }
        return export
    }

    private fun decodeAndValidateManifest(jsonText: String): LifeAtlasBackupManifest {
        val manifest = json.decodeFromString<LifeAtlasBackupManifest>(jsonText)
        require(manifest.app == "LifeAtlas") { "不是岁迹导出的备份包" }
        require(manifest.schemaVersion == 1) { "暂不支持该备份包版本：${manifest.schemaVersion}" }
        return manifest
    }

    private fun readBackupZip(inputStream: InputStream): BackupZipPayload {
        var exportJson: String? = null
        var manifestJson: String? = null
        val mediaEntries = mutableMapOf<String, ByteArray>()

        ZipInputStream(inputStream.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    when (entry.name) {
                        BackupEntries.dataJson -> exportJson = zip.readEntryText()
                        BackupEntries.manifestJson -> manifestJson = zip.readEntryText()
                        else -> if (entry.name.startsWith("media/")) {
                            mediaEntries[entry.name] = zip.readBytes()
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        return BackupZipPayload(
            exportJson = exportJson ?: error("备份包缺少 ${BackupEntries.dataJson}"),
            manifestJson = manifestJson ?: error("备份包缺少 ${BackupEntries.manifestJson}"),
            mediaEntries = mediaEntries
        )
    }

    private fun restoreBackupMediaFiles(
        manifest: LifeAtlasBackupManifest,
        mediaEntries: Map<String, ByteArray>
    ): Map<MediaRestoreKey, String> {
        return manifest.mediaFiles.mapNotNull { media ->
            val bytes = mediaEntries[media.entryName] ?: return@mapNotNull null
            val restoredPath = photoCacheManager.restoreBackupMedia(
                kind = media.kind,
                entryName = media.entryName,
                inputStream = bytes.inputStream()
            ) ?: return@mapNotNull null
            MediaRestoreKey(photoId = media.photoId, kind = media.kind) to restoredPath
        }.toMap()
    }

    private suspend fun collectBackupMediaFiles(): List<BackupMediaFile> {
        return photoDao.getAll().flatMap { photo ->
            listOfNotNull(
                photo.thumbnailPath?.toBackupMediaFile(
                    photoId = photo.id,
                    kind = "thumbnail",
                    entryName = "media/thumbnails/photo_${photo.id}.jpg"
                ),
                photo.compressedPath?.toBackupMediaFile(
                    photoId = photo.id,
                    kind = "compressed",
                    entryName = "media/compressed/photo_${photo.id}.jpg"
                )
            )
        }
    }

    private fun String.toBackupMediaFile(photoId: Long, kind: String, entryName: String): BackupMediaFile? {
        val file = toFile()
        if (!file.isFile) return null
        return BackupMediaFile(
            photoId = photoId,
            kind = kind,
            entryName = entryName,
            sourcePath = this
        )
    }

    private fun String.toFile(): File {
        return if (startsWith("file:")) {
            File(requireNotNull(android.net.Uri.parse(this).path))
        } else {
            File(this)
        }
    }
}

private data class BackupZipPayload(
    val exportJson: String,
    val manifestJson: String,
    val mediaEntries: Map<String, ByteArray>
)

private data class MediaRestoreKey(
    val photoId: Long,
    val kind: String
)

private object BackupEntries {
    const val dataJson = "lifeatlas_export.json"
    const val manifestJson = "backup_manifest.json"
}

private fun ZipOutputStream.putTextEntry(name: String, text: String) {
    putNextEntry(ZipEntry(name))
    write(text.toByteArray(Charsets.UTF_8))
    closeEntry()
}

private fun ZipInputStream.readEntryText(): String {
    return readBytes().toString(Charsets.UTF_8)
}

data class LifeAtlasImportResult(
    val recordCount: Int,
    val photoCount: Int,
    val tagCount: Int
)

data class LifeAtlasImportPreview(
    val schemaVersion: Int,
    val exportedAt: Long,
    val recordCount: Int,
    val photoCount: Int,
    val tagCount: Int,
    val recordTagCount: Int,
    val mediaFileCount: Int?,
    val backupKind: BackupKind
)

enum class BackupKind {
    Json,
    Zip
}

data class LifeAtlasBackupResult(
    val recordCount: Int,
    val photoCount: Int,
    val mediaFileCount: Int
)

data class LifeAtlasBackupImportResult(
    val recordCount: Int,
    val photoCount: Int,
    val tagCount: Int,
    val restoredMediaFileCount: Int
)
