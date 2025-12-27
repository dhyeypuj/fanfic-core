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
            .substringBefore(", a ")
            .substringBefore(" Chapter")
            .substringBefore(" | FanFiction")
            .trim()

        // ---------- AUTHOR ----------
        val profileTop = doc.selectFirst("#profile_top")
            ?: throw IllegalStateException("profile_top not found")

        val author = profileTop
            .select("a.xcontrast_txt")
            .firstOrNull()
            ?.text()
            ?.trim()
            ?: throw IllegalStateException("Author not found")

        val metaText = profileTop.text()

        val chapters = extractInt(metaText, "Chapters:").takeIf { it > 0 } ?: 1
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
        val doc = Jsoup.parse(html)

        val chapterSelect = doc.selectFirst("select#chap_select")

        // ---------- MULTI-CHAPTER ----------
        if (chapterSelect != null) {
            return chapterSelect.select("option").map { option ->
                val number = option.attr("value").toInt()
                val title = option.text()
                    .substringAfter(". ", option.text())
                    .trim()

                Chapter(
                    id = "ffn:$number",
                    number = number,
                    title = title
                )
            }
        }

        // ---------- ONE-SHOT ----------
        val rawTitle = doc.selectFirst("title")?.text()
            ?: throw IllegalStateException("HTML <title> tag not found")

        val storyTitle = rawTitle
            .substringBefore(", a ")
            .substringBefore(" | FanFiction")
            .trim()

        return listOf(
            Chapter(
                id = "ffn:1",
                number = 1,
                title = storyTitle
            )
        )
    }

    // ---------- HELPERS ----------

    private fun extractInt(text: String, label: String): Int {
        val start = text.indexOf(label)
        if (start == -1) return 0

        return text.substring(start + label.length)
            .trim()
            .substringBefore(" ")
            .replace(",", "")
            .toIntOrNull() ?: 0
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
