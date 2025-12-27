package com.dhyey.fanfic.repository

import com.dhyey.fanfic.model.Chapter
import com.dhyey.fanfic.model.FicMetadata
import com.dhyey.fanfic.storage.dao.ChapterDao
import com.dhyey.fanfic.storage.dao.FicDao
import com.dhyey.fanfic.storage.entity.ChapterEntity
import com.dhyey.fanfic.storage.entity.FicEntity
import com.dhyey.fanfic.update.UpdateChecker
import com.dhyey.fanfic.update.UpdateResult

class FanficRepository(
    private val ficDao: FicDao,
    private val chapterDao: ChapterDao
) {

    private val updateChecker = UpdateChecker()

    suspend fun saveFic(
        ficId: String,
        metadata: FicMetadata,
        chapters: List<Chapter>
    ) {
        val ficEntity = FicEntity(
            ficId = ficId,
            site = metadata.site.name,
            url = metadata.url,
            title = metadata.title,
            author = metadata.author,
            chapters = metadata.chapters,
            words = metadata.words,
            published = metadata.published,
            updated = metadata.updated,
            lastChecked = System.currentTimeMillis()
        )

        ficDao.upsertFic(ficEntity)

        val chapterEntities = chapters.map { chapter ->
            ChapterEntity(
                chapterId = "$ficId:${chapter.number}",
                ficOwnerId = ficId,
                chapterNumber = chapter.number,
                title = chapter.title,
                localPath = null,
                lastReadPosition = 0
            )
        }

        chapterDao.deleteChaptersForFic(ficId)
        chapterDao.upsertChapters(chapterEntities)
    }

    suspend fun checkForUpdates(
        ficId: String,
        freshMetadata: FicMetadata,
        now: Long = System.currentTimeMillis()
    ): UpdateResult {

        val stored = ficDao.getFicById(ficId)
            ?: return UpdateResult.NoChange

        val result = updateChecker.checkForUpdate(stored, freshMetadata, now)

        if (result !is UpdateResult.Skipped) {
            ficDao.upsertFic(
                stored.copy(
                    chapters = freshMetadata.chapters,
                    words = freshMetadata.words,
                    updated = freshMetadata.updated,
                    lastChecked = now
                )
            )
        }

        return result
    }

    suspend fun getFic(ficId: String): FicEntity? =
        ficDao.getFicById(ficId)

    suspend fun getChapters(ficId: String): List<ChapterEntity> =
        chapterDao.getChaptersForFic(ficId)

    suspend fun deleteFic(ficId: String) {
        ficDao.deleteFic(ficId)
    }
}
