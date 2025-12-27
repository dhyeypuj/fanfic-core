package com.dhyey.fanfic.adapter

import com.dhyey.fanfic.model.Chapter
import com.dhyey.fanfic.model.FicMetadata

interface FicAdapter {

    fun supports(url: String): Boolean

    fun parseMetadata(html: String, url: String): FicMetadata

    fun parseChapters(html: String): List<Chapter>
}
