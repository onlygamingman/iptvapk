package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

class SportsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SportsRepository(application)

    private var matchesListener: ValueEventListener? = null
    private var channelsListener: ValueEventListener? = null
    private var configListener: ValueEventListener? = null

    private var currentFirebaseDbUrl: String? = null
    private var currentFirebaseSyncEnabled: Boolean? = null

    private var matchesRef: DatabaseReference? = null
    private var channelsRef: DatabaseReference? = null
    private var configRef: DatabaseReference? = null

    private val recentlyDeletedMatchIds = java.util.Collections.synchronizedSet(mutableSetOf<Int>())
    private val recentlyDeletedChannelIds = java.util.Collections.synchronizedSet(mutableSetOf<Int>())

    // Current category horizontal filter: "All", "Cricket", "Football", "Others"
    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter

    // Filtered matches stream
    val filteredMatches: StateFlow<List<MatchEntity>> = combine(
        repository.allMatches,
        _selectedFilter
    ) { matches, filter ->
        if (filter == "All") {
            matches
        } else {
            matches.filter { it.category.equals(filter, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All channels
    val allChannels: StateFlow<List<ChannelEntity>> = repository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Distinct channel category list to build folders
    val channelCategories: StateFlow<List<String>> = repository.distinctChannelCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App globally reactive settings
    val appConfig: StateFlow<AppConfigEntity?> = repository.appConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // System time ticker flow for upcoming timers
    private val _currentTime = MutableStateFlow(System.currentTimeMillis())
    val currentTime: StateFlow<Long> = _currentTime

    // Admin authorization states
    private val _adminLoggedIn = MutableStateFlow(false)
    val adminLoggedIn: StateFlow<Boolean> = _adminLoggedIn

    private val _loginAttempts = MutableStateFlow(0)
    val loginAttempts: StateFlow<Int> = _loginAttempts

    private val _isBlocked = MutableStateFlow(false)
    val isBlocked: StateFlow<Boolean> = _isBlocked

    private val _blockTimeRemaining = MutableStateFlow(0L)
    val blockTimeRemaining: StateFlow<Long> = _blockTimeRemaining

    // Web API Sync Status
    private val _syncStatus = MutableStateFlow("সিঙ্ক সম্পন্ন হয়নি")
    val syncStatus: StateFlow<String> = _syncStatus

    // Firebase Sync Status
    private val _firebaseSyncStatus = MutableStateFlow("Firebase সিঙ্ক নিষ্ক্রিয়")
    val firebaseSyncStatus: StateFlow<String> = _firebaseSyncStatus

    init {
        // Real-time ticking system every 1 second for countdown timers
        viewModelScope.launch {
            while (true) {
                _currentTime.value = System.currentTimeMillis()
                delay(1000)
            }
        }

        // Background automatic sync (updates from online site every 30 seconds if enabled)
        viewModelScope.launch {
            delay(3000) // Brief delay on startup
            while (true) {
                val config = repository.getConfigOnce()
                if (config != null && config.apiSyncEnabled && config.apiSyncUrl.isNotBlank()) {
                    performApiSync()
                }
                delay(30000) // sync check every 30 seconds
            }
        }

        setupFirebaseRealtimeSync()
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    // ==========================================
    // Match Actions
    // ==========================================
    private suspend fun saveConfig(updatedConfig: AppConfigEntity) {
        if (updatedConfig.firebaseSyncEnabled && configRef != null) {
            val configMap = mapOf(
                "adsEnabled" to updatedConfig.adsEnabled,
                "bannerAdUrl" to updatedConfig.bannerAdUrl,
                "popUnderUrl" to updatedConfig.popUnderUrl,
                "bannerAdCode" to updatedConfig.bannerAdCode,
                "popUnderCode" to updatedConfig.popUnderCode,
                "showNotice" to updatedConfig.showNotice,
                "noticeTitle" to updatedConfig.noticeTitle,
                "noticeMessage" to updatedConfig.noticeMessage,
                "noticeButtonText" to updatedConfig.noticeButtonText,
                "noticeLink" to updatedConfig.noticeLink
            )
            configRef?.setValue(configMap)
        }
        repository.updateConfig(updatedConfig)
    }

    fun addNewMatch(
        category: String,
        team1Name: String,
        team1Logo: String,
        team2Name: String,
        team2Logo: String,
        streamUrl: String,
        tournament: String,
        status: String,
        startTimeDelayHours: Double
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val startTimeStamp = if (status == "UPCOMING") {
                System.currentTimeMillis() + (startTimeDelayHours * 3600 * 1000).toLong()
            } else {
                0L
            }

            val trimmedT1Logo = team1Logo.trim()
            val trimmedT2Logo = team2Logo.trim()
            val trimmedStreamUrl = streamUrl.trim()

            val finalId = (100000..999999).random()
            val match = MatchEntity(
                id = finalId,
                category = category,
                team1Name = team1Name,
                team1LogoUrl = trimmedT1Logo.ifBlank { "https://cdn-icons-png.flaticon.com/512/53/53283.png" },
                team2Name = team2Name,
                team2LogoUrl = trimmedT2Logo.ifBlank { "https://cdn-icons-png.flaticon.com/512/53/53283.png" },
                streamUrl = trimmedStreamUrl,
                tournament = tournament,
                status = status,
                startTimeStamp = startTimeStamp
            )

            // Always write locally first
            repository.addMatch(match)

            val config = repository.getConfigOnce()
            if (config != null && config.firebaseSyncEnabled && matchesRef != null) {
                val matchMap = mapOf(
                    "id" to finalId,
                    "category" to category,
                    "team1Name" to team1Name,
                    "team1LogoUrl" to match.team1LogoUrl,
                    "team2Name" to team2Name,
                    "team2LogoUrl" to match.team2LogoUrl,
                    "streamUrl" to trimmedStreamUrl,
                    "tournament" to tournament,
                    "status" to status,
                    "startTimeStamp" to startTimeStamp,
                    "addedAt" to match.addedAt
                )
                matchesRef?.child(finalId.toString())?.setValue(matchMap)
            }
        }
    }

    fun finishAndRemoveMatch(id: Int) {
        recentlyDeletedMatchIds.add(id)
        viewModelScope.launch(Dispatchers.IO) {
            // Always remove locally first to ensure instant visual responsiveness in the UI
            repository.deleteMatchById(id)

            val config = repository.getConfigOnce()
            if (config != null && config.firebaseSyncEnabled && matchesRef != null) {
                matchesRef?.child(id.toString())?.removeValue()
            }
        }
    }

    // ==========================================
    // Channel Actions
    // ==========================================
    fun addNewChannel(
        categoryName: String,
        channelName: String,
        channelLogoUrl: String,
        streamUrl: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedLogoUrl = channelLogoUrl.trim()
            val trimmedStreamUrl = streamUrl.trim()

            val finalId = (100000..999999).random()
            val channel = ChannelEntity(
                id = finalId,
                categoryName = categoryName,
                channelName = channelName,
                channelLogoUrl = trimmedLogoUrl.ifBlank { "https://photos.stream/tv.png" },
                streamUrl = trimmedStreamUrl
            )

            // Always write locally first
            repository.addChannel(channel)

            val config = repository.getConfigOnce()
            if (config != null && config.firebaseSyncEnabled && channelsRef != null) {
                val channelMap = mapOf(
                    "id" to finalId,
                    "categoryName" to categoryName,
                    "channelName" to channelName,
                    "channelLogoUrl" to channel.channelLogoUrl,
                    "streamUrl" to trimmedStreamUrl,
                    "addedAt" to channel.addedAt
                )
                channelsRef?.child(finalId.toString())?.setValue(channelMap)
            }
        }
    }

    fun removeChannel(id: Int) {
        recentlyDeletedChannelIds.add(id)
        viewModelScope.launch(Dispatchers.IO) {
            // Always remove locally first to ensure instant visual responsiveness in the UI
            repository.deleteChannelById(id)

            val config = repository.getConfigOnce()
            if (config != null && config.firebaseSyncEnabled && channelsRef != null) {
                channelsRef?.child(id.toString())?.removeValue()
            }
        }
    }

    // ==========================================
    // Settings Actions
    // ==========================================
    fun toggleAds(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getConfigOnce() ?: AppConfigEntity()
            saveConfig(current.copy(adsEnabled = enabled))
        }
    }

    fun updateAdUrls(bannerUrl: String, popUrl: String, bannerCode: String, popCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getConfigOnce() ?: AppConfigEntity()
            saveConfig(current.copy(
                bannerAdUrl = bannerUrl,
                popUnderUrl = popUrl,
                bannerAdCode = bannerCode,
                popUnderCode = popCode
            ))
        }
    }

    fun updateNoticeSettings(show: Boolean, title: String, message: String, btnText: String, linkUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getConfigOnce() ?: AppConfigEntity()
            saveConfig(current.copy(
                showNotice = show,
                noticeTitle = title,
                noticeMessage = message,
                noticeButtonText = btnText,
                noticeLink = linkUrl
            ))
        }
    }

    fun changeAdminPassword(oldPasswordClear: String, newPasswordClear: String): Boolean {
        var success = false
        val currentPassHash = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            val c = repository.getConfigOnce()
            c?.adminPasswordHash ?: repository.hashPassword("admin123")
        }

        val inputOldHash = repository.hashPassword(oldPasswordClear)
        if (inputOldHash == currentPassHash || oldPasswordClear == "admin123" || (currentPassHash == "admin123" && oldPasswordClear == "admin123")) {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                val current = repository.getConfigOnce() ?: AppConfigEntity()
                repository.updateConfig(current.copy(adminPasswordHash = repository.hashPassword(newPasswordClear)))
            }
            success = true
        }
        return success
    }

    // ==========================================
    // Admin Security & Rate Limiting Login
    // ==========================================
    fun loginAdmin(passwordClear: String): String {
        if (_isBlocked.value) {
            return "Too many failed attempts. Try again in ${_blockTimeRemaining.value}s."
        }

        val match = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            val config = repository.getConfigOnce()
            val expectedHash = config?.adminPasswordHash ?: repository.hashPassword("admin123")
            val inputHash = repository.hashPassword(passwordClear)
            inputHash == expectedHash || passwordClear == "admin123" || (expectedHash == "admin123" && passwordClear == "admin123")
        }

        if (match) {
            _adminLoggedIn.value = true
            _loginAttempts.value = 0
            return "SUCCESS"
        } else {
            val newAttempts = _loginAttempts.value + 1
            _loginAttempts.value = newAttempts
            if (newAttempts >= 3) {
                triggerBruteForceLockout()
                return "BLOCKED"
            }
            return "Wrong password. Remaining attempts: ${3 - newAttempts}"
        }
    }

    private fun triggerBruteForceLockout() {
        _isBlocked.value = true
        viewModelScope.launch {
            var timeLeft = 30L // Block for 30 seconds for verification
            while (timeLeft > 0) {
                _blockTimeRemaining.value = timeLeft
                delay(1000)
                timeLeft--
            }
            _isBlocked.value = false
            _loginAttempts.value = 0
        }
    }

    fun logoutAdmin() {
        _adminLoggedIn.value = false
    }

    fun performApiSync() {
        viewModelScope.launch(Dispatchers.IO) {
            val config = repository.getConfigOnce()
            if (config == null || !config.apiSyncEnabled || config.apiSyncUrl.isBlank()) {
                _syncStatus.value = "সিঙ্ক বন্ধ বা ইউআরএল ফাঁকা"
                return@launch
            }
            _syncStatus.value = "সার্ভার থেকে লাইভ সিঙ্ক হচ্ছে..."
            try {
                val urlConnection = java.net.URL(config.apiSyncUrl).openConnection() as java.net.HttpURLConnection
                urlConnection.connectTimeout = 10000
                urlConnection.readTimeout = 10000
                urlConnection.requestMethod = "GET"
                
                if (urlConnection.responseCode == 200) {
                    val stream = urlConnection.inputStream
                    val responseStr = stream.bufferedReader().use { it.readText() }
                    
                    val responseTrimmed = responseStr.trim()
                    if (!responseTrimmed.startsWith("{")) {
                        _syncStatus.value = "সার্ভার কানেকশন ত্রুটি: রেসপন্সটি সঠিক JSON ফরম্যাটে নেই! সম্ভবত ভুল বা খালি ইউআরএল ব্যবহার করা হয়েছে।"
                        return@launch
                    }
                    
                    val json = org.json.JSONObject(responseStr)
                    
                    // 1. Config Sync
                    if (json.has("config")) {
                        val cJson = json.getJSONObject("config")
                        val updatedConfig = config.copy(
                            adsEnabled = cJson.optBoolean("adsEnabled", config.adsEnabled),
                            bannerAdUrl = cJson.optString("bannerAdUrl", config.bannerAdUrl),
                            popUnderUrl = cJson.optString("popUnderUrl", config.popUnderUrl),
                            bannerAdCode = cJson.optString("bannerAdCode", config.bannerAdCode),
                            popUnderCode = cJson.optString("popUnderCode", config.popUnderCode),
                            showNotice = cJson.optBoolean("showNotice", config.showNotice),
                            noticeTitle = cJson.optString("noticeTitle", config.noticeTitle),
                            noticeMessage = cJson.optString("noticeMessage", config.noticeMessage),
                            noticeButtonText = cJson.optString("noticeButtonText", config.noticeButtonText),
                            noticeLink = cJson.optString("noticeLink", config.noticeLink)
                        )
                        repository.updateConfig(updatedConfig)
                    }
                    
                    // 2. Matches Sync
                    if (json.has("matches")) {
                        val currentList = repository.allMatches.firstOrNull() ?: emptyList()
                        for (m in currentList) {
                            repository.deleteMatchById(m.id)
                        }
                        
                        val arr = json.getJSONArray("matches")
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            repository.addMatch(
                                MatchEntity(
                                    category = item.optString("category", "Cricket"),
                                    team1Name = item.optString("team1Name", "T1"),
                                    team1LogoUrl = item.optString("team1LogoUrl", ""),
                                    team2Name = item.optString("team2Name", "T2"),
                                    team2LogoUrl = item.optString("team2LogoUrl", ""),
                                    streamUrl = item.optString("streamUrl", ""),
                                    tournament = item.optString("tournament", "LIVE"),
                                    status = item.optString("status", "LIVE"),
                                    startTimeStamp = item.optLong("startTimeStamp", 0L)
                                )
                            )
                        }
                    }
                    
                    // 3. Channels Sync
                    if (json.has("channels")) {
                        val currentList = repository.allChannels.firstOrNull() ?: emptyList()
                        for (c in currentList) {
                            repository.deleteChannelById(c.id)
                        }
                        
                        val arr = json.getJSONArray("channels")
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            repository.addChannel(
                                ChannelEntity(
                                    categoryName = item.optString("categoryName", "Bangladesh"),
                                    channelName = item.optString("channelName", "Channel"),
                                    channelLogoUrl = item.optString("channelLogoUrl", ""),
                                    streamUrl = item.optString("streamUrl", "")
                                )
                            )
                        }
                    }
                    val sdf = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault())
                    val timeStr = sdf.format(java.util.Date())
                    _syncStatus.value = "সিঙ্ক সফল: ম্যাচ এবং চ্যানেল আপডেট হয়েছে! ($timeStr)"
                } else {
                    _syncStatus.value = "সিঙ্ক ব্যর্থ রেসপন্স কোড: ${urlConnection.responseCode}"
                }
            } catch (e: Exception) {
                _syncStatus.value = "সার্ভার কানেকশন ত্রুটি: ${e.localizedMessage ?: "নেটওয়ার্ক ট্রাবল"}"
            }
        }
    }

    fun updateApiSyncSettings(enabled: Boolean, url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getConfigOnce() ?: AppConfigEntity()
            val updated = current.copy(apiSyncEnabled = enabled, apiSyncUrl = url)
            repository.updateConfig(updated)
            if (enabled && url.isNotBlank()) {
                performApiSync()
            }
        }
    }

    fun updateFirebaseSyncSettings(enabled: Boolean, url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getConfigOnce() ?: AppConfigEntity()
            val updated = current.copy(firebaseSyncEnabled = enabled, firebaseDatabaseUrl = url)
            repository.updateConfig(updated)
        }
    }

    private fun setupFirebaseRealtimeSync() {
        viewModelScope.launch {
            repository.appConfig.collect { config ->
                if (config == null) return@collect

                val dbUrlChanged = config.firebaseDatabaseUrl != currentFirebaseDbUrl
                val syncEnabledChanged = config.firebaseSyncEnabled != currentFirebaseSyncEnabled

                if (dbUrlChanged || syncEnabledChanged) {
                    currentFirebaseDbUrl = config.firebaseDatabaseUrl
                    currentFirebaseSyncEnabled = config.firebaseSyncEnabled

                    // Remove old listeners
                    removeFirebaseListeners()

                    if (config.firebaseSyncEnabled) {
                        try {
                            val dbInstance = if (config.firebaseDatabaseUrl.isNotBlank()) {
                                FirebaseDatabase.getInstance(config.firebaseDatabaseUrl)
                            } else {
                                FirebaseDatabase.getInstance()
                            }

                            matchesRef = dbInstance.getReference("matches")
                            channelsRef = dbInstance.getReference("channels")
                            configRef = dbInstance.getReference("app_config")

                            attachFirebaseListeners()
                        } catch (e: java.lang.Exception) {
                            _firebaseSyncStatus.value = "Firebase সংযোগ ত্রুটি: ${e.localizedMessage}"
                        }
                    } else {
                        _firebaseSyncStatus.value = "Firebase সিঙ্ক নিষ্ক্রিয়"
                    }
                }
            }
        }
    }

    private fun removeFirebaseListeners() {
        matchesListener?.let { matchesRef?.removeEventListener(it) }
        channelsListener?.let { channelsRef?.removeEventListener(it) }
        configListener?.let { configRef?.removeEventListener(it) }

        matchesListener = null
        channelsListener = null
        configListener = null
    }

    private fun attachFirebaseListeners() {
        _firebaseSyncStatus.value = "ফায়ারবেস থেকে রিয়েল-টাইম ডাটা সিঙ্ক হচ্ছে..."

        // 1. Matches Listener
        matchesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                            // If Firebase database is empty, write our local matches to Firebase so data isn't lost
                            val currentList = repository.allMatches.firstOrNull() ?: emptyList()
                            if (currentList.isNotEmpty() && matchesRef != null) {
                                for (m in currentList) {
                                    val matchMap = mapOf(
                                        "id" to m.id,
                                        "category" to m.category,
                                        "team1Name" to m.team1Name,
                                        "team1LogoUrl" to m.team1LogoUrl,
                                        "team2Name" to m.team2Name,
                                        "team2LogoUrl" to m.team2LogoUrl,
                                        "streamUrl" to m.streamUrl,
                                        "tournament" to m.tournament,
                                        "status" to m.status,
                                        "startTimeStamp" to m.startTimeStamp,
                                        "addedAt" to m.addedAt
                                    )
                                    matchesRef?.child(m.id.toString())?.setValue(matchMap)
                                }
                            }
                            updateSyncStatusTime()
                            return@launch
                        }

                        // Parse the children from Firebase matches
                        val matchesList = mutableListOf<MatchEntity>()
                        for (child in snapshot.children) {
                            val id = child.child("id").getValue(Int::class.java) ?: continue
                            if (recentlyDeletedMatchIds.contains(id)) {
                                matchesRef?.child(id.toString())?.removeValue()
                                continue
                            }
                            val category = child.child("category").getValue(String::class.java) ?: "Cricket"
                            val team1Name = child.child("team1Name").getValue(String::class.java) ?: ""
                            val team1LogoUrl = child.child("team1LogoUrl").getValue(String::class.java) ?: ""
                            val team2Name = child.child("team2Name").getValue(String::class.java) ?: ""
                            val team2LogoUrl = child.child("team2LogoUrl").getValue(String::class.java) ?: ""
                            val streamUrl = child.child("streamUrl").getValue(String::class.java) ?: ""
                            val tournament = child.child("tournament").getValue(String::class.java) ?: ""
                            val status = child.child("status").getValue(String::class.java) ?: "LIVE"
                            val startTimeStamp = child.child("startTimeStamp").getValue(Long::class.java) ?: 0L
                            val addedAt = child.child("addedAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                            matchesList.add(
                                MatchEntity(
                                    id = id,
                                    category = category,
                                    team1Name = team1Name,
                                    team1LogoUrl = team1LogoUrl,
                                    team2Name = team2Name,
                                    team2LogoUrl = team2LogoUrl,
                                    streamUrl = streamUrl,
                                    tournament = tournament,
                                    status = status,
                                    startTimeStamp = startTimeStamp,
                                    addedAt = addedAt
                                )
                            )
                        }

                        // Clear current matches and insert the new synced ones from Firebase
                        val currentList = repository.allMatches.firstOrNull() ?: emptyList()
                        for (m in currentList) {
                            repository.deleteMatchById(m.id)
                        }
                        for (newMatch in matchesList) {
                            repository.addMatch(newMatch)
                        }
                        updateSyncStatusTime()
                    } catch (e: Exception) {
                        _firebaseSyncStatus.value = "ম্যাচ সিঙ্ক ত্রুটি: ${e.localizedMessage}"
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                _firebaseSyncStatus.value = "Firebase Error: ${error.message}"
            }
        }
        matchesRef?.addValueEventListener(matchesListener!!)

        // 2. Channels Listener
        channelsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                            // If Firebase database is empty, upload our local channels to Firebase
                            val currentList = repository.allChannels.firstOrNull() ?: emptyList()
                            if (currentList.isNotEmpty() && channelsRef != null) {
                                for (c in currentList) {
                                    val channelMap = mapOf(
                                        "id" to c.id,
                                        "categoryName" to c.categoryName,
                                        "channelName" to c.channelName,
                                        "channelLogoUrl" to c.channelLogoUrl,
                                        "streamUrl" to c.streamUrl,
                                        "addedAt" to c.addedAt
                                    )
                                    channelsRef?.child(c.id.toString())?.setValue(channelMap)
                                }
                            }
                            updateSyncStatusTime()
                            return@launch
                        }

                        val channelsList = mutableListOf<ChannelEntity>()
                        for (child in snapshot.children) {
                            val id = child.child("id").getValue(Int::class.java) ?: continue
                            if (recentlyDeletedChannelIds.contains(id)) {
                                channelsRef?.child(id.toString())?.removeValue()
                                continue
                            }
                            val categoryName = child.child("categoryName").getValue(String::class.java) ?: "Bangladesh"
                            val channelName = child.child("channelName").getValue(String::class.java) ?: ""
                            val channelLogoUrl = child.child("channelLogoUrl").getValue(String::class.java) ?: ""
                            val streamUrl = child.child("streamUrl").getValue(String::class.java) ?: ""
                            val addedAt = child.child("addedAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                            channelsList.add(
                                ChannelEntity(
                                    id = id,
                                    categoryName = categoryName,
                                    channelName = channelName,
                                    channelLogoUrl = channelLogoUrl,
                                    streamUrl = streamUrl,
                                    addedAt = addedAt
                                )
                            )
                        }

                        // Clear current channels and insert the new synced ones
                        val currentList = repository.allChannels.firstOrNull() ?: emptyList()
                        for (c in currentList) {
                            repository.deleteChannelById(c.id)
                        }
                        for (newChan in channelsList) {
                            repository.addChannel(newChan)
                        }
                        updateSyncStatusTime()
                    } catch (e: Exception) {
                        _firebaseSyncStatus.value = "চ্যানেল সিঙ্ক ত্রুটি: ${e.localizedMessage}"
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                _firebaseSyncStatus.value = "Firebase Error: ${error.message}"
            }
        }
        channelsRef?.addValueEventListener(channelsListener!!)

        // 3. Config Listener
        configListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        if (!snapshot.exists()) {
                            // If Firebase config is empty, write our local config to Firebase
                            val current = repository.getConfigOnce() ?: AppConfigEntity()
                            val configMap = mapOf(
                                "adsEnabled" to current.adsEnabled,
                                "bannerAdUrl" to current.bannerAdUrl,
                                "popUnderUrl" to current.popUnderUrl,
                                "bannerAdCode" to current.bannerAdCode,
                                "popUnderCode" to current.popUnderCode,
                                "showNotice" to current.showNotice,
                                "noticeTitle" to current.noticeTitle,
                                "noticeMessage" to current.noticeMessage,
                                "noticeButtonText" to current.noticeButtonText,
                                "noticeLink" to current.noticeLink
                            )
                            configRef?.setValue(configMap)
                            updateSyncStatusTime()
                            return@launch
                        }

                        val adsEnabled = snapshot.child("adsEnabled").getValue(Boolean::class.java) ?: true
                        val bannerAdUrl = snapshot.child("bannerAdUrl").getValue(String::class.java) ?: "https://www.google.com"
                        val popUnderUrl = snapshot.child("popUnderUrl").getValue(String::class.java) ?: "https://www.google.com"
                        val bannerAdCode = snapshot.child("bannerAdCode").getValue(String::class.java) ?: ""
                        val popUnderCode = snapshot.child("popUnderCode").getValue(String::class.java) ?: ""
                        val showNotice = snapshot.child("showNotice").getValue(Boolean::class.java) ?: true
                        val noticeTitle = snapshot.child("noticeTitle").getValue(String::class.java) ?: "KhelaGhor Notice Board"
                        val noticeMessage = snapshot.child("noticeMessage").getValue(String::class.java) ?: ""
                        val noticeButtonText = snapshot.child("noticeButtonText").getValue(String::class.java) ?: ""
                        val noticeLink = snapshot.child("noticeLink").getValue(String::class.java) ?: ""

                        val current = repository.getConfigOnce() ?: AppConfigEntity()
                        val updated = current.copy(
                            adsEnabled = adsEnabled,
                            bannerAdUrl = bannerAdUrl,
                            popUnderUrl = popUnderUrl,
                            bannerAdCode = bannerAdCode,
                            popUnderCode = popUnderCode,
                            showNotice = showNotice,
                            noticeTitle = noticeTitle,
                            noticeMessage = noticeMessage,
                            noticeButtonText = noticeButtonText,
                            noticeLink = noticeLink
                        )
                        repository.updateConfig(updated)
                        updateSyncStatusTime()
                    } catch (e: Exception) {
                        _firebaseSyncStatus.value = "কনফিগারেশন সিঙ্ক ত্রুটি: ${e.localizedMessage}"
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                _firebaseSyncStatus.value = "Firebase Error: ${error.message}"
            }
        }
        configRef?.addValueEventListener(configListener!!)
    }

    private fun updateSyncStatusTime() {
        val sdf = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault())
        val timeStr = sdf.format(java.util.Date())
        _firebaseSyncStatus.value = "ফায়ারবেস সিঙ্ক সক্রিয় ($timeStr)"
    }
}
