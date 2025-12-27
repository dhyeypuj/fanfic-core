package com.dhyey.fanfic.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dhyey.fanfic.storage.entity.ChapterEntity

@Dao
interface ChapterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChapters(chapters: List<ChapterEntity>)

    @Query("SELECT * FROM chapters WHERE ficOwnerId = :ficId ORDER BY chapterNumber ASC")
    suspend fun getChaptersForFic(ficId: String): List<ChapterEntity>

    @Query("DELETE FROM chapters WHERE ficOwnerId = :ficId")
    suspend fun deleteChaptersForFic(ficId: String)
}
