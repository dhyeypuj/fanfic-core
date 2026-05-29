package com.dhyey.fanfic.ui.library

/**
 * Sorting options for the library
 */
enum class SortOption(val displayName: String) {
    LAST_READ("Last Read"),
    LAST_UPDATED("Last Updated"),
    WORD_COUNT("Word Count"),
    CHAPTER_COUNT("Chapters"),
    ALPHABETICAL("Title (A-Z)"),
    DATE_ADDED("Date Added")
}

/**
 * Filter by fic source
 */
enum class SourceFilter(val displayName: String) {
    ALL("All Sources"),
    FFN("FanFiction.net"),
    AO3("AO3")
}

/**
 * Filter by completion status
 */
enum class StatusFilter(val displayName: String) {
    ALL("All"),
    COMPLETE("Complete"),
    ONGOING("In Progress")
}

/**
 * Current filter/sort state for the library
 */
data class LibraryFilters(
    val query: String = "",
    val sort: SortOption = SortOption.DATE_ADDED,
    val sortAscending: Boolean = false,  // false = descending (newest/highest first)
    val source: SourceFilter = SourceFilter.ALL,
    val status: StatusFilter = StatusFilter.ALL
)
