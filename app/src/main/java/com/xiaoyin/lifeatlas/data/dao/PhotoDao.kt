package com.xiaoyin.lifeatlas.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xiaoyin.lifeatlas.data.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE record_id = :recordId ORDER BY id ASC")
    fun observeByRecordId(recordId: Long): Flow<List<PhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<PhotoEntity>)

    @Query("DELETE FROM photos WHERE record_id = :recordId")
    suspend fun deleteByRecordId(recordId: Long)
}

