package com.xiaoyin.lifeatlas.data.export

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xiaoyin.lifeatlas.core.database.AppDatabase
import com.xiaoyin.lifeatlas.core.media.PhotoCacheManager
import com.xiaoyin.lifeatlas.data.entity.MemoryRecordEntity
import com.xiaoyin.lifeatlas.data.entity.MemoryTagCrossRefEntity
import com.xiaoyin.lifeatlas.data.entity.PhotoEntity
import com.xiaoyin.lifeatlas.data.entity.TagEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LifeAtlasExportServiceTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var exportService: LifeAtlasExportService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = newDatabase()
        exportService = newExportService(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun jsonExportAndImportRestoresStructuredData() = runBlocking {
        seedStructuredData(database, thumbnailPath = "file:///old-device/thumb.jpg")

        val jsonText = exportService.exportJson()
        val targetDatabase = newDatabase()
        try {
            val result = newExportService(targetDatabase).importJson(jsonText)

            assertEquals(1, result.recordCount)
            assertEquals(1, result.photoCount)
            assertEquals(1, result.tagCount)
            assertEquals("第一次旅行", targetDatabase.memoryRecordDao().getAll().single().title)
            assertEquals("旅行", targetDatabase.tagDao().getAllTags().single().name)
            assertEquals(1, targetDatabase.tagDao().getAllCrossRefs().size)

            val restoredPhoto = targetDatabase.photoDao().getAll().single()
            assertEquals("content://lifeatlas/photo/1", restoredPhoto.originalUri)
            assertEquals(null, restoredPhoto.thumbnailPath)
            assertEquals(null, restoredPhoto.compressedPath)
        } finally {
            targetDatabase.close()
        }
    }

    @Test
    fun backupZipImportRestoresMediaCachePath() = runBlocking {
        val sourceThumbnail = createSourceMediaFile(name = "source-thumb.jpg", content = "thumbnail-content")
        seedStructuredData(database, thumbnailPath = Uri.fromFile(sourceThumbnail).toString())

        val backupBytes = ByteArrayOutputStream().use { output ->
            exportService.exportBackupZip(output)
            output.toByteArray()
        }
        val targetDatabase = newDatabase()
        try {
            val result = newExportService(targetDatabase).importBackupZip(ByteArrayInputStream(backupBytes))

            assertEquals(1, result.recordCount)
            assertEquals(1, result.photoCount)
            assertEquals(1, result.tagCount)
            assertEquals(1, result.restoredMediaFileCount)

            val restoredPhoto = targetDatabase.photoDao().getAll().single()
            val restoredThumbnailPath = restoredPhoto.thumbnailPath
            assertNotNull(restoredThumbnailPath)
            val restoredFile = File(requireNotNull(Uri.parse(restoredThumbnailPath).path))
            assertTrue(restoredFile.isFile)
            assertEquals("thumbnail-content", restoredFile.readText())
        } finally {
            targetDatabase.close()
        }
    }

    private fun newDatabase(): AppDatabase {
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    private fun newExportService(database: AppDatabase): LifeAtlasExportService {
        return LifeAtlasExportService(
            database = database,
            memoryRecordDao = database.memoryRecordDao(),
            photoDao = database.photoDao(),
            tagDao = database.tagDao(),
            photoCacheManager = PhotoCacheManager(context)
        )
    }

    private suspend fun seedStructuredData(database: AppDatabase, thumbnailPath: String?) {
        database.memoryRecordDao().insertAll(
            listOf(
                MemoryRecordEntity(
                    id = recordId,
                    title = "第一次旅行",
                    content = "带照片和标签的记录",
                    recordTime = 1_700_000_000_000,
                    latitude = 30.5,
                    longitude = 114.3,
                    locationName = "武汉",
                    mood = "开心",
                    importance = 5,
                    createdAt = 1_700_000_000_000,
                    updatedAt = 1_700_000_000_000
                )
            )
        )
        database.photoDao().insertAll(
            listOf(
                PhotoEntity(
                    id = photoId,
                    recordId = recordId,
                    originalUri = "content://lifeatlas/photo/1",
                    thumbnailPath = thumbnailPath,
                    compressedPath = null,
                    takenAt = null,
                    latitude = null,
                    longitude = null,
                    createdAt = 1_700_000_000_000
                )
            )
        )
        database.tagDao().insertTags(
            listOf(
                TagEntity(
                    id = tagId,
                    name = "旅行",
                    color = "#4D7CFE",
                    createdAt = 1_700_000_000_000
                )
            )
        )
        database.tagDao().insertCrossRefs(
            listOf(MemoryTagCrossRefEntity(recordId = recordId, tagId = tagId))
        )
    }

    private fun createSourceMediaFile(name: String, content: String): File {
        val dir = File(context.cacheDir, "lifeatlas-export-test").apply { mkdirs() }
        return File(dir, name).apply { writeText(content) }
    }

    private companion object {
        const val recordId = 100L
        const val photoId = 200L
        const val tagId = 300L
    }
}
