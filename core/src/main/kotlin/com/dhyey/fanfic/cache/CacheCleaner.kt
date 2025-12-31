package com.dhyey.fanfic.cache

class CacheCleaner(
    private val chapterCache: ChapterCache
) {

    fun evictIfNeeded(
        maxCacheSizeBytes: Long
    ) {
        val cached = chapterCache.listCachedChapters()
            .sortedBy { it.cachedAt } // oldest first

        var currentSize = cached.sumOf { it.sizeBytes }

        if (currentSize <= maxCacheSizeBytes) return

        for (chapter in cached) {
            chapterCache.deleteChapter(chapter.path)
            currentSize -= chapter.sizeBytes

            if (currentSize <= maxCacheSizeBytes) break
        }
    }

    fun removeOrphanedChapters(
        validChapterPaths: Set<String>
    ) {
        val cached = chapterCache.listCachedChapters()

        cached
            .filter { it.path !in validChapterPaths }
            .forEach { chapterCache.deleteChapter(it.path) }
    }
}
