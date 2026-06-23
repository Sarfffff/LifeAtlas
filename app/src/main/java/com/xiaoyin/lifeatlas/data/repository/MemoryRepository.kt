package com.xiaoyin.lifeatlas.data.repository

import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.data.dao.MemoryRecordDao
import com.xiaoyin.lifeatlas.data.entity.MemoryRecordEntity
import com.xiaoyin.lifeatlas.data.mapper.toEntity
import com.xiaoyin.lifeatlas.data.mapper.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MemoryRepository(
    private val memoryRecordDao: MemoryRecordDao
) {
    fun observeAllRecords(): Flow<List<MemoryRecord>> {
        return memoryRecordDao.observeAll().map { records ->
            records.map { it.toModel() }
        }
    }

    fun observeRecord(id: Long): Flow<MemoryRecord?> {
        return memoryRecordDao.observeById(id).map { it?.toModel() }
    }

    suspend fun addRecord(record: MemoryRecord): Long {
        return memoryRecordDao.insert(record.toEntity())
    }

    suspend fun updateRecord(record: MemoryRecord) {
        memoryRecordDao.update(record.toEntity())
    }

    suspend fun deleteRecord(id: Long) {
        memoryRecordDao.deleteById(id)
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

