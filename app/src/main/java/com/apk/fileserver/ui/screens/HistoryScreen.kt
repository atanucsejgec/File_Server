package com.apk.fileserver.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apk.fileserver.utils.FileUtils
import com.apk.fileserver.utils.TransferRecord
import com.apk.fileserver.utils.TransferType
import com.apk.fileserver.viewmodel.MainViewModel

// ═══════════════════════════════════════════════
//              FILTER OPTIONS
// ═══════════════════════════════════════════════

enum class TransferFilter {
    ALL, UPLOADS, DOWNLOADS, FAILED
}

// ═══════════════════════════════════════════════
//              HISTORY SCREEN
// ═══════════════════════════════════════════════

@Composable
fun HistoryScreen(viewModel: MainViewModel) {
    val transfers by viewModel.transfers.collectAsStateWithLifecycle()

    // Filter state
    var activeFilter by remember { mutableStateOf(TransferFilter.ALL) }

    // Apply filter to transfers list
    val filteredTransfers = remember(transfers, activeFilter) {
        when (activeFilter) {
            TransferFilter.ALL       -> transfers
            TransferFilter.UPLOADS   -> transfers.filter {
                it.type == TransferType.UPLOAD
            }
            TransferFilter.DOWNLOADS -> transfers.filter {
                it.type == TransferType.DOWNLOAD
            }
            TransferFilter.FAILED    -> transfers.filter {
                !it.success
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start  = 16.dp,
                end    = 16.dp,
                top    = 16.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Summary Cards ────────────────────
            item {
                HistorySummaryRow(transfers = transfers)
            }

            // ── Filter + Clear Row ───────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Filter",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    // Clear all button
                    if (transfers.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearTransferHistory() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Clear All",
                                fontSize = 13.sp,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }

            // ── Filter Chips ─────────────────────
            item {
                FilterChipRow(
                    activeFilter = activeFilter,
                    transfers    = transfers,
                    onFilterSelected = { activeFilter = it }
                )
            }

            // ── Transfer Count ───────────────────
            item {
                Text(
                    text = when {
                        filteredTransfers.isEmpty() -> "No transfers found"
                        filteredTransfers.size == 1 -> "1 transfer"
                        else -> "${filteredTransfers.size} transfers"
                    },
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            // ── Transfer Items ───────────────────
            if (filteredTransfers.isEmpty()) {
                item {
                    HistoryEmptyState(filter = activeFilter)
                }
            } else {
                itemsIndexed(
                    items = filteredTransfers,
                    key   = { _, record -> record.id }
                ) { index, record ->
                    AnimatedVisibility(
                        visible = true,
                        enter   = slideInVertically(
                            animationSpec = tween(
                                durationMillis = 200,
                                delayMillis    = (index * 30).coerceAtMost(300)
                            )
                        ) + fadeIn(
                            animationSpec = tween(200)
                        )
                    ) {
                        HistoryTransferItem(record = record)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
//          HISTORY SUMMARY ROW
// ═══════════════════════════════════════════════

@Composable
private fun HistorySummaryRow(transfers: List<TransferRecord>) {
    val uploadCount   = transfers.count { it.type == TransferType.UPLOAD }
    val downloadCount = transfers.count { it.type == TransferType.DOWNLOAD }
    val totalBytes    = transfers.sumOf { it.fileSize }
    val successCount  = transfers.count { it.success }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Total transfers
        SummaryCard(
            value    = transfers.size.toString(),
            label    = "Total",
            color    = Color(0xFF3B82F6),
            modifier = Modifier.weight(1f)
        )

        // Uploads
        SummaryCard(
            value    = uploadCount.toString(),
            label    = "Uploads",
            color    = Color(0xFF22C55E),
            modifier = Modifier.weight(1f)
        )

        // Downloads
        SummaryCard(
            value    = downloadCount.toString(),
            label    = "Downloads",
            color    = Color(0xFFA855F7),
            modifier = Modifier.weight(1f)
        )

        // Total data
        SummaryCard(
            value    = FileUtils.formatFileSize(totalBytes),
            label    = "Data",
            color    = Color(0xFFF59E0B),
            modifier = Modifier.weight(1f)
        )
    }
}

// ═══════════════════════════════════════════════
//          SUMMARY CARD
// ═══════════════════════════════════════════════

@Composable
private fun SummaryCard(
    value: String,
    label: String,
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Color accent line
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )

            Text(
                text       = value,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFFF1F5F9),
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )

            Text(
                text     = label,
                fontSize = 10.sp,
                color    = Color(0xFF64748B)
            )
        }
    }
}

// ═══════════════════════════════════════════════
//          FILTER CHIP ROW
// ═══════════════════════════════════════════════

@Composable
private fun FilterChipRow(
    activeFilter: TransferFilter,
    transfers: List<TransferRecord>,
    onFilterSelected: (TransferFilter) -> Unit
) {
    val filters = listOf(
        Triple(
            TransferFilter.ALL,
            "All (${transfers.size})",
            Color(0xFF3B82F6)
        ),
        Triple(
            TransferFilter.UPLOADS,
            "Uploads (${transfers.count { it.type == TransferType.UPLOAD }})",
            Color(0xFF22C55E)
        ),
        Triple(
            TransferFilter.DOWNLOADS,
            "Downloads (${transfers.count { it.type == TransferType.DOWNLOAD }})",
            Color(0xFFA855F7)
        ),
        Triple(
            TransferFilter.FAILED,
            "Failed (${transfers.count { !it.success }})",
            Color(0xFFEF4444)
        )
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { (filter, label, color) ->
            FilterChip(
                label      = label,
                isSelected = activeFilter == filter,
                color      = color,
                onClick    = { onFilterSelected(filter) }
            )
        }
    }
}

// ═══════════════════════════════════════════════
//          FILTER CHIP
// ═══════════════════════════════════════════════

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) color.copy(alpha = 0.2f)
                else Color(0xFF1E293B)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) color else Color(0xFF334155),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            fontSize   = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold
            else FontWeight.Normal,
            color      = if (isSelected) color else Color(0xFF64748B)
        )
    }
}

// ═══════════════════════════════════════════════
//          HISTORY TRANSFER ITEM
// ═══════════════════════════════════════════════

@Composable
private fun HistoryTransferItem(record: TransferRecord) {
    val isUpload   = record.type == TransferType.UPLOAD
    val typeColor  = if (isUpload) Color(0xFF22C55E) else Color(0xFFA855F7)
    val typeBg     = if (isUpload) Color(0xFF14532D) else Color(0xFF2E1065)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Type Icon ────────────────────────
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(typeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUpload) Icons.Default.Upload
                    else Icons.Default.Download,
                    contentDescription = record.type.name,
                    tint = typeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // ── File Details ─────────────────────
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // File name
                Text(
                    text       = record.fileName,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFFF1F5F9),
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )

                // File path
                Text(
                    text     = record.filePath,
                    fontSize = 11.sp,
                    color    = Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Meta row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(typeBg)
                            .padding(
                                horizontal = 6.dp,
                                vertical   = 2.dp
                            )
                    ) {
                        Text(
                            text       = if (isUpload) "UPLOAD" else "DOWNLOAD",
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color      = typeColor
                        )
                    }

                    // File size
                    Text(
                        text     = FileUtils.formatFileSize(record.fileSize),
                        fontSize = 11.sp,
                        color    = Color(0xFF64748B)
                    )

                    // Separator
                    Text(
                        text     = "•",
                        fontSize = 11.sp,
                        color    = Color(0xFF334155)
                    )

                    // Timestamp
                    Text(
                        text     = FileUtils.formatDate(record.timestamp),
                        fontSize = 11.sp,
                        color    = Color(0xFF64748B)
                    )
                }
            }

            // ── Right side ───────────────────────
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Success/Fail indicator
                Icon(
                    imageVector = if (record.success)
                        Icons.Default.CheckCircle
                    else
                        Icons.Default.ErrorOutline,
                    contentDescription = if (record.success) "Success" else "Failed",
                    tint = if (record.success) Color(0xFF22C55E)
                    else Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                )

                // Client IP
                if (record.clientIp.isNotEmpty()) {
                    Text(
                        text       = record.clientIp,
                        fontSize   = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color      = Color(0xFF475569)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
//          EMPTY STATE
// ═══════════════════════════════════════════════

@Composable
private fun HistoryEmptyState(filter: TransferFilter) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (filter) {
                    TransferFilter.ALL       -> Icons.Default.History
                    TransferFilter.UPLOADS   -> Icons.Default.Upload
                    TransferFilter.DOWNLOADS -> Icons.Default.Download
                    TransferFilter.FAILED    -> Icons.Default.ErrorOutline
                },
                contentDescription = "Empty",
                tint = Color(0xFF334155),
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text       = when (filter) {
                TransferFilter.ALL       -> "No transfers yet"
                TransferFilter.UPLOADS   -> "No uploads yet"
                TransferFilter.DOWNLOADS -> "No downloads yet"
                TransferFilter.FAILED    -> "No failed transfers"
            },
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Color(0xFF475569)
        )

        Text(
            text      = when (filter) {
                TransferFilter.ALL       ->
                    "Start the server and connect\nfrom your PC to see history"
                TransferFilter.UPLOADS   ->
                    "Upload files from your PC\nto see them here"
                TransferFilter.DOWNLOADS ->
                    "Download files from your phone\nto see them here"
                TransferFilter.FAILED    ->
                    "All transfers completed\nsuccessfully"
            },
            fontSize   = 13.sp,
            color      = Color(0xFF334155),
            textAlign  = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}