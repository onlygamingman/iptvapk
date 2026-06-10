package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SportsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SportsRepository(application)

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

    init {
        // Real-time ticking system every 1 second for countdown timers
        viewModelScope.launch {
            while (true) {
                _currentTime.value = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    // ==========================================
    // Match Actions
    // ==========================================
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
            val match = MatchEntity(
                category = category,
                team1Name = team1Name,
                team1LogoUrl = team1Logo.ifBlank { "https://cdn-icons-png.flaticon.com/512/53/53283.png" },
                team2Name = team2Name,
                team2LogoUrl = team2Logo.ifBlank { "https://cdn-icons-png.flaticon.com/512/53/53283.png" },
                streamUrl = streamUrl,
                tournament = tournament,
                status = status,
                startTimeStamp = startTimeStamp
            )
            repository.addMatch(match)
        }
    }

    fun finishAndRemoveMatch(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMatchById(id)
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
            val channel = ChannelEntity(
                categoryName = categoryName,
                channelName = channelName,
                channelLogoUrl = channelLogoUrl.ifBlank { "https://photos.stream/tv.png" },
                streamUrl = streamUrl
            )
            repository.addChannel(channel)
        }
    }

    fun removeChannel(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteChannelById(id)
        }
    }

    // ==========================================
    // Settings Actions
    // ==========================================
    fun toggleAds(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getConfigOnce() ?: AppConfigEntity()
            repository.updateConfig(current.copy(adsEnabled = enabled))
        }
    }

    fun updateAdUrls(bannerUrl: String, popUrl: String, bannerCode: String, popCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getConfigOnce() ?: AppConfigEntity()
            repository.updateConfig(current.copy(
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
            repository.updateConfig(current.copy(
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
        val currentPassHash = run {
            var hash = ""
            // Simple blocking operation to load DB quickly on scope thread
            val thread = Thread {
                kotlin.runCatching {
                    val dbHash = viewModelScope.launch(Dispatchers.IO) {
                        val c = repository.getConfigOnce()
                        hash = c?.adminPasswordHash ?: ""
                    }
                }
            }
            thread.start()
            thread.join(1000)
            hash
        }

        val inputOldHash = repository.hashPassword(oldPasswordClear)
        if (inputOldHash == currentPassHash || currentPassHash == "admin123") {
            viewModelScope.launch(Dispatchers.IO) {
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

        var match = false
        val job = viewModelScope.launch(Dispatchers.IO) {
            val config = repository.getConfigOnce()
            val expectedHash = config?.adminPasswordHash ?: repository.hashPassword("admin123")
            val inputHash = repository.hashPassword(passwordClear)
            match = inputHash == expectedHash || passwordClear == "admin123" // Fallback fallback password
        }

        // Wait brief time
        run {
            val t = Thread {
                try { Thread.sleep(300) } catch (e: Exception) {}
            }
            t.start()
            t.join()
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
}
