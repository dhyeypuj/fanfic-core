package com.dhyey.fanfic.reader

import com.dhyey.fanfic.storage.entity.ReadingProgressEntity

data class ReadingStats(
    val chaptersStarted: Int,
    val chaptersCompleted: Int,
    val totalTimeSpentMillis: Long
)

object ReadingStatsCalculator {

    fun calculate(
        progress: List<ReadingProgressEntity>,
        chapterLengths: Map<Int, Int>
    ): ReadingStats {

        val chaptersStarted = progress.count { it.lastReadPosition > 0 }

        val chaptersCompleted = progress.count {
            val length = chapterLengths[it.chapterNumber] ?: Int.MAX_VALUE
            it.lastReadPosition >= length
        }

        val totalTime = progress.sumOf { it.totalTimeSpentMillis }

        return ReadingStats(
            chaptersStarted = chaptersStarted,
            chaptersCompleted = chaptersCompleted,
            totalTimeSpentMillis = totalTime
        )
    }
}
