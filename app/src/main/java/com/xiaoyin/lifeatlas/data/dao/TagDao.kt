package com.xiaoyin.lifeatlas.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xiaoyin.lifeatlas.data.entity.MemoryTagCrossRefEntity
import com.xiaoyin.lifeatlas.data.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTags(): List<TagEntity>

    @Query("SELECT * FROM memory_tag_cross_ref ORDER BY record_id ASC, tag_id ASC")
    suspend fun getAllCrossRefs(): List<MemoryTagCrossRefEntity>

    @Query(
        """
        SELECT tags.* FROM tags
        INNER JOIN memory_tag_cross_ref ON tags.id = memory_tag_cross_ref.tag_id
        WHERE memory_tag_cross_ref.record_id = :recordId
        ORDER BY tags.name ASC
        """
    )
    fun observeTagsForRecord(recordId: Long): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<MemoryTagCrossRefEntity>)

    @Query("DELETE FROM memory_tag_cross_ref WHERE record_id = :recordId")
    suspend fun clearTagsForRecord(recordId: Long)
}
