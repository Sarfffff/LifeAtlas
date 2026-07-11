package com.xiaoyin.lifeatlas.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xiaoyin.lifeatlas.data.entity.FavoriteRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteRecordDao {
    @Query("SELECT record_id FROM favorite_records ORDER BY created_at DESC")
    fun observeFavoriteRecordIds(): Flow<List<Long>>

    @Query("SELECT record_id FROM favorite_records ORDER BY created_at DESC")
    suspend fun getFavoriteRecordIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteRecordEntity)

    @Query("DELETE FROM favorite_records WHERE record_id = :recordId")
    suspend fun delete(recordId: Long)

    @Query("DELETE FROM favorite_records")
    suspend fun clearAll()
}
