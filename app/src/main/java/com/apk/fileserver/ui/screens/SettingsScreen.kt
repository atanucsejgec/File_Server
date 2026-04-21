package com.apk.fileserver.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PermDeviceInformation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apk.fileserver.utils.NetworkUtils
import com.apk.fileserver.utils.PermissionUtils
import com.apk.fileserver.viewmodel.AppSettings
import com.apk.fileserver.viewmodel.MainViewModel

// ═══════════════════════════════════════════════
//              SETTINGS SCREEN
// ═══════════════════════════════════════════════

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val serverState by viewModel.serverState.collectAsStateWithLifecycle()
    val permStatus by viewModel.uiState.collectAsStateWithLifecycle()

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Server Settings ──────────────────
            item {
                SettingsSection(title = "Server Configuration") {
                    // Port setting
                    PortSettingItem(
                        currentPort = settings.serverPort,
                        isRunning   = serverState.isServerRunning,
                        onPortChange = { viewModel.updateServerPort(it) }
                    )
                }
            }

            // ── Security Settings ────────────────
            item {
                SettingsSection(title = "Security") {
                    // Password toggle
                    SettingsToggleItem(
                        icon            = Icons.Default.PlayArrow,
                        iconColor       = Color(0xFF22C55E),
                        title           = "Auto-Start Server",
                        subtitle        = "Start server when app opens (WiFi required)",
                        checked         = settings.autoStartServer,
                        onCheckedChange = { viewModel.updateAutoStartServer(it) }
                    )

                    // Password input (visible when enabled)
                    AnimatedVisibility(
                        visible = settings.passwordEnabled,
                        enter   = expandVertically() + fadeIn(),
                        exit    = shrinkVertically() + fadeOut()
                    ) {
                        PasswordInputItem(
                            password = settings.password,
                            onPasswordChange = {
                                viewModel.updatePassword(it)
                            }
                        )
                    }
                }
            }

            // ── Display Settings ─────────────────
            item {
                SettingsSection(title = "Display") {
                    // Dark mode web toggle
                    SettingsToggleItem(
                        icon      = Icons.Default.DarkMode,
                        iconColor = Color(0xFF8B5CF6),
                        title     = "Dark Mode (Web)",
                        subtitle  = "Dark theme for browser interface",
                        checked   = settings.darkModeWeb,
                        onCheckedChange = {
                            viewModel.updateDarkModeWeb(it)
                        }
                    )

                    // Show hidden files toggle
                    SettingsToggleItem(
                        icon      = Icons.Default.Visibility,
                        iconColor = Color(0xFF06B6D4),
                        title     = "Show Hidden Files",
                        subtitle  = "Show files starting with dot (.)",
                        checked   = settings.showHiddenFiles,
                        onCheckedChange = {
                            viewModel.updateShowHiddenFiles(it)
                        }
                    )
                }
            }

            // ── Network Info ─────────────────────
            item {
                SettingsSection(title = "Network Info") {
                    NetworkInfoItem(
                        label = "Local IP Address",
                        value = serverState.localIpAddress
                            .ifEmpty { "Not connected" },
                        icon  = Icons.Default.NetworkCheck
                    )
                    NetworkInfoItem(
                        label = "Server Port",
                        value = settings.serverPort.toString(),
                        icon  = Icons.Default.Code
                    )
                    NetworkInfoItem(
                        label = "Server URL",
                        value = serverState.serverUrl
                            .ifEmpty { "Server not running" },
                        icon  = Icons.Default.FolderOpen
                    )
                    NetworkInfoItem(
                        label = "WiFi Status",
                        value = if (serverState.isWifiConnected)
                            "Connected" else "Disconnected",
                        icon  = Icons.Default.NetworkCheck,
                        valueColor = if (serverState.isWifiConnected)
                            Color(0xFF22C55E) else Color(0xFFEF4444)
                    )
                }
            }

            // ── Permissions Info ─────────────────
            item {
                SettingsSection(title = "Permissions") {
                    val permissionStatus = permStatus.permissionStatus

                    PermissionStatusItem(
                        label   = "Storage Access",
                        granted = permissionStatus?.hasBasicStorageAccess
                            ?: false
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        PermissionStatusItem(
                            label   = "Full Storage Access",
                            granted = permissionStatus?.hasFullStorageAccess
                                ?: false
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionStatusItem(
                            label   = "Notifications",
                            granted = permissionStatus?.hasNotificationAccess
                                ?: false
                        )
                    }

                    // Open app settings button
                    SettingsActionItem(
                        icon      = Icons.Default.Security,
                        iconColor = Color(0xFF3B82F6),
                        title     = "Manage Permissions",
                        subtitle  = "Open app permission settings",
                        onClick   = {
                            PermissionUtils.openAppSettings(context)
                        }
                    )
                }
            }

            // ── About ────────────────────────────
            item {
                SettingsSection(title = "About") {
                    SettingsInfoItem(
                        icon      = Icons.Default.Info,
                        iconColor = Color(0xFF64748B),
                        title     = "LocalShare",
                        subtitle  = "Version 1.0.0"
                    )
                    SettingsInfoItem(
                        icon      = Icons.Default.PermDeviceInformation,
                        iconColor = Color(0xFF64748B),
                        title     = "Android Version",
                        subtitle  = "API ${Build.VERSION.SDK_INT} " +
                                "(${Build.VERSION.RELEASE})"
                    )
                    SettingsActionItem(
                        icon      = Icons.Default.Policy,
                        iconColor = Color(0xFF64748B),
                        title     = "Privacy Policy",
                        subtitle  = "View privacy information",
                        onClick   = {
                            // Open privacy policy URL
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://example.com/privacy")
                            )
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
//          SETTINGS SECTION WRAPPER
// ═══════════════════════════════════════════════

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        // Section title
        Text(
            text     = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color    = Color(0xFF64748B),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(
                start  = 4.dp,
                bottom = 6.dp
            )
        )

        // Section card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(16.dp),
            colors   = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            )
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 0.dp,
                    vertical   = 4.dp
                )
            ) {
                content()
            }
        }
    }
}

// ═══════════════════════════════════════════════
//          PORT SETTING ITEM
// ═══════════════════════════════════════════════

@Composable
private fun PortSettingItem(
    currentPort: Int,
    isRunning: Boolean,
    onPortChange: (Int) -> Unit
) {
    // Common ports: 1024-65535
    // Slider maps to ports: 8000-9000 for simplicity
    var sliderValue by remember(currentPort) {
        mutableStateOf(
            ((currentPort - 8000f) / (9000f - 8000f))
                .coerceIn(0f, 1f)
        )
    }

    Column(
        modifier = Modifier.padding(
            horizontal = 16.dp,
            vertical   = 14.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon box
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF22C55E).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = "Port",
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "Server Port",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF1F5F9)
                    )
                    Text(
                        text = if (isRunning)
                            "Stop server to change port"
                        else
                            "Port range: 8000 - 9000",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Current port display
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF334155),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(
                        horizontal = 10.dp,
                        vertical   = 6.dp
                    )
            ) {
                Text(
                    text = currentPort.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF22C55E)
                )
            }
        }

        // Port slider
        Slider(
            value = sliderValue,
            onValueChange = { value ->
                if (!isRunning) {
                    sliderValue = value
                    val port = (8000 + (value * 1000)).toInt()
                    onPortChange(port)
                }
            },
            enabled = !isRunning,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor        = Color(0xFF22C55E),
                activeTrackColor  = Color(0xFF22C55E),
                inactiveTrackColor = Color(0xFF334155),
                disabledThumbColor = Color(0xFF334155),
                disabledActiveTrackColor = Color(0xFF334155)
            )
        )

        // Quick port presets
        if (!isRunning) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(8080, 8081, 8888, 9000).forEach { port ->
                    PortPresetChip(
                        port      = port,
                        isSelected = currentPort == port,
                        onClick   = {
                            onPortChange(port)
                            sliderValue = ((port - 8000f) / 1000f)
                                .coerceIn(0f, 1f)
                        },
                        modifier  = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
//          PORT PRESET CHIP
// ═══════════════════════════════════════════════

@Composable
private fun PortPresetChip(
    port: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) Color(0xFF14532D)
                else Color(0xFF0F172A)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF22C55E)
                else Color(0xFF334155),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(
                horizontal = 8.dp,
                vertical   = 6.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = port.toString(),
            fontSize   = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color      = if (isSelected) Color(0xFF22C55E)
            else Color(0xFF64748B)
        )
    }
}

// ═══════════════════════════════════════════════
//          PASSWORD INPUT ITEM
// ═══════════════════════════════════════════════

@Composable
private fun PasswordInputItem(
    password: String,
    onPasswordChange: (String) -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(
            start  = 16.dp,
            end    = 16.dp,
            bottom = 14.dp
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Access Password",
            fontSize = 12.sp,
            color = Color(0xFF64748B)
        )

        OutlinedTextField(
            value         = password,
            onValueChange = onPasswordChange,
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = {
                Text(
                    text  = "Enter password",
                    color = Color(0xFF475569)
                )
            },
            visualTransformation = if (showPassword)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword)
                            Icons.Default.Visibility
                        else
                            Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password",
                        tint = Color(0xFF64748B)
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFF334155),
                focusedTextColor     = Color(0xFFF1F5F9),
                unfocusedTextColor   = Color(0xFFF1F5F9),
                cursorColor          = Color(0xFF3B82F6),
                focusedContainerColor   = Color(0xFF0F172A),
                unfocusedContainerColor = Color(0xFF0F172A)
            )
        )
    }
}

// ═══════════════════════════════════════════════
//          SETTINGS TOGGLE ITEM
// ═══════════════════════════════════════════════

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical   = 14.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF1F5F9)
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor    = Color.White,
                checkedTrackColor    = Color(0xFF3B82F6),
                uncheckedThumbColor  = Color(0xFF94A3B8),
                uncheckedTrackColor  = Color(0xFF334155),
                uncheckedBorderColor = Color(0xFF334155)
            )
        )
    }
}

// ═══════════════════════════════════════════════
//          NETWORK INFO ITEM
// ═══════════════════════════════════════════════

@Composable
private fun NetworkInfoItem(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color = Color(0xFF94A3B8)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical   = 12.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF475569),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                color = Color(0xFF94A3B8)
            )
        }

        Text(
            text = value,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

// ═══════════════════════════════════════════════
//          PERMISSION STATUS ITEM
// ═══════════════════════════════════════════════

@Composable
private fun PermissionStatusItem(
    label: String,
    granted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical   = 12.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF94A3B8)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (granted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Granted",
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Granted",
                    fontSize = 12.sp,
                    color = Color(0xFF22C55E),
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "Not Granted",
                    fontSize = 12.sp,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════
//          SETTINGS ACTION ITEM (clickable)
// ═══════════════════════════════════════════════

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(
                horizontal = 16.dp,
                vertical   = 14.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = title,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                color      = Color(0xFFF1F5F9)
            )
            Text(
                text     = subtitle,
                fontSize = 12.sp,
                color    = Color(0xFF64748B)
            )
        }

        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = Color(0xFF334155),
            modifier = Modifier.size(16.dp)
        )
    }
}

// ═══════════════════════════════════════════════
//          SETTINGS INFO ITEM (non-clickable)
// ═══════════════════════════════════════════════

@Composable
private fun SettingsInfoItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical   = 14.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFF1F5F9)
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}