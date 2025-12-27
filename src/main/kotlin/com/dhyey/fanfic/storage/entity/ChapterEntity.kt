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
    val chapterId: String,     // e.g. "ffn:8277618:1"

    val ficOwnerId: String,

    val chapterNumber: Int,
    val title: String,

    val localPath: String?,    // where HTML/text will be cached later
    val lastReadPosition: Int  // for reading progress (future)
)
