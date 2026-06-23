package com.xiaoyin.lifeatlas.core.model

data class MemoryRecord(
    val id: Long,
    val title: String,
    val content: String,
    val recordTime: Long,
    val latitude: Double?,
    val longitude: Double?,
    val locationName: String?,
    val mood: String?,
    val importance: Int,
    val createdAt: Long,
    val updatedAt: Long
)

