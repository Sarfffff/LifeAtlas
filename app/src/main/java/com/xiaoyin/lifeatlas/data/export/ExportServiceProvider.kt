package com.xiaoyin.lifeatlas.data.export

import android.content.Context
import com.xiaoyin.lifeatlas.core.database.AppDatabase

object ExportServiceProvider {
    fun exportService(context: Context): LifeAtlasExportService {
        val database = AppDatabase.getInstance(context)
        return LifeAtlasExportService(
            database = database,
            memoryRecordDao = database.memoryRecordDao(),
            photoDao = database.photoDao(),
            tagDao = database.tagDao()
        )
    }
}
