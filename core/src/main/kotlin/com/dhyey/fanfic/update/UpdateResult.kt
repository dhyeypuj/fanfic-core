package com.dhyey.fanfic.update

sealed class UpdateResult {

    object Skipped : UpdateResult()
    object NoChange : UpdateResult()

    data class MetadataChanged(
        val oldWords: Int,
        val newWords: Int,
        val oldUpdated: String?,
        val newUpdated: String?
    ) : UpdateResult()

    data class NewChapters(
        val oldChapterCount: Int,
        val newChapterCount: Int
    ) : UpdateResult()
}
