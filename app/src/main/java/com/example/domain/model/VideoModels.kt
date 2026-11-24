package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ServerHost(val displayName: String) {
    DOODSTREAM("Doodstream"),
    MIXDROP("MixDrop"),
    DIRECT_HLS("HLS (M3U8)"),
    DIRECT_MP4("MP4 مباشر"),
    UNKNOWN("خادم خارجي")
}

@Serializable
enum class ResolutionBadge(val label: String, val heightPx: Int, val priorityFor360p: Int) {
    P360("360p", 360, 0),
    P480("480p", 480, 1),
    P240("240p", 240, 2),
    P720("720p", 720, 3),
    P1080("1080p", 1080, 4),
    P1440("1440p", 1440, 5),
    P2160("2160p", 2160, 6),
    UNKNOWN("جودة غير محددة", 0, 7);

    val isTarget360p: Boolean get() = this == P360

    companion object {
        fun fromHeight(height: Int): ResolutionBadge = when {
            height <= 0 -> UNKNOWN
            height <= 270 -> P240
            height <= 400 -> P360
            height <= 540 -> P480
            height <= 780 -> P720
            height <= 1150 -> P1080
            height <= 1500 -> P1440
            else -> P2160
        }

        fun fromText(text: String?): ResolutionBadge {
            if (text.isNullOrBlank()) return UNKNOWN
            val match = Regex("""\b(2160|1440|1080|720|480|360|240)\s*p?\b""", RegexOption.IGNORE_CASE)
                .find(text)
            val height = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return UNKNOWN
            return fromHeight(height)
        }
    }
}

@Serializable
data class QualityInspectionResult(
    val resolution: ResolutionBadge = ResolutionBadge.UNKNOWN,
    val width: Int? = null,
    val height: Int? = null,
    val bandwidthBps: Long? = null,
    val contentLengthBytes: Long? = null,
    val contentType: String? = null,
    val supportsRange: Boolean = false,
    val inspectionSource: String = ""
)

@Serializable
data class ExtractedVideo(
    val id: String,
    val sourcePageUrl: String,
    val host: ServerHost,
    val title: String,
    val directUrl: String,
    val headers: Map<String, String> = emptyMap(),
    val subtitleUrl: String? = null,
    val quality: QualityInspectionResult = QualityInspectionResult(),
    val is360pPreferred: Boolean = false,
    val isOfflineSample: Boolean = false,
    val extractedAtEpochMs: Long = System.currentTimeMillis()
)

@Serializable
data class ScrapedServerCandidate(
    val serverName: String,
    val serverUrl: String,
    val hostHint: ServerHost,
    val qualityHint: String? = null
)

@Serializable
data class EpisodeCandidate(
    val title: String,
    val url: String
)

data class ScrapeInspectionReport(
    val inputUrl: String,
    val pageTitle: String,
    val posterUrl: String? = null,
    val candidates: List<ScrapedServerCandidate> = emptyList(),
    val episodeLinks: List<EpisodeCandidate> = emptyList(),
    val resolvedVideos: List<ExtractedVideo> = emptyList(),
    val recommendedVideo: ExtractedVideo? = null,
    val diagnostics: List<String> = emptyList(),
    val durationMs: Long = 0L,
    val isOfflineSample: Boolean = false
)

enum class ResolveMode {
    INTERACTIVE,
    AUTO_BATCH
}

@Serializable
enum class QualityPreferencePolicy(val arabicLabel: String, val shortTag: String) {
    TARGET_360P_SMART_FALLBACK("تفضيل 360p (مع تراجع ذكي)", "360p + تراجع"),
    STRICT_360P_ONLY("360p فقط (بدون بدائل)", "360p صارم"),
    LOWEST_DATA("أقل استهلاك للبيانات", "توفير بيانات"),
    HIGHEST_AVAILABLE("أعلى جودة متاحة", "أعلى دقة")
}

@Serializable
enum class DownloadStatus(val arabicLabel: String) {
    QUEUED("في الطابور"),
    RESOLVING("استخراج JIT"),
    DOWNLOADING("جاري التحميل"),
    PAUSED("متوقف مؤقتاً"),
    COMPLETED("مكتمل"),
    FAILED("فشل"),
    CANCELLED("ملغى")
}

data class DownloadTask(
    val id: Long = 0L,
    val title: String,
    val sourcePageUrl: String,
    val directUrl: String,
    val host: ServerHost,
    val resolutionLabel: String,
    val is360p: Boolean,
    val expectedSizeBytes: Long,
    val downloadedBytes: Long,
    val speedBytesPerSec: Long = 0L,
    val status: DownloadStatus,
    val headers: Map<String, String> = emptyMap(),
    val subtitleUrl: String? = null,
    val localUri: String? = null,
    val fileName: String,
    val supportsRange: Boolean = false,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val delaySecondsBeforeStart: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (expectedSizeBytes > 0L) {
            (downloadedBytes.toFloat() / expectedSizeBytes.toFloat()).coerceIn(0f, 1f)
        } else if (status == DownloadStatus.COMPLETED) 1f else 0f
}

data class BatchItemProgress(
    val url: String,
    val state: BatchItemState = BatchItemState.PENDING,
    val resolvedTitle: String? = null,
    val chosenQualityLabel: String? = null,
    val is360pMatch: Boolean = false,
    val message: String = "بانتظار المعالجة"
)

enum class BatchItemState {
    PENDING,
    RESOLVING,
    QUEUED,
    SKIPPED_NO_360P,
    FAILED
}

data class ResolvedHistoryItem(
    val id: Long = 0L,
    val inputUrl: String,
    val pageTitle: String,
    val serverCount: Int,
    val has360p: Boolean,
    val bestQualityLabel: String,
    val hostSummary: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class ScraperSettings(
    val qualityPolicy: QualityPreferencePolicy = QualityPreferencePolicy.TARGET_360P_SMART_FALLBACK,
    val maxConcurrentDownloads: Int = 2,
    val interDownloadDelaySec: Int = 3,
    val autoRetryCount: Int = 2,
    val preferMixDropSubtitles: Boolean = true,
    val customUserAgent: String = DEFAULT_DESKTOP_USER_AGENT,
    val useDarkTheme: Boolean = true
) {
    companion object {
        const val DEFAULT_DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
        const val DEFAULT_MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
    }
}
