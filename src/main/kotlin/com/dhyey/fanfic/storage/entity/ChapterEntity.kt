package com.dhyey.fanfic.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = FicEntity::class,
            parentColumns = ["ficId"],
            childColumns = ["ficOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("ficOwnerId")
    ]
)
data class ChapterEntity(

    @PrimaryKey
    val chapterId: String,

    val ficOwnerId: String,
    val chapterNumber: Int,
    val title: String,

    // ---- OFFLINE READER FIELDS ----
    val localPath: String?,          // filesystem path to cached HTML
    val cachedAt: Long?,             // when this chapter was cached

    // ---- READING PROGRESS ----
    val lastReadPosition: Int        // cursor offset
)
