package com.codexlabs.auroratv.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "categories",
    primaryKeys = ["section", "remoteId"],
)
data class CategoryEntity(
    val section: String,
    val remoteId: String,
    val name: String,
    val isAdult: Boolean,
    val hidden: Boolean,
    val sortOrder: Int,
)

@Entity(
    tableName = "channels",
    indices = [
        Index("categoryRemoteId"),
        Index("epgChannelId"),
    ],
)
data class ChannelEntity(
    @PrimaryKey val streamId: Long,
    val categoryRemoteId: String,
    val name: String,
    val channelNumber: Int?,
    val logoUrl: String?,
    val epgChannelId: String?,
    val containerExtension: String?,
    val directSource: String?,
    val customSid: String?,
    val isAdult: Boolean,
    val hasCatchup: Boolean,
    val catchupDurationHours: Int,
    val addedAt: Long?,
)

@Entity(
    tableName = "movies",
    indices = [Index("categoryRemoteId")],
)
data class MovieEntity(
    @PrimaryKey val streamId: Long,
    val categoryRemoteId: String,
    val name: String,
    val artworkUrl: String?,
    val plot: String?,
    val rating: String?,
    val releaseYear: String?,
    val containerExtension: String?,
    val directSource: String?,
    val isAdult: Boolean,
    val addedAt: Long?,
)

@Entity(
    tableName = "series",
    indices = [Index("categoryRemoteId")],
)
data class SeriesEntity(
    @PrimaryKey val seriesId: Long,
    val categoryRemoteId: String,
    val name: String,
    val artworkUrl: String?,
    val plot: String?,
    val rating: String?,
    val releaseYear: String?,
    val isAdult: Boolean,
    val addedAt: Long?,
)

@Entity(
    tableName = "episodes",
    indices = [Index("seriesId"), Index("seasonNumber")],
)
data class EpisodeEntity(
    @PrimaryKey val episodeId: Long,
    val seriesId: Long,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val artworkUrl: String?,
    val plot: String?,
    val durationSeconds: Long?,
    val containerExtension: String?,
    val directSource: String?,
    val addedAt: Long?,
)

@Entity(
    tableName = "epg_events",
    primaryKeys = ["channelEpgId", "startEpochMillis"],
)
data class EpgEventEntity(
    val channelEpgId: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val title: String,
    val description: String?,
)

@Entity(
    tableName = "favorite_items",
    primaryKeys = ["targetType", "targetId"],
)
data class FavoriteItemEntity(
    val targetType: String,
    val targetId: String,
    val title: String,
    val subtitle: String?,
    val artworkUrl: String?,
    val addedAt: Long,
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey val id: String,
    val targetType: String,
    val targetId: String,
    val title: String,
    val subtitle: String?,
    val artworkUrl: String?,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedAt: Long,
)

@Entity(tableName = "recent_channels")
data class RecentChannelEntity(
    @PrimaryKey val channelId: Long,
    val title: String,
    val artworkUrl: String?,
    val categoryId: String?,
    val lastPlayedAt: Long,
)

@Dao
interface MediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(items: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannels(items: List<ChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMovies(items: List<MovieEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeries(items: List<SeriesEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEpisodes(items: List<EpisodeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEpg(items: List<EpgEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavorite(item: FavoriteItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaybackHistory(item: PlaybackHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentChannel(item: RecentChannelEntity)

    @Query("DELETE FROM categories WHERE section = :section")
    suspend fun clearCategories(section: String)

    @Query("DELETE FROM channels")
    suspend fun clearChannels()

    @Query("DELETE FROM movies")
    suspend fun clearMovies()

    @Query("DELETE FROM series")
    suspend fun clearSeries()

    @Query("DELETE FROM episodes WHERE seriesId = :seriesId")
    suspend fun clearEpisodesForSeries(seriesId: Long)

    @Query("DELETE FROM epg_events")
    suspend fun clearEpg()

    @Query("DELETE FROM favorite_items WHERE targetType = :targetType AND targetId = :targetId")
    suspend fun deleteFavorite(targetType: String, targetId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_items WHERE targetType = :targetType AND targetId = :targetId)")
    suspend fun isFavorite(targetType: String, targetId: String): Boolean

    @Query("SELECT remoteId FROM categories WHERE section = :section AND hidden = 1")
    suspend fun hiddenCategoryIds(section: String): List<String>

    @Query("UPDATE categories SET hidden = :hidden WHERE section = :section AND remoteId = :remoteId")
    suspend fun setCategoryHidden(section: String, remoteId: String, hidden: Boolean)

    @Query(
        """
        SELECT * FROM categories
        WHERE section = :section
          AND hidden = 0
          AND (:includeAdult = 1 OR isAdult = 0)
        ORDER BY sortOrder, name
        """,
    )
    fun observeVisibleCategories(section: String, includeAdult: Boolean): Flow<List<CategoryEntity>>

    @Query(
        """
        SELECT * FROM channels
        WHERE (:categoryId IS NULL OR categoryRemoteId = :categoryId)
          AND (:includeAdult = 1 OR isAdult = 0)
          AND categoryRemoteId NOT IN (
              SELECT remoteId FROM categories WHERE section = 'live' AND hidden = 1
          )
        ORDER BY
            CASE WHEN channelNumber IS NULL THEN 1 ELSE 0 END,
            channelNumber,
            name
        """,
    )
    fun observeChannels(categoryId: String?, includeAdult: Boolean): Flow<List<ChannelEntity>>

    @Query(
        """
        SELECT * FROM movies
        WHERE (:categoryId IS NULL OR categoryRemoteId = :categoryId)
          AND (:includeAdult = 1 OR isAdult = 0)
          AND categoryRemoteId NOT IN (
              SELECT remoteId FROM categories WHERE section = 'movies' AND hidden = 1
          )
        ORDER BY name
        """,
    )
    fun observeMovies(categoryId: String?, includeAdult: Boolean): Flow<List<MovieEntity>>

    @Query(
        """
        SELECT * FROM series
        WHERE (:categoryId IS NULL OR categoryRemoteId = :categoryId)
          AND (:includeAdult = 1 OR isAdult = 0)
          AND categoryRemoteId NOT IN (
              SELECT remoteId FROM categories WHERE section = 'series' AND hidden = 1
          )
        ORDER BY name
        """,
    )
    fun observeSeries(categoryId: String?, includeAdult: Boolean): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY seasonNumber, episodeNumber, title")
    fun observeEpisodes(seriesId: Long): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM channels WHERE streamId = :streamId")
    suspend fun channelById(streamId: Long): ChannelEntity?

    @Query("SELECT * FROM movies WHERE streamId = :streamId")
    suspend fun movieById(streamId: Long): MovieEntity?

    @Query("SELECT * FROM series WHERE seriesId = :seriesId")
    suspend fun seriesById(seriesId: Long): SeriesEntity?

    @Query("SELECT * FROM episodes WHERE episodeId = :episodeId")
    suspend fun episodeById(episodeId: Long): EpisodeEntity?

    @Query("SELECT COUNT(*) FROM episodes WHERE seriesId = :seriesId")
    suspend fun episodeCountForSeries(seriesId: Long): Int

    @Query("UPDATE series SET artworkUrl = :artworkUrl, plot = :plot, rating = :rating, releaseYear = :releaseYear WHERE seriesId = :seriesId")
    suspend fun updateSeriesDetails(
        seriesId: Long,
        artworkUrl: String?,
        plot: String?,
        rating: String?,
        releaseYear: String?,
    )

    @Query("SELECT * FROM epg_events WHERE channelEpgId = :channelEpgId AND endEpochMillis >= :fromEpochMillis ORDER BY startEpochMillis LIMIT :limit")
    fun observeGuide(
        channelEpgId: String,
        fromEpochMillis: Long,
        limit: Int,
    ): Flow<List<EpgEventEntity>>

    @Query("SELECT COUNT(*) FROM epg_events WHERE channelEpgId = :channelEpgId AND endEpochMillis >= :fromEpochMillis")
    suspend fun futureGuideCount(channelEpgId: String, fromEpochMillis: Long): Int

    @Query(
        """
        SELECT * FROM channels
        WHERE name LIKE '%' || :query || '%'
          AND (:includeAdult = 1 OR isAdult = 0)
          AND categoryRemoteId NOT IN (
              SELECT remoteId FROM categories WHERE section = 'live' AND hidden = 1
          )
        ORDER BY name
        LIMIT :limit
        """,
    )
    suspend fun searchChannels(query: String, includeAdult: Boolean, limit: Int): List<ChannelEntity>

    @Query(
        """
        SELECT * FROM movies
        WHERE name LIKE '%' || :query || '%'
          AND (:includeAdult = 1 OR isAdult = 0)
          AND categoryRemoteId NOT IN (
              SELECT remoteId FROM categories WHERE section = 'movies' AND hidden = 1
          )
        ORDER BY name
        LIMIT :limit
        """,
    )
    suspend fun searchMovies(query: String, includeAdult: Boolean, limit: Int): List<MovieEntity>

    @Query(
        """
        SELECT * FROM series
        WHERE name LIKE '%' || :query || '%'
          AND (:includeAdult = 1 OR isAdult = 0)
          AND categoryRemoteId NOT IN (
              SELECT remoteId FROM categories WHERE section = 'series' AND hidden = 1
          )
        ORDER BY name
        LIMIT :limit
        """,
    )
    suspend fun searchSeries(query: String, includeAdult: Boolean, limit: Int): List<SeriesEntity>

    @Query("SELECT targetId FROM favorite_items WHERE targetType = :targetType")
    fun observeFavoriteIds(targetType: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM favorite_items")
    fun observeFavoriteCount(): Flow<Int>

    @Query("SELECT * FROM favorite_items WHERE targetType = :targetType ORDER BY addedAt DESC LIMIT :limit")
    fun observeFavoriteItems(targetType: String, limit: Int): Flow<List<FavoriteItemEntity>>

    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeContinueWatching(limit: Int): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM recent_channels ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeRecentChannels(limit: Int): Flow<List<RecentChannelEntity>>

    @Query("SELECT COUNT(*) FROM channels")
    fun observeChannelCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM movies")
    fun observeMovieCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM series")
    fun observeSeriesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM episodes")
    fun observeEpisodeCount(): Flow<Int>

    @Query(
        """
        SELECT streamId FROM channels
        WHERE categoryRemoteId = :categoryId
          AND (:includeAdult = 1 OR isAdult = 0)
        ORDER BY
            CASE WHEN channelNumber IS NULL THEN 1 ELSE 0 END,
            channelNumber,
            name
        """,
    )
    suspend fun channelIdsForCategory(categoryId: String, includeAdult: Boolean): List<Long>
}

@Database(
    entities = [
        CategoryEntity::class,
        ChannelEntity::class,
        MovieEntity::class,
        SeriesEntity::class,
        EpisodeEntity::class,
        EpgEventEntity::class,
        FavoriteItemEntity::class,
        PlaybackHistoryEntity::class,
        RecentChannelEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "aurora_tv.db",
            ).fallbackToDestructiveMigration().build()
        }
    }
}
