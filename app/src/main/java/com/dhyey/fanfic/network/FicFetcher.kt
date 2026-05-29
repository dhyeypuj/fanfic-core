package com.dhyey.fanfic.network

import com.dhyey.fanfic.adapter.AO3Adapter
import com.dhyey.fanfic.adapter.FFNAdapter
import com.dhyey.fanfic.adapter.FicAdapter
import com.dhyey.fanfic.model.Chapter
import com.dhyey.fanfic.model.FicMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class FicFetcher @Inject constructor(
    private val client: OkHttpClient,
    private val webViewFetcher: WebViewFetcher
) {
    private val adapters: List<FicAdapter> = listOf(
        FFNAdapter(),
        AO3Adapter()
    )

    private fun getAdapter(url: String): FicAdapter {
        return adapters.firstOrNull { it.supports(url) }
            ?: throw IllegalArgumentException("Unsupported site: $url")
    }

    suspend fun fetchHtml(url: String): String {
        val normalizedUrl = normalizeUrl(url)

        // Try OkHttp first (faster)
        try {
            val html = fetchWithOkHttp(normalizedUrl)
            if (isValidHtml(html, normalizedUrl)) {
                return html
            }
        } catch (e: Exception) {
            // Fall through to WebView
        }

        // Fall back to WebView (handles Cloudflare)
        return webViewFetcher.fetchHtml(normalizedUrl)
    }

    private fun normalizeUrl(url: String): String {
        return when {
            // FFN normalization
            url.contains("fanfiction.net") -> url
                .replace("m.fanfiction.net", "www.fanfiction.net")
                .replace("://fanfiction.net/", "://www.fanfiction.net/")
            
            // AO3 normalization
            url.contains("ao3.org") -> url
                .replace("ao3.org", "archiveofourown.org")
            
            else -> url
        }
    }

    private suspend fun fetchWithOkHttp(url: String): String = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .header("Sec-Fetch-User", "?1")
            .header("Upgrade-Insecure-Requests", "1")

        if (url.contains("archiveofourown.org")) {
            requestBuilder.header("Cookie", "accepted_tos=20180510; view_adult=true")
        }

        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}")
            }
            response.body?.string() ?: throw IllegalStateException("Empty response")
        }
    }

    private fun isValidHtml(html: String, url: String): Boolean {
        // Common validity checks
        if (!html.contains("<title>") ||
            html.contains("challenge-running") ||
            html.contains("cf-spinner") ||
            html.contains("Just a moment")) {
            return false
        }

        return when {
            // FFN-specific checks
            url.contains("fanfiction.net") -> html.contains("profile_top")
            
            // AO3-specific checks
            url.contains("archiveofourown.org") -> 
                html.contains("workskin") || html.contains("preface group")
            
            else -> true
        }
    }

    suspend fun fetchMetadata(url: String): FicMetadata {
        val html = fetchHtml(url)
        val adapter = getAdapter(url)
        return adapter.parseMetadata(html, url)
    }

    suspend fun fetchChapters(url: String): List<Chapter> {
        val html = fetchHtml(url)
        val adapter = getAdapter(url)
        return adapter.parseChapters(html)
    }

    /**
     * Fetch chapter content. For AO3, pass the full chapterId (e.g., "ao3:32705163")
     * to navigate to a specific chapter. For FFN, only chapterNumber is needed.
     */
    suspend fun fetchChapterContent(baseUrl: String, chapterNumber: Int, chapterId: String? = null): String {
        var resolvedChapterId = chapterId
        if (baseUrl.contains("archiveofourown.org") && (chapterId == null || !chapterId.startsWith("ao3:"))) {
            try {
                val mainHtml = fetchHtml(baseUrl)
                val doc = org.jsoup.Jsoup.parse(mainHtml)
                val chapterSelect = doc.selectFirst("select#selected_id")
                if (chapterSelect != null) {
                    val options = chapterSelect.select("option")
                    val option = options.getOrNull(chapterNumber - 1)
                    if (option != null) {
                        val numericId = option.attr("value")
                        resolvedChapterId = "ao3:$numericId"
                    }
                }
            } catch (e: Exception) {
                // Fallback to passed chapterId
            }
        }
        val chapterUrl = buildChapterUrl(baseUrl, chapterNumber, resolvedChapterId)
        return fetchHtml(chapterUrl)
    }


    private fun buildChapterUrl(baseUrl: String, chapterNumber: Int, chapterId: String? = null): String {
        return when {
            // FFN URL pattern: https://www.fanfiction.net/s/{id}/{chapter}/{title}
            baseUrl.contains("fanfiction.net") -> {
                val parts = baseUrl.trimEnd('/').split("/")
                val sIndex = parts.indexOf("s")
                if (sIndex != -1 && sIndex + 1 < parts.size) {
                    val base = parts.take(sIndex + 2).joinToString("/")
                    "$base/$chapterNumber"
                } else {
                    baseUrl
                }
            }
            
            // AO3 URL pattern: https://archiveofourown.org/works/{workId}/chapters/{chapterId}
            baseUrl.contains("archiveofourown.org") -> {
                val workIdMatch = Regex("/works/(\\d+)").find(baseUrl)
                val workId = workIdMatch?.groupValues?.get(1)
                
                if (workId != null && chapterId != null) {
                    // Extract the numeric chapter ID from "ao3:32705163" format
                    val numericChapterId = chapterId.removePrefix("ao3:")
                    "https://archiveofourown.org/works/$workId/chapters/$numericChapterId?view_adult=true"
                } else if (workId != null) {
                    // No chapter ID - fetch the first chapter / work page
                    "https://archiveofourown.org/works/$workId?view_adult=true"
                } else {
                    baseUrl
                }
            }
            
            else -> baseUrl
        }
    }

    /**
     * Parses chapter content from the fetched HTML.
     * Uses the appropriate adapter based on the URL.
     */
    fun parseChapterContent(html: String, url: String): String {
        return when {
            url.contains("archiveofourown.org") -> {
                (getAdapter(url) as? AO3Adapter)?.parseChapterContent(html) ?: ""
            }
            url.contains("fanfiction.net") -> {
                // FFN content is in #storytext
                val doc = org.jsoup.Jsoup.parse(html)
                doc.selectFirst("#storytext")?.html() ?: ""
            }
            else -> ""
        }
    }
}


