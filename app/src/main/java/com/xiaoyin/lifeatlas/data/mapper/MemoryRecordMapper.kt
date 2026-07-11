package com.xiaoyin.lifeatlas.data.mapper

import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.data.entity.MemoryRecordEntity

fun MemoryRecordEntity.toModel(): MemoryRecord {
    return MemoryRecord(
        id = id,
        title = title,
        content = content,
        recordTime = recordTime,
        latitude = latitude,
        longitude = longitude,
        locationName = locationName,
        mood = mood,
        importance = importance,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )
}

fun MemoryRecord.toEntity(): MemoryRecordEntity {
    return MemoryRecordEntity(
        id = id,
        title = title,
        content = content,
        recordTime = recordTime,
        latitude = latitude,
        longitude = longitude,
        locationName = locationName,
        mood = mood,
        importance = importance,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )
}
