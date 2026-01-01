package com.dhyey.fanfic.model

import com.dhyey.fanfic.site.FicSite

data class FicMetadata(
    val site: FicSite,
    val url: String,
    val title: String,
    val author: String,
    val summary: String,
    val rating: String?,
    val language: String?,
    val genres: List<String>,
    val characters: List<String>,
    val chapters: Int,
    val words: Int,
    val published: String?,
    val updated: String?,
    val isComplete: Boolean = false
)

