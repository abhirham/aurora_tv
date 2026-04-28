package com.codexlabs.auroratv.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.codexlabs.auroratv.BuildConfig
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "categories",
    primaryKeys = ["section", "remoteId"],
    indices = [
        Index(value = ["section", "hidden", "isAdult", "sortOrder", "name"]),
        Index("syncToken"),
    ],
)
data class CategoryEntity(
    val section: String,
    val remoteId: String,
    val name: String,
    val isAdult: Boolean,
    val hidden: Boolean,
    val sortOrder: Int,
    val syncToken: Long,
)

@Entity(
    tableName = "channels",
    indices = [
        Index("categoryRemoteId"),
        Index("epgChannelId"),
        Index(value = ["categoryRemoteId", "categoryHidden", "isAdult", "channelNumber", "name", "streamId"]),
        Index("syncToken"),
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
    val categoryHidden: Boolean,
    val hasCatchup: Boolean,
    val catchupDurationHours: Int,
    val addedAt: Long?,
    val syncToken: Long,
)

@Fts4(tokenizer = "unicode61")
@Entity(tableName = "channel_search")
data class ChannelSearchEntity(
    @PrimaryKey
    @androidx.room.ColumnInfo(name = "rowid")
    val rowId: Long,
    val name: String,
)

@Entity(
    tableName = "movies",
    indices = [
        Index("categoryRemoteId"),
        Index(value = ["categoryRemoteId", "categoryHidden", "isAdult", "name", "streamId"]),
        Index("syncToken"),
    ],
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
    val categoryHidden: Boolean,
    val addedAt: Long?,
    val syncToken: Long,
)

@Fts4(tokenizer = "unicode61")
@Entity(tableName = "movie_search")
data class MovieSearchEntity(
    @PrimaryKey
    @androidx.room.ColumnInfo(name = "rowid")
    val rowId: Long,
    val name: String,
)

@Entity(
    tableName = "series",
    indices = [
        Index("categoryRemoteId"),
        Index(value = ["categoryRemoteId", "categoryHidden", "isAdult", "name", "seriesId"]),
        Index("syncToken"),
    ],
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
    val categoryHidden: Boolean,
    val addedAt: Long?,
    val syncToken: Long,
)

@Fts4(tokenizer = "unicode61")
@Entity(tableName = "series_search")
data class SeriesSearchEntity(
    @PrimaryKey
    @androidx.room.ColumnInfo(name = "rowid")
    val rowId: Long,
    val name: String,
)

@Entity(
    tableName = "episodes",
    indices = [
        Index("seriesId"),
        Index("seasonNumber"),
        Index(value = ["seriesId", "seasonNumber", "episodeNumber", "episodeId"]),
    ],
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
    indices = [
        Index(value = ["channelEpgId", "endEpochMillis", "startEpochMillis"]),
    ],
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
    indices = [
        Index(value = ["targetType", "addedAt"]),
    ],
)
data class FavoriteItemEntity(
    val targetType: String,
    val targetId: String,
    val title: String,
    val subtitle: String?,
    val artworkUrl: String?,
    val addedAt: Long,
)

@Entity(
    tableName = "playback_history",
    indices = [
        Index("lastPlayedAt"),
    ],
)
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

@Entity(
    tableName = "recent_channels",
    indices = [
        Index("lastPlayedAt"),
        Index(value = ["categoryId", "lastPlayedAt"]),
    ],
)
data class RecentChannelEntity(
    @PrimaryKey val channelId: Long,
    val title: String,
    val artworkUrl: String?,
    val categoryId: String?,
    val lastPlayedAt: Long,
)

data class ChannelListItem(
    val streamId: Long,
    val categoryRemoteId: String,
    val name: String,
    val channelNumber: Int?,
    val logoUrl: String?,
)

data class MovieListItem(
    val streamId: Long,
    val categoryRemoteId: String,
    val name: String,
    val artworkUrl: String?,
    val plot: String?,
    val rating: String?,
    val releaseYear: String?,
)

data class SeriesListItem(
    val seriesId: Long,
    val categoryRemoteId: String,
    val name: String,
    val artworkUrl: String?,
    val plot: String?,
    val rating: String?,
    val releaseYear: String?,
)

@Dao
@RewriteQueriesToDropUnusedColumns
interface MediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(items: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannels(items: List<ChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannelSearch(items: List<ChannelSearchEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMovies(items: List<MovieEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMovieSearch(items: List<MovieSearchEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeries(items: List<SeriesEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeriesSearch(items: List<SeriesSearchEntity>)

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

    @Query("DELETE FROM categories WHERE section = :section AND syncToken != :syncToken")
    suspend fun deleteStaleCategories(section: String, syncToken: Long)

    @Query("DELETE FROM channels WHERE syncToken != :syncToken")
    suspend fun deleteStaleChannels(syncToken: Long)

    @Query("DELETE FROM channel_search WHERE rowid NOT IN (SELECT streamId FROM channels)")
    suspend fun deleteStaleChannelSearch()

    @Query("DELETE FROM movies WHERE syncToken != :syncToken")
    suspend fun deleteStaleMovies(syncToken: Long)

    @Query("DELETE FROM movie_search WHERE rowid NOT IN (SELECT streamId FROM movies)")
    suspend fun deleteStaleMovieSearch()

    @Query("DELETE FROM series WHERE syncToken != :syncToken")
    suspend fun deleteStaleSeries(syncToken: Long)

    @Query("DELETE FROM series_search WHERE rowid NOT IN (SELECT seriesId FROM series)")
    suspend fun deleteStaleSeriesSearch()

    @Query("DELETE FROM favorite_items WHERE targetType = :targetType AND targetId = :targetId")
    suspend fun deleteFavorite(targetType: String, targetId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_items WHERE targetType = :targetType AND targetId = :targetId)")
    suspend fun isFavorite(targetType: String, targetId: String): Boolean

    @Query("SELECT remoteId FROM categories WHERE section = :section AND hidden = 1")
    suspend fun hiddenCategoryIds(section: String): List<String>

    @Query("UPDATE categories SET hidden = :hidden WHERE section = :section AND remoteId = :remoteId")
    suspend fun setCategoryHidden(section: String, remoteId: String, hidden: Boolean)

    @Query("UPDATE channels SET categoryHidden = :hidden WHERE categoryRemoteId = :remoteId")
    suspend fun setChannelCategoryHidden(remoteId: String, hidden: Boolean)

    @Query("UPDATE movies SET categoryHidden = :hidden WHERE categoryRemoteId = :remoteId")
    suspend fun setMovieCategoryHidden(remoteId: String, hidden: Boolean)

    @Query("UPDATE series SET categoryHidden = :hidden WHERE categoryRemoteId = :remoteId")
    suspend fun setSeriesCategoryHidden(remoteId: String, hidden: Boolean)

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
        SELECT streamId, categoryRemoteId, name, channelNumber, logoUrl FROM channels
        WHERE (:categoryId IS NULL OR categoryRemoteId = :categoryId)
          AND categoryHidden = 0
          AND (:includeAdult = 1 OR isAdult = 0)
        ORDER BY
            CASE WHEN channelNumber IS NULL THEN 1 ELSE 0 END,
            channelNumber,
            name
        LIMIT :limit
        """,
    )
    fun observeChannels(categoryId: String?, includeAdult: Boolean, limit: Int): Flow<List<ChannelListItem>>

    @Query(
        """
        SELECT streamId, categoryRemoteId, name, artworkUrl, plot, rating, releaseYear FROM movies
        WHERE (:categoryId IS NULL OR categoryRemoteId = :categoryId)
          AND categoryHidden = 0
          AND (:includeAdult = 1 OR isAdult = 0)
        ORDER BY name
        LIMIT :limit
        """,
    )
    fun observeMovies(categoryId: String?, includeAdult: Boolean, limit: Int): Flow<List<MovieListItem>>

    @Query(
        """
        SELECT seriesId, categoryRemoteId, name, artworkUrl, plot, rating, releaseYear FROM series
        WHERE (:categoryId IS NULL OR categoryRemoteId = :categoryId)
          AND categoryHidden = 0
          AND (:includeAdult = 1 OR isAdult = 0)
        ORDER BY name
        LIMIT :limit
        """,
    )
    fun observeSeries(categoryId: String?, includeAdult: Boolean, limit: Int): Flow<List<SeriesListItem>>

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

    @Query(
        """
        SELECT * FROM episodes
        WHERE seriesId = :seriesId
          AND (
              seasonNumber > :seasonNumber
              OR (seasonNumber = :seasonNumber AND episodeNumber > :episodeNumber)
              OR (seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber AND episodeId > :episodeId)
          )
        ORDER BY seasonNumber, episodeNumber, episodeId
        LIMIT 1
        """,
    )
    suspend fun nextEpisodeAfter(
        seriesId: Long,
        seasonNumber: Int,
        episodeNumber: Int,
        episodeId: Long,
    ): EpisodeEntity?

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
        SELECT channels.* FROM channel_search
        JOIN channels ON channels.streamId = channel_search.rowid
        WHERE channel_search MATCH :query
          AND channels.categoryHidden = 0
          AND (:includeAdult = 1 OR channels.isAdult = 0)
        ORDER BY channels.name
        LIMIT :limit
        """,
    )
    suspend fun searchChannels(query: String, includeAdult: Boolean, limit: Int): List<ChannelEntity>

    @Query(
        """
        SELECT movies.* FROM movie_search
        JOIN movies ON movies.streamId = movie_search.rowid
        WHERE movie_search MATCH :query
          AND movies.categoryHidden = 0
          AND (:includeAdult = 1 OR movies.isAdult = 0)
        ORDER BY movies.name
        LIMIT :limit
        """,
    )
    suspend fun searchMovies(query: String, includeAdult: Boolean, limit: Int): List<MovieEntity>

    @Query(
        """
        SELECT series.* FROM series_search
        JOIN series ON series.seriesId = series_search.rowid
        WHERE series_search MATCH :query
          AND series.categoryHidden = 0
          AND (:includeAdult = 1 OR series.isAdult = 0)
        ORDER BY series.name
        LIMIT :limit
        """,
    )
    suspend fun searchSeries(query: String, includeAdult: Boolean, limit: Int): List<SeriesEntity>

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
        SELECT candidate.* FROM channels AS candidate
        JOIN channels AS cur ON cur.streamId = :currentChannelId
        WHERE cur.categoryRemoteId = :categoryId
          AND candidate.categoryRemoteId = :categoryId
          AND candidate.categoryHidden = 0
          AND (:includeAdult = 1 OR candidate.isAdult = 0)
          AND (
              CASE WHEN candidate.channelNumber IS NULL THEN 1 ELSE 0 END
                  > CASE WHEN cur.channelNumber IS NULL THEN 1 ELSE 0 END
              OR (
                  CASE WHEN candidate.channelNumber IS NULL THEN 1 ELSE 0 END
                      = CASE WHEN cur.channelNumber IS NULL THEN 1 ELSE 0 END
                  AND (
                      (cur.channelNumber IS NOT NULL AND candidate.channelNumber > cur.channelNumber)
                      OR (
                          (
                              (cur.channelNumber IS NULL AND candidate.channelNumber IS NULL)
                              OR candidate.channelNumber = cur.channelNumber
                          )
                          AND (
                              candidate.name > cur.name
                              OR (candidate.name = cur.name AND candidate.streamId > cur.streamId)
                          )
                      )
                  )
              )
          )
        ORDER BY
            CASE WHEN candidate.channelNumber IS NULL THEN 1 ELSE 0 END,
            candidate.channelNumber,
            candidate.name,
            candidate.streamId
        LIMIT 1
        """,
    )
    suspend fun nextChannelInCategory(
        currentChannelId: Long,
        categoryId: String,
        includeAdult: Boolean,
    ): ChannelEntity?

    @Query(
        """
        SELECT candidate.* FROM channels AS candidate
        JOIN channels AS cur ON cur.streamId = :currentChannelId
        WHERE cur.categoryRemoteId = :categoryId
          AND candidate.categoryRemoteId = :categoryId
          AND candidate.categoryHidden = 0
          AND (:includeAdult = 1 OR candidate.isAdult = 0)
          AND (
              CASE WHEN candidate.channelNumber IS NULL THEN 1 ELSE 0 END
                  < CASE WHEN cur.channelNumber IS NULL THEN 1 ELSE 0 END
              OR (
                  CASE WHEN candidate.channelNumber IS NULL THEN 1 ELSE 0 END
                      = CASE WHEN cur.channelNumber IS NULL THEN 1 ELSE 0 END
                  AND (
                      (cur.channelNumber IS NOT NULL AND candidate.channelNumber < cur.channelNumber)
                      OR (
                          (
                              (cur.channelNumber IS NULL AND candidate.channelNumber IS NULL)
                              OR candidate.channelNumber = cur.channelNumber
                          )
                          AND (
                              candidate.name < cur.name
                              OR (candidate.name = cur.name AND candidate.streamId < cur.streamId)
                          )
                      )
                  )
              )
          )
        ORDER BY
            CASE WHEN candidate.channelNumber IS NULL THEN 1 ELSE 0 END DESC,
            candidate.channelNumber DESC,
            candidate.name DESC,
            candidate.streamId DESC
        LIMIT 1
        """,
    )
    suspend fun previousChannelInCategory(
        currentChannelId: Long,
        categoryId: String,
        includeAdult: Boolean,
    ): ChannelEntity?
}

@Database(
    entities = [
        CategoryEntity::class,
        ChannelEntity::class,
        ChannelSearchEntity::class,
        MovieEntity::class,
        MovieSearchEntity::class,
        SeriesEntity::class,
        SeriesSearchEntity::class,
        EpisodeEntity::class,
        EpgEventEntity::class,
        FavoriteItemEntity::class,
        PlaybackHistoryEntity::class,
        RecentChannelEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        private val MIGRATION_1_5 = object : Migration(1, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN syncToken INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE channels ADD COLUMN categoryHidden INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE channels ADD COLUMN syncToken INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE movies ADD COLUMN categoryHidden INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE movies ADD COLUMN syncToken INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE series ADD COLUMN categoryHidden INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE series ADD COLUMN syncToken INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    """
                    UPDATE channels
                    SET categoryHidden = 1
                    WHERE categoryRemoteId IN (
                        SELECT remoteId FROM categories WHERE section = 'live' AND hidden = 1
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    UPDATE movies
                    SET categoryHidden = 1
                    WHERE categoryRemoteId IN (
                        SELECT remoteId FROM categories WHERE section = 'movies' AND hidden = 1
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    UPDATE series
                    SET categoryHidden = 1
                    WHERE categoryRemoteId IN (
                        SELECT remoteId FROM categories WHERE section = 'series' AND hidden = 1
                    )
                    """.trimIndent(),
                )

                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS channel_search USING FTS4(name, tokenize=unicode61)")
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS movie_search USING FTS4(name, tokenize=unicode61)")
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS series_search USING FTS4(name, tokenize=unicode61)")
                db.execSQL("INSERT INTO channel_search(rowid, name) SELECT streamId, name FROM channels")
                db.execSQL("INSERT INTO movie_search(rowid, name) SELECT streamId, name FROM movies")
                db.execSQL("INSERT INTO series_search(rowid, name) SELECT seriesId, name FROM series")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_section_hidden_isAdult_sortOrder_name ON categories(section, hidden, isAdult, sortOrder, name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_syncToken ON categories(syncToken)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_channels_categoryRemoteId_categoryHidden_isAdult_channelNumber_name_streamId ON channels(categoryRemoteId, categoryHidden, isAdult, channelNumber, name, streamId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_channels_syncToken ON channels(syncToken)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_movies_categoryRemoteId_categoryHidden_isAdult_name_streamId ON movies(categoryRemoteId, categoryHidden, isAdult, name, streamId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_movies_syncToken ON movies(syncToken)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_series_categoryRemoteId_categoryHidden_isAdult_name_seriesId ON series(categoryRemoteId, categoryHidden, isAdult, name, seriesId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_series_syncToken ON series(syncToken)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_episodes_seriesId_seasonNumber_episodeNumber_episodeId ON episodes(seriesId, seasonNumber, episodeNumber, episodeId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_epg_events_channelEpgId_endEpochMillis_startEpochMillis ON epg_events(channelEpgId, endEpochMillis, startEpochMillis)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_favorite_items_targetType_addedAt ON favorite_items(targetType, addedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_history_lastPlayedAt ON playback_history(lastPlayedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recent_channels_lastPlayedAt ON recent_channels(lastPlayedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recent_channels_categoryId_lastPlayedAt ON recent_channels(categoryId, lastPlayedAt)")
            }
        }

        fun create(context: Context): AppDatabase {
            val builder = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "aurora_tv.db",
            ).addMigrations(MIGRATION_1_5)

            if (BuildConfig.DEBUG) {
                builder.fallbackToDestructiveMigration(dropAllTables = false)
            }

            return builder.build()
        }
    }
}
