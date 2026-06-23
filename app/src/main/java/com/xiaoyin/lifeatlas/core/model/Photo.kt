package com.xiaoyin.lifeatlas.core.model

data class Photo(
    val id: Long,
    val recordId: Long,
    val originalUri: String,
    val thumbnailPath: String?,
    val compressedPath: String?,
    val takenAt: Long?,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: Long
)

