package com.dhyey.fanfic.cache

data class CachedChapter(
    val ficId: String,
    val chapterNumber: Int,
    val path: String,
    val cachedAt: Long,
    val sizeBytes: Long
)
