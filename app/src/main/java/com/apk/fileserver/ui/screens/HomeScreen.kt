package com.apk.fileserver.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apk.fileserver.ui.components.QRCodeView
import com.apk.fileserver.ui.components.ServerStatusCard
import com.apk.fileserver.utils.FileUtils
import com.apk.fileserver.utils.PermissionUtils
import com.apk.fileserver.utils.TransferRecord
import com.apk.fileserver.utils.TransferType
import com.apk.fileserver.viewmodel.MainViewModel
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════
//              HOME SCREEN
// ═══════════════════════════════════════════════

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    RequestBatteryOptimization()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val serverState by viewModel.serverState.collectAsStateWithLifecycle()
    val transfers by viewModel.transfers.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ── Permission Launchers ─────────────────────

    // Standard permissions launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            viewModel.checkPermissions()
            viewModel.startServer()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Storage permission denied. " +
                            "Please grant permission to continue."
                )
            }
        }
    }

    // MANAGE_EXTERNAL_STORAGE launcher (Android 11+)
    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.checkPermissions()
        if (PermissionUtils.hasManageStoragePermission()) {
            viewModel.startServer()
        }
    }

    // ── Snackbar Effect ──────────────────────────
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    // ── Permission check on compose ──────────────
    LaunchedEffect(Unit) {
        viewModel.checkPermissions()
        viewModel.checkNetworkStatus()
    }

    // ── Main Layout ──────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Permission Banner ────────────────
            item {
                val permStatus = uiState.permissionStatus
                if (permStatus != null && !permStatus.canFunction()) {
                    PermissionBanner(
                        onGrantClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                // Android 11+: open settings
                                val intent = Intent(
                                    android.provider.Settings
                                        .ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                manageStorageLauncher.launch(intent)
                            } else {
                                // Android 6-10: runtime permission
                                permissionLauncher.launch(
                                    PermissionUtils.getRequiredPermissions()
                                )
                            }
                        }
                    )
                }
            }

            // ── Server Status Card ───────────────
            item {
                ServerStatusCard(
                    serverState = serverState,
                    uptime = viewModel.getServerUptime()
                )
            }

            // ── Start / Stop Button ──────────────
            item {
                ServerControlButton(
                    isRunning  = serverState.isServerRunning,
                    isStarting = serverState.isStarting,
                    isWifi     = serverState.isWifiConnected,
                    onStart = {
                        val permStatus = uiState.permissionStatus
                        when {
                            // Has permission → start
                            permStatus?.canFunction() == true -> {
                                viewModel.startServer()
                            }
                            // Android 11+ → manage storage
                            Build.VERSION.SDK_INT >=
                                    Build.VERSION_CODES.R -> {
                                val intent = Intent(
                                    android.provider.Settings
                                        .ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                manageStorageLauncher.launch(intent)
                            }
                            // Below Android 11 → request perms
                            else -> {
                                permissionLauncher.launch(
                                    PermissionUtils.getRequiredPermissions()
                                )
                            }
                        }
                    },
                    onStop = { viewModel.stopServer() }
                )
            }

            // ── Open in Browser Button ───────────
            item {
                AnimatedVisibility(
                    visible = serverState.isServerRunning,
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut()
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(serverState.serverUrl)
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF3B82F6)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Open Browser",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open in Browser",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── QR Code ──────────────────────────
            item {
                QRCodeView(
                    qrBitmap  = serverState.qrCodeBitmap,
                    serverUrl = serverState.serverUrl
                )
            }

            // ── Quick Stats ──────────────────────
            item {
                AnimatedVisibility(
                    visible = serverState.isServerRunning ||
                            transfers.isNotEmpty(),
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut()
                ) {
                    QuickStatsRow(
                        totalTransfers = transfers.size,
                        uploadCount    = transfers.count {
                            it.type == TransferType.UPLOAD
                        },
                        downloadCount  = transfers.count {
                            it.type == TransferType.DOWNLOAD
                        },
                        totalBytes = viewModel.getTotalTransferred()
                    )
                }
            }

            // ── Recent Transfers Header ──────────
            if (transfers.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Recent Transfers",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF1F5F9)
                            )
                        }

                        TextButton(
                            onClick = { viewModel.clearTransferHistory() }
                        ) {
                            Text(
                                text = "Clear",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                // ── Transfer List ────────────────
                items(
                    items = transfers.take(10),
                    key   = { it.id }
                ) { record ->
                    TransferRecordItem(record = record)
                }
            }

            // ── Empty state ──────────────────────
            if (transfers.isEmpty() && !serverState.isServerRunning) {
                item {
                    EmptyTransferState()
                }
            }
        }

        // ── Snackbar ─────────────────────────────
        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) { data ->
            Snackbar(
                snackbarData     = data,
                containerColor   = Color(0xFF1E293B),
                contentColor     = Color(0xFFF1F5F9),
                actionColor      = Color(0xFF3B82F6),
                shape            = RoundedCornerShape(12.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════
//          PERMISSION BANNER
// ═══════════════════════════════════════════════

@Composable
private fun PermissionBanner(onGrantClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF451A03)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Storage Permission Required",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFEF3C7)
                )
                Text(
                    text = "Grant access to browse and share files",
                    fontSize = 12.sp,
                    color = Color(0xFFFDE68A)
                )
            }
            TextButton(onClick = onGrantClick) {
                Text(
                    text = "Grant",
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════
//          SERVER CONTROL BUTTON
// ═══════════════════════════════════════════════

@Composable
private fun ServerControlButton(
    isRunning: Boolean,
    isStarting: Boolean,
    isWifi: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Button(
        onClick  = if (isRunning) onStop else onStart,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape    = RoundedCornerShape(14.dp),
        enabled  = !isStarting,
        colors   = ButtonDefaults.buttonColors(
            containerColor = when {
                isStarting -> Color(0xFF92400E)
                isRunning  -> Color(0xFF991B1B)
                !isWifi    -> Color(0xFF334155)
                else       -> Color(0xFF1D4ED8)
            },
            disabledContainerColor = Color(0xFF334155)
        )
    ) {
        Icon(
            imageVector = when {
                isStarting -> Icons.Default.Circle
                isRunning  -> Icons.Default.Stop
                else       -> Icons.Default.PlayArrow
            },
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = when {
                isStarting -> "Starting Server..."
                isRunning  -> "Stop Server"
                !isWifi    -> "Connect to WiFi First"
                else       -> "Start Server"
            },
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ═══════════════════════════════════════════════
//          QUICK STATS ROW
// ═══════════════════════════════════════════════

@Composable
private fun QuickStatsRow(
    totalTransfers: Int,
    uploadCount: Int,
    downloadCount: Int,
    totalBytes: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MiniStatCard(
            icon  = Icons.Default.FolderOpen,
            label = "Total",
            value = "$totalTransfers",
            color = Color(0xFF3B82F6),
            modifier = Modifier.weight(1f)
        )
        MiniStatCard(
            icon  = Icons.Default.Upload,
            label = "Uploads",
            value = "$uploadCount",
            color = Color(0xFF22C55E),
            modifier = Modifier.weight(1f)
        )
        MiniStatCard(
            icon  = Icons.Default.Download,
            label = "Downloads",
            value = "$downloadCount",
            color = Color(0xFFA855F7),
            modifier = Modifier.weight(1f)
        )
        MiniStatCard(
            icon  = Icons.Default.Security,
            label = "Transferred",
            value = FileUtils.formatFileSize(totalBytes),
            color = Color(0xFFF59E0B),
            modifier = Modifier.weight(1f)
        )
    }
}

// ═══════════════════════════════════════════════
//          MINI STAT CARD
// ═══════════════════════════════════════════════

@Composable
private fun MiniStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF1F5F9),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

// ═══════════════════════════════════════════════
//          TRANSFER RECORD ITEM
// ═══════════════════════════════════════════════

@Composable
private fun TransferRecordItem(record: TransferRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical   = 10.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Transfer type icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (record.type == TransferType.UPLOAD)
                            Color(0xFF14532D)
                        else Color(0xFF1E1B4B)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (record.type == TransferType.UPLOAD)
                        Icons.Default.Upload
                    else Icons.Default.Download,
                    contentDescription = record.type.name,
                    tint = if (record.type == TransferType.UPLOAD)
                        Color(0xFF22C55E)
                    else Color(0xFFA855F7),
                    modifier = Modifier.size(18.dp)
                )
            }

            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = record.fileName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color    = Color(0xFFF1F5F9),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text     = FileUtils.formatFileSize(record.fileSize),
                        fontSize = 11.sp,
                        color    = Color(0xFF64748B)
                    )
                    Text(
                        text  = "•",
                        fontSize = 11.sp,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text     = FileUtils.formatDate(record.timestamp),
                        fontSize = 11.sp,
                        color    = Color(0xFF64748B)
                    )
                }
            }

            // Client IP
            if (record.clientIp.isNotEmpty()) {
                Text(
                    text     = record.clientIp,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color    = Color(0xFF475569)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════
//          EMPTY STATE
// ═══════════════════════════════════════════════

@Composable
private fun EmptyTransferState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text     = "📂",
            fontSize = 48.sp
        )
        Text(
            text       = "No transfers yet",
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Color(0xFF475569)
        )
        Text(
            text      = "Start the server and connect from\nyour PC browser to transfer files",
            fontSize  = 13.sp,
            color     = Color(0xFF334155),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}


@Composable
private fun RequestBatteryOptimization() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.M
        ) {
            val pm = context.getSystemService(
                Context.POWER_SERVICE
            ) as android.os.PowerManager

            if (!pm.isIgnoringBatteryOptimizations(
                    context.packageName)
            ) {
                try {
                    val intent = android.content.Intent(
                        android.provider.Settings
                            .ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    ).apply {
                        data = android.net.Uri.parse(
                            "package:${context.packageName}"
                        )
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Ignore if not available
                }
            }
        }
    }
}