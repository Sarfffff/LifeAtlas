package com.xiaoyin.lifeatlas.data.export

import android.content.Context
import com.xiaoyin.lifeatlas.core.database.AppDatabase
import com.xiaoyin.lifeatlas.core.media.PhotoCacheManager

object ExportServiceProvider {
    fun exportService(context: Context): LifeAtlasExportService {
        val database = AppDatabase.getInstance(context)
        return LifeAtlasExportService(
            database = database,
            memoryRecordDao = database.memoryRecordDao(),
            photoDao = database.photoDao(),
            tagDao = database.tagDao(),
            favoriteRecordDao = database.favoriteRecordDao(),
            photoCacheManager = PhotoCacheManager(context)
        )
    }
}
