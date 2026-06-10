package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.security.MessageDigest

class SportsRepository(context: Context) {

    private val db: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "khelaghor_db"
    ).fallbackToDestructiveMigration().build()

    private val matchDao = db.matchDao()
    private val channelDao = db.channelDao()
    private val appConfigDao = db.appConfigDao()

    val allMatches: Flow<List<MatchEntity>> = matchDao.getAllMatches()
    val allChannels: Flow<List<ChannelEntity>> = channelDao.getAllChannels()
    val distinctChannelCategories: Flow<List<String>> = channelDao.getDistinctCategories()
    val appConfig: Flow<AppConfigEntity?> = appConfigDao.getConfig()

    init {
        // Seeding initial data on a background thread if db is empty
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            seedInitialConfig()
            seedInitialMatches()
            seedInitialChannels()
        }
    }

    private suspend fun seedInitialConfig() {
        val existing = appConfigDao.getConfigOnce()
        if (existing == null) {
            val defaultConfig = AppConfigEntity(
                id = 1,
                adsEnabled = true,
                bannerAdUrl = "https://gotechbd.com",
                popUnderUrl = "https://gotechbd.com",
                adminPasswordHash = hashPassword("admin123"), // Seeded default password
                apiSyncEnabled = false,
                apiSyncUrl = "",
                firebaseSyncEnabled = true,
                firebaseDatabaseUrl = "https://khelaghor-44301-default-rtdb.firebaseio.com"
            )
            appConfigDao.insertOrUpdateConfig(defaultConfig)
        } else if (existing.firebaseDatabaseUrl.isBlank()) {
            appConfigDao.insertOrUpdateConfig(existing.copy(
                firebaseDatabaseUrl = "https://khelaghor-44301-default-rtdb.firebaseio.com",
                firebaseSyncEnabled = true
            ))
        }
    }

    private suspend fun seedInitialMatches() {
        val count = matchDao.getAllMatches().firstOrNull()?.size ?: 0
        if (count == 0) {
            // Add a LIVE Football match
            matchDao.insertMatch(
                MatchEntity(
                    category = "Football",
                    team1Name = "BAN",
                    team1LogoUrl = "https://img.icons8.com/color/120/bangladesh-flag.png",
                    team2Name = "NZ",
                    team2LogoUrl = "https://img.icons8.com/color/120/new-zealand-flag.png",
                    streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", // Reliable HLS test stream
                    tournament = "FIFA FRIENDLY",
                    status = "LIVE",
                    startTimeStamp = 0L
                )
            )

            // Add a LIVE Cricket match
            matchDao.insertMatch(
                MatchEntity(
                    category = "Cricket",
                    team1Name = "IND",
                    team1LogoUrl = "https://img.icons8.com/color/120/india-flag.png",
                    team2Name = "AUS",
                    team2LogoUrl = "https://img.icons8.com/color/120/australia-flag.png",
                    streamUrl = "https://demo.unified-streaming.com/k8s/live/stable/sintel.isml/.m3u8",
                    tournament = "IPL T20",
                    status = "LIVE",
                    startTimeStamp = 0L
                )
            )

            // Add an UPCOMING Match
            matchDao.insertMatch(
                MatchEntity(
                    category = "Football",
                    team1Name = "ARG",
                    team1LogoUrl = "https://img.icons8.com/color/120/argentina-flag.png",
                    team2Name = "FRA",
                    team2LogoUrl = "https://img.icons8.com/color/120/france-flag.png",
                    streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                    tournament = "COPA AMERICA",
                    status = "UPCOMING",
                    startTimeStamp = System.currentTimeMillis() + (3600 * 3 * 1000) // starts in 3 hours
                )
            )
        }
    }

    private suspend fun seedInitialChannels() {
        val count = channelDao.getAllChannels().firstOrNull()?.size ?: 0
        if (count == 0) {
            // Bangladesh Category
            channelDao.insertChannel(
                ChannelEntity(
                    categoryName = "Bangladesh",
                    channelName = "T Sports",
                    channelLogoUrl = "https://seeklogo.com/images/T/t-sports-logo-0E4FE46A6A-seeklogo.com.png",
                    streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                )
            )
            channelDao.insertChannel(
                ChannelEntity(
                    categoryName = "Bangladesh",
                    channelName = "GTV Sports",
                    channelLogoUrl = "https://upload.wikimedia.org/wikipedia/en/3/30/Gazi_TV_logo.png",
                    streamUrl = "https://demo.unified-streaming.com/k8s/live/stable/sintel.isml/.m3u8"
                )
            )

            // International Category
            channelDao.insertChannel(
                ChannelEntity(
                    categoryName = "International",
                    channelName = "Sky Sports LIVE",
                    channelLogoUrl = "https://upload.wikimedia.org/wikipedia/commons/d/df/Sky-sports-logo-2020.jpg",
                    streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                )
            )
            channelDao.insertChannel(
                ChannelEntity(
                    categoryName = "International",
                    channelName = "Sony Ten 1",
                    channelLogoUrl = "https://upload.wikimedia.org/wikipedia/commons/2/23/Logo_of_Sony_Sports_Network.png",
                    streamUrl = "https://demo.unified-streaming.com/k8s/live/stable/sintel.isml/.m3u8"
                )
            )
        }
    }

    // ==========================================
    // Operations
    // ==========================================

    suspend fun addMatch(match: MatchEntity) = matchDao.insertMatch(match)
    suspend fun updateMatch(match: MatchEntity) = matchDao.updateMatch(match)
    suspend fun deleteMatchById(id: Int) = matchDao.deleteMatchById(id)

    suspend fun addChannel(channel: ChannelEntity) = channelDao.insertChannel(channel)
    suspend fun deleteChannelById(id: Int) = channelDao.deleteChannelById(id)

    suspend fun updateConfig(config: AppConfigEntity) = appConfigDao.insertOrUpdateConfig(config)
    suspend fun getConfigOnce(): AppConfigEntity? = appConfigDao.getConfigOnce()

    fun getChannelsByCategory(category: String): Flow<List<ChannelEntity>> =
        channelDao.getChannelsByCategory(category)

    // ==========================================
    // Security Helper: SHA-256 Hashing
    // ==========================================
    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
