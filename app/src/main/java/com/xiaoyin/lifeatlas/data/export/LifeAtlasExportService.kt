package com.xiaoyin.lifeatlas.data.export

import com.xiaoyin.lifeatlas.data.dao.MemoryRecordDao
import com.xiaoyin.lifeatlas.data.dao.PhotoDao
import com.xiaoyin.lifeatlas.data.dao.TagDao
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LifeAtlasExportService(
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
}

