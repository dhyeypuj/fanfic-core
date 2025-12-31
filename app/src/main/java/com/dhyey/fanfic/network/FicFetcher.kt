package com.dhyey.fanfic.network

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
        FFNAdapter()
    )

    private fun getAdapter(url: String): FicAdapter {
        return adapters.firstOrNull { it.supports(url) }
            ?: throw IllegalArgumentException("Unsupported site: $url")
    }

    suspend fun fetchHtml(url: String): String {
        // Normalize URL to use www
        val normalizedUrl = url
            .replace("m.fanfiction.net", "www.fanfiction.net")
            .replace("://fanfiction.net/", "://www.fanfiction.net/")

        // Try OkHttp first (faster)
        try {
            val html = fetchWithOkHttp(normalizedUrl)
            if (isValidHtml(html)) {
                return html
            }
        } catch (e: Exception) {
            // Fall through to WebView
        }

        // Fall back to WebView (handles Cloudflare)
        return webViewFetcher.fetchHtml(normalizedUrl)
    }

    private suspend fun fetchWithOkHttp(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .header("Sec-Fetch-User", "?1")
            .header("Upgrade-Insecure-Requests", "1")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}")
            }
            response.body?.string() ?: throw IllegalStateException("Empty response")
        }
    }

    private fun isValidHtml(html: String): Boolean {
        return html.contains("<title>") && 
               html.contains("profile_top") && 
               !html.contains("challenge-running") &&
               !html.contains("cf-spinner") &&
               !html.contains("Just a moment")
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

    suspend fun fetchChapterContent(baseUrl: String, chapterNumber: Int): String {
        val chapterUrl = buildChapterUrl(baseUrl, chapterNumber)
        return fetchHtml(chapterUrl)
    }

    private fun buildChapterUrl(baseUrl: String, chapterNumber: Int): String {
        // FFN URL pattern: https://www.fanfiction.net/s/{id}/{chapter}/{title}
        val parts = baseUrl.trimEnd('/').split("/")
        val sIndex = parts.indexOf("s")
        if (sIndex != -1 && sIndex + 1 < parts.size) {
            val base = parts.take(sIndex + 2).joinToString("/")
            return "$base/$chapterNumber"
        }
        return baseUrl
    }
}

