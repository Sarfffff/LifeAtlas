package com.xiaoyin.lifeatlas.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xiaoyin.lifeatlas.core.database.AppDatabase
import com.xiaoyin.lifeatlas.core.media.PhotoCacheManager
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
    fun deletedStarterRecords_doNotReappearWhenSeedingRunsAgain() = runBlocking {
        repository.seedIfEmpty()
        val starterRecords = database.memoryRecordDao().getAllIncludingDeleted()
        assertEquals(2, starterRecords.size)

        starterRecords.forEach { repository.deleteRecord(it.id) }
        assertEquals(0, database.memoryRecordDao().count())

        repository.seedIfEmpty()
        assertEquals(0, database.memoryRecordDao().count())
        assertEquals(2, database.memoryRecordDao().countIncludingDeleted())

        starterRecords.forEach { repository.permanentlyDeleteRecord(it.id) }
        repository.seedIfEmpty()
        assertEquals(0, database.memoryRecordDao().countIncludingDeleted())
    }

    private companion object {
        const val preferencesName = "memory_repository_seed_test"
    }
}
