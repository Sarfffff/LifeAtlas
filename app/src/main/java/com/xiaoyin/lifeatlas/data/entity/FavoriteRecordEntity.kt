package com.xiaoyin.lifeatlas.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorite_records",
    foreignKeys = [
        ForeignKey(
            entity = MemoryRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["record_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["record_id"], unique = true)]
)
data class FavoriteRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "record_id")
    val recordId: Long,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
