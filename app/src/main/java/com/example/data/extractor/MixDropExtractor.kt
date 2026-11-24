package com.example.data.extractor

import com.example.data.network.HttpClientHelper
import com.example.domain.model.ExtractedVideo
import com.example.domain.model.ScraperSettings
import com.example.domain.model.ServerHost
import org.jsoup.Jsoup
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MixDrop Extractor modeled directly on Aniyomi's `MixDropExtractor`:
 * 1. Requests embed page with `Referer: https://mixdrop.co/` and desktop `User-Agent`.
 * 2. Parses Jsoup document and locates `script:containsData(eval):containsData(MDCore)`.
 * 3. Unpacks the obfuscated JS via `Unpacker`.
 * 4. Extracts `Core.wurl="..."` (with `https:` prefix) and optional `Core.remotesub="..."`
 *    decoded via `URLDecoder.decode(..., "UTF-8")`.
 */
@Singleton
class MixDropExtractor @Inject constructor(
    private val httpClient: HttpClientHelper,
    private val qualityInspector: QualityInspector
) {
    fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return SUPPORTED_DOMAINS.any { lower.contains(it) }
    }

    suspend fun extract(
        embedUrl: String,
        referer: String = DEFAULT_REFERER,
        qualityHint: String? = null,
        userAgent: String = ScraperSettings.DEFAULT_DESKTOP_USER_AGENT
    ): List<ExtractedVideo> {
        val requestHeaders = mapOf(
            "User-Agent" to userAgent,
            "Referer" to referer
        )
        val response = httpClient.get(embedUrl, requestHeaders)
        val parsed = parseEmbedHtml(response.body, response.finalUrl)
            ?: throw IllegalStateException("تعذر فك تشفير سكربت MDCore في صفحة MixDrop")

        val mixHeaders = mapOf(
            "User-Agent" to userAgent,
            "Referer" to referer
        )

        return qualityInspector.inspectMedia(
            sourcePageUrl = embedUrl,
            directUrl = parsed.videoUrl,
            host = ServerHost.MIXDROP,
            title = parsed.title.ifBlank { "MixDrop Video" },
            headers = mixHeaders,
            subtitleUrl = parsed.subtitleUrl,
            qualityHint = qualityHint
        )
    }

    fun parseEmbedHtml(html: String, baseUrl: String): MixDropParsed? {
        val doc = Jsoup.parse(html, baseUrl)
        val title = doc.selectFirst("title")?.text()?.trim().orEmpty()

        val packedScript = doc.select("script:containsData(eval):containsData(MDCore)")
            .firstOrNull()?.data()
            ?: doc.select("script").firstOrNull {
                val data = it.data()
                data.contains("eval(") && (data.contains("MDCore") || data.contains("wurl"))
            }?.data()
            ?: return null

        val unpacked = Unpacker.unpack(packedScript)
        if (!unpacked.contains("Core.wurl=\"") && !unpacked.contains("MDCore.wurl=\"")) {
            return null
        }

        val rawWurl = when {
            unpacked.contains("Core.wurl=\"") ->
                unpacked.substringAfter("Core.wurl=\"").substringBefore("\"")
            else ->
                unpacked.substringAfter("MDCore.wurl=\"").substringBefore("\"")
        }
        if (rawWurl.isBlank()) return null

        val normalizedVideoUrl = normalizeProtocol(rawWurl)

        val subtitleUrl = if (unpacked.contains("Core.remotesub=\"")) {
            val rawSub = unpacked.substringAfter("Core.remotesub=\"").substringBefore("\"")
            if (rawSub.isNotBlank()) {
                val decoded = runCatching { URLDecoder.decode(rawSub, "UTF-8") }.getOrDefault(rawSub)
                normalizeProtocol(decoded)
            } else null
        } else null

        return MixDropParsed(
            title = title,
            videoUrl = normalizedVideoUrl,
            subtitleUrl = subtitleUrl,
            unpackedScriptPreview = unpacked
        )
    }

    private fun normalizeProtocol(url: String): String = when {
        url.startsWith("//") -> "https:$url"
        url.startsWith("http://") || url.startsWith("https://") -> url
        else -> "https://$url"
    }

    data class MixDropParsed(
        val title: String,
        val videoUrl: String,
        val subtitleUrl: String?,
        val unpackedScriptPreview: String
    )

    companion object {
        const val DEFAULT_REFERER = "https://mixdrop.co/"

        val SUPPORTED_DOMAINS = listOf(
            "mixdrop.",
            "mixdroop.",
            "mdy48tn97.",
            "md3b0j6hj.",
            "mdfx9dc8n.",
            "mdzsmutpcvykb."
        )
    }
}
