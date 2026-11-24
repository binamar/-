package com.example.data.extractor

import com.example.data.network.HttpClientHelper
import com.example.domain.model.EpisodeCandidate
import com.example.domain.model.ExtractedVideo
import com.example.domain.model.QualityInspectionResult
import com.example.domain.model.QualityPreferencePolicy
import com.example.domain.model.ResolutionBadge
import com.example.domain.model.ScrapeInspectionReport
import com.example.domain.model.ScrapedServerCandidate
import com.example.domain.model.ScraperSettings
import com.example.domain.model.ServerHost
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jsoup.nodes.Document
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EgyDeadScraper @Inject constructor(
    private val httpClient: HttpClientHelper,
    private val doodExtractor: DoodExtractor,
    private val mixDropExtractor: MixDropExtractor,
    private val qualityInspector: QualityInspector
) {
    private val embeddedHostUrlRegex = Regex(
        """https?://(?:www\.)?(?:[a-z0-9-]+\.)?(?:dood[a-z0-9]*|d000+d|ds2play|ds2video|vidply|myvidplay|mixdrop[a-z0-9]*|mdy48tn97)\.[a-z]{2,}/[^\s"'<>\\]+""",
        RegexOption.IGNORE_CASE
    )
    private val directMediaRegex = Regex(
        """https?://[^\s"'<>\\]+\.(?:m3u8|mp4|mkv)(?:\?[^\s"'<>\\]*)?""",
        RegexOption.IGNORE_CASE
    )

    suspend fun inspectAndResolve(
        inputUrl: String,
        policy: QualityPreferencePolicy,
        userAgent: String = ScraperSettings.DEFAULT_DESKTOP_USER_AGENT
    ): ScrapeInspectionReport = coroutineScope {
        val startMs = System.currentTimeMillis()
        val cleanUrl = inputUrl.trim()
        val diagnostics = mutableListOf<String>()

        // 1. Check if inputUrl is directly an embed host or media stream
        val directHost = classifyHost(cleanUrl)
        if (directHost != ServerHost.UNKNOWN) {
            diagnostics.add("تم التعرف مباشرة على الخادم: ${directHost.displayName}")
            val candidate = ScrapedServerCandidate(
                serverName = directHost.displayName,
                serverUrl = cleanUrl,
                hostHint = directHost,
                qualityHint = ResolutionBadge.fromText(cleanUrl).takeIf { it != ResolutionBadge.UNKNOWN }?.label
            )
            val resolved = resolveCandidate(candidate, userAgent, diagnostics)
            val recommended = qualityInspector.selectBestByPolicy(resolved, policy)
            return@coroutineScope ScrapeInspectionReport(
                inputUrl = cleanUrl,
                pageTitle = resolved.firstOrNull()?.title ?: directHost.displayName,
                candidates = listOf(candidate),
                resolvedVideos = resolved,
                recommendedVideo = recommended,
                diagnostics = diagnostics,
                durationMs = System.currentTimeMillis() - startMs
            )
        }

        // 2. Otherwise fetch and parse HTML watch / episode / series page via OkHttp + Jsoup
        diagnostics.add("جاري سحب الصفحة عبر OkHttp: $cleanUrl")
        val pageResp = httpClient.get(
            url = cleanUrl,
            headers = mapOf(
                "User-Agent" to userAgent,
                "Accept-Language" to "ar,en-US;q=0.9,en;q=0.8"
            )
        )
        val doc = pageResp.asJsoup()
        val pageTitle = extractPageTitle(doc)
        val posterUrl = extractPosterUrl(doc)
        val episodeLinks = extractEpisodeLinks(doc)
        val candidates = extractServerCandidates(doc, pageResp.body)

        diagnostics.add("تم تحليل HTML: العنوان «$pageTitle» | السيرفرات المكتشفة: ${candidates.size} | الحلقات: ${episodeLinks.size}")

        // 3. Resolve discovered server candidates concurrently (bounded to 3)
        val semaphore = Semaphore(3)
        val resolvedVideos = candidates.map { candidate ->
            async {
                semaphore.withPermit {
                    resolveCandidate(candidate, userAgent, diagnostics)
                }
            }
        }.awaitAll().flatten()

        val sortedVideos = resolvedVideos.sortedWith(
            compareBy<ExtractedVideo> { it.quality.resolution.priorityFor360p }
                .thenBy { it.host.ordinal }
        )
        val recommended = qualityInspector.selectBestByPolicy(sortedVideos, policy)

        ScrapeInspectionReport(
            inputUrl = cleanUrl,
            pageTitle = pageTitle,
            posterUrl = posterUrl,
            candidates = candidates,
            episodeLinks = episodeLinks,
            resolvedVideos = sortedVideos,
            recommendedVideo = recommended,
            diagnostics = diagnostics,
            durationMs = System.currentTimeMillis() - startMs
        )
    }

    /**
     * Runs authentic parser/extractor logic offline on realistic HTML & packed JS fixtures
     * (Doodstream /pass_md5/ + MixDrop eval(MDCore) Unpacker + M3U8 360p inspection)
     * so users can inspect the pipeline immediately even without external site access.
     */
    fun runOfflineLabInspection(policy: QualityPreferencePolicy): ScrapeInspectionReport {
        val startMs = System.currentTimeMillis()
        val diagnostics = mutableListOf<String>()
        diagnostics.add("[Lab] تشغيل فحص المحاكاة المحلي على شيفرات حقيقية لـ Dood وMixDrop وM3U8")

        // 1. Doodstream HTML fixture parsed by real DoodExtractor.parseEmbedHtml
        val doodHtml = """
            <html><head><title>Episode 01 - 360p.mp4 - DoodStream</title></head>
            <body><script>
            $.get('/pass_md5/89421-abc99/tok360xyz987', function(data) {
                var videoUrl = data + makePlay();
            });
            </script></body></html>
        """.trimIndent()
        val doodParsed = doodExtractor.parseEmbedHtml(doodHtml, "https://ds2play.com/e/demo360")
        val doodHash = doodExtractor.createHashTable(10)
        val doodExpiry = System.currentTimeMillis()
        val doodDirect = "https://cdn-dood-edge.example.net/stream/${doodParsed?.token}$doodHash?token=${doodParsed?.token}&expiry=$doodExpiry"
        val doodHeaders = doodExtractor.buildDoodHeaders("https://ds2play.com", ScraperSettings.DEFAULT_DESKTOP_USER_AGENT)
        val doodBadge = ResolutionBadge.fromText(doodParsed?.titleResolution)
        diagnostics.add("[Lab/DoodExtractor] تم استخراج ${doodParsed?.md5Path} | الدقة من <title>: ${doodBadge.label}")

        val doodVideo = ExtractedVideo(
            id = UUID.randomUUID().toString(),
            sourcePageUrl = "https://ds2play.com/e/demo360",
            host = ServerHost.DOODSTREAM,
            title = doodParsed?.pageTitle ?: "Doodstream 360p",
            directUrl = doodDirect,
            headers = doodHeaders,
            quality = QualityInspectionResult(
                resolution = doodBadge,
                height = doodBadge.heightPx,
                contentLengthBytes = 58_420_000L,
                contentType = "video/mp4",
                supportsRange = true,
                inspectionSource = "Dood <title> + pass_md5"
            ),
            is360pPreferred = doodBadge.isTarget360p,
            isOfflineSample = true
        )

        // 2. MixDrop packed MDCore fixture parsed by real Unpacker + MixDropExtractor.parseEmbedHtml
        val mixDropHtml = """
            <html><head><title>Episode 01 - MixDrop 480p</title></head>
            <body><script>
            eval(function(p,a,c,k,e,d){e=function(c){return c.toString(36)};if(!''.replace(/^/,String)){while(c--){d[c.toString(a)]=k[c]||c.toString(a)}k=[function(e){return d[e]}];e=function(){return'\\w+'};c=1};while(c--){if(k[c]){p=p.replace(new RegExp('\\b'+e(c)+'\\b','g'),k[c])}}return p}('0.1="//2.3.4/5/6.7?8=9";0.a="//b.3.4/c/d+e.f";',16,16,'Core|wurl|s-delivery|mixdrop|co|v|ep01_480p|mp4|s|ab12cd|remotesub|sub|ar|ep01|sub|srt'.split('|'),0,{}))
            // MDCore marker
            </script></body></html>
        """.trimIndent()
        val mixParsed = mixDropExtractor.parseEmbedHtml(mixDropHtml, "https://mixdrop.co/e/demo480")
        val mixBadge = ResolutionBadge.fromText(mixParsed?.title)
        diagnostics.add("[Lab/MixDropExtractor+Unpacker] فك التشفير: ${mixParsed?.videoUrl} | ترجمة: ${mixParsed?.subtitleUrl}")

        val mixVideo = ExtractedVideo(
            id = UUID.randomUUID().toString(),
            sourcePageUrl = "https://mixdrop.co/e/demo480",
            host = ServerHost.MIXDROP,
            title = mixParsed?.title ?: "MixDrop 480p",
            directUrl = mixParsed?.videoUrl ?: "https://mixdrop.co/v/fallback.mp4",
            headers = mapOf(
                "User-Agent" to ScraperSettings.DEFAULT_DESKTOP_USER_AGENT,
                "Referer" to MixDropExtractor.DEFAULT_REFERER
            ),
            subtitleUrl = mixParsed?.subtitleUrl,
            quality = QualityInspectionResult(
                resolution = mixBadge,
                height = mixBadge.heightPx,
                contentLengthBytes = 94_100_000L,
                contentType = "video/mp4",
                supportsRange = true,
                inspectionSource = "Unpacker (MDCore)"
            ),
            is360pPreferred = mixBadge.isTarget360p,
            isOfflineSample = true
        )

        // 3. Adaptive M3U8 fixture parsed by real QualityInspector.parseM3u8Content
        val m3u8Content = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=2800000,RESOLUTION=1280x720,NAME="720p"
            720p/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=650000,RESOLUTION=640x360,NAME="360p"
            360p/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=350000,RESOLUTION=426x240,NAME="240p"
            240p/index.m3u8
        """.trimIndent()
        val hlsVariants = qualityInspector.parseM3u8Content(
            playlistUrl = "https://cdn-hls.example.net/master.m3u8",
            playlistContent = m3u8Content,
            sourcePageUrl = "https://cdn-hls.example.net/master.m3u8",
            host = ServerHost.DIRECT_HLS,
            title = "HLS Adaptive Stream",
            headers = mapOf("User-Agent" to ScraperSettings.DEFAULT_DESKTOP_USER_AGENT),
            isOfflineSample = true
        )
        diagnostics.add("[Lab/QualityInspector] تحليل M3U8: تم استخراج ${hlsVariants.size} جودات (${hlsVariants.joinToString { it.quality.resolution.label }})")

        val allResolved = (listOf(doodVideo, mixVideo) + hlsVariants)
            .sortedWith(compareBy<ExtractedVideo> { it.quality.resolution.priorityFor360p })
        val recommended = qualityInspector.selectBestByPolicy(allResolved, policy)

        return ScrapeInspectionReport(
            inputUrl = "lab://egydead-dood-mixdrop-m3u8",
            pageTitle = "مختبر الفحص المحلي — Episode 01 (Dood + MixDrop + HLS)",
            candidates = listOf(
                ScrapedServerCandidate("Doodstream", "https://ds2play.com/e/demo360", ServerHost.DOODSTREAM, "360p"),
                ScrapedServerCandidate("MixDrop", "https://mixdrop.co/e/demo480", ServerHost.MIXDROP, "480p"),
                ScrapedServerCandidate("HLS Master", "https://cdn-hls.example.net/master.m3u8", ServerHost.DIRECT_HLS, "720p/360p/240p")
            ),
            resolvedVideos = allResolved,
            recommendedVideo = recommended,
            diagnostics = diagnostics,
            durationMs = System.currentTimeMillis() - startMs,
            isOfflineSample = true
        )
    }

    private suspend fun resolveCandidate(
        candidate: ScrapedServerCandidate,
        userAgent: String,
        diagnostics: MutableList<String>
    ): List<ExtractedVideo> {
        return runCatching {
            when (candidate.hostHint) {
                ServerHost.DOODSTREAM -> {
                    val items = doodExtractor.extract(
                        embedUrl = candidate.serverUrl,
                        qualityHint = candidate.qualityHint,
                        userAgent = userAgent
                    )
                    diagnostics.add("✓ DoodExtractor: تم استخراج ${items.size} رابط (${items.joinToString { it.quality.resolution.label }})")
                    items
                }
                ServerHost.MIXDROP -> {
                    val items = mixDropExtractor.extract(
                        embedUrl = candidate.serverUrl,
                        qualityHint = candidate.qualityHint,
                        userAgent = userAgent
                    )
                    diagnostics.add("✓ MixDropExtractor: تم فك التشفير واستخراج ${items.size} رابط")
                    items
                }
                ServerHost.DIRECT_HLS, ServerHost.DIRECT_MP4 -> {
                    val items = qualityInspector.inspectMedia(
                        sourcePageUrl = candidate.serverUrl,
                        directUrl = candidate.serverUrl,
                        host = candidate.hostHint,
                        title = candidate.serverName,
                        headers = mapOf("User-Agent" to userAgent),
                        qualityHint = candidate.qualityHint
                    )
                    diagnostics.add("✓ QualityInspector: تم فحص ${candidate.serverName} (${items.size} مسار)")
                    items
                }
                ServerHost.UNKNOWN -> {
                    diagnostics.add("• تم تخطي خادم غير مدعوم مباشرة: ${candidate.serverUrl}")
                    emptyList()
                }
            }
        }.getOrElse { error ->
            diagnostics.add("✗ تعذر استخراج ${candidate.serverName}: ${error.message ?: "خطأ شبكة"}")
            emptyList()
        }
    }

    private fun extractPageTitle(doc: Document): String {
        val raw = doc.selectFirst("h1, .singleTitle, .info-title")?.text()
            ?: doc.selectFirst("meta[property=og:title]")?.attr("content")
            ?: doc.title()
        return raw.trim().ifBlank { "صفحة بدون عنوان" }
    }

    private fun extractPosterUrl(doc: Document): String? {
        return doc.selectFirst("meta[property=og:image]")?.attr("abs:content")?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst(".poster img, .single-poster img")?.attr("abs:src")?.takeIf { it.isNotBlank() }
    }

    private fun extractEpisodeLinks(doc: Document): List<EpisodeCandidate> {
        val seen = LinkedHashSet<String>()
        val episodes = mutableListOf<EpisodeCandidate>()
        doc.select(".EpsList a[href], .episodes-list a[href], ul.episodes a[href]").forEach { el ->
            val href = el.attr("abs:href").trim()
            if (href.isNotBlank() && seen.add(href)) {
                episodes.add(
                    EpisodeCandidate(
                        title = el.text().trim().ifBlank { href.substringAfterLast("/") },
                        url = href
                    )
                )
            }
        }
        return episodes
    }

    private fun extractServerCandidates(doc: Document, rawBody: String): List<ScrapedServerCandidate> {
        val seen = LinkedHashSet<String>()
        val list = mutableListOf<ScrapedServerCandidate>()

        fun addCandidate(name: String, url: String, hintText: String? = null) {
            val clean = url.trim()
            if (!clean.startsWith("http")) return
            if (!seen.add(clean)) return
            val host = classifyHost(clean)
            list.add(
                ScrapedServerCandidate(
                    serverName = name.ifBlank { host.displayName },
                    serverUrl = clean,
                    hostHint = host,
                    qualityHint = ResolutionBadge.fromText(hintText ?: name).takeIf { it != ResolutionBadge.UNKNOWN }?.label
                )
            )
        }

        // EgyDead-style server list items (`ul.serversList li[data-link]`)
        doc.select("ul.serversList li[data-link], .servers-list [data-link], [data-url]").forEach { el ->
            val link = el.attr("data-link").ifBlank { el.attr("data-url") }
            addCandidate(el.text(), link, el.text())
        }

        // Embedded iframes
        doc.select("iframe[src], iframe[data-src]").forEach { iframe ->
            val src = iframe.attr("abs:src").ifBlank { iframe.attr("abs:data-src") }
            addCandidate("Iframe Player", src)
        }

        // Regex fallback for Dood / MixDrop / M3U8 / MP4 inside inline scripts
        embeddedHostUrlRegex.findAll(rawBody).forEach { match ->
            addCandidate("Script Embed", match.value)
        }
        directMediaRegex.findAll(rawBody).forEach { match ->
            addCandidate("Direct Media", match.value)
        }

        return list
    }

    fun classifyHost(url: String): ServerHost {
        val lower = url.lowercase()
        return when {
            doodExtractor.canHandle(lower) -> ServerHost.DOODSTREAM
            mixDropExtractor.canHandle(lower) -> ServerHost.MIXDROP
            lower.contains(".m3u8") -> ServerHost.DIRECT_HLS
            lower.contains(".mp4") || lower.contains(".mkv") -> ServerHost.DIRECT_MP4
            else -> ServerHost.UNKNOWN
        }
    }
}
