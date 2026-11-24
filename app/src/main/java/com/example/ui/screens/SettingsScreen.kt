package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.QualityPreferencePolicy
import com.example.domain.model.ScraperSettings
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: ScraperSettings,
    onQualityPolicyChange: (QualityPreferencePolicy) -> Unit,
    onMaxConcurrentChange: (Int) -> Unit,
    onInterDelayChange: (Int) -> Unit,
    onAutoRetryChange: (Int) -> Unit,
    onPreferSubtitlesChange: (Boolean) -> Unit,
    onUserAgentChange: (String) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "إعدادات المحرك والطابور الثقيل",
                style = MaterialTheme.typography.titleLarge
            )
        }

        // 1. Quality Policy (360p Preference & Smart Fallback)
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "سياسة فحص واختيار الجودة (360p)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    QualityPreferencePolicy.entries.forEach { policy ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.qualityPolicy == policy,
                                onClick = { onQualityPolicyChange(policy) }
                            )
                            Text(
                                text = policy.arabicLabel,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // 2. Heavy Queue Concurrency & Rate-Limit Delay
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ضبط الطابور الثقيل ومنع الحظر",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("أقصى عدد تحميلات متوازية", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${settings.maxConcurrentDownloads}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = settings.maxConcurrentDownloads.toFloat(),
                            onValueChange = { onMaxConcurrentChange(it.roundToInt()) },
                            valueRange = 1f..3f,
                            steps = 1,
                            modifier = Modifier.testTag("max_concurrent_slider")
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الفاصل الزمني بين الطلبات (Delay)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${settings.interDownloadDelaySec} ثانية",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Slider(
                            value = settings.interDownloadDelaySec.toFloat(),
                            onValueChange = { onInterDelayChange(it.roundToInt()) },
                            valueRange = 0f..15f,
                            steps = 14,
                            modifier = Modifier.testTag("inter_delay_slider")
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("إعادة المحاولة التلقائية (مع JIT)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${settings.autoRetryCount} مرات",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = settings.autoRetryCount.toFloat(),
                            onValueChange = { onAutoRetryChange(it.roundToInt()) },
                            valueRange = 0f..5f,
                            steps = 4
                        )
                    }
                }
            }
        }

        // 3. Extractors & Appearance
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تنزيل ترجمات MixDrop (remotesub)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "حفظ ملف .srt عند توفره بجانب الفيديو",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.preferMixDropSubtitles,
                            onCheckedChange = onPreferSubtitlesChange
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("الوضع الداكن (Studio Dark)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "واجهة عالية التباين لمراقبة الفحص والتحميل",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.useDarkTheme,
                            onCheckedChange = onDarkThemeChange,
                            modifier = Modifier.testTag("dark_theme_switch")
                        )
                    }

                    HorizontalDivider()

                    Text("هوية المتصفح (User-Agent):", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isDesktop = settings.customUserAgent == ScraperSettings.DEFAULT_DESKTOP_USER_AGENT
                        FilterChip(
                            selected = isDesktop,
                            onClick = { onUserAgentChange(ScraperSettings.DEFAULT_DESKTOP_USER_AGENT) },
                            label = { Text("Desktop Chrome (MixDrop)") }
                        )
                        FilterChip(
                            selected = !isDesktop,
                            onClick = { onUserAgentChange(ScraperSettings.DEFAULT_MOBILE_USER_AGENT) },
                            label = { Text("Mobile Chrome") }
                        )
                    }
                }
            }
        }

        // 4. Architecture Reference Card
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "المعمارية التقنية المعتمدة",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• الشبكة والتحليل: OkHttpClient + Jsoup + Kotlin Unpacker\n" +
                            "• المستخرجات: DoodExtractor (/pass_md5/ + createHashTable) & MixDropExtractor (MDCore)\n" +
                            "• فحص الدقات: QualityInspector (M3U8 + HEAD/Range Probe + 360p Spotlight)\n" +
                            "• الطابور الثقيل: WorkManager + Room + Coroutines Flow + JIT Re-Extraction",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
