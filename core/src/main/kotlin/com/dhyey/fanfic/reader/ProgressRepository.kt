package com.dhyey.fanfic.reader

import com.dhyey.fanfic.storage.dao.ReadingProgressDao
import com.dhyey.fanfic.storage.entity.ReadingProgressEntity

class ProgressRepository(
    private val dao: ReadingProgressDao
) {

    suspend fun updateProgress(
        chapterId: String,
        ficId: String,
        chapterNumber: Int,
        newPosition: Int,
        sessionTimeMillis: Long
    ) {
        val existing = dao.getByChapter(chapterId)

        val updated = ReadingProgressEntity(
            chapterId = chapterId,
            ficId = ficId,
            chapterNumber = chapterNumber,
            lastReadPosition = newPosition,
            lastReadAt = System.currentTimeMillis(),
            totalTimeSpentMillis =
                (existing?.totalTimeSpentMillis ?: 0L) + sessionTimeMillis
        )

        dao.upsert(updated)
    }

    suspend fun getProgressForFic(ficId: String): List<ReadingProgressEntity> {
        return dao.getForFic(ficId)
    }
}
