package com.dhyey.fanfic.reader

data class ReaderChapter(
    val chapterId: String,
    val ficId: String,
    val chapterNumber: Int,
    val title: String,
    val htmlContent: String
)
