package com.xiaoyin.lifeatlas.data.export

import kotlinx.serialization.Serializable

@Serializable
data class LifeAtlasExport(
    val schemaVersion: Int,
    val app: String,
    val exportedAt: Long,
    val records: List<ExportRecord>,
    val photos: List<ExportPhoto>,
    val tags: List<ExportTag>,
    val recordTags: List<ExportRecordTag>
)

@Serializable
data class ExportRecord(
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

@Serializable
data class ExportPhoto(
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

@Serializable
data class ExportTag(
    val id: Long,
    val name: String,
    val color: String?,
    val createdAt: Long
)

@Serializable
data class ExportRecordTag(
    val recordId: Long,
    val tagId: Long
)

@Serializable
data class LifeAtlasBackupManifest(
    val schemaVersion: Int,
    val app: String,
    val exportedAt: Long,
    val jsonEntry: String,
    val mediaFiles: List<BackupMediaFile>
)

@Serializable
data class BackupMediaFile(
    val photoId: Long,
    val kind: String,
    val entryName: String,
    val sourcePath: String
)
