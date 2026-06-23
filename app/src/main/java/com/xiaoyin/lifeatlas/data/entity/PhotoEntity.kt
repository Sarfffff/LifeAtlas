package com.xiaoyin.lifeatlas.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = MemoryRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["record_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["record_id"])]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "record_id")
    val recordId: Long,
    @ColumnInfo(name = "original_uri")
    val originalUri: String,
    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String?,
    @ColumnInfo(name = "compressed_path")
    val compressedPath: String?,
    @ColumnInfo(name = "taken_at")
    val takenAt: Long?,
    val latitude: Double?,
    val longitude: Double?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

