package com.dhyey.fanfic.repository

import com.dhyey.fanfic.cache.CacheCleaner
import com.dhyey.fanfic.cache.ChapterCache
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
    private val chapterDao: ChapterDao,
    private val chapterCache: ChapterCache,
    private val cacheCleaner: CacheCleaner
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
                cachedAt = null,
                lastReadPosition = 0
            )
        }

        chapterDao.deleteChaptersForFic(ficId)
        chapterDao.upsertChapters(chapterEntities)
    }

    suspend fun cacheChapterContent(
        ficId: String,
        chapterNumber: Int,
        html: String,
        maxCacheSizeBytes: Long
    ) {
        val path = chapterCache.writeChapter(ficId, chapterNumber, html)

        val chapters = chapterDao.getChaptersForFic(ficId)
        val chapter = chapters.first { it.chapterNumber == chapterNumber }

        chapterDao.upsertChapters(
            listOf(
                chapter.copy(
                    localPath = path,
                    cachedAt = System.currentTimeMillis()
                )
            )
        )

        cacheCleaner.evictIfNeeded(maxCacheSizeBytes)
    }

    suspend fun cleanOrphanedCache(ficId: String) {
        val validPaths = chapterDao.getChaptersForFic(ficId)
            .mapNotNull { it.localPath }
            .toSet()

        cacheCleaner.removeOrphanedChapters(validPaths)
    }

    suspend fun loadChapterContent(chapter: ChapterEntity): String {
        return chapter.localPath?.let {
            chapterCache.readChapter(it)
        } ?: throw IllegalStateException("Chapter not cached")
    }

    suspend fun deleteFic(ficId: String) {
        chapterCache.deleteFicCache(ficId)
        ficDao.deleteFic(ficId)
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

    suspend fun clearChapterCache(ficId: String, chapterNumber: Int) {
        val chapters = chapterDao.getChaptersForFic(ficId)
        val chapter = chapters.firstOrNull { it.chapterNumber == chapterNumber } ?: return
        
        chapter.localPath?.let { path ->
            chapterCache.deleteChapter(path)
        }
        
        chapterDao.upsertChapters(
            listOf(chapter.copy(localPath = null, cachedAt = null))
        )
    }

    suspend fun clearAllOfflineCache(ficId: String) {
        chapterCache.deleteFicCache(ficId)
        
        val chapters = chapterDao.getChaptersForFic(ficId)
        val clearedChapters = chapters.map { it.copy(localPath = null, cachedAt = null) }
        chapterDao.upsertChapters(clearedChapters)
    }

    suspend fun saveReadingPosition(ficId: String, chapterNumber: Int, position: Int) {
        val chapters = chapterDao.getChaptersForFic(ficId)
        val chapter = chapters.firstOrNull { it.chapterNumber == chapterNumber } ?: return
        chapterDao.upsertChapters(listOf(chapter.copy(lastReadPosition = position)))
    }

    suspend fun getReadingPosition(ficId: String, chapterNumber: Int): Int {
        val chapters = chapterDao.getChaptersForFic(ficId)
        return chapters.firstOrNull { it.chapterNumber == chapterNumber }?.lastReadPosition ?: 0
    }
}
