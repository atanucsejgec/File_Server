package com.apk.fileserver.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════
//              QR CODE CARD
// ═══════════════════════════════════════════════

@Composable
fun QRCodeView(
    qrBitmap: Bitmap?,
    serverUrl: String,
    modifier: Modifier = Modifier
) {
    // Collapsible - starts expanded when QR is available
    var isExpanded by remember(qrBitmap) {
        mutableStateOf(qrBitmap != null)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // ── Header (always visible) ──────────────
            QRCodeHeader(
                isExpanded = isExpanded,
                hasQrCode  = qrBitmap != null,
                onClick    = { isExpanded = !isExpanded }
            )

            // ── QR Content (collapsible) ─────────────
            AnimatedVisibility(
                visible = isExpanded,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    if (qrBitmap != null) {
                        QRCodeImage(bitmap = qrBitmap)
                        QRCodeInstructions()
                    } else {
                        QRCodePlaceholder()
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
//              QR HEADER ROW
// ═══════════════════════════════════════════════

@Composable
private fun QRCodeHeader(
    isExpanded: Boolean,
    hasQrCode: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon + Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (hasQrCode) Color(0xFF1D4ED8)
                        else Color(0xFF334155)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "QR Code",
                    tint = if (hasQrCode) Color.White
                    else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = "QR Code",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF1F5F9)
                )
                Text(
                    text = if (hasQrCode) "Scan to connect from phone"
                    else "Start server to generate",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        // Expand/Collapse arrow
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess
            else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = Color(0xFF64748B),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ═══════════════════════════════════════════════
//              QR CODE IMAGE
// ═══════════════════════════════════════════════

@Composable
private fun QRCodeImage(bitmap: Bitmap) {
    Box(
        modifier = Modifier
            .size(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "QR Code",
            modifier = Modifier.size(196.dp),
            contentScale = ContentScale.Fit
        )
    }
}

// ═══════════════════════════════════════════════
//              INSTRUCTIONS
// ═══════════════════════════════════════════════

@Composable
private fun QRCodeInstructions() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A))
            .border(
                width = 1.dp,
                color = Color(0xFF334155),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "How to connect from PC:",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF94A3B8)
        )

        InstructionStep(number = "1", text = "Open any browser on your PC")
        InstructionStep(number = "2", text = "Type the URL shown above")
        InstructionStep(number = "3", text = "Browse and transfer files!")

        Text(
            text = "💡 Or scan QR code with your phone",
            fontSize = 11.sp,
            color = Color(0xFF64748B)
        )
    }
}

// ═══════════════════════════════════════════════
//              INSTRUCTION STEP
// ═══════════════════════════════════════════════

@Composable
private fun InstructionStep(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Number circle
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFF1D4ED8)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

// ═══════════════════════════════════════════════
//              QR PLACEHOLDER (no server)
// ═══════════════════════════════════════════════

@Composable
private fun QRCodePlaceholder() {
    Column(
        modifier = Modifier
            .size(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .border(
                width = 1.dp,
                color = Color(0xFF334155),
                shape = RoundedCornerShape(12.dp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.QrCode,
            contentDescription = "QR Placeholder",
            tint = Color(0xFF334155),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Start server to\ngenerate QR code",
            fontSize = 13.sp,
            color = Color(0xFF475569),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}