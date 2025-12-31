package com.dhyey.fanfic.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dhyey.fanfic.storage.dao.ChapterDao
import com.dhyey.fanfic.storage.dao.FicDao
import com.dhyey.fanfic.storage.dao.ReadingProgressDao
import com.dhyey.fanfic.storage.entity.ChapterEntity
import com.dhyey.fanfic.storage.entity.FicEntity
import com.dhyey.fanfic.storage.entity.ReadingProgressEntity

@Database(
    entities = [
        FicEntity::class,
        ChapterEntity::class,
        ReadingProgressEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FanficDatabase : RoomDatabase() {

    abstract fun ficDao(): FicDao
    abstract fun chapterDao(): ChapterDao
    abstract fun readingProgressDao(): ReadingProgressDao
}
