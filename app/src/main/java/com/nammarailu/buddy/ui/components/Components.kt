package com.nammarailu.buddy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.nammarailu.buddy.R
import com.nammarailu.buddy.data.model.TrainStatus
import com.nammarailu.buddy.ui.theme.RailuColors

// ── Status Chip ───────────────────────────────────────────────────────────────
@Composable
fun StatusChip(status: TrainStatus, modifier: Modifier = Modifier) {
    val (color, labelRes) = when (status) {
        TrainStatus.ON_TIME  -> RailuColors.OnTime  to R.string.on_time
        TrainStatus.DELAYED  -> RailuColors.Delayed to R.string.delayed
        TrainStatus.WARNING  -> RailuColors.Warning to R.string.warning
        TrainStatus.UNKNOWN  -> Color.Gray          to R.string.unknown
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(labelRes), color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Platform Badge ────────────────────────────────────────────────────────────
@Composable
fun PlatformBadge(number: Int, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(listOf(RailuColors.Purple, RailuColors.PurpleLight))
            )
    ) {
        Text(
            text = "P$number",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

// ── Live Pulse Indicator ──────────────────────────────────────────────────────
@Composable
fun LivePulse(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "scale"
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            Modifier
                .size(10.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(RailuColors.OnTime)
        )
        Spacer(Modifier.width(6.dp))
        Text("LIVE", color = RailuColors.OnTime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Skeleton Loader ───────────────────────────────────────────────────────────
@Composable
fun SkeletonBox(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    )
}

@Composable
fun TrainCardSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBox(Modifier.fillMaxWidth(0.6f).height(20.dp))
            SkeletonBox(Modifier.fillMaxWidth(0.4f).height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBox(Modifier.width(60.dp).height(28.dp))
                SkeletonBox(Modifier.width(80.dp).height(28.dp))
            }
        }
    }
}

// ── Section Header ────────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// ── Gradient Card ─────────────────────────────────────────────────────────────
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardMod = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    Card(
        modifier = cardMod,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        content = content
    )
}

// ── Icon Button Card ──────────────────────────────────────────────────────────
@Composable
fun NavQuickCard(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GradientCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f))
            ) {
                Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.8f))
        }
    }
}

// ── Error State ───────────────────────────────────────────────────────────────
@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(Icons.Default.CloudOff, contentDescription = null, tint = RailuColors.Delayed, modifier = Modifier.size(48.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurface.copy(0.7f), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = RailuColors.Purple)) {
            Text(stringResource(R.string.retry))
        }
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────
@Composable
fun EmptyState(message: String, icon: ImageVector = Icons.Default.Train) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.size(48.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), style = MaterialTheme.typography.bodyMedium)
    }
}
