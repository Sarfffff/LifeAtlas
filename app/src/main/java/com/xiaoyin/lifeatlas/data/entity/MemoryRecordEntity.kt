package com.xiaoyin.lifeatlas.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "memory_records", indices = [Index(value = ["deleted_at"])])
data class MemoryRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    @ColumnInfo(name = "record_time")
    val recordTime: Long,
    val latitude: Double?,
    val longitude: Double?,
    @ColumnInfo(name = "location_name")
    val locationName: String?,
    val mood: String?,
    val importance: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null
)
