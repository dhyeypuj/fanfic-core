package com.dhyey.fanfic.cache

import java.io.File

class ChapterCache(
    private val baseDir: File
) {

    fun writeChapter(
        ficId: String,
        chapterNumber: Int,
        html: String
    ): String {
        val ficDir = File(baseDir, ficId).apply {
            if (!exists()) mkdirs()
        }

        val file = File(ficDir, "chapter_$chapterNumber.html")
        file.writeText(html)

        return file.absolutePath
    }

    fun readChapter(localPath: String): String {
        return File(localPath).readText()
    }

    fun deleteFicCache(ficId: String) {
        File(baseDir, ficId).deleteRecursively()
    }

    // ---------- INSPECTION ----------

    fun listCachedChapters(): List<CachedChapter> {
        if (!baseDir.exists()) return emptyList()

        return baseDir.listFiles()
            ?.flatMap { ficDir ->
                ficDir.listFiles()?.mapNotNull { file ->
                    val chapterNumber = file.name
                        .removePrefix("chapter_")
                        .removeSuffix(".html")
                        .toIntOrNull()
                        ?: return@mapNotNull null

                    CachedChapter(
                        ficId = ficDir.name,
                        chapterNumber = chapterNumber,
                        path = file.absolutePath,
                        cachedAt = file.lastModified(),
                        sizeBytes = file.length()
                    )
                } ?: emptyList()
            } ?: emptyList()
    }

    fun deleteChapter(path: String) {
        File(path).delete()
    }

    fun totalCacheSizeBytes(): Long {
        return listCachedChapters().sumOf { it.sizeBytes }
    }
}
