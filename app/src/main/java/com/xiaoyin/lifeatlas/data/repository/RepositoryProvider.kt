package com.xiaoyin.lifeatlas.data.repository

import android.content.Context
import com.xiaoyin.lifeatlas.core.database.AppDatabase
import com.xiaoyin.lifeatlas.core.media.PhotoCacheManager

object RepositoryProvider {
    fun memoryRepository(context: Context): MemoryRepository {
        val database = AppDatabase.getInstance(context)
        return MemoryRepository(
            memoryRecordDao = database.memoryRecordDao(),
            photoDao = database.photoDao(),
            tagDao = database.tagDao(),
            favoriteRecordDao = database.favoriteRecordDao(),
            photoCacheManager = PhotoCacheManager(context)
        )
    }
}
