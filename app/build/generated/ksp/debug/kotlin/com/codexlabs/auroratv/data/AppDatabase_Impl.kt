package com.codexlabs.auroratv.`data`

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _mediaDao: Lazy<MediaDao> = lazy {
    MediaDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1, "ac7b5269e15a3c22c3fb952949dbf817", "758b8966b9301e0a66ae32c3d0b67291") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`section` TEXT NOT NULL, `remoteId` TEXT NOT NULL, `name` TEXT NOT NULL, `isAdult` INTEGER NOT NULL, `hidden` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`section`, `remoteId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `channels` (`streamId` INTEGER NOT NULL, `categoryRemoteId` TEXT NOT NULL, `name` TEXT NOT NULL, `channelNumber` INTEGER, `logoUrl` TEXT, `epgChannelId` TEXT, `containerExtension` TEXT, `directSource` TEXT, `customSid` TEXT, `isAdult` INTEGER NOT NULL, `hasCatchup` INTEGER NOT NULL, `catchupDurationHours` INTEGER NOT NULL, `addedAt` INTEGER, PRIMARY KEY(`streamId`))")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_categoryRemoteId` ON `channels` (`categoryRemoteId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_epgChannelId` ON `channels` (`epgChannelId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `movies` (`streamId` INTEGER NOT NULL, `categoryRemoteId` TEXT NOT NULL, `name` TEXT NOT NULL, `artworkUrl` TEXT, `plot` TEXT, `rating` TEXT, `releaseYear` TEXT, `containerExtension` TEXT, `directSource` TEXT, `isAdult` INTEGER NOT NULL, `addedAt` INTEGER, PRIMARY KEY(`streamId`))")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_movies_categoryRemoteId` ON `movies` (`categoryRemoteId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `series` (`seriesId` INTEGER NOT NULL, `categoryRemoteId` TEXT NOT NULL, `name` TEXT NOT NULL, `artworkUrl` TEXT, `plot` TEXT, `rating` TEXT, `releaseYear` TEXT, `isAdult` INTEGER NOT NULL, `addedAt` INTEGER, PRIMARY KEY(`seriesId`))")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_series_categoryRemoteId` ON `series` (`categoryRemoteId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `episodes` (`episodeId` INTEGER NOT NULL, `seriesId` INTEGER NOT NULL, `seasonNumber` INTEGER NOT NULL, `episodeNumber` INTEGER NOT NULL, `title` TEXT NOT NULL, `artworkUrl` TEXT, `plot` TEXT, `durationSeconds` INTEGER, `containerExtension` TEXT, `directSource` TEXT, `addedAt` INTEGER, PRIMARY KEY(`episodeId`))")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_episodes_seriesId` ON `episodes` (`seriesId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_episodes_seasonNumber` ON `episodes` (`seasonNumber`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `epg_events` (`channelEpgId` TEXT NOT NULL, `startEpochMillis` INTEGER NOT NULL, `endEpochMillis` INTEGER NOT NULL, `title` TEXT NOT NULL, `description` TEXT, PRIMARY KEY(`channelEpgId`, `startEpochMillis`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `favorite_items` (`targetType` TEXT NOT NULL, `targetId` TEXT NOT NULL, `title` TEXT NOT NULL, `subtitle` TEXT, `artworkUrl` TEXT, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`targetType`, `targetId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `playback_history` (`id` TEXT NOT NULL, `targetType` TEXT NOT NULL, `targetId` TEXT NOT NULL, `title` TEXT NOT NULL, `subtitle` TEXT, `artworkUrl` TEXT, `positionMs` INTEGER NOT NULL, `durationMs` INTEGER NOT NULL, `lastPlayedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `recent_channels` (`channelId` INTEGER NOT NULL, `title` TEXT NOT NULL, `artworkUrl` TEXT, `categoryId` TEXT, `lastPlayedAt` INTEGER NOT NULL, PRIMARY KEY(`channelId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'ac7b5269e15a3c22c3fb952949dbf817')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `categories`")
        connection.execSQL("DROP TABLE IF EXISTS `channels`")
        connection.execSQL("DROP TABLE IF EXISTS `movies`")
        connection.execSQL("DROP TABLE IF EXISTS `series`")
        connection.execSQL("DROP TABLE IF EXISTS `episodes`")
        connection.execSQL("DROP TABLE IF EXISTS `epg_events`")
        connection.execSQL("DROP TABLE IF EXISTS `favorite_items`")
        connection.execSQL("DROP TABLE IF EXISTS `playback_history`")
        connection.execSQL("DROP TABLE IF EXISTS `recent_channels`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsCategories: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCategories.put("section", TableInfo.Column("section", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("remoteId", TableInfo.Column("remoteId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("isAdult", TableInfo.Column("isAdult", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("hidden", TableInfo.Column("hidden", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("sortOrder", TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCategories: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCategories: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoCategories: TableInfo = TableInfo("categories", _columnsCategories, _foreignKeysCategories, _indicesCategories)
        val _existingCategories: TableInfo = read(connection, "categories")
        if (!_infoCategories.equals(_existingCategories)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |categories(com.codexlabs.auroratv.data.CategoryEntity).
              | Expected:
              |""".trimMargin() + _infoCategories + """
              |
              | Found:
              |""".trimMargin() + _existingCategories)
        }
        val _columnsChannels: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsChannels.put("streamId", TableInfo.Column("streamId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChannels.put("categoryRemoteId", TableInfo.Column("categoryRemoteId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChannels.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChannels.put("channelNumber", TableInfo.Column("channelNumber", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChannels.put("logoUrl", TableInfo.Column("logoUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChannels.put("epgChannelId", TableInfo.Column("epgChannelId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChannels.put("containerExtension", TableInfo.Column("containerExtension", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChannels.put("directSource", TableInfo.Column("directSource", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChannels.put("customSid", TableInfo.Column("customSid", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChannels.put("isAdult", TableInfo.Column("isAdult", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChannels.put("hasCatchup", TableInfo.Column("hasCatchup", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChannels.put("catchupDurationHours", TableInfo.Column("catchupDurationHours", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChannels.put("addedAt", TableInfo.Column("addedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysChannels: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesChannels: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesChannels.add(TableInfo.Index("index_channels_categoryRemoteId", false, listOf("categoryRemoteId"), listOf("ASC")))
        _indicesChannels.add(TableInfo.Index("index_channels_epgChannelId", false, listOf("epgChannelId"), listOf("ASC")))
        val _infoChannels: TableInfo = TableInfo("channels", _columnsChannels, _foreignKeysChannels, _indicesChannels)
        val _existingChannels: TableInfo = read(connection, "channels")
        if (!_infoChannels.equals(_existingChannels)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |channels(com.codexlabs.auroratv.data.ChannelEntity).
              | Expected:
              |""".trimMargin() + _infoChannels + """
              |
              | Found:
              |""".trimMargin() + _existingChannels)
        }
        val _columnsMovies: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsMovies.put("streamId", TableInfo.Column("streamId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMovies.put("categoryRemoteId", TableInfo.Column("categoryRemoteId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMovies.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMovies.put("artworkUrl", TableInfo.Column("artworkUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMovies.put("plot", TableInfo.Column("plot", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMovies.put("rating", TableInfo.Column("rating", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMovies.put("releaseYear", TableInfo.Column("releaseYear", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMovies.put("containerExtension", TableInfo.Column("containerExtension", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMovies.put("directSource", TableInfo.Column("directSource", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMovies.put("isAdult", TableInfo.Column("isAdult", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMovies.put("addedAt", TableInfo.Column("addedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysMovies: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesMovies: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesMovies.add(TableInfo.Index("index_movies_categoryRemoteId", false, listOf("categoryRemoteId"), listOf("ASC")))
        val _infoMovies: TableInfo = TableInfo("movies", _columnsMovies, _foreignKeysMovies, _indicesMovies)
        val _existingMovies: TableInfo = read(connection, "movies")
        if (!_infoMovies.equals(_existingMovies)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |movies(com.codexlabs.auroratv.data.MovieEntity).
              | Expected:
              |""".trimMargin() + _infoMovies + """
              |
              | Found:
              |""".trimMargin() + _existingMovies)
        }
        val _columnsSeries: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSeries.put("seriesId", TableInfo.Column("seriesId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSeries.put("categoryRemoteId", TableInfo.Column("categoryRemoteId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSeries.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSeries.put("artworkUrl", TableInfo.Column("artworkUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSeries.put("plot", TableInfo.Column("plot", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSeries.put("rating", TableInfo.Column("rating", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSeries.put("releaseYear", TableInfo.Column("releaseYear", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSeries.put("isAdult", TableInfo.Column("isAdult", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSeries.put("addedAt", TableInfo.Column("addedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSeries: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSeries: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesSeries.add(TableInfo.Index("index_series_categoryRemoteId", false, listOf("categoryRemoteId"), listOf("ASC")))
        val _infoSeries: TableInfo = TableInfo("series", _columnsSeries, _foreignKeysSeries, _indicesSeries)
        val _existingSeries: TableInfo = read(connection, "series")
        if (!_infoSeries.equals(_existingSeries)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |series(com.codexlabs.auroratv.data.SeriesEntity).
              | Expected:
              |""".trimMargin() + _infoSeries + """
              |
              | Found:
              |""".trimMargin() + _existingSeries)
        }
        val _columnsEpisodes: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsEpisodes.put("episodeId", TableInfo.Column("episodeId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEpisodes.put("seriesId", TableInfo.Column("seriesId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEpisodes.put("seasonNumber", TableInfo.Column("seasonNumber", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEpisodes.put("episodeNumber", TableInfo.Column("episodeNumber", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEpisodes.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEpisodes.put("artworkUrl", TableInfo.Column("artworkUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEpisodes.put("plot", TableInfo.Column("plot", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEpisodes.put("durationSeconds", TableInfo.Column("durationSeconds", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEpisodes.put("containerExtension", TableInfo.Column("containerExtension", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEpisodes.put("directSource", TableInfo.Column("directSource", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEpisodes.put("addedAt", TableInfo.Column("addedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysEpisodes: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesEpisodes: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesEpisodes.add(TableInfo.Index("index_episodes_seriesId", false, listOf("seriesId"), listOf("ASC")))
        _indicesEpisodes.add(TableInfo.Index("index_episodes_seasonNumber", false, listOf("seasonNumber"), listOf("ASC")))
        val _infoEpisodes: TableInfo = TableInfo("episodes", _columnsEpisodes, _foreignKeysEpisodes, _indicesEpisodes)
        val _existingEpisodes: TableInfo = read(connection, "episodes")
        if (!_infoEpisodes.equals(_existingEpisodes)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |episodes(com.codexlabs.auroratv.data.EpisodeEntity).
              | Expected:
              |""".trimMargin() + _infoEpisodes + """
              |
              | Found:
              |""".trimMargin() + _existingEpisodes)
        }
        val _columnsEpgEvents: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsEpgEvents.put("channelEpgId", TableInfo.Column("channelEpgId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEpgEvents.put("startEpochMillis", TableInfo.Column("startEpochMillis", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEpgEvents.put("endEpochMillis", TableInfo.Column("endEpochMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEpgEvents.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEpgEvents.put("description", TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysEpgEvents: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesEpgEvents: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoEpgEvents: TableInfo = TableInfo("epg_events", _columnsEpgEvents, _foreignKeysEpgEvents, _indicesEpgEvents)
        val _existingEpgEvents: TableInfo = read(connection, "epg_events")
        if (!_infoEpgEvents.equals(_existingEpgEvents)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |epg_events(com.codexlabs.auroratv.data.EpgEventEntity).
              | Expected:
              |""".trimMargin() + _infoEpgEvents + """
              |
              | Found:
              |""".trimMargin() + _existingEpgEvents)
        }
        val _columnsFavoriteItems: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsFavoriteItems.put("targetType", TableInfo.Column("targetType", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFavoriteItems.put("targetId", TableInfo.Column("targetId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFavoriteItems.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFavoriteItems.put("subtitle", TableInfo.Column("subtitle", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFavoriteItems.put("artworkUrl", TableInfo.Column("artworkUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFavoriteItems.put("addedAt", TableInfo.Column("addedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysFavoriteItems: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesFavoriteItems: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoFavoriteItems: TableInfo = TableInfo("favorite_items", _columnsFavoriteItems, _foreignKeysFavoriteItems, _indicesFavoriteItems)
        val _existingFavoriteItems: TableInfo = read(connection, "favorite_items")
        if (!_infoFavoriteItems.equals(_existingFavoriteItems)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |favorite_items(com.codexlabs.auroratv.data.FavoriteItemEntity).
              | Expected:
              |""".trimMargin() + _infoFavoriteItems + """
              |
              | Found:
              |""".trimMargin() + _existingFavoriteItems)
        }
        val _columnsPlaybackHistory: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPlaybackHistory.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaybackHistory.put("targetType", TableInfo.Column("targetType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaybackHistory.put("targetId", TableInfo.Column("targetId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaybackHistory.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaybackHistory.put("subtitle", TableInfo.Column("subtitle", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaybackHistory.put("artworkUrl", TableInfo.Column("artworkUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaybackHistory.put("positionMs", TableInfo.Column("positionMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaybackHistory.put("durationMs", TableInfo.Column("durationMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaybackHistory.put("lastPlayedAt", TableInfo.Column("lastPlayedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPlaybackHistory: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesPlaybackHistory: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoPlaybackHistory: TableInfo = TableInfo("playback_history", _columnsPlaybackHistory, _foreignKeysPlaybackHistory, _indicesPlaybackHistory)
        val _existingPlaybackHistory: TableInfo = read(connection, "playback_history")
        if (!_infoPlaybackHistory.equals(_existingPlaybackHistory)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |playback_history(com.codexlabs.auroratv.data.PlaybackHistoryEntity).
              | Expected:
              |""".trimMargin() + _infoPlaybackHistory + """
              |
              | Found:
              |""".trimMargin() + _existingPlaybackHistory)
        }
        val _columnsRecentChannels: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRecentChannels.put("channelId", TableInfo.Column("channelId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecentChannels.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecentChannels.put("artworkUrl", TableInfo.Column("artworkUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecentChannels.put("categoryId", TableInfo.Column("categoryId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecentChannels.put("lastPlayedAt", TableInfo.Column("lastPlayedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRecentChannels: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRecentChannels: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRecentChannels: TableInfo = TableInfo("recent_channels", _columnsRecentChannels, _foreignKeysRecentChannels, _indicesRecentChannels)
        val _existingRecentChannels: TableInfo = read(connection, "recent_channels")
        if (!_infoRecentChannels.equals(_existingRecentChannels)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |recent_channels(com.codexlabs.auroratv.data.RecentChannelEntity).
              | Expected:
              |""".trimMargin() + _infoRecentChannels + """
              |
              | Found:
              |""".trimMargin() + _existingRecentChannels)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "categories", "channels", "movies", "series", "episodes", "epg_events", "favorite_items", "playback_history", "recent_channels")
  }

  public override fun clearAllTables() {
    super.performClear(false, "categories", "channels", "movies", "series", "episodes", "epg_events", "favorite_items", "playback_history", "recent_channels")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(MediaDao::class, MediaDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun mediaDao(): MediaDao = _mediaDao.value
}
