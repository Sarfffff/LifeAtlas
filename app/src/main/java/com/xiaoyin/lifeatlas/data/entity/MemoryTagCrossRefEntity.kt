package com.xiaoyin.lifeatlas.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "memory_tag_cross_ref",
    primaryKeys = ["record_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = MemoryRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["record_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["record_id"]),
        Index(value = ["tag_id"])
    ]
)
data class MemoryTagCrossRefEntity(
    @ColumnInfo(name = "record_id")
    val recordId: Long,
    @ColumnInfo(name = "tag_id")
    val tagId: Long
)

