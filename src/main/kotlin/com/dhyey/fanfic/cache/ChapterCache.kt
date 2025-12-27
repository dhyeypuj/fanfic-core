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
}
