package com.example

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
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
                var showSplash by remember { mutableStateOf(true) }
                val appConfig by viewModel.appConfig.collectAsState()
                var showNoticeDialog by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(onDismiss = { showSplash = false })
                } else {
                    var currentScreen by remember { mutableStateOf("Live Match") }
                    var activeStreamUrl by remember { mutableStateOf<String?>(null) }
                    var activeStreamTitle by remember { mutableStateOf<String?>(null) }

                    if (appConfig?.showNotice == true && showNoticeDialog) {
                        val originalNoticeTitle = appConfig?.noticeTitle ?: "KhelaGhor Notice Board"
                        val displayNoticeTitle = if (originalNoticeTitle == "খেলাঘর নোটিশ বোর্ড") "KhelaGhor Notice Board" else originalNoticeTitle.replace("খেলাঘর", "KhelaGhor")
                        NoticeDialog(
                            title = displayNoticeTitle,
                            message = appConfig?.noticeMessage ?: "লাইভ খেলা দেখতে ও যেকোনো সমস্যায় আমাদের Telegram চ্যানেলে যুক্ত থাকুন।",
                            buttonText = appConfig?.noticeButtonText ?: "টেলিগ্রামে জয়েন করুন",
                            linkUrl = appConfig?.noticeLink ?: "https://t.me/khelaghor",
                            onDismiss = { showNoticeDialog = false }
                        )
                    }

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
                                        Image(
                                            painter = painterResource(id = R.drawable.khelaghor_logo_1781080798786),
                                            contentDescription = "KhelaGhor Logo",
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(50))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
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
}

@Composable
fun SplashScreen(onDismiss: () -> Unit) {
    var startAnim by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        kotlinx.coroutines.delay(2500)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkGreenBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Spinning glow circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "loader")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )

                // Outer rotating sweep arc
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, NeonGreen, Color.Transparent)
                        ),
                        startAngle = rotation,
                        sweepAngle = 280f,
                        useCenter = false,
                        style = Stroke(
                            width = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }

                // Inner static custom brand logo representation
                Image(
                    painter = painterResource(id = R.drawable.khelaghor_logo_1781080798786),
                    contentDescription = "KhelaGhor Logo",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(50))
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Text animations
            Text(
                text = "KhelaGhor",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                modifier = Modifier.graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    alpha = alpha
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Live Sports & Entertainment Hub",
                color = NeonGreen.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier.graphicsLayer(
                    alpha = alpha
                )
            )
        }
    }
}

@Composable
fun NoticeDialog(
    title: String,
    message: String,
    buttonText: String,
    linkUrl: String,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Elegant neon shimmering gradient border animation for the notice dialog
    val infiniteTransition = rememberInfiniteTransition(label = "dialog_neon_border")
    val borderOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val neonBorderBrush = Brush.linearGradient(
        colors = listOf(
            ForestGreen.copy(alpha = 0.3f),
            NeonGreen,
            ForestGreen.copy(alpha = 0.3f),
            NeonGreen,
            ForestGreen.copy(alpha = 0.3f)
        ),
        start = androidx.compose.ui.geometry.Offset(borderOffset, 0f),
        end = androidx.compose.ui.geometry.Offset(borderOffset + 400f, 400f)
    )

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkGreenSurface),
            border = androidx.compose.foundation.BorderStroke(2.dp, neonBorderBrush),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Bell icon with gentle pulsing scale animation
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bell_pulse"
                )

                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notification Bell",
                    tint = NeonGreen,
                    modifier = Modifier
                        .size(42.dp)
                        .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = message,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "বন্ধ করুন",
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (linkUrl.isNotBlank()) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {}
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text(
                            text = buttonText,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
