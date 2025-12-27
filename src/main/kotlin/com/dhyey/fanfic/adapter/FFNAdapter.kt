package com.dhyey.fanfic.adapter

import com.dhyey.fanfic.model.Chapter
import com.dhyey.fanfic.model.FicMetadata
import com.dhyey.fanfic.site.FicSite
import org.jsoup.Jsoup

class FFNAdapter : FicAdapter {

    override fun supports(url: String): Boolean {
        return url.contains("fanfiction.net")
    }

    override fun parseMetadata(html: String, url: String): FicMetadata {
        val doc = Jsoup.parse(html)

        // ---------- TITLE ----------
        val rawTitle = doc.selectFirst("title")?.text()
            ?: throw IllegalStateException("HTML <title> tag not found")

        val title = rawTitle
            .substringBefore(" Chapter")
            .substringBefore(" | FanFiction")
            .trim()

        // ---------- AUTHOR ----------
        val profileTop = doc.selectFirst("#profile_top")
            ?: throw IllegalStateException("profile_top section not found")

        val author = profileTop
            .select("a.xcontrast_txt")
            .firstOrNull()
            ?.text()
            ?.trim()
            ?: throw IllegalStateException("Author not found")

        // ---------- METADATA LINE ----------
        val metaText = profileTop.text()

        val chapters = extractInt(metaText, "Chapters:")
        val words = extractInt(metaText, "Words:")

        val published = extractDate(metaText, "Published:")
        val updated = extractDate(metaText, "Updated:")

        return FicMetadata(
            site = FicSite.FFN,
            url = url,
            title = title,
            author = author,
            summary = "",
            rating = null,
            language = null,
            genres = emptyList(),
            characters = emptyList(),
            chapters = chapters,
            words = words,
            published = published,
            updated = updated
        )
    }

    override fun parseChapters(html: String): List<Chapter> {
        throw NotImplementedError("Chapter parsing not implemented yet")
    }

    // ---------- HELPERS ----------

    private fun extractInt(text: String, label: String): Int {
        val start = text.indexOf(label)
        if (start == -1) return 0

        val valuePart = text.substring(start + label.length)
            .trim()
            .substringBefore(" ")
            .replace(",", "")

        return valuePart.toIntOrNull() ?: 0
    }

    private fun extractDate(text: String, label: String): String? {
        val start = text.indexOf(label)
        if (start == -1) return null

        return text.substring(start + label.length)
            .trim()
            .substringBefore(" -")
            .trim()
    }
}
