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
    @Query("SELECT * FROM memory_records ORDER BY record_time DESC")
    fun observeAll(): Flow<List<MemoryRecordEntity>>

    @Query("SELECT * FROM memory_records WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<MemoryRecordEntity?>

    @Query("SELECT COUNT(*) FROM memory_records")
    suspend fun count(): Int

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
}

