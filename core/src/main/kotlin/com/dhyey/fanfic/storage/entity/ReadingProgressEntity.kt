package com.dhyey.fanfic.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(

    @PrimaryKey
    val chapterId: String,

    val ficId: String,
    val chapterNumber: Int,

    val lastReadPosition: Int,
    val lastReadAt: Long,

    val totalTimeSpentMillis: Long
)
