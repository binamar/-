package com.example.data.extractor

import com.example.data.network.HttpClientHelper
import com.example.domain.model.ExtractedVideo
import com.example.domain.model.ScraperSettings
import com.example.domain.model.ServerHost
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Doodstream Extractor modeled directly on Aniyomi's EgyDead `DoodExtractor`:
 * 1. Fetches embed page and resolves redirected finalUrl + doodHost (`getBaseUrl`).
 * 2. Inspects `<title>` for `\d{3,4}p` resolution tag if present.
 * 3. Extracts `/pass_md5/[^']*` path and trailing `token`.
 * 4. Generates 10-char random hash (`createHashTable`) + `expiry` timestamp.
 * 5. Calls `pass_md5` endpoint with `Referer: finalUrl` and builds final stream URL + `doodHeaders`.
 */
@Singleton
class DoodExtractor @Inject constructor(
    private val httpClient: HttpClientHelper,
    private val qualityInspector: QualityInspector
) {
    private val titleQualityRegex = Regex("""\d{3,4}p""", RegexOption.IGNORE_CASE)
    private val passMd5Regex = Regex("""/pass_md5/[^'"]+""")

    fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return SUPPORTED_DOMAINS.any { lower.contains(it) }
    }

    suspend fun extract(
        embedUrl: String,
        qualityHint: String? = null,
        userAgent: String = ScraperSettings.DEFAULT_DESKTOP_USER_AGENT
    ): List<ExtractedVideo> {
        val initialHeaders = mapOf(
            "User-Agent" to userAgent,
            "Accept-Language" to "ar,en-US;q=0.9,en;q=0.8"
        )
        val pageResponse = httpClient.get(embedUrl, initialHeaders)
        val newUrl = pageResponse.finalUrl
        val doodHost = HttpClientHelper.getBaseUrl(newUrl)

        val parsed = parseEmbedHtml(pageResponse.body, newUrl)
            ?: throw IllegalStateException("لم يتم العثور على مسار pass_md5 داخل صفحة Doodstream")

        val md5Endpoint = "$doodHost${parsed.md5Path}"
        val md5Response = httpClient.get(
            url = md5Endpoint,
            headers = mapOf(
                "User-Agent" to userAgent,
                "Referer" to newUrl
            )
        )

        val randomHash = createHashTable(10)
        val expiry = System.currentTimeMillis()
        val finalDirectUrl = "${md5Response.body.trim()}$randomHash?token=${parsed.token}&expiry=$expiry"
        val doodHeaders = buildDoodHeaders(doodHost, userAgent)
        val resolvedTitle = parsed.pageTitle.ifBlank { "Doodstream Video" }

        return qualityInspector.inspectMedia(
            sourcePageUrl = embedUrl,
            directUrl = finalDirectUrl,
            host = ServerHost.DOODSTREAM,
            title = resolvedTitle,
            headers = doodHeaders,
            qualityHint = parsed.titleResolution ?: qualityHint
        )
    }

    fun parseEmbedHtml(html: String, baseUrl: String): DoodEmbedParsed? {
        val doc = Jsoup.parse(html, baseUrl)
        val pageTitle = doc.title().trim()
        val titleResolution = titleQualityRegex.find(pageTitle)?.value
        val md5Path = passMd5Regex.find(html)?.value ?: return null
        val token = md5Path.substringAfterLast("/")
        return DoodEmbedParsed(
            pageTitle = pageTitle,
            titleResolution = titleResolution,
            md5Path = md5Path,
            token = token
        )
    }

    fun createHashTable(length: Int = 10): String {
        return buildString(length) {
            repeat(length) {
                append(HASH_ALPHABET[Random.nextInt(HASH_ALPHABET.length)])
            }
        }
    }

    fun buildDoodHeaders(doodHost: String, userAgent: String): Map<String, String> = mapOf(
        "User-Agent" to userAgent,
        "Referer" to "$doodHost/"
    )

    data class DoodEmbedParsed(
        val pageTitle: String,
        val titleResolution: String?,
        val md5Path: String,
        val token: String
    )

    companion object {
        private const val HASH_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        val SUPPORTED_DOMAINS = listOf(
            "dood.",
            "d000d.",
            "d0000d.",
            "ds2play.",
            "ds2video.",
            "do0od.",
            "dooood.",
            "doodstream.",
            "doods.",
            "vidply.",
            "myvidplay."
        )
    }
}
