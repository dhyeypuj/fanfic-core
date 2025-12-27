package com.dhyey.fanfic.reader

class ReaderEngine(
    private val ficId: String,
    private val readerRepository: ReaderRepository
) {

    private var currentChapterNumber: Int = 1

    suspend fun openChapter(chapterNumber: Int): ReaderChapter {
        currentChapterNumber = chapterNumber
        return readerRepository.loadChapter(ficId, chapterNumber)
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

    suspend fun getCurrentChapter(): ReaderChapter {
        return readerRepository.loadChapter(ficId, currentChapterNumber)
    }
}
