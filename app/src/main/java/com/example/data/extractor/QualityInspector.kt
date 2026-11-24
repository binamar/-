package com.example.data.extractor

import com.example.data.network.HttpClientHelper
import com.example.domain.model.ExtractedVideo
import com.example.domain.model.QualityInspectionResult
import com.example.domain.model.QualityPreferencePolicy
import com.example.domain.model.ResolutionBadge
import com.example.domain.model.ServerHost
import java.net.URI
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QualityInspector @Inject constructor(
    private val httpClient: HttpClientHelper
) {
    /**
     * Inspects a resolved media URL without downloading the payload:
     * - If M3U8 master playlist -> extracts every variant resolution (highlighting 360p).
     * - Otherwise executes lightweight HEAD / Range: bytes=0-0 probe for Content-Length and resumability.
     */
    suspend fun inspectMedia(
        sourcePageUrl: String,
        directUrl: String,
        host: ServerHost,
        title: String,
        headers: Map<String, String>,
        subtitleUrl: String? = null,
        qualityHint: String? = null
    ): List<ExtractedVideo> {
        val isHls = directUrl.contains(".m3u8", ignoreCase = true) ||
            host == ServerHost.DIRECT_HLS

        if (isHls) {
            val hlsVariants = runCatching {
                val playlistResp = httpClient.get(directUrl, headers)
                parseM3u8Content(
                    playlistUrl = playlistResp.finalUrl,
                    playlistContent = playlistResp.body,
                    sourcePageUrl = sourcePageUrl,
                    host = if (host == ServerHost.UNKNOWN) ServerHost.DIRECT_HLS else host,
                    title = title,
                    headers = headers,
                    subtitleUrl = subtitleUrl
                )
            }.getOrNull()

            if (!hlsVariants.isNullOrEmpty()) {
                return sortBy360pFirst(hlsVariants)
            }
        }

        // Single-file MP4/direct stream inspection via HEAD / 1-byte Range probe
        val headData = runCatching {
            httpClient.headOrProbe(directUrl, headers)
        }.getOrNull()

        // Check if HEAD revealed an m3u8 playlist
        if (headData?.contentType?.contains("mpegurl", ignoreCase = true) == true) {
            val hlsVariants = runCatching {
                val playlistResp = httpClient.get(headData.finalUrl, headers)
                parseM3u8Content(
                    playlistUrl = playlistResp.finalUrl,
                    playlistContent = playlistResp.body,
                    sourcePageUrl = sourcePageUrl,
                    host = ServerHost.DIRECT_HLS,
                    title = title,
                    headers = headers,
                    subtitleUrl = subtitleUrl
                )
            }.getOrNull()
            if (!hlsVariants.isNullOrEmpty()) {
                return sortBy360pFirst(hlsVariants)
            }
        }

        val resolvedBadge = resolveBadge(qualityHint, title, directUrl)
        val qualityResult = QualityInspectionResult(
            resolution = resolvedBadge,
            height = resolvedBadge.heightPx.takeIf { it > 0 },
            contentLengthBytes = headData?.contentLength,
            contentType = headData?.contentType ?: if (isHls) "application/vnd.apple.mpegurl" else "video/mp4",
            supportsRange = headData?.supportsRange ?: false,
            inspectionSource = when {
                headData?.contentLength != null -> "HEAD / Range Probe"
                resolvedBadge != ResolutionBadge.UNKNOWN -> "Title / Regex"
                else -> "Direct Stream"
            }
        )

        return listOf(
            ExtractedVideo(
                id = UUID.randomUUID().toString(),
                sourcePageUrl = sourcePageUrl,
                host = host,
                title = title,
                directUrl = headData?.finalUrl ?: directUrl,
                headers = headers,
                subtitleUrl = subtitleUrl,
                quality = qualityResult,
                is360pPreferred = resolvedBadge.isTarget360p
            )
        )
    }

    /**
     * Parses an M3U8 master playlist into discrete ExtractedVideo variants with exact dimensions and bitrates.
     */
    fun parseM3u8Content(
        playlistUrl: String,
        playlistContent: String,
        sourcePageUrl: String,
        host: ServerHost,
        title: String,
        headers: Map<String, String>,
        subtitleUrl: String? = null,
        isOfflineSample: Boolean = false
    ): List<ExtractedVideo> {
        if (!playlistContent.contains("#EXTM3U")) return emptyList()
        val lines = playlistContent.lines().map { it.trim() }
        val results = mutableListOf<ExtractedVideo>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                val attrs = line.substringAfter("#EXT-X-STREAM-INF:")
                val nextUriLine = lines.drop(i + 1).firstOrNull { it.isNotEmpty() && !it.startsWith("#") }
                if (nextUriLine != null) {
                    val resolvedUri = resolveRelativeUrl(playlistUrl, nextUriLine)
                    val resMatch = Regex("""RESOLUTION=(\d+)x(\d+)""").find(attrs)
                    val width = resMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
                    val height = resMatch?.groupValues?.getOrNull(2)?.toIntOrNull()
                    val bandwidth = Regex("""BANDWIDTH=(\d+)""").find(attrs)
                        ?.groupValues?.getOrNull(1)?.toLongOrNull()
                    val nameAttr = Regex("""NAME="([^"]+)"""").find(attrs)
                        ?.groupValues?.getOrNull(1)

                    val badge = when {
                        height != null -> ResolutionBadge.fromHeight(height)
                        nameAttr != null -> ResolutionBadge.fromText(nameAttr)
                        else -> ResolutionBadge.fromText(nextUriLine)
                    }

                    results.add(
                        ExtractedVideo(
                            id = UUID.randomUUID().toString(),
                            sourcePageUrl = sourcePageUrl,
                            host = host,
                            title = "$title (${badge.label})",
                            directUrl = resolvedUri,
                            headers = headers,
                            subtitleUrl = subtitleUrl,
                            quality = QualityInspectionResult(
                                resolution = badge,
                                width = width,
                                height = height ?: badge.heightPx.takeIf { it > 0 },
                                bandwidthBps = bandwidth,
                                contentType = "application/vnd.apple.mpegurl",
                                supportsRange = false,
                                inspectionSource = "M3U8 Manifest"
                            ),
                            is360pPreferred = badge.isTarget360p,
                            isOfflineSample = isOfflineSample
                        )
                    )
                }
            }
            i++
        }

        return sortBy360pFirst(results)
    }

    fun selectBestByPolicy(
        videos: List<ExtractedVideo>,
        policy: QualityPreferencePolicy
    ): ExtractedVideo? {
        if (videos.isEmpty()) return null
        return when (policy) {
            QualityPreferencePolicy.TARGET_360P_SMART_FALLBACK -> {
                videos.minWithOrNull(
                    compareBy<ExtractedVideo> { it.quality.resolution.priorityFor360p }
                        .thenBy { hostPriority(it.host) }
                )
            }
            QualityPreferencePolicy.STRICT_360P_ONLY -> {
                videos.filter { it.quality.resolution == ResolutionBadge.P360 }
                    .minByOrNull { hostPriority(it.host) }
            }
            QualityPreferencePolicy.LOWEST_DATA -> {
                videos.minWithOrNull(
                    compareBy<ExtractedVideo> {
                        val h = it.quality.resolution.heightPx
                        if (h > 0) h else Int.MAX_VALUE
                    }.thenBy { it.quality.contentLengthBytes ?: Long.MAX_VALUE }
                )
            }
            QualityPreferencePolicy.HIGHEST_AVAILABLE -> {
                videos.maxWithOrNull(
                    compareBy<ExtractedVideo> { it.quality.resolution.heightPx }
                        .thenBy { it.quality.bandwidthBps ?: 0L }
                )
            }
        }
    }

    private fun hostPriority(host: ServerHost): Int = when (host) {
        ServerHost.DOODSTREAM -> 0
        ServerHost.MIXDROP -> 1
        ServerHost.DIRECT_MP4 -> 2
        ServerHost.DIRECT_HLS -> 3
        ServerHost.UNKNOWN -> 4
    }

    private fun sortBy360pFirst(list: List<ExtractedVideo>): List<ExtractedVideo> {
        return list.sortedWith(
            compareBy<ExtractedVideo> { it.quality.resolution.priorityFor360p }
                .thenBy { it.quality.resolution.heightPx }
        )
    }

    private fun resolveBadge(vararg candidates: String?): ResolutionBadge {
        for (text in candidates) {
            val badge = ResolutionBadge.fromText(text)
            if (badge != ResolutionBadge.UNKNOWN) return badge
        }
        return ResolutionBadge.UNKNOWN
    }

    private fun resolveRelativeUrl(baseUrl: String, relative: String): String {
        if (relative.startsWith("http://") || relative.startsWith("https://")) return relative
        return try {
            URI(baseUrl).resolve(relative).toString()
        } catch (_: Exception) {
            relative
        }
    }
}
