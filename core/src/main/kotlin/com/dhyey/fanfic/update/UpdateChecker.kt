package com.dhyey.fanfic.update

import com.dhyey.fanfic.model.FicMetadata
import com.dhyey.fanfic.storage.entity.FicEntity
import kotlin.math.abs

class UpdateChecker {

    companion object {
        private const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L
    }

    fun shouldCheck(now: Long, lastChecked: Long): Boolean {
        return abs(now - lastChecked) >= ONE_DAY_MILLIS
    }

    fun checkForUpdate(
        stored: FicEntity,
        fresh: FicMetadata,
        now: Long
    ): UpdateResult {

        if (!shouldCheck(now, stored.lastChecked)) {
            return UpdateResult.Skipped
        }

        // New chapters always win
        if (fresh.chapters > stored.chapters) {
            return UpdateResult.NewChapters(
                oldChapterCount = stored.chapters,
                newChapterCount = fresh.chapters
            )
        }

        // Metadata changes (word count or updated date)
        if (fresh.words != stored.words || fresh.updated != stored.updated) {
            return UpdateResult.MetadataChanged(
                oldWords = stored.words,
                newWords = fresh.words,
                oldUpdated = stored.updated,
                newUpdated = fresh.updated
            )
        }

        return UpdateResult.NoChange
    }
}
