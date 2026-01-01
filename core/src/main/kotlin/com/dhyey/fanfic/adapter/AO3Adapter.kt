package com.dhyey.fanfic.adapter

import com.dhyey.fanfic.model.Chapter
import com.dhyey.fanfic.model.FicMetadata
import com.dhyey.fanfic.site.FicSite
import org.jsoup.Jsoup

class AO3Adapter : FicAdapter {

    override fun supports(url: String): Boolean {
        return url.contains("archiveofourown.org") || url.contains("ao3.org")
    }

    override fun parseMetadata(html: String, url: String): FicMetadata {
        val doc = Jsoup.parse(html)

        // ---------- TITLE ----------
        // AO3 title is in the preface section: <h2 class="title heading">Title</h2>
        val title = doc.selectFirst(".preface.group h2.title")?.text()?.trim()
            ?: doc.selectFirst("h2.title.heading")?.text()?.trim()
            ?: doc.selectFirst("h2.title")?.text()?.trim()
            // Fallback: extract from page title
            ?: doc.selectFirst("title")?.text()?.substringBefore(" - ")?.trim()
            ?: "Unknown Title"

        // ---------- AUTHOR ----------
        val author = doc.selectFirst("a[rel=author]")?.text()?.trim()
            ?: doc.selectFirst(".byline a")?.text()?.trim()
            ?: "Unknown Author"

        // ---------- SUMMARY ----------
        val summary = doc.selectFirst(".summary .userstuff")?.text()?.trim()
            ?: doc.selectFirst("blockquote.userstuff")?.text()?.trim()
            ?: ""

        // ---------- RATING ----------
        val rating = doc.selectFirst("dd.rating.tags a.tag")?.text()?.trim()

        // ---------- LANGUAGE ----------
        val language = doc.selectFirst("dd.language")?.text()?.trim()

        // ---------- FANDOM / GENRES ----------
        val fandoms = doc.select("dd.fandom.tags a.tag").map { it.text().trim() }
        val freeformTags = doc.select("dd.freeform.tags a.tag").map { it.text().trim() }

        // ---------- CHARACTERS ----------
        val characters = doc.select("dd.character.tags a.tag").map { it.text().trim() }

        // ---------- WORD COUNT ----------
        val wordsText = doc.selectFirst("dd.words")?.text()?.replace(",", "")?.trim() ?: "0"
        val words = wordsText.toIntOrNull() ?: 0

        // ---------- CHAPTER COUNT ----------
        // First try to count from dropdown (more accurate for multi-chapter)
        val chapterDropdown = doc.selectFirst("select#selected_id")
        val chapters = if (chapterDropdown != null) {
            chapterDropdown.select("option").size
        } else {
            // Fallback to stats dd.chapters (format: "X/Y" or "X/?")
            val chaptersText = doc.selectFirst("dd.chapters")?.text()?.trim() ?: "1/1"
            chaptersText.substringBefore("/").toIntOrNull() ?: 1
        }

        // ---------- DATES ----------
        val published = doc.selectFirst("dd.published")?.text()?.trim()
        val updated = doc.selectFirst("dd.status")?.text()?.trim()

        return FicMetadata(
            site = FicSite.AO3,
            url = url,
            title = title,
            author = author,
            summary = summary,
            rating = rating,
            language = language,
            genres = fandoms + freeformTags,
            characters = characters,
            chapters = chapters,
            words = words,
            published = published,
            updated = updated
        )
    }

    override fun parseChapters(html: String): List<Chapter> {
        val doc = Jsoup.parse(html)

        // ---------- CHAPTER DROPDOWN (MULTI-CHAPTER) ----------
        val chapterSelect = doc.selectFirst("select#selected_id")

        if (chapterSelect != null) {
            return chapterSelect.select("option").mapIndexed { index, option ->
                val chapterId = option.attr("value")
                val chapterText = option.text().trim()
                
                // Parse title from format "1. Chapter Title" or just "Chapter 1"
                val title = chapterText
                    .substringAfter(". ", chapterText)
                    .trim()

                Chapter(
                    id = "ao3:$chapterId",
                    number = index + 1,
                    title = title.ifBlank { "Chapter ${index + 1}" }
                )
            }
        }

        // ---------- ONE-SHOT ----------
        // For one-shots, use the story title as chapter title
        val storyTitle = doc.selectFirst("h2.title.heading")?.text()?.trim()
            ?: doc.selectFirst(".preface.group h2")?.text()?.trim()
            ?: "Chapter 1"

        return listOf(
            Chapter(
                id = "ao3:1",
                number = 1,
                title = storyTitle
            )
        )
    }

    /**
     * Parses the chapter content from AO3 HTML.
     */
    fun parseChapterContent(html: String): String {
        val doc = Jsoup.parse(html)

        // Main story content - AO3 uses .userstuff.module with role="article"
        // The selector .userstuff[role=article] matches <div class="userstuff module" role="article">
        val content = doc.selectFirst("div.userstuff[role=article]")
            ?: doc.selectFirst(".userstuff.module[role=article]")
            ?: doc.selectFirst("#chapters .userstuff")
            ?: doc.selectFirst("div.userstuff")

        return content?.html() ?: ""
    }

    /**
     * Extracts series information if the work is part of a series.
     */
    fun parseSeriesInfo(html: String): SeriesInfo? {
        val doc = Jsoup.parse(html)

        val seriesElement = doc.selectFirst(".series")
            ?: doc.selectFirst("dd:contains(Part) a")
            ?: return null

        val seriesLink = seriesElement.selectFirst("a")
        val seriesName = seriesLink?.text()?.trim() ?: return null
        val seriesUrl = seriesLink.attr("href")

        // Try to parse "Part X of Series"
        val partText = doc.selectFirst(".series")?.text() ?: ""
        val partNumber = Regex("Part (\\d+)").find(partText)?.groupValues?.get(1)?.toIntOrNull()

        return SeriesInfo(
            name = seriesName,
            url = if (seriesUrl.startsWith("http")) seriesUrl else "https://archiveofourown.org$seriesUrl",
            partNumber = partNumber
        )
    }

    data class SeriesInfo(
        val name: String,
        val url: String,
        val partNumber: Int?
    )
}
