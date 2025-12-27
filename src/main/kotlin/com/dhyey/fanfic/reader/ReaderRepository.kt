package com.dhyey.fanfic.reader

import com.dhyey.fanfic.repository.FanficRepository
import com.dhyey.fanfic.storage.entity.ChapterEntity

class ReaderRepository(
    private val repository: FanficRepository
) {

    suspend fun loadChapter(
        ficId: String,
        chapterNumber: Int
    ): ReaderChapter {

        val chapters = repository.getChapters(ficId)
        val chapter = chapters.firstOrNull {
            it.chapterNumber == chapterNumber
        } ?: throw IllegalArgumentException("Chapter not found")

        val html = repository.loadChapterContent(chapter)

        return ReaderChapter(
            chapterId = chapter.chapterId,
            ficId = ficId,
            chapterNumber = chapter.chapterNumber,
            title = chapter.title,
            htmlContent = html
        )
    }

    suspend fun listChapters(ficId: String): List<ChapterEntity> {
        return repository.getChapters(ficId)
    }
}
