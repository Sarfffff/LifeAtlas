package com.xiaoyin.lifeatlas.data.mapper

import com.xiaoyin.lifeatlas.core.model.Photo
import com.xiaoyin.lifeatlas.data.entity.PhotoEntity

fun PhotoEntity.toModel(): Photo {
    return Photo(
        id = id,
        recordId = recordId,
        originalUri = originalUri,
        thumbnailPath = thumbnailPath,
        compressedPath = compressedPath,
        takenAt = takenAt,
        latitude = latitude,
        longitude = longitude,
        createdAt = createdAt
    )
}

