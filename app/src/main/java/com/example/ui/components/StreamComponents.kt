package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.ServerHost
import com.example.ui.theme.LocalStreamGrabSemantics

@Composable
fun ResolutionPill(
    label: String,
    is360p: Boolean,
    modifier: Modifier = Modifier
) {
    val semantics = LocalStreamGrabSemantics.current
    val bgColor = if (is360p) semantics.badge360pBg else MaterialTheme.colorScheme.surfaceContainerHigh
    val fgColor = if (is360p) semantics.badge360pText else MaterialTheme.colorScheme.onSurfaceVariant
    val border = if (is360p) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null

    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = border
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (is360p) Icons.Rounded.AutoAwesome else Icons.Rounded.Hd,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (is360p) "$label • هدف" else label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = fgColor
            )
        }
    }
}

@Composable
fun HostBadge(
    host: ServerHost,
    modifier: Modifier = Modifier
) {
    val semantics = LocalStreamGrabSemantics.current
    val (bg, fg) = when (host) {
        ServerHost.DOODSTREAM -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) to semantics.doodAccent
        ServerHost.MIXDROP -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f) to semantics.mixDropAccent
        ServerHost.DIRECT_HLS -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f) to MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = host.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = fg
            )
        }
    }
}

@Composable
fun MetaChip(
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    hasSubtitleIcon: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (hasSubtitleIcon) {
                Icon(
                    imageVector = Icons.Rounded.Subtitles,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = tint
            )
        }
    }
}

fun formatBytes(bytes: Long?): String {
    if (bytes == null || bytes <= 0L) return "غير معروف"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        else -> String.format("%.0f KB", kb)
    }
}

fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0L) return "0 KB/s"
    return when {
        bytesPerSec >= 1024L * 1024L -> String.format("%.1f MB/s", bytesPerSec / (1024f * 1024f))
        bytesPerSec >= 1024L -> String.format("%.0f KB/s", bytesPerSec / 1024f)
        else -> "$bytesPerSec B/s"
    }
}
