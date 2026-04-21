package com.apk.fileserver.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apk.fileserver.viewmodel.ServerState
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════
//          SERVER STATUS CARD (Main Component)
// ═══════════════════════════════════════════════

@Composable
fun ServerStatusCard(
    serverState: ServerState,
    uptime: String,
    modifier: Modifier = Modifier
) {
    // Animate card border color based on server state
    val borderColor by animateColorAsState(
        targetValue = when {
            serverState.isStarting     -> Color(0xFFF59E0B) // Amber
            serverState.isServerRunning -> Color(0xFF22C55E) // Green
            else                        -> Color(0xFF334155) // Gray
        },
        animationSpec = tween(600),
        label = "border_color"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header Row ──────────────────────────
            ServerCardHeader(serverState = serverState)

            // ── Divider ─────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF334155))
            )

            // ── Content: Running or Idle ─────────────
            AnimatedContent(
                targetState = serverState.isServerRunning,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                },
                label = "server_content"
            ) { isRunning ->
                if (isRunning) {
                    ServerRunningContent(
                        serverState = serverState,
                        uptime      = uptime
                    )
                } else {
                    ServerIdleContent(
                        isWifiConnected = serverState.isWifiConnected,
                        isStarting      = serverState.isStarting
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
//              CARD HEADER
// ═══════════════════════════════════════════════

@Composable
private fun ServerCardHeader(serverState: ServerState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title + Status dot
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Animated status indicator dot
            StatusDot(
                isRunning  = serverState.isServerRunning,
                isStarting = serverState.isStarting
            )

            Column {
                Text(
                    text = "LocalShare",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF1F5F9)
                )
                Text(
                    text = when {
                        serverState.isStarting      -> "Starting..."
                        serverState.isServerRunning -> "Server Active"
                        !serverState.isWifiConnected -> "No WiFi"
                        else                         -> "Server Stopped"
                    },
                    fontSize = 12.sp,
                    color = when {
                        serverState.isStarting       -> Color(0xFFF59E0B)
                        serverState.isServerRunning  -> Color(0xFF22C55E)
                        !serverState.isWifiConnected -> Color(0xFFEF4444)
                        else                         -> Color(0xFF94A3B8)
                    }
                )
            }
        }

        // Connection count badge
        if (serverState.isServerRunning) {
            ConnectionBadge(count = serverState.activeConnections.size)
        }
    }
}

// ═══════════════════════════════════════════════
//              STATUS DOT (Animated)
// ═══════════════════════════════════════════════

@Composable
private fun StatusDot(isRunning: Boolean, isStarting: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    // Pulse scale animation when running
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = if (isRunning || isStarting) 1.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_scale"
    )

    val dotColor = when {
        isStarting -> Color(0xFFF59E0B)  // Amber
        isRunning  -> Color(0xFF22C55E)  // Green
        else       -> Color(0xFF64748B)  // Gray
    }

    Box(
        modifier = Modifier
            .size(14.dp)
            .scale(if (isRunning || isStarting) scale else 1f)
            .clip(CircleShape)
            .background(dotColor)
    )
}

// ═══════════════════════════════════════════════
//          CONNECTION BADGE
// ═══════════════════════════════════════════════

@Composable
private fun ConnectionBadge(count: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF334155))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = Icons.Default.People,
            contentDescription = "Connections",
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "$count",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF94A3B8)
        )
    }
}

// ═══════════════════════════════════════════════
//          SERVER RUNNING CONTENT
// ═══════════════════════════════════════════════

@Composable
private fun ServerRunningContent(
    serverState: ServerState,
    uptime: String
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    // Reset "Copied!" after 2 seconds
    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // ── URL Display ──────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Access URL",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B),
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F172A))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF334155),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = serverState.serverUrl,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Copy button
                IconButton(
                    onClick = {
                        clipboardManager.setText(
                            AnnotatedString(serverState.serverUrl)
                        )
                        copied = true
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy URL",
                        tint = if (copied) Color(0xFF22C55E)
                        else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Copied confirmation text
            if (copied) {
                Text(
                    text = "✓ Copied to clipboard",
                    fontSize = 11.sp,
                    color = Color(0xFF22C55E)
                )
            }
        }

        // ── Stats Row ────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // IP Address stat
            StatChip(
                icon  = Icons.Default.Lan,
                label = "IP Address",
                value = serverState.localIpAddress,
                modifier = Modifier.weight(1f)
            )

            // Uptime stat
            StatChip(
                icon  = Icons.Default.Timer,
                label = "Uptime",
                value = uptime,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Instruction text ─────────────────────
        Text(
            text = "Open the URL above in your PC browser " +
                    "to access files on this device",
            fontSize = 12.sp,
            color = Color(0xFF64748B),
            lineHeight = 18.sp
        )
    }
}

// ═══════════════════════════════════════════════
//          SERVER IDLE CONTENT
// ═══════════════════════════════════════════════

@Composable
private fun ServerIdleContent(
    isWifiConnected: Boolean,
    isStarting: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isStarting) {
            // Starting state
            Text(
                text = "⏳ Starting server...",
                fontSize = 14.sp,
                color = Color(0xFFF59E0B),
                textAlign = TextAlign.Center
            )
        } else if (!isWifiConnected) {
            // No WiFi state
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = "No WiFi",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "No WiFi Connection",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFEF4444)
            )
            Text(
                text = "Connect to a WiFi network to start sharing files",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        } else {
            // Ready to start
            Text(
                text = "🚀 Ready to Start",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Press the button below to start the " +
                        "local file server",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════
//              STAT CHIP
// ═══════════════════════════════════════════════

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A))
            .border(
                width = 1.dp,
                color = Color(0xFF334155),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                letterSpacing = 0.5.sp
            )
        }
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFFF1F5F9)
        )
    }
}