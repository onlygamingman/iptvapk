package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// ==========================================
// Entities
// ==========================================

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // e.g. "Cricket", "Football", "Others"
    val team1Name: String,
    val team1LogoUrl: String,
    val team2Name: String,
    val team2LogoUrl: String,
    val streamUrl: String,
    val tournament: String,
    val status: String, // "LIVE" or "UPCOMING"
    val startTimeStamp: Long = 0L, // When UPCOMING, timestamp in MS
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryName: String, // e.g., "Bangladesh", "International"
    val channelName: String,
    val channelLogoUrl: String,
    val streamUrl: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val id: Int = 1,
    val adsEnabled: Boolean = true,
    val bannerAdUrl: String = "https://www.google.com",
    val popUnderUrl: String = "https://www.google.com",
    val bannerAdCode: String = "",
    val popUnderCode: String = "",
    val adminPasswordHash: String = "admin123", // Default admin password
    val apiSyncEnabled: Boolean = false,
    val apiSyncUrl: String = ""
)

// ==========================================
// DAOs
// ==========================================

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY status DESC, startTimeStamp ASC, addedAt DESC")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity)

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Delete
    suspend fun deleteMatch(match: MatchEntity)

    @Query("DELETE FROM matches WHERE id = :id")
    suspend fun deleteMatchById(id: Int)

    @Query("SELECT * FROM matches WHERE id = :id LIMIT 1")
    suspend fun getMatchById(id: Int): MatchEntity?
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY addedAt DESC")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT DISTINCT categoryName FROM channels")
    fun getDistinctCategories(): Flow<List<String>>

    @Query("SELECT * FROM channels WHERE categoryName = :categoryName ORDER BY addedAt DESC")
    fun getChannelsByCategory(categoryName: String): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Delete
    suspend fun deleteChannel(channel: ChannelEntity)

    @Query("DELETE FROM channels WHERE id = :id")
    suspend fun deleteChannelById(id: Int)
}

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<AppConfigEntity?>

    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigOnce(): AppConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: AppConfigEntity)
}

// ==========================================
// Database
// ==========================================

@Database(entities = [MatchEntity::class, ChannelEntity::class, AppConfigEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao
    abstract fun channelDao(): ChannelDao
    abstract fun appConfigDao(): AppConfigDao
}
