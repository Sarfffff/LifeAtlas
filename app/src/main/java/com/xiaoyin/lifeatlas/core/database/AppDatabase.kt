package com.xiaoyin.lifeatlas.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xiaoyin.lifeatlas.data.dao.MemoryRecordDao
import com.xiaoyin.lifeatlas.data.dao.PhotoDao
import com.xiaoyin.lifeatlas.data.dao.TagDao
import com.xiaoyin.lifeatlas.data.entity.MemoryTagCrossRefEntity
import com.xiaoyin.lifeatlas.data.entity.MemoryRecordEntity
import com.xiaoyin.lifeatlas.data.entity.PhotoEntity
import com.xiaoyin.lifeatlas.data.entity.TagEntity

@Database(
    entities = [MemoryRecordEntity::class, PhotoEntity::class, TagEntity::class, MemoryTagCrossRefEntity::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryRecordDao(): MemoryRecordDao
    abstract fun photoDao(): PhotoDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `photos` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `record_id` INTEGER NOT NULL,
                        `original_uri` TEXT NOT NULL,
                        `thumbnail_path` TEXT,
                        `compressed_path` TEXT,
                        `taken_at` INTEGER,
                        `latitude` REAL,
                        `longitude` REAL,
                        `created_at` INTEGER NOT NULL,
                        FOREIGN KEY(`record_id`) REFERENCES `memory_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_photos_record_id` ON `photos` (`record_id`)")
            }
        }

        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tags` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `color` TEXT,
                        `created_at` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `memory_tag_cross_ref` (
                        `record_id` INTEGER NOT NULL,
                        `tag_id` INTEGER NOT NULL,
                        PRIMARY KEY(`record_id`, `tag_id`),
                        FOREIGN KEY(`record_id`) REFERENCES `memory_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`tag_id`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_memory_tag_cross_ref_record_id` ON `memory_tag_cross_ref` (`record_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_memory_tag_cross_ref_tag_id` ON `memory_tag_cross_ref` (`tag_id`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lifeatlas.db"
                ).addMigrations(migration1To2, migration2To3).build().also { instance = it }
            }
        }
    }
}
