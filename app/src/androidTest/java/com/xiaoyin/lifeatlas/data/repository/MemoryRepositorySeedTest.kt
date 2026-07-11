package com.xiaoyin.lifeatlas.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xiaoyin.lifeatlas.core.database.AppDatabase
import com.xiaoyin.lifeatlas.core.media.PhotoCacheManager
import com.xiaoyin.lifeatlas.data.entity.MemoryRecordEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemoryRepositorySeedTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: MemoryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MemoryRepository(
            memoryRecordDao = database.memoryRecordDao(),
            photoDao = database.photoDao(),
            tagDao = database.tagDao(),
            favoriteRecordDao = database.favoriteRecordDao(),
            photoCacheManager = PhotoCacheManager(context),
            starterContentPreferences = preferences
        )
    }

    @After
    fun tearDown() {
        database.close()
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun legacyStarterRecords_areRemovedAndNeverRecreated() = runBlocking {
        val now = System.currentTimeMillis()
        database.memoryRecordDao().insertAll(
            listOf(
                MemoryRecordEntity(
                    title = "第一次拿到房本",
                    content = "今天终于拿到了房本，算是人生阶段性节点。",
                    recordTime = 1781452800000,
                    latitude = 30.5,
                    longitude = 114.3,
                    locationName = "武汉市洪山区",
                    mood = "激动",
                    importance = 5,
                    createdAt = now,
                    updatedAt = now
                ),
                MemoryRecordEntity(
                    title = "上海生活记录",
                    content = "最近工作比较忙，但也慢慢稳定了。",
                    recordTime = 1780848000000,
                    latitude = 31.2,
                    longitude = 121.4,
                    locationName = "上海市徐汇区",
                    mood = "平静",
                    importance = 3,
                    createdAt = now,
                    updatedAt = now
                )
            )
        )

        repository.seedIfEmpty()
        assertEquals(0, database.memoryRecordDao().countIncludingDeleted())
        repository.seedIfEmpty()
        assertEquals(0, database.memoryRecordDao().countIncludingDeleted())
    }

    private companion object {
        const val preferencesName = "memory_repository_seed_test"
    }
}
