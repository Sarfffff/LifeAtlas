package com.xiaoyin.lifeatlas.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2_keepsRecordsAndCreatesPhotosTable() {
        helper.createDatabase(testDbName, 1).apply {
            insertVersion1Record()
            close()
        }

        val db = helper.runMigrationsAndValidate(
            testDbName,
            2,
            true,
            AppDatabase.migration1To2
        )

        assertEquals(1, db.queryCount("memory_records"))
        assertEquals("旧记录", db.querySingleText("SELECT title FROM memory_records WHERE id = 1"))
        assertTableExists(db, "photos")
        assertIndexExists(db, "index_photos_record_id")
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3_keepsRecordsAndPhotosAndCreatesTagTables() {
        helper.createDatabase(testDbName, 2).apply {
            insertVersion1Record()
            insertVersion2Photo()
            close()
        }

        val db = helper.runMigrationsAndValidate(
            testDbName,
            3,
            true,
            AppDatabase.migration2To3
        )

        assertEquals(1, db.queryCount("memory_records"))
        assertEquals(1, db.queryCount("photos"))
        assertTableExists(db, "tags")
        assertTableExists(db, "memory_tag_cross_ref")
        assertIndexExists(db, "index_tags_name")
        assertIndexExists(db, "index_memory_tag_cross_ref_record_id")
        assertIndexExists(db, "index_memory_tag_cross_ref_tag_id")
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To3_keepsExistingDataAndValidatesLatestSchema() {
        helper.createDatabase(testDbName, 1).apply {
            insertVersion1Record()
            close()
        }

        val db = helper.runMigrationsAndValidate(
            testDbName,
            3,
            true,
            AppDatabase.migration1To2,
            AppDatabase.migration2To3
        )

        assertEquals(1, db.queryCount("memory_records"))
        assertEquals("旧记录", db.querySingleText("SELECT title FROM memory_records WHERE id = 1"))
        assertTableExists(db, "photos")
        assertTableExists(db, "tags")
        assertTableExists(db, "memory_tag_cross_ref")
        db.close()
    }

    private fun SupportSQLiteDatabase.insertVersion1Record() {
        execSQL(
            """
            INSERT INTO memory_records (
                id, title, content, record_time, latitude, longitude,
                location_name, mood, importance, created_at, updated_at
            ) VALUES (
                1, '旧记录', '迁移前的数据', 1700000000000, 30.5, 114.3,
                '武汉', '平静', 4, 1700000000000, 1700000000000
            )
            """.trimIndent()
        )
    }

    private fun SupportSQLiteDatabase.insertVersion2Photo() {
        execSQL(
            """
            INSERT INTO photos (
                id, record_id, original_uri, thumbnail_path, compressed_path,
                taken_at, latitude, longitude, created_at
            ) VALUES (
                10, 1, 'content://lifeatlas/photo/10', 'file:///thumb.jpg', NULL,
                NULL, NULL, NULL, 1700000000000
            )
            """.trimIndent()
        )
    }

    private fun SupportSQLiteDatabase.queryCount(table: String): Int {
        query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0)
        }
    }

    private fun SupportSQLiteDatabase.querySingleText(sql: String): String {
        query(sql).use { cursor ->
            cursor.moveToFirst()
            return cursor.getString(0)
        }
    }

    private fun assertTableExists(db: SupportSQLiteDatabase, tableName: String) {
        db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(tableName)).use { cursor ->
            assertEquals(1, cursor.count)
        }
    }

    private fun assertIndexExists(db: SupportSQLiteDatabase, indexName: String) {
        db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name = ?", arrayOf(indexName)).use { cursor ->
            assertNotNull(indexName, cursor)
            assertEquals(1, cursor.count)
        }
    }

    private companion object {
        const val testDbName = "migration-test"
    }
}
