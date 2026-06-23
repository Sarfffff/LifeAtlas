package com.xiaoyin.lifeatlas.data.export

import com.xiaoyin.lifeatlas.data.dao.MemoryRecordDao
import com.xiaoyin.lifeatlas.data.dao.PhotoDao
import com.xiaoyin.lifeatlas.data.dao.TagDao
import com.xiaoyin.lifeatlas.core.database.AppDatabase
import com.xiaoyin.lifeatlas.data.entity.MemoryRecordEntity
import com.xiaoyin.lifeatlas.data.entity.MemoryTagCrossRefEntity
import com.xiaoyin.lifeatlas.data.entity.PhotoEntity
import com.xiaoyin.lifeatlas.data.entity.TagEntity
import androidx.room.withTransaction
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LifeAtlasExportService(
    private val database: AppDatabase,
    private val memoryRecordDao: MemoryRecordDao,
    private val photoDao: PhotoDao,
    private val tagDao: TagDao
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

    suspend fun importJson(jsonText: String): LifeAtlasImportResult {
        val export = json.decodeFromString<LifeAtlasExport>(jsonText)
        require(export.app == "LifeAtlas") { "不是岁迹导出的备份文件" }
        require(export.schemaVersion == 1) { "暂不支持该备份版本：${export.schemaVersion}" }

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
                        thumbnailPath = it.thumbnailPath,
                        compressedPath = it.compressedPath,
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

        return LifeAtlasImportResult(
            recordCount = export.records.size,
            photoCount = export.photos.size,
            tagCount = export.tags.size
        )
    }
}

data class LifeAtlasImportResult(
    val recordCount: Int,
    val photoCount: Int,
    val tagCount: Int
)
