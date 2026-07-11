package com.xiaoyin.lifeatlas.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xiaoyin.lifeatlas.data.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT photos.* FROM photos INNER JOIN memory_records ON photos.record_id = memory_records.id WHERE memory_records.deleted_at IS NULL ORDER BY photos.record_id ASC, photos.id ASC")
    fun observeAll(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos ORDER BY record_id ASC, id ASC")
    fun observeAllIncludingDeleted(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE record_id = :recordId ORDER BY id ASC")
    fun observeByRecordId(recordId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos ORDER BY record_id ASC, id ASC")
    suspend fun getAll(): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE record_id = :recordId ORDER BY id ASC")
    suspend fun getByRecordId(recordId: Long): List<PhotoEntity>

    @Query("SELECT COUNT(*) FROM photos INNER JOIN memory_records ON photos.record_id = memory_records.id WHERE memory_records.deleted_at IS NULL")
    fun observePhotoCount(): Flow<Int>

    @Query("SELECT photos.* FROM photos INNER JOIN memory_records ON photos.record_id = memory_records.id WHERE memory_records.deleted_at IS NULL AND photos.id IN (SELECT MIN(id) FROM photos GROUP BY record_id) ORDER BY photos.record_id ASC")
    fun observeFirstPhotosByRecord(): Flow<List<PhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<PhotoEntity>)

    @Query("DELETE FROM photos WHERE record_id = :recordId")
    suspend fun deleteByRecordId(recordId: Long)
}
