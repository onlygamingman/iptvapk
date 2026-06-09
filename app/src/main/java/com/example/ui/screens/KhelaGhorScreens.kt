package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ChannelEntity
import com.example.data.MatchEntity
import com.example.ui.SportsViewModel
import com.example.ui.theme.*

// ==========================================
// Helper functions for countdown
// ==========================================
fun formatRemainingTime(startMs: Long, currentMs: Long): String {
    val diff = startMs - currentMs
    if (diff <= 0) return "00:00:00"
    val hrs = diff / 3600000
    val mins = (diff % 3600000) / 60000
    val secs = (diff % 60000) / 1000
    return String.format("%02d:%02d:%02d", hrs, mins, secs)
}

// ==========================================
// Blinking Red Live Dot Component
// ==========================================
@Composable
fun BlinkingLiveDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "LiveBlink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LiveAlpha"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(RedLive.copy(alpha = alpha), CircleShape)
                .border(1.dp, Color.White.copy(alpha = alpha), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "LIVE",
            color = RedLive,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

// ==========================================
// Adsterra Raw HTML/JS Script Ad Component
// ==========================================
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AdsterraBannerAd(htmlCode: String) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                val wrappedHtml = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>
                            body {
                                margin: 0;
                                padding: 0;
                                display: flex;
                                justify-content: center;
                                align-items: center;
                                background-color: transparent;
                                overflow: hidden;
                            }
                        </style>
                    </head>
                    <body>
                        $htmlCode
                    </body>
                    </html>
                """.trimIndent()
                loadDataWithBaseURL("https://asg.gotechbd.com", wrappedHtml, "text/html", "UTF-8", null)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

// ==========================================
// Promotional Ad Banner Component
// ==========================================
@Composable
fun AdBanner(bannerAdUrl: String?, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 16.dp)
            .height(72.dp)
            .clickable {
                onClick()
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(bannerAdUrl ?: "https://www.google.com"))
                    context.startActivity(intent)
                } catch (e: Exception) {}
            },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E16)),
        border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(ForestGreen, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Ad logo",
                        tint = NeonGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "KhelaGhor Premium Promo",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Premium HLS lagfree coverage streaming system",
                        color = GrayText,
                        fontSize = 10.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "AD",
                    color = NeonGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

// ==========================================
// HOME: Live Matches Screen
// ==========================================
@Composable
fun KhelaGhorHomeScreen(
    viewModel: SportsViewModel,
    onWatchClick: (streamUrl: String, matchTitle: String, popUrl: String) -> Unit
) {
    val filteredMatches by viewModel.filteredMatches.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()
    val context = LocalContext.current
    var popUnderTriggerCode by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        "All" to com.example.R.drawable.ic_all,
        "Cricket" to com.example.R.drawable.ic_cricket,
        "Football" to com.example.R.drawable.ic_football,
        "Others" to com.example.R.drawable.ic_others
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkGreenBg)
    ) {
        // Horizontal Sports Categories Filters
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories) { pair ->
                val category = pair.first
                val iconRes = pair.second
                val isSelected = selectedFilter == category
                val borderColor = if (isSelected) NeonGreen else Color.Transparent
                val textCol = if (isSelected) NeonGreen else Color.Gray

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isSelected) DarkGreenSurface else Color(0xFF131D18))
                        .border(
                            BorderStroke(1.5.dp, if (isSelected) NeonGreen else Color(0xFF223129)),
                            RoundedCornerShape(24.dp)
                        )
                        .clickable { viewModel.setFilter(category) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = category,
                        tint = if (isSelected) NeonGreen else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = category,
                        color = textCol,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Match items List
        if (filteredMatches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "No matches",
                        tint = GrayText.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "কোন ম্যাচ নেই",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "অ্যাডমিন প্যানেল থেকে ম্যাচ যুক্ত করুন",
                        color = GrayText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(filteredMatches) { match ->
                    MatchCard(
                        match = match,
                        onWatchClick = {
                            // If popUnder is enabled, trigger background redirect or raw JS render
                            if (appConfig?.adsEnabled == true) {
                                if (!appConfig?.popUnderCode.isNullOrBlank()) {
                                    popUnderTriggerCode = appConfig?.popUnderCode
                                } else if (!appConfig?.popUnderUrl.isNullOrBlank()) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(appConfig?.popUnderUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {}
                                }
                            }
                            onWatchClick(match.streamUrl, "${match.team1Name} VS ${match.team2Name}", appConfig?.popUnderUrl ?: "")
                        }
                    )

                    // Banner Ads placement after match card if enabled
                    if (appConfig?.adsEnabled == true) {
                        if (!appConfig?.bannerAdCode.isNullOrBlank()) {
                            AdsterraBannerAd(appConfig!!.bannerAdCode)
                        } else if (!appConfig?.bannerAdUrl.isNullOrBlank()) {
                            AdBanner(appConfig?.bannerAdUrl) {
                                // Clicked Ad banner analytics or redirect already handled
                            }
                        }
                    }
                }
            }
        }
    }

    if (popUnderTriggerCode != null) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    val wrappedHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        </head>
                        <body>
                            $popUnderTriggerCode
                        </body>
                        </html>
                    """.trimIndent()
                    loadDataWithBaseURL("https://asg.gotechbd.com", wrappedHtml, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.size(1.dp)
        )
    }
}

@Composable
fun MatchCard(
    match: MatchEntity,
    onWatchClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("match_card_${match.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkGreenSurface),
        border = BorderStroke(1.2.dp, ForestGreen.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header parts (Top category capsule and top-right Live tracker)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Capsule
                Box(
                    modifier = Modifier
                        .background(ForestGreen.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = match.category,
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Status tag
                if (match.status == "LIVE") {
                    BlinkingLiveDot()
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Upcoming timing",
                            tint = NeonGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "UPCOMING",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Team VS representation block
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Team 1 Logo + Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    AsyncImage(
                        model = match.team1LogoUrl,
                        contentDescription = "${match.team1Name} logo",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = match.team1Name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // VS circle indicator
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(ForestGreen.copy(alpha = 0.4f), CircleShape)
                        .border(1.dp, NeonGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VS",
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Team 2 Name + Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = match.team2Name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    AsyncImage(
                        model = match.team2LogoUrl,
                        contentDescription = "${match.team2Name} logo",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Action / Footer sections
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Tournament Label
                Text(
                    text = match.tournament,
                    color = GrayText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Neon Action Button "▶ দেখুন"
                Button(
                    onClick = onWatchClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("watch_btn_${match.id}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play watch button",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "▶ দেখুন",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// CATEGORIES: Folder Navigation & Channels
// ==========================================
@Composable
fun KhelaGhorCategoriesScreen(
    viewModel: SportsViewModel,
    onChannelClick: (streamUrl: String, channelName: String) -> Unit
) {
    val categories by viewModel.channelCategories.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()
    var expandedCategory by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkGreenBg)
            .padding(16.dp)
    ) {
        Text(
            text = "স্পোর্টস ও টিভি ক্যাটাগরি",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        if (categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Empty folders",
                        tint = GrayText.copy(alpha = 0.5f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "কোন ক্যাটাগরি তৈরি নেই",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories) { category ->
                val isExpanded = expandedCategory == category
                val channelsInCat = allChannels.filter { it.categoryName == category }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedCategory = if (isExpanded) null else category },
                    colors = CardDefaults.cardColors(containerColor = DarkGreenSurface),
                    border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = "Folder Icon",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = category,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${channelsInCat.size}টি লাইভ চ্যানেল",
                                        color = GrayText,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand collapsible folder",
                                tint = NeonGreen
                            )
                        }

                        // Child channels list
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.2f))
                                    .padding(bottom = 12.dp)
                            ) {
                                Divider(color = ForestGreen.copy(alpha = 0.2f))
                                if (channelsInCat.isEmpty()) {
                                    Text(
                                        text = "এই ফোল্ডারে কোন চ্যানেল নেই",
                                        color = GrayText,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                } else {
                                    channelsInCat.forEach { channel ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onChannelClick(channel.streamUrl, channel.channelName) }
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                AsyncImage(
                                                    model = channel.channelLogoUrl,
                                                    contentDescription = channel.channelName,
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.White.copy(alpha = 0.1f)),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Text(
                                                    text = channel.channelName,
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(RedLive, CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "অনলাইন",
                                                    color = NeonGreen,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Play channel stream",
                                                    tint = NeonGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Divider(color = ForestGreen.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// UPCOMING matches countdown Screen
// ==========================================
@Composable
fun KhelaGhorUpcomingScreen(
    viewModel: SportsViewModel,
    onWatchClick: (streamUrl: String, matchTitle: String) -> Unit
) {
    val matches by viewModel.filteredMatches.collectAsState()
    val tickerTime by viewModel.currentTime.collectAsState()

    val upcomingMatches = matches.filter { it.status == "UPCOMING" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkGreenBg)
            .padding(16.dp)
    ) {
        Text(
            text = "আসন্ন খেলাধুলার তালিকা (Upcoming)",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (upcomingMatches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Empty calendar List",
                        tint = GrayText.copy(alpha = 0.5f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "কোন আসন্ন ম্যাচ শিডিউল করা নেই",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(upcomingMatches) { match ->
                    val timeRemainingStr = formatRemainingTime(match.startTimeStamp, tickerTime)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkGreenSurface),
                        border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Category & Title
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = match.category,
                                    color = NeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(ForestGreen.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )

                                Text(
                                    text = match.tournament,
                                    color = GrayText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Team vs layouts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Text(
                                    text = match.team1Name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .background(ForestGreen.copy(alpha = 0.3f), CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = "VS", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = match.team2Name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Live countdown timer box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .border(BorderStroke(1.dp, NeonGreen.copy(alpha = 0.2f)), RoundedCornerShape(10.dp))
                                    .padding(vertical = 10.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Timer count icon",
                                        tint = RedLive,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "খেলা শুরু হতে বাঁকি: $timeRemainingStr",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace // Monospace prevent shifting
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

// ==========================================
// ADMIN DASHBOARD FORM OVERLAYS & VIEWS
// ==========================================
@Composable
fun KhelaGhorAdminPanel(viewModel: SportsViewModel) {
    val loggedIn by viewModel.adminLoggedIn.collectAsState()
    val isBlocked by viewModel.isBlocked.collectAsState()
    val blockTimeRemaining by viewModel.blockTimeRemaining.collectAsState()
    val allMatches by viewModel.filteredMatches.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()

    var passwordInput by remember { mutableStateOf("") }
    var loginMessage by remember { mutableStateOf("") }

    // Forms Inputs
    var activeTab by remember { mutableStateOf("Matches") } // Matches, Channels, Settings

    // Match Inputs
    var gameCategory by remember { mutableStateOf("Cricket") }
    var team1Name by remember { mutableStateOf("") }
    var team1Logo by remember { mutableStateOf("") }
    var team2Name by remember { mutableStateOf("") }
    var team2Logo by remember { mutableStateOf("") }
    var streamUrl by remember { mutableStateOf("") }
    var tournamentName by remember { mutableStateOf("") }
    var matchStatus by remember { mutableStateOf("LIVE") } // LIVE, UPCOMING
    var startTimeHours by remember { mutableStateOf("1.0") }

    // Channel Inputs
    var charCategoryName by remember { mutableStateOf("Bangladesh") }
    var channelName by remember { mutableStateOf("") }
    var channelLogoUrl by remember { mutableStateOf("") }
    var channelStreamUrl by remember { mutableStateOf("") }

    // Change Password inputs
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var passChangeMessage by remember { mutableStateOf("") }

    // Ad Settings inputs
    var bannerUrlInput by remember { mutableStateOf("") }
    var popUrlInput by remember { mutableStateOf("") }
    var bannerCodeInput by remember { mutableStateOf("") }
    var popCodeInput by remember { mutableStateOf("") }
    var adSettingsSucceed by remember { mutableStateOf("") }

    LaunchedEffect(appConfig) {
        appConfig?.let {
            bannerUrlInput = it.bannerAdUrl
            popUrlInput = it.popUnderUrl
            bannerCodeInput = it.bannerAdCode
            popCodeInput = it.popUnderCode
        }
    }

    // Mock reCAPTCHA checkbox state for security guidelines
    var capthaChecked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkGreenBg)
            .padding(16.dp)
    ) {
        // Secure Login Form
        if (!loggedIn) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                colors = CardDefaults.cardColors(containerColor = DarkGreenSurface),
                border = BorderStroke(1.2.dp, ForestGreen)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure login Icon",
                        tint = NeonGreen,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "সুরক্ষিত অ্যাডমিন লগইন (Security Admin)",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    TextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        modifier = Modifier.fillMaxWidth().testTag("admin_password_input"),
                        label = { Text("অ্যাডমিন পাসওয়ার্ড লিখুন") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.5f),
                            focusedLabelColor = NeonGreen,
                            focusedIndicatorColor = NeonGreen
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulated secure reCAPTCHA check to avoid brute-forcing
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .border(1.dp, ForestGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .clickable { if (!isBlocked) capthaChecked = !capthaChecked }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = capthaChecked,
                            onCheckedChange = { if (!isBlocked) capthaChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = NeonGreen, checkmarkColor = Color.Black)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "আমি কোন রোবট নই (reCAPTCHA)",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                    if (loginMessage.isNotEmpty()) {
                        Text(
                            text = loginMessage,
                            color = if (loginMessage.startsWith("SUC")) NeonGreen else RedLive,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }

                    if (isBlocked) {
                        Text(
                            text = "ভুল পাসওয়ার্ডের কারণে লক! বঁাকি: ${blockTimeRemaining}s",
                            color = RedLive,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (!capthaChecked) {
                                loginMessage = "reCAPTCHA ভেরিফাই করুন!"
                                return@Button
                            }
                            val res = viewModel.loginAdmin(passwordInput)
                            if (res == "SUCCESS") {
                                loginMessage = "লগইন সফল!"
                            } else {
                                loginMessage = res
                            }
                        },
                        enabled = !isBlocked,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(text = "লগইন করুন", fontWeight = FontWeight.Black)
                    }
                }
            }
        } else {
            // Logged in admin control desk layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "KhelaGhor অ্যাডমিন প্যানেল",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                TextButton(
                    onClick = { viewModel.logoutAdmin() },
                    colors = ButtonDefaults.textButtonColors(contentColor = RedLive)
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Log out")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "লগআউট", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub tabs bar selector: Matches, Channels, Settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Matches", "Channels", "Settings").forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ForestGreen else Color.Transparent)
                            .clickable { activeTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            color = if (isSelected) Color.White else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Panel screens mapping
            when (activeTab) {
                "Matches" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkGreenSurface),
                                border = BorderStroke(1.dp, ForestGreen)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "নতুন ম্যাচ যোগ করুন",
                                        color = NeonGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    // Category selector Cricket/Football/Others
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        listOf("Cricket", "Football", "Others").forEach { cat ->
                                            val isSelected = gameCategory == cat
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(if (isSelected) ForestGreen else Color.Black, RoundedCornerShape(20.dp))
                                                    .border(1.dp, if (isSelected) NeonGreen else Color.Gray, RoundedCornerShape(20.dp))
                                                    .clickable { gameCategory = cat }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = cat, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Team names Row
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        TextField(
                                            value = team1Name,
                                            onValueChange = { team1Name = it },
                                            label = { Text("১ম দল") },
                                            modifier = Modifier.weight(1f),
                                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black)
                                        )
                                        TextField(
                                            value = team2Name,
                                            onValueChange = { team2Name = it },
                                            label = { Text("২য় দল") },
                                            modifier = Modifier.weight(1f),
                                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Logos URLs
                                    TextField(
                                        value = team1Logo,
                                        onValueChange = { team1Logo = it },
                                        label = { Text("১ম দলের লোগো URL") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    TextField(
                                        value = team2Logo,
                                        onValueChange = { team2Logo = it },
                                        label = { Text("২য় দলের লোগো URL") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Streaming source link url
                                    TextField(
                                        value = streamUrl,
                                        onValueChange = { streamUrl = it },
                                        label = { Text("m3u8 বা আইপি লিংক") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    TextField(
                                        value = tournamentName,
                                        onValueChange = { tournamentName = it },
                                        label = { Text("টুর্নামেন্টের নাম (যেমন: FIFA, IPL)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Live/Upcoming toggle Switch selector
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "ম্যাচের বর্তমান স্ট্যাটাস:", color = Color.White, fontSize = 13.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            listOf("LIVE", "UPCOMING").forEach { mode ->
                                                val isSelected = matchStatus == mode
                                                Box(
                                                    modifier = Modifier
                                                        .background(if (isSelected) ForestGreen else Color.Black, RoundedCornerShape(8.dp))
                                                        .border(1.dp, if (isSelected) NeonGreen else Color.Gray, RoundedCornerShape(8.dp))
                                                        .clickable { matchStatus = mode }
                                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                                ) {
                                                    Text(text = mode, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    if (matchStatus == "UPCOMING") {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        TextField(
                                            value = startTimeHours,
                                            onValueChange = { startTimeHours = it },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            label = { Text("কয় ঘণ্টা পর শুরু হবে? (যেমন: ২.৫)") },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            if (team1Name.isBlank() || team2Name.isBlank() || streamUrl.isBlank() || tournamentName.isBlank()) {
                                                return@Button
                                            }
                                            viewModel.addNewMatch(
                                                category = gameCategory,
                                                team1Name = team1Name,
                                                team1Logo = team1Logo,
                                                team2Name = team2Name,
                                                team2Logo = team2Logo,
                                                streamUrl = streamUrl,
                                                tournament = tournamentName,
                                                status = matchStatus,
                                                startTimeDelayHours = startTimeHours.toDoubleOrNull() ?: 1.0
                                            )
                                            // Reset forms
                                            team1Name = ""
                                            team1Logo = ""
                                            team2Name = ""
                                            team2Logo = ""
                                            streamUrl = ""
                                            tournamentName = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "ম্যাচটি ডেটাবেজে সেভ করুন", fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }

                        // Matches remove lists
                        item {
                            Text(text = "ম্যাচ রিমুভ করুন (Finish Matches)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        items(allMatches) { match ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkGreenSurface, RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "${match.team1Name} VS ${match.team2Name}", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(text = "Status: ${match.status} | Cat: ${match.category}", color = GrayText, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { viewModel.finishAndRemoveMatch(match.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RedLive, contentColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(text = "Finish/End Match", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                "Channels" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkGreenSurface),
                                border = BorderStroke(1.dp, ForestGreen)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "নতুন ক্যাটাগরি ও টিভি চ্যানেল যোগ করুন",
                                        color = NeonGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    TextField(
                                        value = charCategoryName,
                                        onValueChange = { charCategoryName = it },
                                        label = { Text("ক্যাটাগরি ফোল্ডার (যেমন: Bangladesh, International)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    TextField(
                                        value = channelName,
                                        onValueChange = { channelName = it },
                                        label = { Text("চ্যানেলের নাম (যেমন: GTV Sports, T Sports)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    TextField(
                                        value = channelLogoUrl,
                                        onValueChange = { channelLogoUrl = it },
                                        label = { Text("চ্যানেলের লোগো URL (ঐচ্ছিক)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    TextField(
                                        value = channelStreamUrl,
                                        onValueChange = { channelStreamUrl = it },
                                        label = { Text("লাইভ স্ট্রিম লিংক URL") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            if (charCategoryName.isBlank() || channelName.isBlank() || channelStreamUrl.isBlank()) {
                                                return@Button
                                            }
                                            viewModel.addNewChannel(
                                                categoryName = charCategoryName,
                                                channelName = channelName,
                                                channelLogoUrl = channelLogoUrl,
                                                streamUrl = channelStreamUrl
                                            )
                                            channelName = ""
                                            channelLogoUrl = ""
                                            channelStreamUrl = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "চ্যানেলটি ডেটাবেজে সেভ করুন", fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }

                        item {
                            Text(text = "বিদ্যমান টিভি চ্যানেল রিমুভ করুন", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        items(allChannels) { channel ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkGreenSurface, RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = channel.channelName, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(text = "ক্যাটাগরি: ${channel.categoryName}", color = GrayText, fontSize = 11.sp)
                                }

                                IconButton(onClick = { viewModel.removeChannel(channel.id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Channel", tint = RedLive)
                                }
                            }
                        }
                    }
                }

                "Settings" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkGreenSurface, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = "বিজ্ঞাপন প্লেসমেন্ট সেটিংস (Ad Controllers)", color = NeonGreen, fontWeight = FontWeight.Bold)

                        // Ads Master Toggle setting
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "মাস্টার বিজ্ঞাপন অন/অফ সুইচ", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(text = "সব বিজ্ঞাপন তাত্ক্ষণিকভাবে অন/অফ করুন", color = GrayText, fontSize = 10.sp)
                            }

                            Switch(
                                checked = appConfig?.adsEnabled ?: true,
                                onCheckedChange = { viewModel.toggleAds(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                            )
                        }

                        // Ads Config inputs
                        Text(text = "ব্যানার ও পপ-আন্ডার ইউআরএল এবং অ্যাড কোড সেটিংস", color = NeonGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))

                        TextField(
                            value = bannerUrlInput,
                            onValueChange = { bannerUrlInput = it },
                            label = { Text("ব্যানার বিজ্ঞাপন 728x90 URL") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black)
                        )

                        TextField(
                            value = bannerCodeInput,
                            onValueChange = { bannerCodeInput = it },
                            label = { Text("⚡ Adsterra 728x90 ব্যানার স্ক্রিপ্ট কোড (HTML/JS Code)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black),
                            maxLines = 4
                        )

                        TextField(
                            value = popUrlInput,
                            onValueChange = { popUrlInput = it },
                            label = { Text("পপ-আন্ডার প্রমোশনাল Redirect URL") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black)
                        )

                        TextField(
                            value = popCodeInput,
                            onValueChange = { popCodeInput = it },
                            label = { Text("⚡ Adsterra পপ-আন্ডার স্ক্রিপ্ট কোড (HTML/JS Code)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black),
                            maxLines = 4
                        )

                        if (adSettingsSucceed.isNotEmpty()) {
                            Text(text = adSettingsSucceed, color = NeonGreen, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.updateAdUrls(
                                    bannerUrl = bannerUrlInput,
                                    popUrl = popUrlInput,
                                    bannerCode = bannerCodeInput,
                                    popCode = popCodeInput
                                )
                                adSettingsSucceed = "বিজ্ঞাপন সেটিংস সফলভাবে সেভ করা হয়েছে!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "বিজ্ঞাপন সেটিংস সেভ করুন", fontWeight = FontWeight.Black)
                        }

                        Text(text = "পাসওয়ার্ড পরিবর্তন করুন (Change Password)", color = NeonGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))

                        TextField(
                            value = oldPassword,
                            onValueChange = { oldPassword = it },
                            visualTransformation = PasswordVisualTransformation(),
                            label = { Text("বর্তমান পাসওয়ার্ড") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black)
                        )

                        TextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            visualTransformation = PasswordVisualTransformation(),
                            label = { Text("নতুন পাসওয়ার্ড") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black)
                        )

                        if (passChangeMessage.isNotEmpty()) {
                            Text(
                                text = passChangeMessage,
                                color = if (passChangeMessage.contains("সফল")) NeonGreen else RedLive,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Button(
                            onClick = {
                                if (oldPassword.isBlank() || newPassword.isBlank()) return@Button
                                val ok = viewModel.changeAdminPassword(oldPassword, newPassword)
                                if (ok) {
                                    passChangeMessage = "পাসওয়ার্ড সফলভাবে পরিবর্তিত হয়েছে!"
                                    oldPassword = ""
                                    newPassword = ""
                                } else {
                                    passChangeMessage = "বর্তমান পাসওয়ার্ড ভুল!"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "পাসওয়ার্ড আপডেট করুন", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}
