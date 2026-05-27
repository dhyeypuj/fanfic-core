package com.dhyey.fanfic.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dhyey.fanfic.storage.entity.ReadingProgressEntity

@Dao
interface ReadingProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ReadingProgressEntity)

    @Query("SELECT * FROM reading_progress WHERE chapterId = :chapterId")
    suspend fun getByChapter(chapterId: String): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress WHERE ficId = :ficId")
    suspend fun getForFic(ficId: String): List<ReadingProgressEntity>

    @Query("DELETE FROM reading_progress WHERE chapterId = :chapterId")
    suspend fun deleteProgress(chapterId: String)

    @Query("DELETE FROM reading_progress")
    suspend fun clearAllProgress()
}
