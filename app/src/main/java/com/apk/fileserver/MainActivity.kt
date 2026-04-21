package com.apk.fileserver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apk.fileserver.ui.screens.HistoryScreen
import com.apk.fileserver.ui.screens.HomeScreen
import com.apk.fileserver.ui.screens.SettingsScreen
import com.apk.fileserver.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    // ViewModel scoped to Activity lifetime
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppRoot(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions when returning from settings
        viewModel.checkPermissions()
        viewModel.checkNetworkStatus()
    }
}

// ═══════════════════════════════════════════════
//              APP ROOT
// ═══════════════════════════════════════════════

@Composable
private fun AppRoot(viewModel: MainViewModel) {
    // Track selected bottom nav tab
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top App Bar ──────────────────────
            AppTopBar(selectedTab = selectedTab)

            // ── Screen Content ───────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(viewModel = viewModel)
                    1 -> HistoryScreen(viewModel = viewModel)
                    2 -> SettingsScreen(viewModel = viewModel)
                }
            }

            // ── Bottom Navigation ────────────────
            AppBottomNav(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    }
}

// ═══════════════════════════════════════════════
//              TOP APP BAR
// ═══════════════════════════════════════════════

@Composable
private fun AppTopBar(selectedTab: Int) {
    val titles = listOf("LocalShare", "History", "Settings")
    val subtitles = listOf(
        "Local File Server",
        "Transfer Records",
        "App Configuration"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical   = 14.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            // Left: Title + Subtitle
            Column {
                Text(
                    text       = titles[selectedTab],
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFFF1F5F9)
                )
                Text(
                    text     = subtitles[selectedTab],
                    fontSize = 12.sp,
                    color    = Color(0xFF64748B)
                )
            }

            // Right: App logo/icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1D4ED8)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text     = "📁",
                    fontSize = 20.sp
                )
            }
        }

        // Bottom border line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF334155))
                .align(Alignment.BottomCenter)
        )
    }
}

// ═══════════════════════════════════════════════
//              BOTTOM NAVIGATION
// ═══════════════════════════════════════════════

@Composable
private fun AppBottomNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    // Nav items definition
    data class NavItem(
        val label: String,
        val icon: ImageVector,
        val selectedColor: Color
    )

    val navItems = listOf(
        NavItem(
            label         = "Home",
            icon          = Icons.Default.Home,
            selectedColor = Color(0xFF3B82F6)
        ),
        NavItem(
            label         = "History",
            icon          = Icons.Default.History,
            selectedColor = Color(0xFFA855F7)
        ),
        NavItem(
            label         = "Settings",
            icon          = Icons.Default.Settings,
            selectedColor = Color(0xFF22C55E)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B))
    ) {
        // Top border line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF334155))
                .align(Alignment.TopCenter)
        )

        NavigationBar(
            modifier         = Modifier.navigationBarsPadding(),
            containerColor   = Color.Transparent,
            contentColor     = Color(0xFF64748B),
            tonalElevation   = 0.dp
        ) {
            navItems.forEachIndexed { index, item ->
                val isSelected = selectedTab == index

                NavigationBarItem(
                    selected = isSelected,
                    onClick  = { onTabSelected(index) },
                    icon = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Active indicator pill
                            AnimatedVisibility(
                                visible = isSelected,
                                enter   = fadeIn(),
                                exit    = fadeOut()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(item.selectedColor)
                                )
                            }

                            // Invisible spacer when not selected
                            if (!isSelected) {
                                Spacer(modifier = Modifier.height(3.dp))
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(22.dp),
                                tint = if (isSelected) item.selectedColor
                                else Color(0xFF475569)
                            )
                        }
                    },
                    label = {
                        Text(
                            text       = item.label,
                            fontSize   = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold
                            else FontWeight.Normal,
                            color      = if (isSelected) item.selectedColor
                            else Color(0xFF475569)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor   = Color.Transparent,
                        unselectedIconColor = Color.Transparent,
                        selectedTextColor   = Color.Transparent,
                        unselectedTextColor = Color.Transparent,
                        indicatorColor      = Color.Transparent
                    )
                )
            }
        }
    }
}