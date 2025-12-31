package com.dhyey.fanfic.network

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * WebView-based fetcher that can handle Cloudflare JavaScript challenges.
 * Falls back to this when OkHttp gets blocked.
 */
@Singleton
class WebViewFetcher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun fetchHtml(url: String, timeoutMs: Long = 30000): String = 
        suspendCancellableCoroutine { continuation ->
            mainHandler.post {
                val webView = WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        blockNetworkImage = true // Speed up loading
                    }
                }
                
                // Enable cookies
                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(webView, true)
                }

                var isCompleted = false
                var retryCount = 0
                val maxRetries = 3

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, pageUrl: String) {
                        if (isCompleted) return

                        // Extract HTML content after page loads
                        view.evaluateJavascript(
                            "(function() { return document.documentElement.outerHTML; })();"
                        ) { html ->
                            if (isCompleted) return@evaluateJavascript
                            
                            // Unescape the JavaScript string
                            val content = html
                                ?.trim('"')
                                ?.replace("\\u003C", "<")
                                ?.replace("\\u003E", ">")
                                ?.replace("\\n", "\n")
                                ?.replace("\\\"", "\"")
                                ?.replace("\\\\", "\\")
                                ?: ""

                            // Check if it's a challenge page (Cloudflare)
                            if (content.contains("challenge-running") || 
                                content.contains("cf-spinner") ||
                                content.contains("Just a moment")) {
                                // Still loading challenge, wait
                                retryCount++
                                if (retryCount >= maxRetries) {
                                    isCompleted = true
                                    webView.destroy()
                                    continuation.resumeWithException(
                                        IllegalStateException("Cloudflare challenge could not be solved")
                                    )
                                }
                                return@evaluateJavascript
                            }

                            // Check for valid content
                            if (content.contains("<title>") || content.contains("profile_top")) {
                                isCompleted = true
                                webView.destroy()
                                continuation.resume(content)
                            } else if (retryCount < maxRetries) {
                                retryCount++
                                // Wait a bit and try again
                            } else {
                                isCompleted = true
                                webView.destroy()
                                continuation.resumeWithException(
                                    IllegalStateException("Failed to load valid page content")
                                )
                            }
                        }
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        return false // Allow redirects
                    }
                }

                // Start loading
                webView.loadUrl(url)

                // Set timeout
                mainHandler.postDelayed({
                    if (!isCompleted) {
                        isCompleted = true
                        webView.destroy()
                        continuation.resumeWithException(
                            IllegalStateException("WebView fetch timeout after ${timeoutMs}ms")
                        )
                    }
                }, timeoutMs)

                // Handle cancellation
                continuation.invokeOnCancellation {
                    mainHandler.post {
                        if (!isCompleted) {
                            isCompleted = true
                            webView.destroy()
                        }
                    }
                }
            }
        }
}
