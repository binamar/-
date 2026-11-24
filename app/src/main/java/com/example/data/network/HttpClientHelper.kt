package com.example.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

data class HttpResponseData(
    val requestedUrl: String,
    val finalUrl: String,
    val statusCode: Int,
    val headers: Map<String, String>,
    val body: String
) {
    fun asJsoup(): Document = Jsoup.parse(body, finalUrl)
}

data class HttpHeadData(
    val finalUrl: String,
    val statusCode: Int,
    val contentLength: Long?,
    val contentType: String?,
    val supportsRange: Boolean
)

@Singleton
class HttpClientHelper @Inject constructor(
    private val client: OkHttpClient
) {
    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): HttpResponseData = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .headers(headers.toHeaders())
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val responseHeaders = response.headers.toMultimap()
                .mapValues { (_, values) -> values.joinToString(", ") }
            val bodyString = response.body?.string().orEmpty()
            if (!response.isSuccessful && bodyString.isBlank()) {
                throw IOException("HTTP ${response.code} عند الاتصال بـ $url")
            }
            HttpResponseData(
                requestedUrl = url,
                finalUrl = response.request.url.toString(),
                statusCode = response.code,
                headers = responseHeaders,
                body = bodyString
            )
        }
    }

    suspend fun headOrProbe(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): HttpHeadData = withContext(Dispatchers.IO) {
        val headReq = Request.Builder()
            .url(url)
            .headers(headers.toHeaders())
            .head()
            .build()

        runCatching {
            client.newCall(headReq).execute().use { resp ->
                val len = resp.header("Content-Length")?.toLongOrNull()
                val type = resp.header("Content-Type")
                val acceptRanges = resp.header("Accept-Ranges")?.contains("bytes", ignoreCase = true) == true
                if (resp.isSuccessful && len != null) {
                    return@withContext HttpHeadData(
                        finalUrl = resp.request.url.toString(),
                        statusCode = resp.code,
                        contentLength = len,
                        contentType = type,
                        supportsRange = acceptRanges
                    )
                }
                // Fallback to 1-byte Range GET probe when server blocks HEAD or omits Content-Length
                if (resp.code == 405 || resp.code == 403 || len == null) {
                    return@withContext probeRangeZero(url, headers)
                }
                HttpHeadData(
                    finalUrl = resp.request.url.toString(),
                    statusCode = resp.code,
                    contentLength = len,
                    contentType = type,
                    supportsRange = acceptRanges
                )
            }
        }.getOrElse {
            probeRangeZero(url, headers)
        }
    }

    private fun probeRangeZero(url: String, headers: Map<String, String>): HttpHeadData {
        val probeHeaders = headers.toMutableMap().apply {
            put("Range", "bytes=0-0")
        }
        val req = Request.Builder()
            .url(url)
            .headers(probeHeaders.toHeaders())
            .get()
            .build()

        return client.newCall(req).execute().use { resp ->
            val contentRange = resp.header("Content-Range")
            val totalFromRange = contentRange
                ?.substringAfter("/", "")
                ?.toLongOrNull()
            val len = totalFromRange ?: resp.header("Content-Length")?.toLongOrNull()
            val supportsRange = resp.code == 206 ||
                resp.header("Accept-Ranges")?.contains("bytes", ignoreCase = true) == true
            HttpHeadData(
                finalUrl = resp.request.url.toString(),
                statusCode = resp.code,
                contentLength = len,
                contentType = resp.header("Content-Type"),
                supportsRange = supportsRange
            )
        }
    }

    companion object {
        fun getBaseUrl(url: String): String {
            return try {
                val uri = URI(url)
                val scheme = uri.scheme ?: "https"
                val host = uri.host ?: return url
                val port = if (uri.port > 0) ":${uri.port}" else ""
                "$scheme://$host$port"
            } catch (_: Exception) {
                url.substringBefore("/", url)
            }
        }
    }
}
