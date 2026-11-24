package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.BatchItemProgress
import com.example.domain.model.BatchItemState
import com.example.domain.model.ExtractedVideo
import com.example.domain.model.QualityPreferencePolicy
import com.example.domain.model.ResolveMode
import com.example.domain.model.ScrapeInspectionReport
import com.example.domain.model.ScraperSettings
import com.example.domain.repository.DownloadRepository
import com.example.domain.repository.HistoryRepository
import com.example.domain.repository.ScraperRepository
import com.example.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResolverUiState(
    val mode: ResolveMode = ResolveMode.INTERACTIVE,
    val singleUrlInput: String = "",
    val batchUrlsInput: String = "",
    val isInspecting: Boolean = false,
    val isBatchRunning: Boolean = false,
    val report: ScrapeInspectionReport? = null,
    val errorMessage: String? = null,
    val batchProgress: List<BatchItemProgress> = emptyList(),
    val selectedVideoForDetails: ExtractedVideo? = null,
    val showDiagnosticsSheet: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class ResolverViewModel @Inject constructor(
    private val scraperRepository: ScraperRepository,
    private val downloadRepository: DownloadRepository,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResolverUiState())
    val uiState: StateFlow<ResolverUiState> = _uiState.asStateFlow()

    val settings: StateFlow<ScraperSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ScraperSettings()
    )

    private var batchJob: Job? = null

    fun setMode(mode: ResolveMode) {
        _uiState.update { it.copy(mode = mode, errorMessage = null) }
    }

    fun updateSingleUrl(url: String) {
        _uiState.update { it.copy(singleUrlInput = url, errorMessage = null) }
    }

    fun updateBatchUrls(urls: String) {
        _uiState.update { it.copy(batchUrlsInput = urls, errorMessage = null) }
    }

    fun updateQualityPolicy(policy: QualityPreferencePolicy) {
        viewModelScope.launch {
            settingsRepository.updateQualityPolicy(policy)
        }
    }

    fun inspectSingleUrl(urlOverride: String? = null) {
        val targetUrl = (urlOverride ?: _uiState.value.singleUrlInput).trim()
        if (targetUrl.isBlank()) {
            _uiState.update { it.copy(errorMessage = "يرجى إدخال رابط صفحة مشاهدة أو رابط خادم (Doodstream / MixDrop / M3U8)") }
            return
        }
        if (urlOverride != null) {
            _uiState.update { it.copy(singleUrlInput = urlOverride, mode = ResolveMode.INTERACTIVE) }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isInspecting = true, errorMessage = null) }
            val currentPolicy = settings.value.qualityPolicy
            runCatching {
                scraperRepository.inspectAndResolve(targetUrl, currentPolicy)
            }.onSuccess { report ->
                if (report.resolvedVideos.isNotEmpty()) {
                    historyRepository.saveHistory(report)
                }
                _uiState.update {
                    it.copy(
                        isInspecting = false,
                        report = report,
                        errorMessage = if (report.resolvedVideos.isEmpty() && report.episodeLinks.isEmpty()) {
                            "لم يتم العثور على سيرفرات مدعومة أو روابط قابلة للاستخراج في هذه الصفحة"
                        } else null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isInspecting = false,
                        errorMessage = err.message ?: "تعذر الاتصال بالرابط المحدد"
                    )
                }
            }
        }
    }

    fun runOfflineLabSample() {
        viewModelScope.launch {
            _uiState.update { it.copy(isInspecting = true, errorMessage = null, mode = ResolveMode.INTERACTIVE) }
            val report = scraperRepository.runOfflineLabInspection(settings.value.qualityPolicy)
            _uiState.update {
                it.copy(
                    isInspecting = false,
                    singleUrlInput = report.inputUrl,
                    report = report,
                    snackbarMessage = "تم تشغيل مختبر الفحص المحلي على Dood وMixDrop وM3U8"
                )
            }
        }
    }

    fun sendScrapedEpisodesToBatch() {
        val episodes = _uiState.value.report?.episodeLinks.orEmpty()
        if (episodes.isEmpty()) return
        val joined = episodes.joinToString("\n") { it.url }
        _uiState.update {
            it.copy(
                mode = ResolveMode.AUTO_BATCH,
                batchUrlsInput = joined,
                snackbarMessage = "تم نقل ${episodes.size} حلقة إلى الوضع الآلي المجمّع"
            )
        }
    }

    fun startAutoBatchGrabber() {
        val rawUrls = _uiState.value.batchUrlsInput
            .lines()
            .map { it.trim() }
            .filter { it.startsWith("http://") || it.startsWith("https://") }
            .distinct()

        if (rawUrls.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "أدخل رابطاً واحداً على الأقل يبدأ بـ http:// أو https:// في القائمة المجمّعة") }
            return
        }

        batchJob?.cancel()
        batchJob = viewModelScope.launch {
            val currentSettings = settingsRepository.settings.first()
            val initialItems = rawUrls.map { BatchItemProgress(url = it) }.toMutableList()
            _uiState.update {
                it.copy(
                    isBatchRunning = true,
                    errorMessage = null,
                    batchProgress = initialItems.toList()
                )
            }

            var queuedCount = 0
            for (index in rawUrls.indices) {
                val url = rawUrls[index]
                initialItems[index] = initialItems[index].copy(
                    state = BatchItemState.RESOLVING,
                    message = "جاري فحص السيرفرات والدقات..."
                )
                _uiState.update { it.copy(batchProgress = initialItems.toList()) }

                runCatching {
                    scraperRepository.inspectAndResolve(url, currentSettings.qualityPolicy)
                }.onSuccess { report ->
                    val chosen = report.recommendedVideo
                    if (chosen != null) {
                        val delayForItem = if (queuedCount > 0) currentSettings.interDownloadDelaySec else 0
                        downloadRepository.enqueueVideo(chosen, delaySeconds = delayForItem)
                        queuedCount++
                        initialItems[index] = initialItems[index].copy(
                            state = BatchItemState.QUEUED,
                            resolvedTitle = chosen.title,
                            chosenQualityLabel = chosen.quality.resolution.label,
                            is360pMatch = chosen.is360pPreferred,
                            message = if (chosen.is360pPreferred) {
                                "تم التقاط 360p وإرساله للطابور"
                            } else {
                                "تراجع ذكي: ${chosen.quality.resolution.label} في الطابور"
                            }
                        )
                    } else {
                        initialItems[index] = initialItems[index].copy(
                            state = BatchItemState.SKIPPED_NO_360P,
                            resolvedTitle = report.pageTitle,
                            message = "لم يتوفر رابط مطابق لقاعدة الجودة (${currentSettings.qualityPolicy.shortTag})"
                        )
                    }
                }.onFailure { err ->
                    initialItems[index] = initialItems[index].copy(
                        state = BatchItemState.FAILED,
                        message = err.message ?: "فشل الفحص"
                    )
                }

                _uiState.update { it.copy(batchProgress = initialItems.toList()) }

                if (index < rawUrls.lastIndex && currentSettings.interDownloadDelaySec > 0) {
                    delay((currentSettings.interDownloadDelaySec * 1000L).coerceAtMost(5000L))
                }
            }

            _uiState.update {
                it.copy(
                    isBatchRunning = false,
                    snackbarMessage = "اكتمل الفحص الآلي: تمت إضافة $queuedCount عنصر إلى طابور التحميل"
                )
            }
        }
    }

    fun cancelAutoBatch() {
        batchJob?.cancel()
        _uiState.update { it.copy(isBatchRunning = false, snackbarMessage = "تم إيقاف الفحص المجمّع") }
    }

    fun enqueueSingleVideo(video: ExtractedVideo) {
        if (video.isOfflineSample) {
            _uiState.update {
                it.copy(snackbarMessage = "هذه عينة مختبر محلية للمعاينة فقط — أدخل رابطاً حقيقياً للتحميل الفعلي")
            }
            return
        }
        viewModelScope.launch {
            val delaySec = settings.value.interDownloadDelaySec
            downloadRepository.enqueueVideo(video, delaySeconds = 0)
            _uiState.update {
                it.copy(
                    snackbarMessage = "تمت إضافة «${video.title}» (${video.quality.resolution.label}) للطابور (فاصل الطابور ${delaySec}ث)"
                )
            }
        }
    }

    fun openVideoDetails(video: ExtractedVideo?) {
        _uiState.update { it.copy(selectedVideoForDetails = video) }
    }

    fun setDiagnosticsSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showDiagnosticsSheet = visible) }
    }

    fun showMessage(msg: String) {
        _uiState.update { it.copy(snackbarMessage = msg) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
