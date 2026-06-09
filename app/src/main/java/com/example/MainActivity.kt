package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SportsViewModel
import com.example.ui.player.CustomVideoPlayer
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: SportsViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf("Live Match") }
                var activeStreamUrl by remember { mutableStateOf<String?>(null) }
                var activeStreamTitle by remember { mutableStateOf<String?>(null) }

                if (activeStreamUrl != null) {
                    // Start immersive full-screen landscale video player for requested matches/channels
                    CustomVideoPlayer(
                        streamUrl = activeStreamUrl!!,
                        title = activeStreamTitle ?: "KhelaGhor Live",
                        onBackClick = { activeStreamUrl = null }
                    )
                } else {
                    // Standard portrait guide screen
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkGreenBg),
                        topBar = {
                            // High professional minimal top header (Profile, Notification, Video icons completely removed)
                            CenterAlignedTopAppBar(
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Sport Glimpse",
                                            tint = NeonGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "KhelaGhor",
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = DarkGreenBg
                                ),
                                modifier = Modifier.statusBarsPadding()
                            )
                        },
                        bottomBar = {
                            // Material 3 Responsive Bottom Navigation
                            NavigationBar(
                                containerColor = DarkGreenSurface,
                                modifier = Modifier.navigationBarsPadding(),
                                tonalElevation = 8.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen == "Live Match",
                                    onClick = { currentScreen = "Live Match" },
                                    label = { Text("Live Match") },
                                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Live Match") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = NeonGreen,
                                        indicatorColor = NeonGreen,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    ),
                                    modifier = Modifier.testTag("nav_live_match")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == "Categories",
                                    onClick = { currentScreen = "Categories" },
                                    label = { Text("Categories") },
                                    icon = { Icon(Icons.Default.List, contentDescription = "Categories") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = NeonGreen,
                                        indicatorColor = NeonGreen,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    ),
                                    modifier = Modifier.testTag("nav_categories")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == "Upcoming",
                                    onClick = { currentScreen = "Upcoming" },
                                    label = { Text("Upcoming") },
                                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Upcoming") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = NeonGreen,
                                        indicatorColor = NeonGreen,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    ),
                                    modifier = Modifier.testTag("nav_upcoming")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == "Admin Panel",
                                    onClick = { currentScreen = "Admin Panel" },
                                    label = { Text("Admin Panel") },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Admin Panel") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = NeonGreen,
                                        indicatorColor = NeonGreen,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    ),
                                    modifier = Modifier.testTag("nav_admin")
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(DarkGreenBg)
                        ) {
                            when (currentScreen) {
                                "Live Match" -> KhelaGhorHomeScreen(
                                    viewModel = viewModel,
                                    onWatchClick = { url, title, _ ->
                                        activeStreamUrl = url
                                        activeStreamTitle = title
                                    }
                                )
                                "Categories" -> KhelaGhorCategoriesScreen(
                                    viewModel = viewModel,
                                    onChannelClick = { url, name ->
                                        activeStreamUrl = url
                                        activeStreamTitle = name
                                    }
                                )
                                "Upcoming" -> KhelaGhorUpcomingScreen(
                                    viewModel = viewModel,
                                    onWatchClick = { url, title ->
                                        activeStreamUrl = url
                                        activeStreamTitle = title
                                    }
                                )
                                "Admin Panel" -> KhelaGhorAdminPanel(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
