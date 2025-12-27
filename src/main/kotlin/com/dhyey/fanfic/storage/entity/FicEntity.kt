package com.dhyey.fanfic.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fics")
data class FicEntity(

    @PrimaryKey
    val ficId: String,         // e.g. "ffn:8277618"

    val site: String,          // "FFN", "AO3"
    val url: String,

    val title: String,
    val author: String,

    val chapters: Int,
    val words: Int,

    val published: String?,
    val updated: String?,

    val lastChecked: Long      // epoch millis
)
