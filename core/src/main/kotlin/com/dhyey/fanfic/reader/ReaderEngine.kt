package com.dhyey.fanfic.reader

class ReaderEngine(
    private val ficId: String,
    private val readerRepository: ReaderRepository,
    private val progressRepository: ProgressRepository
) {

    private var currentChapterNumber: Int = 1
    private var sessionStartTime: Long = 0L

    suspend fun openChapter(chapterNumber: Int): ReaderChapter {
        endSessionIfActive()
        currentChapterNumber = chapterNumber
        sessionStartTime = System.currentTimeMillis()
        return readerRepository.loadChapter(ficId, chapterNumber)
    }

    suspend fun updateReadingPosition(
        chapter: ReaderChapter,
        newPosition: Int
    ) {
        val sessionTime = System.currentTimeMillis() - sessionStartTime

        progressRepository.updateProgress(
            chapterId = chapter.chapterId,
            ficId = ficId,
            chapterNumber = chapter.chapterNumber,
            newPosition = newPosition,
            sessionTimeMillis = sessionTime
        )

        sessionStartTime = System.currentTimeMillis()
    }

    suspend fun nextChapter(): ReaderChapter {
        return openChapter(currentChapterNumber + 1)
    }

    suspend fun previousChapter(): ReaderChapter {
        if (currentChapterNumber <= 1) {
            throw IllegalStateException("Already at first chapter")
        }
        return openChapter(currentChapterNumber - 1)
    }

    private suspend fun endSessionIfActive() {
        // No-op for now; future analytics hook
    }
}
