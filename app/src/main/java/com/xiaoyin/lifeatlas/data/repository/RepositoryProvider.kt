package com.xiaoyin.lifeatlas.data.repository

import android.content.Context
import com.xiaoyin.lifeatlas.core.database.AppDatabase

object RepositoryProvider {
    fun memoryRepository(context: Context): MemoryRepository {
        val database = AppDatabase.getInstance(context)
        return MemoryRepository(database.memoryRecordDao(), database.photoDao(), database.tagDao())
    }
}
