package com.xiaoyin.lifeatlas.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xiaoyin.lifeatlas.data.entity.MemoryRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryRecordDao {
    @Query("SELECT * FROM memory_records WHERE deleted_at IS NULL ORDER BY record_time DESC")
    fun observeAll(): Flow<List<MemoryRecordEntity>>

    @Query("SELECT * FROM memory_records WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    fun observeDeleted(): Flow<List<MemoryRecordEntity>>

    @Query(
        """
        SELECT * FROM memory_records
        WHERE deleted_at IS NULL AND latitude IS NOT NULL AND longitude IS NOT NULL
        ORDER BY record_time DESC
        """
    )
    fun observeLocatedRecords(): Flow<List<MemoryRecordEntity>>

    @Query(
        """
        SELECT memory_records.* FROM memory_records
        INNER JOIN memory_tag_cross_ref ON memory_records.id = memory_tag_cross_ref.record_id
        WHERE memory_tag_cross_ref.tag_id = :tagId AND memory_records.deleted_at IS NULL
        ORDER BY memory_records.record_time DESC
        """
    )
    fun observeByTag(tagId: Long): Flow<List<MemoryRecordEntity>>

    @Query("SELECT * FROM memory_records WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    fun observeById(id: Long): Flow<MemoryRecordEntity?>

    @Query("SELECT COUNT(*) FROM memory_records WHERE deleted_at IS NULL")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM memory_records")
    suspend fun countIncludingDeleted(): Int

    @Query("SELECT * FROM memory_records WHERE deleted_at IS NULL ORDER BY record_time DESC")
    suspend fun getAll(): List<MemoryRecordEntity>

    @Query("SELECT * FROM memory_records ORDER BY record_time DESC")
    suspend fun getAllIncludingDeleted(): List<MemoryRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MemoryRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<MemoryRecordEntity>)

    @Update
    suspend fun update(record: MemoryRecordEntity)

    @Delete
    suspend fun delete(record: MemoryRecordEntity)

    @Query("DELETE FROM memory_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE memory_records SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :id")
    suspend fun moveToTrash(id: Long, deletedAt: Long)

    @Query("UPDATE memory_records SET deleted_at = NULL, updated_at = :restoredAt WHERE id = :id")
    suspend fun restoreFromTrash(id: Long, restoredAt: Long)

    @Query("DELETE FROM memory_records WHERE deleted_at IS NOT NULL")
    suspend fun deleteAllFromTrash()
}
