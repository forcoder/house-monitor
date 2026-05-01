package com.housemonitor.service

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume

class MeituanWebView(private val context: Context, private val platformParser: PlatformParser) {

    private var webView: WebView? = null
    private var pageLoadCallback: (() -> Unit)? = null

    private val jsBridge = object {
        @JavascriptInterface
        fun onCalendarDataReceived(data: String) {
            // 处理从JavaScript返回的日历数据
        }
    }

    fun initialize(): WebView {
        webView = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = false
                displayZoomControls = false
            }

            addJavascriptInterface(jsBridge, "AndroidBridge")

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    pageLoadCallback?.invoke()
                }

                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    // 处理加载错误
                }
            }
        }

        return webView!!
    }

    fun setOnPageLoadedListener(listener: () -> Unit) {
        pageLoadCallback = listener
    }

    suspend fun loadUrl(url: String) = suspendCancellableCoroutine<Unit> { continuation ->
        pageLoadCallback = {
            if (continuation.isActive) {
                continuation.resume(Unit)
            }
        }
        webView?.loadUrl(url)
        continuation.invokeOnCancellation {
            pageLoadCallback = null
        }
    }

    suspend fun evaluateCalendarStatus(): List<String> =
        suspendCancellableCoroutine { continuation ->
            val jsCode = platformParser.buildCalendarDetectionJs()

            webView?.evaluateJavascript(jsCode) { result ->
                val dates = if (result != null && result != "null" && result.isNotEmpty()) {
                    try {
                        val jsonStr = result.removeSurrounding("\"", "\"")
                            .replace("\\\"", "\"")
                        if (jsonStr.startsWith("[") && jsonStr.endsWith("]")) {
                            JSONArray(jsonStr).let { array ->
                                List(array.length()) { index -> array.getString(index) }
                            }
                        } else {
                            emptyList()
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
                if (continuation.isActive) {
                    continuation.resume(dates)
                }
            } ?: run {
                if (continuation.isActive) {
                    continuation.resume(emptyList())
                }
            }
        }

    fun destroy() {
        webView?.destroy()
        webView = null
        pageLoadCallback = null
    }
}