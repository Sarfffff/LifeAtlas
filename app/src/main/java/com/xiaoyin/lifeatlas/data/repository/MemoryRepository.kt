package com.xiaoyin.lifeatlas.data.repository

import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.core.model.Photo
import com.xiaoyin.lifeatlas.core.model.Tag
import com.xiaoyin.lifeatlas.data.dao.MemoryRecordDao
import com.xiaoyin.lifeatlas.data.dao.PhotoDao
import com.xiaoyin.lifeatlas.data.dao.TagDao
import com.xiaoyin.lifeatlas.data.entity.MemoryTagCrossRefEntity
import com.xiaoyin.lifeatlas.data.entity.MemoryRecordEntity
import com.xiaoyin.lifeatlas.data.entity.PhotoEntity
import com.xiaoyin.lifeatlas.data.entity.TagEntity
import com.xiaoyin.lifeatlas.data.mapper.toEntity
import com.xiaoyin.lifeatlas.data.mapper.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MemoryRepository(
    private val memoryRecordDao: MemoryRecordDao,
    private val photoDao: PhotoDao,
    private val tagDao: TagDao
) {
    fun observeAllRecords(): Flow<List<MemoryRecord>> {
        return memoryRecordDao.observeAll().map { records ->
            records.map { it.toModel() }
        }
    }

    fun observeLocatedRecords(): Flow<List<MemoryRecord>> {
        return memoryRecordDao.observeLocatedRecords().map { records ->
            records.map { it.toModel() }
        }
    }

    fun observeRecordsByTag(tagId: Long): Flow<List<MemoryRecord>> {
        return memoryRecordDao.observeByTag(tagId).map { records ->
            records.map { it.toModel() }
        }
    }

    fun observeRecord(id: Long): Flow<MemoryRecord?> {
        return memoryRecordDao.observeById(id).map { it?.toModel() }
    }

    fun observePhotos(recordId: Long): Flow<List<Photo>> {
        return photoDao.observeByRecordId(recordId).map { photos ->
            photos.map { it.toModel() }
        }
    }

    fun observeFirstPhotosByRecord(): Flow<Map<Long, Photo>> {
        return photoDao.observeFirstPhotosByRecord().map { photos ->
            photos.associate { it.recordId to it.toModel() }
        }
    }

    fun observeTags(recordId: Long): Flow<List<Tag>> {
        return tagDao.observeTagsForRecord(recordId).map { tags ->
            tags.map { it.toModel() }
        }
    }

    fun observeAllTags(): Flow<List<Tag>> {
        return tagDao.observeAllTags().map { tags ->
            tags.map { it.toModel() }
        }
    }

    suspend fun addRecord(record: MemoryRecord): Long {
        return memoryRecordDao.insert(record.toEntity())
    }

    suspend fun addRecord(record: MemoryRecord, photoUris: List<String>): Long {
        val recordId = memoryRecordDao.insert(record.toEntity())
        addPhotos(recordId, photoUris)
        return recordId
    }

    suspend fun addRecord(record: MemoryRecord, photoUris: List<String>, tagNames: List<String>): Long {
        val recordId = memoryRecordDao.insert(record.toEntity())
        addPhotos(recordId, photoUris)
        replaceTags(recordId, tagNames)
        return recordId
    }

    suspend fun updateRecord(record: MemoryRecord) {
        memoryRecordDao.update(record.toEntity())
    }

    suspend fun updateRecord(record: MemoryRecord, tagNames: List<String>) {
        memoryRecordDao.update(record.toEntity())
        replaceTags(record.id, tagNames)
    }

    suspend fun deleteRecord(id: Long) {
        memoryRecordDao.deleteById(id)
    }

    private suspend fun addPhotos(recordId: Long, photoUris: List<String>) {
        if (photoUris.isEmpty()) return

        val now = System.currentTimeMillis()
        photoDao.insertAll(
            photoUris.distinct().map { uri ->
                PhotoEntity(
                    recordId = recordId,
                    originalUri = uri,
                    thumbnailPath = null,
                    compressedPath = null,
                    takenAt = null,
                    latitude = null,
                    longitude = null,
                    createdAt = now
                )
            }
        )
    }

    private suspend fun replaceTags(recordId: Long, tagNames: List<String>) {
        tagDao.clearTagsForRecord(recordId)
        val normalizedNames = tagNames.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (normalizedNames.isEmpty()) return

        val now = System.currentTimeMillis()
        val tagIds = normalizedNames.map { name ->
            val existing = tagDao.findByName(name)
            existing?.id ?: tagDao.insertTag(
                TagEntity(
                    name = name,
                    color = null,
                    createdAt = now
                )
            )
        }

        tagDao.insertCrossRefs(
            tagIds.map { tagId ->
                MemoryTagCrossRefEntity(recordId = recordId, tagId = tagId)
            }
        )
    }

    suspend fun seedIfEmpty() {
        if (memoryRecordDao.count() > 0) return

        val now = System.currentTimeMillis()
        memoryRecordDao.insertAll(
            listOf(
                MemoryRecordEntity(
                    title = "第一次拿到房本",
                    content = "今天终于拿到了房本，算是人生阶段性节点。",
                    recordTime = 1781452800000,
                    latitude = 30.5,
                    longitude = 114.3,
                    locationName = "武汉市洪山区",
                    mood = "激动",
                    importance = 5,
                    createdAt = now,
                    updatedAt = now
                ),
                MemoryRecordEntity(
                    title = "上海生活记录",
                    content = "最近工作比较忙，但也慢慢稳定了。",
                    recordTime = 1780848000000,
                    latitude = 31.2,
                    longitude = 121.4,
                    locationName = "上海市徐汇区",
                    mood = "平静",
                    importance = 3,
                    createdAt = now,
                    updatedAt = now
                )
            )
        )
    }
}
