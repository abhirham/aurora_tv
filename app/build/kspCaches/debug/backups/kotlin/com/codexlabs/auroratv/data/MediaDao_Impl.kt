package com.codexlabs.auroratv.`data`

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class MediaDao_Impl(
  __db: RoomDatabase,
) : MediaDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCategoryEntity: EntityInsertAdapter<CategoryEntity>

  private val __insertAdapterOfChannelEntity: EntityInsertAdapter<ChannelEntity>

  private val __insertAdapterOfMovieEntity: EntityInsertAdapter<MovieEntity>

  private val __insertAdapterOfSeriesEntity: EntityInsertAdapter<SeriesEntity>

  private val __insertAdapterOfEpisodeEntity: EntityInsertAdapter<EpisodeEntity>

  private val __insertAdapterOfEpgEventEntity: EntityInsertAdapter<EpgEventEntity>

  private val __insertAdapterOfFavoriteItemEntity: EntityInsertAdapter<FavoriteItemEntity>

  private val __insertAdapterOfPlaybackHistoryEntity: EntityInsertAdapter<PlaybackHistoryEntity>

  private val __insertAdapterOfRecentChannelEntity: EntityInsertAdapter<RecentChannelEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfCategoryEntity = object : EntityInsertAdapter<CategoryEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `categories` (`section`,`remoteId`,`name`,`isAdult`,`hidden`,`sortOrder`) VALUES (?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CategoryEntity) {
        statement.bindText(1, entity.section)
        statement.bindText(2, entity.remoteId)
        statement.bindText(3, entity.name)
        val _tmp: Int = if (entity.isAdult) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        val _tmp_1: Int = if (entity.hidden) 1 else 0
        statement.bindLong(5, _tmp_1.toLong())
        statement.bindLong(6, entity.sortOrder.toLong())
      }
    }
    this.__insertAdapterOfChannelEntity = object : EntityInsertAdapter<ChannelEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `channels` (`streamId`,`categoryRemoteId`,`name`,`channelNumber`,`logoUrl`,`epgChannelId`,`containerExtension`,`directSource`,`customSid`,`isAdult`,`hasCatchup`,`catchupDurationHours`,`addedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ChannelEntity) {
        statement.bindLong(1, entity.streamId)
        statement.bindText(2, entity.categoryRemoteId)
        statement.bindText(3, entity.name)
        val _tmpChannelNumber: Int? = entity.channelNumber
        if (_tmpChannelNumber == null) {
          statement.bindNull(4)
        } else {
          statement.bindLong(4, _tmpChannelNumber.toLong())
        }
        val _tmpLogoUrl: String? = entity.logoUrl
        if (_tmpLogoUrl == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpLogoUrl)
        }
        val _tmpEpgChannelId: String? = entity.epgChannelId
        if (_tmpEpgChannelId == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpEpgChannelId)
        }
        val _tmpContainerExtension: String? = entity.containerExtension
        if (_tmpContainerExtension == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpContainerExtension)
        }
        val _tmpDirectSource: String? = entity.directSource
        if (_tmpDirectSource == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpDirectSource)
        }
        val _tmpCustomSid: String? = entity.customSid
        if (_tmpCustomSid == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpCustomSid)
        }
        val _tmp: Int = if (entity.isAdult) 1 else 0
        statement.bindLong(10, _tmp.toLong())
        val _tmp_1: Int = if (entity.hasCatchup) 1 else 0
        statement.bindLong(11, _tmp_1.toLong())
        statement.bindLong(12, entity.catchupDurationHours.toLong())
        val _tmpAddedAt: Long? = entity.addedAt
        if (_tmpAddedAt == null) {
          statement.bindNull(13)
        } else {
          statement.bindLong(13, _tmpAddedAt)
        }
      }
    }
    this.__insertAdapterOfMovieEntity = object : EntityInsertAdapter<MovieEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `movies` (`streamId`,`categoryRemoteId`,`name`,`artworkUrl`,`plot`,`rating`,`releaseYear`,`containerExtension`,`directSource`,`isAdult`,`addedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MovieEntity) {
        statement.bindLong(1, entity.streamId)
        statement.bindText(2, entity.categoryRemoteId)
        statement.bindText(3, entity.name)
        val _tmpArtworkUrl: String? = entity.artworkUrl
        if (_tmpArtworkUrl == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpArtworkUrl)
        }
        val _tmpPlot: String? = entity.plot
        if (_tmpPlot == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpPlot)
        }
        val _tmpRating: String? = entity.rating
        if (_tmpRating == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpRating)
        }
        val _tmpReleaseYear: String? = entity.releaseYear
        if (_tmpReleaseYear == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpReleaseYear)
        }
        val _tmpContainerExtension: String? = entity.containerExtension
        if (_tmpContainerExtension == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpContainerExtension)
        }
        val _tmpDirectSource: String? = entity.directSource
        if (_tmpDirectSource == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpDirectSource)
        }
        val _tmp: Int = if (entity.isAdult) 1 else 0
        statement.bindLong(10, _tmp.toLong())
        val _tmpAddedAt: Long? = entity.addedAt
        if (_tmpAddedAt == null) {
          statement.bindNull(11)
        } else {
          statement.bindLong(11, _tmpAddedAt)
        }
      }
    }
    this.__insertAdapterOfSeriesEntity = object : EntityInsertAdapter<SeriesEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `series` (`seriesId`,`categoryRemoteId`,`name`,`artworkUrl`,`plot`,`rating`,`releaseYear`,`isAdult`,`addedAt`) VALUES (?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SeriesEntity) {
        statement.bindLong(1, entity.seriesId)
        statement.bindText(2, entity.categoryRemoteId)
        statement.bindText(3, entity.name)
        val _tmpArtworkUrl: String? = entity.artworkUrl
        if (_tmpArtworkUrl == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpArtworkUrl)
        }
        val _tmpPlot: String? = entity.plot
        if (_tmpPlot == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpPlot)
        }
        val _tmpRating: String? = entity.rating
        if (_tmpRating == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpRating)
        }
        val _tmpReleaseYear: String? = entity.releaseYear
        if (_tmpReleaseYear == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpReleaseYear)
        }
        val _tmp: Int = if (entity.isAdult) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        val _tmpAddedAt: Long? = entity.addedAt
        if (_tmpAddedAt == null) {
          statement.bindNull(9)
        } else {
          statement.bindLong(9, _tmpAddedAt)
        }
      }
    }
    this.__insertAdapterOfEpisodeEntity = object : EntityInsertAdapter<EpisodeEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `episodes` (`episodeId`,`seriesId`,`seasonNumber`,`episodeNumber`,`title`,`artworkUrl`,`plot`,`durationSeconds`,`containerExtension`,`directSource`,`addedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: EpisodeEntity) {
        statement.bindLong(1, entity.episodeId)
        statement.bindLong(2, entity.seriesId)
        statement.bindLong(3, entity.seasonNumber.toLong())
        statement.bindLong(4, entity.episodeNumber.toLong())
        statement.bindText(5, entity.title)
        val _tmpArtworkUrl: String? = entity.artworkUrl
        if (_tmpArtworkUrl == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpArtworkUrl)
        }
        val _tmpPlot: String? = entity.plot
        if (_tmpPlot == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpPlot)
        }
        val _tmpDurationSeconds: Long? = entity.durationSeconds
        if (_tmpDurationSeconds == null) {
          statement.bindNull(8)
        } else {
          statement.bindLong(8, _tmpDurationSeconds)
        }
        val _tmpContainerExtension: String? = entity.containerExtension
        if (_tmpContainerExtension == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpContainerExtension)
        }
        val _tmpDirectSource: String? = entity.directSource
        if (_tmpDirectSource == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpDirectSource)
        }
        val _tmpAddedAt: Long? = entity.addedAt
        if (_tmpAddedAt == null) {
          statement.bindNull(11)
        } else {
          statement.bindLong(11, _tmpAddedAt)
        }
      }
    }
    this.__insertAdapterOfEpgEventEntity = object : EntityInsertAdapter<EpgEventEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `epg_events` (`channelEpgId`,`startEpochMillis`,`endEpochMillis`,`title`,`description`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: EpgEventEntity) {
        statement.bindText(1, entity.channelEpgId)
        statement.bindLong(2, entity.startEpochMillis)
        statement.bindLong(3, entity.endEpochMillis)
        statement.bindText(4, entity.title)
        val _tmpDescription: String? = entity.description
        if (_tmpDescription == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpDescription)
        }
      }
    }
    this.__insertAdapterOfFavoriteItemEntity = object : EntityInsertAdapter<FavoriteItemEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `favorite_items` (`targetType`,`targetId`,`title`,`subtitle`,`artworkUrl`,`addedAt`) VALUES (?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: FavoriteItemEntity) {
        statement.bindText(1, entity.targetType)
        statement.bindText(2, entity.targetId)
        statement.bindText(3, entity.title)
        val _tmpSubtitle: String? = entity.subtitle
        if (_tmpSubtitle == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpSubtitle)
        }
        val _tmpArtworkUrl: String? = entity.artworkUrl
        if (_tmpArtworkUrl == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpArtworkUrl)
        }
        statement.bindLong(6, entity.addedAt)
      }
    }
    this.__insertAdapterOfPlaybackHistoryEntity = object : EntityInsertAdapter<PlaybackHistoryEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `playback_history` (`id`,`targetType`,`targetId`,`title`,`subtitle`,`artworkUrl`,`positionMs`,`durationMs`,`lastPlayedAt`) VALUES (?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: PlaybackHistoryEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.targetType)
        statement.bindText(3, entity.targetId)
        statement.bindText(4, entity.title)
        val _tmpSubtitle: String? = entity.subtitle
        if (_tmpSubtitle == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpSubtitle)
        }
        val _tmpArtworkUrl: String? = entity.artworkUrl
        if (_tmpArtworkUrl == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpArtworkUrl)
        }
        statement.bindLong(7, entity.positionMs)
        statement.bindLong(8, entity.durationMs)
        statement.bindLong(9, entity.lastPlayedAt)
      }
    }
    this.__insertAdapterOfRecentChannelEntity = object : EntityInsertAdapter<RecentChannelEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `recent_channels` (`channelId`,`title`,`artworkUrl`,`categoryId`,`lastPlayedAt`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: RecentChannelEntity) {
        statement.bindLong(1, entity.channelId)
        statement.bindText(2, entity.title)
        val _tmpArtworkUrl: String? = entity.artworkUrl
        if (_tmpArtworkUrl == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpArtworkUrl)
        }
        val _tmpCategoryId: String? = entity.categoryId
        if (_tmpCategoryId == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpCategoryId)
        }
        statement.bindLong(5, entity.lastPlayedAt)
      }
    }
  }

  public override suspend fun upsertCategories(items: List<CategoryEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfCategoryEntity.insert(_connection, items)
  }

  public override suspend fun upsertChannels(items: List<ChannelEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfChannelEntity.insert(_connection, items)
  }

  public override suspend fun upsertMovies(items: List<MovieEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfMovieEntity.insert(_connection, items)
  }

  public override suspend fun upsertSeries(items: List<SeriesEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfSeriesEntity.insert(_connection, items)
  }

  public override suspend fun upsertEpisodes(items: List<EpisodeEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfEpisodeEntity.insert(_connection, items)
  }

  public override suspend fun upsertEpg(items: List<EpgEventEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfEpgEventEntity.insert(_connection, items)
  }

  public override suspend fun upsertFavorite(item: FavoriteItemEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfFavoriteItemEntity.insert(_connection, item)
  }

  public override suspend fun upsertPlaybackHistory(item: PlaybackHistoryEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfPlaybackHistoryEntity.insert(_connection, item)
  }

  public override suspend fun upsertRecentChannel(item: RecentChannelEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfRecentChannelEntity.insert(_connection, item)
  }

  public override suspend fun isFavorite(targetType: String, targetId: String): Boolean {
    val _sql: String = "SELECT EXISTS(SELECT 1 FROM favorite_items WHERE targetType = ? AND targetId = ?)"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, targetType)
        _argIndex = 2
        _stmt.bindText(_argIndex, targetId)
        val _result: Boolean
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp != 0
        } else {
          _result = false
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun hiddenCategoryIds(section: String): List<String> {
    val _sql: String = "SELECT remoteId FROM categories WHERE section = ? AND hidden = 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, section)
        val _result: MutableList<String> = mutableListOf()
        while (_stmt.step()) {
          val _item: String
          _item = _stmt.getText(0)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeVisibleCategories(section: String, includeAdult: Boolean): Flow<List<CategoryEntity>> {
    val _sql: String = """
        |
        |        SELECT * FROM categories
        |        WHERE section = ?
        |          AND hidden = 0
        |          AND (? = 1 OR isAdult = 0)
        |        ORDER BY sortOrder, name
        |        
        """.trimMargin()
    return createFlow(__db, false, arrayOf("categories")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, section)
        _argIndex = 2
        val _tmp: Int = if (includeAdult) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        val _columnIndexOfSection: Int = getColumnIndexOrThrow(_stmt, "section")
        val _columnIndexOfRemoteId: Int = getColumnIndexOrThrow(_stmt, "remoteId")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfIsAdult: Int = getColumnIndexOrThrow(_stmt, "isAdult")
        val _columnIndexOfHidden: Int = getColumnIndexOrThrow(_stmt, "hidden")
        val _columnIndexOfSortOrder: Int = getColumnIndexOrThrow(_stmt, "sortOrder")
        val _result: MutableList<CategoryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CategoryEntity
          val _tmpSection: String
          _tmpSection = _stmt.getText(_columnIndexOfSection)
          val _tmpRemoteId: String
          _tmpRemoteId = _stmt.getText(_columnIndexOfRemoteId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpIsAdult: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsAdult).toInt()
          _tmpIsAdult = _tmp_1 != 0
          val _tmpHidden: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfHidden).toInt()
          _tmpHidden = _tmp_2 != 0
          val _tmpSortOrder: Int
          _tmpSortOrder = _stmt.getLong(_columnIndexOfSortOrder).toInt()
          _item = CategoryEntity(_tmpSection,_tmpRemoteId,_tmpName,_tmpIsAdult,_tmpHidden,_tmpSortOrder)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeChannels(categoryId: String?, includeAdult: Boolean): Flow<List<ChannelEntity>> {
    val _sql: String = """
        |
        |        SELECT * FROM channels
        |        WHERE (? IS NULL OR categoryRemoteId = ?)
        |          AND (? = 1 OR isAdult = 0)
        |          AND categoryRemoteId NOT IN (
        |              SELECT remoteId FROM categories WHERE section = 'live' AND hidden = 1
        |          )
        |        ORDER BY
        |            CASE WHEN channelNumber IS NULL THEN 1 ELSE 0 END,
        |            channelNumber,
        |            name
        |        
        """.trimMargin()
    return createFlow(__db, false, arrayOf("channels", "categories")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        if (categoryId == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, categoryId)
        }
        _argIndex = 2
        if (categoryId == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, categoryId)
        }
        _argIndex = 3
        val _tmp: Int = if (includeAdult) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        val _columnIndexOfStreamId: Int = getColumnIndexOrThrow(_stmt, "streamId")
        val _columnIndexOfCategoryRemoteId: Int = getColumnIndexOrThrow(_stmt, "categoryRemoteId")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfChannelNumber: Int = getColumnIndexOrThrow(_stmt, "channelNumber")
        val _columnIndexOfLogoUrl: Int = getColumnIndexOrThrow(_stmt, "logoUrl")
        val _columnIndexOfEpgChannelId: Int = getColumnIndexOrThrow(_stmt, "epgChannelId")
        val _columnIndexOfContainerExtension: Int = getColumnIndexOrThrow(_stmt, "containerExtension")
        val _columnIndexOfDirectSource: Int = getColumnIndexOrThrow(_stmt, "directSource")
        val _columnIndexOfCustomSid: Int = getColumnIndexOrThrow(_stmt, "customSid")
        val _columnIndexOfIsAdult: Int = getColumnIndexOrThrow(_stmt, "isAdult")
        val _columnIndexOfHasCatchup: Int = getColumnIndexOrThrow(_stmt, "hasCatchup")
        val _columnIndexOfCatchupDurationHours: Int = getColumnIndexOrThrow(_stmt, "catchupDurationHours")
        val _columnIndexOfAddedAt: Int = getColumnIndexOrThrow(_stmt, "addedAt")
        val _result: MutableList<ChannelEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ChannelEntity
          val _tmpStreamId: Long
          _tmpStreamId = _stmt.getLong(_columnIndexOfStreamId)
          val _tmpCategoryRemoteId: String
          _tmpCategoryRemoteId = _stmt.getText(_columnIndexOfCategoryRemoteId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpChannelNumber: Int?
          if (_stmt.isNull(_columnIndexOfChannelNumber)) {
            _tmpChannelNumber = null
          } else {
            _tmpChannelNumber = _stmt.getLong(_columnIndexOfChannelNumber).toInt()
          }
          val _tmpLogoUrl: String?
          if (_stmt.isNull(_columnIndexOfLogoUrl)) {
            _tmpLogoUrl = null
          } else {
            _tmpLogoUrl = _stmt.getText(_columnIndexOfLogoUrl)
          }
          val _tmpEpgChannelId: String?
          if (_stmt.isNull(_columnIndexOfEpgChannelId)) {
            _tmpEpgChannelId = null
          } else {
            _tmpEpgChannelId = _stmt.getText(_columnIndexOfEpgChannelId)
          }
          val _tmpContainerExtension: String?
          if (_stmt.isNull(_columnIndexOfContainerExtension)) {
            _tmpContainerExtension = null
          } else {
            _tmpContainerExtension = _stmt.getText(_columnIndexOfContainerExtension)
          }
          val _tmpDirectSource: String?
          if (_stmt.isNull(_columnIndexOfDirectSource)) {
            _tmpDirectSource = null
          } else {
            _tmpDirectSource = _stmt.getText(_columnIndexOfDirectSource)
          }
          val _tmpCustomSid: String?
          if (_stmt.isNull(_columnIndexOfCustomSid)) {
            _tmpCustomSid = null
          } else {
            _tmpCustomSid = _stmt.getText(_columnIndexOfCustomSid)
          }
          val _tmpIsAdult: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsAdult).toInt()
          _tmpIsAdult = _tmp_1 != 0
          val _tmpHasCatchup: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfHasCatchup).toInt()
          _tmpHasCatchup = _tmp_2 != 0
          val _tmpCatchupDurationHours: Int
          _tmpCatchupDurationHours = _stmt.getLong(_columnIndexOfCatchupDurationHours).toInt()
          val _tmpAddedAt: Long?
          if (_stmt.isNull(_columnIndexOfAddedAt)) {
            _tmpAddedAt = null
          } else {
            _tmpAddedAt = _stmt.getLong(_columnIndexOfAddedAt)
          }
          _item = ChannelEntity(_tmpStreamId,_tmpCategoryRemoteId,_tmpName,_tmpChannelNumber,_tmpLogoUrl,_tmpEpgChannelId,_tmpContainerExtension,_tmpDirectSource,_tmpCustomSid,_tmpIsAdult,_tmpHasCatchup,_tmpCatchupDurationHours,_tmpAddedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeMovies(categoryId: String?, includeAdult: Boolean): Flow<List<MovieEntity>> {
    val _sql: String = """
        |
        |        SELECT * FROM movies
        |        WHERE (? IS NULL OR categoryRemoteId = ?)
        |          AND (? = 1 OR isAdult = 0)
        |          AND categoryRemoteId NOT IN (
        |              SELECT remoteId FROM categories WHERE section = 'movies' AND hidden = 1
        |          )
        |        ORDER BY name
        |        
        """.trimMargin()
    return createFlow(__db, false, arrayOf("movies", "categories")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        if (categoryId == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, categoryId)
        }
        _argIndex = 2
        if (categoryId == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, categoryId)
        }
        _argIndex = 3
        val _tmp: Int = if (includeAdult) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        val _columnIndexOfStreamId: Int = getColumnIndexOrThrow(_stmt, "streamId")
        val _columnIndexOfCategoryRemoteId: Int = getColumnIndexOrThrow(_stmt, "categoryRemoteId")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfArtworkUrl: Int = getColumnIndexOrThrow(_stmt, "artworkUrl")
        val _columnIndexOfPlot: Int = getColumnIndexOrThrow(_stmt, "plot")
        val _columnIndexOfRating: Int = getColumnIndexOrThrow(_stmt, "rating")
        val _columnIndexOfReleaseYear: Int = getColumnIndexOrThrow(_stmt, "releaseYear")
        val _columnIndexOfContainerExtension: Int = getColumnIndexOrThrow(_stmt, "containerExtension")
        val _columnIndexOfDirectSource: Int = getColumnIndexOrThrow(_stmt, "directSource")
        val _columnIndexOfIsAdult: Int = getColumnIndexOrThrow(_stmt, "isAdult")
        val _columnIndexOfAddedAt: Int = getColumnIndexOrThrow(_stmt, "addedAt")
        val _result: MutableList<MovieEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: MovieEntity
          val _tmpStreamId: Long
          _tmpStreamId = _stmt.getLong(_columnIndexOfStreamId)
          val _tmpCategoryRemoteId: String
          _tmpCategoryRemoteId = _stmt.getText(_columnIndexOfCategoryRemoteId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpArtworkUrl: String?
          if (_stmt.isNull(_columnIndexOfArtworkUrl)) {
            _tmpArtworkUrl = null
          } else {
            _tmpArtworkUrl = _stmt.getText(_columnIndexOfArtworkUrl)
          }
          val _tmpPlot: String?
          if (_stmt.isNull(_columnIndexOfPlot)) {
            _tmpPlot = null
          } else {
            _tmpPlot = _stmt.getText(_columnIndexOfPlot)
          }
          val _tmpRating: String?
          if (_stmt.isNull(_columnIndexOfRating)) {
            _tmpRating = null
          } else {
            _tmpRating = _stmt.getText(_columnIndexOfRating)
          }
          val _tmpReleaseYear: String?
          if (_stmt.isNull(_columnIndexOfReleaseYear)) {
            _tmpReleaseYear = null
          } else {
            _tmpReleaseYear = _stmt.getText(_columnIndexOfReleaseYear)
          }
          val _tmpContainerExtension: String?
          if (_stmt.isNull(_columnIndexOfContainerExtension)) {
            _tmpContainerExtension = null
          } else {
            _tmpContainerExtension = _stmt.getText(_columnIndexOfContainerExtension)
          }
          val _tmpDirectSource: String?
          if (_stmt.isNull(_columnIndexOfDirectSource)) {
            _tmpDirectSource = null
          } else {
            _tmpDirectSource = _stmt.getText(_columnIndexOfDirectSource)
          }
          val _tmpIsAdult: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsAdult).toInt()
          _tmpIsAdult = _tmp_1 != 0
          val _tmpAddedAt: Long?
          if (_stmt.isNull(_columnIndexOfAddedAt)) {
            _tmpAddedAt = null
          } else {
            _tmpAddedAt = _stmt.getLong(_columnIndexOfAddedAt)
          }
          _item = MovieEntity(_tmpStreamId,_tmpCategoryRemoteId,_tmpName,_tmpArtworkUrl,_tmpPlot,_tmpRating,_tmpReleaseYear,_tmpContainerExtension,_tmpDirectSource,_tmpIsAdult,_tmpAddedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeSeries(categoryId: String?, includeAdult: Boolean): Flow<List<SeriesEntity>> {
    val _sql: String = """
        |
        |        SELECT * FROM series
        |        WHERE (? IS NULL OR categoryRemoteId = ?)
        |          AND (? = 1 OR isAdult = 0)
        |          AND categoryRemoteId NOT IN (
        |              SELECT remoteId FROM categories WHERE section = 'series' AND hidden = 1
        |          )
        |        ORDER BY name
        |        
        """.trimMargin()
    return createFlow(__db, false, arrayOf("series", "categories")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        if (categoryId == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, categoryId)
        }
        _argIndex = 2
        if (categoryId == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, categoryId)
        }
        _argIndex = 3
        val _tmp: Int = if (includeAdult) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        val _columnIndexOfSeriesId: Int = getColumnIndexOrThrow(_stmt, "seriesId")
        val _columnIndexOfCategoryRemoteId: Int = getColumnIndexOrThrow(_stmt, "categoryRemoteId")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfArtworkUrl: Int = getColumnIndexOrThrow(_stmt, "artworkUrl")
        val _columnIndexOfPlot: Int = getColumnIndexOrThrow(_stmt, "plot")
        val _columnIndexOfRating: Int = getColumnIndexOrThrow(_stmt, "rating")
        val _columnIndexOfReleaseYear: Int = getColumnIndexOrThrow(_stmt, "releaseYear")
        val _columnIndexOfIsAdult: Int = getColumnIndexOrThrow(_stmt, "isAdult")
        val _columnIndexOfAddedAt: Int = getColumnIndexOrThrow(_stmt, "addedAt")
        val _result: MutableList<SeriesEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SeriesEntity
          val _tmpSeriesId: Long
          _tmpSeriesId = _stmt.getLong(_columnIndexOfSeriesId)
          val _tmpCategoryRemoteId: String
          _tmpCategoryRemoteId = _stmt.getText(_columnIndexOfCategoryRemoteId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpArtworkUrl: String?
          if (_stmt.isNull(_columnIndexOfArtworkUrl)) {
            _tmpArtworkUrl = null
          } else {
            _tmpArtworkUrl = _stmt.getText(_columnIndexOfArtworkUrl)
          }
          val _tmpPlot: String?
          if (_stmt.isNull(_columnIndexOfPlot)) {
            _tmpPlot = null
          } else {
            _tmpPlot = _stmt.getText(_columnIndexOfPlot)
          }
          val _tmpRating: String?
          if (_stmt.isNull(_columnIndexOfRating)) {
            _tmpRating = null
          } else {
            _tmpRating = _stmt.getText(_columnIndexOfRating)
          }
          val _tmpReleaseYear: String?
          if (_stmt.isNull(_columnIndexOfReleaseYear)) {
            _tmpReleaseYear = null
          } else {
            _tmpReleaseYear = _stmt.getText(_columnIndexOfReleaseYear)
          }
          val _tmpIsAdult: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsAdult).toInt()
          _tmpIsAdult = _tmp_1 != 0
          val _tmpAddedAt: Long?
          if (_stmt.isNull(_columnIndexOfAddedAt)) {
            _tmpAddedAt = null
          } else {
            _tmpAddedAt = _stmt.getLong(_columnIndexOfAddedAt)
          }
          _item = SeriesEntity(_tmpSeriesId,_tmpCategoryRemoteId,_tmpName,_tmpArtworkUrl,_tmpPlot,_tmpRating,_tmpReleaseYear,_tmpIsAdult,_tmpAddedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeEpisodes(seriesId: Long): Flow<List<EpisodeEntity>> {
    val _sql: String = "SELECT * FROM episodes WHERE seriesId = ? ORDER BY seasonNumber, episodeNumber, title"
    return createFlow(__db, false, arrayOf("episodes")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, seriesId)
        val _columnIndexOfEpisodeId: Int = getColumnIndexOrThrow(_stmt, "episodeId")
        val _columnIndexOfSeriesId: Int = getColumnIndexOrThrow(_stmt, "seriesId")
        val _columnIndexOfSeasonNumber: Int = getColumnIndexOrThrow(_stmt, "seasonNumber")
        val _columnIndexOfEpisodeNumber: Int = getColumnIndexOrThrow(_stmt, "episodeNumber")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfArtworkUrl: Int = getColumnIndexOrThrow(_stmt, "artworkUrl")
        val _columnIndexOfPlot: Int = getColumnIndexOrThrow(_stmt, "plot")
        val _columnIndexOfDurationSeconds: Int = getColumnIndexOrThrow(_stmt, "durationSeconds")
        val _columnIndexOfContainerExtension: Int = getColumnIndexOrThrow(_stmt, "containerExtension")
        val _columnIndexOfDirectSource: Int = getColumnIndexOrThrow(_stmt, "directSource")
        val _columnIndexOfAddedAt: Int = getColumnIndexOrThrow(_stmt, "addedAt")
        val _result: MutableList<EpisodeEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: EpisodeEntity
          val _tmpEpisodeId: Long
          _tmpEpisodeId = _stmt.getLong(_columnIndexOfEpisodeId)
          val _tmpSeriesId: Long
          _tmpSeriesId = _stmt.getLong(_columnIndexOfSeriesId)
          val _tmpSeasonNumber: Int
          _tmpSeasonNumber = _stmt.getLong(_columnIndexOfSeasonNumber).toInt()
          val _tmpEpisodeNumber: Int
          _tmpEpisodeNumber = _stmt.getLong(_columnIndexOfEpisodeNumber).toInt()
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpArtworkUrl: String?
          if (_stmt.isNull(_columnIndexOfArtworkUrl)) {
            _tmpArtworkUrl = null
          } else {
            _tmpArtworkUrl = _stmt.getText(_columnIndexOfArtworkUrl)
          }
          val _tmpPlot: String?
          if (_stmt.isNull(_columnIndexOfPlot)) {
            _tmpPlot = null
          } else {
            _tmpPlot = _stmt.getText(_columnIndexOfPlot)
          }
          val _tmpDurationSeconds: Long?
          if (_stmt.isNull(_columnIndexOfDurationSeconds)) {
            _tmpDurationSeconds = null
          } else {
            _tmpDurationSeconds = _stmt.getLong(_columnIndexOfDurationSeconds)
          }
          val _tmpContainerExtension: String?
          if (_stmt.isNull(_columnIndexOfContainerExtension)) {
            _tmpContainerExtension = null
          } else {
            _tmpContainerExtension = _stmt.getText(_columnIndexOfContainerExtension)
          }
          val _tmpDirectSource: String?
          if (_stmt.isNull(_columnIndexOfDirectSource)) {
            _tmpDirectSource = null
          } else {
            _tmpDirectSource = _stmt.getText(_columnIndexOfDirectSource)
          }
          val _tmpAddedAt: Long?
          if (_stmt.isNull(_columnIndexOfAddedAt)) {
            _tmpAddedAt = null
          } else {
            _tmpAddedAt = _stmt.getLong(_columnIndexOfAddedAt)
          }
          _item = EpisodeEntity(_tmpEpisodeId,_tmpSeriesId,_tmpSeasonNumber,_tmpEpisodeNumber,_tmpTitle,_tmpArtworkUrl,_tmpPlot,_tmpDurationSeconds,_tmpContainerExtension,_tmpDirectSource,_tmpAddedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun channelById(streamId: Long): ChannelEntity? {
    val _sql: String = "SELECT * FROM channels WHERE streamId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, streamId)
        val _columnIndexOfStreamId: Int = getColumnIndexOrThrow(_stmt, "streamId")
        val _columnIndexOfCategoryRemoteId: Int = getColumnIndexOrThrow(_stmt, "categoryRemoteId")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfChannelNumber: Int = getColumnIndexOrThrow(_stmt, "channelNumber")
        val _columnIndexOfLogoUrl: Int = getColumnIndexOrThrow(_stmt, "logoUrl")
        val _columnIndexOfEpgChannelId: Int = getColumnIndexOrThrow(_stmt, "epgChannelId")
        val _columnIndexOfContainerExtension: Int = getColumnIndexOrThrow(_stmt, "containerExtension")
        val _columnIndexOfDirectSource: Int = getColumnIndexOrThrow(_stmt, "directSource")
        val _columnIndexOfCustomSid: Int = getColumnIndexOrThrow(_stmt, "customSid")
        val _columnIndexOfIsAdult: Int = getColumnIndexOrThrow(_stmt, "isAdult")
        val _columnIndexOfHasCatchup: Int = getColumnIndexOrThrow(_stmt, "hasCatchup")
        val _columnIndexOfCatchupDurationHours: Int = getColumnIndexOrThrow(_stmt, "catchupDurationHours")
        val _columnIndexOfAddedAt: Int = getColumnIndexOrThrow(_stmt, "addedAt")
        val _result: ChannelEntity?
        if (_stmt.step()) {
          val _tmpStreamId: Long
          _tmpStreamId = _stmt.getLong(_columnIndexOfStreamId)
          val _tmpCategoryRemoteId: String
          _tmpCategoryRemoteId = _stmt.getText(_columnIndexOfCategoryRemoteId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpChannelNumber: Int?
          if (_stmt.isNull(_columnIndexOfChannelNumber)) {
            _tmpChannelNumber = null
          } else {
            _tmpChannelNumber = _stmt.getLong(_columnIndexOfChannelNumber).toInt()
          }
          val _tmpLogoUrl: String?
          if (_stmt.isNull(_columnIndexOfLogoUrl)) {
            _tmpLogoUrl = null
          } else {
            _tmpLogoUrl = _stmt.getText(_columnIndexOfLogoUrl)
          }
          val _tmpEpgChannelId: String?
          if (_stmt.isNull(_columnIndexOfEpgChannelId)) {
            _tmpEpgChannelId = null
          } else {
            _tmpEpgChannelId = _stmt.getText(_columnIndexOfEpgChannelId)
          }
          val _tmpContainerExtension: String?
          if (_stmt.isNull(_columnIndexOfContainerExtension)) {
            _tmpContainerExtension = null
          } else {
            _tmpContainerExtension = _stmt.getText(_columnIndexOfContainerExtension)
          }
          val _tmpDirectSource: String?
          if (_stmt.isNull(_columnIndexOfDirectSource)) {
            _tmpDirectSource = null
          } else {
            _tmpDirectSource = _stmt.getText(_columnIndexOfDirectSource)
          }
          val _tmpCustomSid: String?
          if (_stmt.isNull(_columnIndexOfCustomSid)) {
            _tmpCustomSid = null
          } else {
            _tmpCustomSid = _stmt.getText(_columnIndexOfCustomSid)
          }
          val _tmpIsAdult: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsAdult).toInt()
          _tmpIsAdult = _tmp != 0
          val _tmpHasCatchup: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfHasCatchup).toInt()
          _tmpHasCatchup = _tmp_1 != 0
          val _tmpCatchupDurationHours: Int
          _tmpCatchupDurationHours = _stmt.getLong(_columnIndexOfCatchupDurationHours).toInt()
          val _tmpAddedAt: Long?
          if (_stmt.isNull(_columnIndexOfAddedAt)) {
            _tmpAddedAt = null
          } else {
            _tmpAddedAt = _stmt.getLong(_columnIndexOfAddedAt)
          }
          _result = ChannelEntity(_tmpStreamId,_tmpCategoryRemoteId,_tmpName,_tmpChannelNumber,_tmpLogoUrl,_tmpEpgChannelId,_tmpContainerExtension,_tmpDirectSource,_tmpCustomSid,_tmpIsAdult,_tmpHasCatchup,_tmpCatchupDurationHours,_tmpAddedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun movieById(streamId: Long): MovieEntity? {
    val _sql: String = "SELECT * FROM movies WHERE streamId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, streamId)
        val _columnIndexOfStreamId: Int = getColumnIndexOrThrow(_stmt, "streamId")
        val _columnIndexOfCategoryRemoteId: Int = getColumnIndexOrThrow(_stmt, "categoryRemoteId")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfArtworkUrl: Int = getColumnIndexOrThrow(_stmt, "artworkUrl")
        val _columnIndexOfPlot: Int = getColumnIndexOrThrow(_stmt, "plot")
        val _columnIndexOfRating: Int = getColumnIndexOrThrow(_stmt, "rating")
        val _columnIndexOfReleaseYear: Int = getColumnIndexOrThrow(_stmt, "releaseYear")
        val _columnIndexOfContainerExtension: Int = getColumnIndexOrThrow(_stmt, "containerExtension")
        val _columnIndexOfDirectSource: Int = getColumnIndexOrThrow(_stmt, "directSource")
        val _columnIndexOfIsAdult: Int = getColumnIndexOrThrow(_stmt, "isAdult")
        val _columnIndexOfAddedAt: Int = getColumnIndexOrThrow(_stmt, "addedAt")
        val _result: MovieEntity?
        if (_stmt.step()) {
          val _tmpStreamId: Long
          _tmpStreamId = _stmt.getLong(_columnIndexOfStreamId)
          val _tmpCategoryRemoteId: String
          _tmpCategoryRemoteId = _stmt.getText(_columnIndexOfCategoryRemoteId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpArtworkUrl: String?
          if (_stmt.isNull(_columnIndexOfArtworkUrl)) {
            _tmpArtworkUrl = null
          } else {
            _tmpArtworkUrl = _stmt.getText(_columnIndexOfArtworkUrl)
          }
          val _tmpPlot: String?
          if (_stmt.isNull(_columnIndexOfPlot)) {
            _tmpPlot = null
          } else {
            _tmpPlot = _stmt.getText(_columnIndexOfPlot)
          }
          val _tmpRating: String?
          if (_stmt.isNull(_columnIndexOfRating)) {
            _tmpRating = null
          } else {
            _tmpRating = _stmt.getText(_columnIndexOfRating)
          }
          val _tmpReleaseYear: String?
          if (_stmt.isNull(_columnIndexOfReleaseYear)) {
            _tmpReleaseYear = null
          } else {
            _tmpReleaseYear = _stmt.getText(_columnIndexOfReleaseYear)
          }
          val _tmpContainerExtension: String?
          if (_stmt.isNull(_columnIndexOfContainerExtension)) {
            _tmpContainerExtension = null
          } else {
            _tmpContainerExtension = _stmt.getText(_columnIndexOfContainerExtension)
          }
          val _tmpDirectSource: String?
          if (_stmt.isNull(_columnIndexOfDirectSource)) {
            _tmpDirectSource = null
          } else {
            _tmpDirectSource = _stmt.getText(_columnIndexOfDirectSource)
          }
          val _tmpIsAdult: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsAdult).toInt()
          _tmpIsAdult = _tmp != 0
          val _tmpAddedAt: Long?
          if (_stmt.isNull(_columnIndexOfAddedAt)) {
            _tmpAddedAt = null
          } else {
            _tmpAddedAt = _stmt.getLong(_columnIndexOfAddedAt)
          }
          _result = MovieEntity(_tmpStreamId,_tmpCategoryRemoteId,_tmpName,_tmpArtworkUrl,_tmpPlot,_tmpRating,_tmpReleaseYear,_tmpContainerExtension,_tmpDirectSource,_tmpIsAdult,_tmpAddedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun seriesById(seriesId: Long): SeriesEntity? {
    val _sql: String = "SELECT * FROM series WHERE seriesId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, seriesId)
        val _columnIndexOfSeriesId: Int = getColumnIndexOrThrow(_stmt, "seriesId")
        val _columnIndexOfCategoryRemoteId: Int = getColumnIndexOrThrow(_stmt, "categoryRemoteId")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfArtworkUrl: Int = getColumnIndexOrThrow(_stmt, "artworkUrl")
        val _columnIndexOfPlot: Int = getColumnIndexOrThrow(_stmt, "plot")
        val _columnIndexOfRating: Int = getColumnIndexOrThrow(_stmt, "rating")
        val _columnIndexOfReleaseYear: Int = getColumnIndexOrThrow(_stmt, "releaseYear")
        val _columnIndexOfIsAdult: Int = getColumnIndexOrThrow(_stmt, "isAdult")
        val _columnIndexOfAddedAt: Int = getColumnIndexOrThrow(_stmt, "addedAt")
        val _result: SeriesEntity?
        if (_stmt.step()) {
          val _tmpSeriesId: Long
          _tmpSeriesId = _stmt.getLong(_columnIndexOfSeriesId)
          val _tmpCategoryRemoteId: String
          _tmpCategoryRemoteId = _stmt.getText(_columnIndexOfCategoryRemoteId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpArtworkUrl: String?
          if (_stmt.isNull(_columnIndexOfArtworkUrl)) {
            _tmpArtworkUrl = null
          } else {
            _tmpArtworkUrl = _stmt.getText(_columnIndexOfArtworkUrl)
          }
          val _tmpPlot: String?
          if (_stmt.isNull(_columnIndexOfPlot)) {
            _tmpPlot = null
          } else {
            _tmpPlot = _stmt.getText(_columnIndexOfPlot)
          }
          val _tmpRating: String?
          if (_stmt.isNull(_columnIndexOfRating)) {
            _tmpRating = null
          } else {
            _tmpRating = _stmt.getText(_columnIndexOfRating)
          }
          val _tmpReleaseYear: String?
          if (_stmt.isNull(_columnIndexOfReleaseYear)) {
            _tmpReleaseYear = null
          } else {
            _tmpReleaseYear = _stmt.getText(_columnIndexOfReleaseYear)
          }
          val _tmpIsAdult: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsAdult).toInt()
          _tmpIsAdult = _tmp != 0
          val _tmpAddedAt: Long?
          if (_stmt.isNull(_columnIndexOfAddedAt)) {
            _tmpAddedAt = null
          } else {
            _tmpAddedAt = _stmt.getLong(_columnIndexOfAddedAt)
          }
          _result = SeriesEntity(_tmpSeriesId,_tmpCategoryRemoteId,_tmpName,_tmpArtworkUrl,_tmpPlot,_tmpRating,_tmpReleaseYear,_tmpIsAdult,_tmpAddedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun episodeById(episodeId: Long): EpisodeEntity? {
    val _sql: String = "SELECT * FROM episodes WHERE episodeId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, episodeId)
        val _columnIndexOfEpisodeId: Int = getColumnIndexOrThrow(_stmt, "episodeId")
        val _columnIndexOfSeriesId: Int = getColumnIndexOrThrow(_stmt, "seriesId")
        val _columnIndexOfSeasonNumber: Int = getColumnIndexOrThrow(_stmt, "seasonNumber")
        val _columnIndexOfEpisodeNumber: Int = getColumnIndexOrThrow(_stmt, "episodeNumber")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfArtworkUrl: Int = getColumnIndexOrThrow(_stmt, "artworkUrl")
        val _columnIndexOfPlot: Int = getColumnIndexOrThrow(_stmt, "plot")
        val _columnIndexOfDurationSeconds: Int = getColumnIndexOrThrow(_stmt, "durationSeconds")
        val _columnIndexOfContainerExtension: Int = getColumnIndexOrThrow(_stmt, "containerExtension")
        val _columnIndexOfDirectSource: Int = getColumnIndexOrThrow(_stmt, "directSource")
        val _columnIndexOfAddedAt: Int = getColumnIndexOrThrow(_stmt, "addedAt")
        val _result: EpisodeEntity?
        if (_stmt.step()) {
          val _tmpEpisodeId: Long
          _tmpEpisodeId = _stmt.getLong(_columnIndexOfEpisodeId)
          val _tmpSeriesId: Long
          _tmpSeriesId = _stmt.getLong(_columnIndexOfSeriesId)
          val _tmpSeasonNumber: Int
          _tmpSeasonNumber = _stmt.getLong(_columnIndexOfSeasonNumber).toInt()
          val _tmpEpisodeNumber: Int
          _tmpEpisodeNumber = _stmt.getLong(_columnIndexOfEpisodeNumber).toInt()
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpArtworkUrl: String?
          if (_stmt.isNull(_columnIndexOfArtworkUrl)) {
            _tmpArtworkUrl = null
          } else {
            _tmpArtworkUrl = _stmt.getText(_columnIndexOfArtworkUrl)
          }
          val _tmpPlot: String?
          if (_stmt.isNull(_columnIndexOfPlot)) {
            _tmpPlot = null
          } else {
            _tmpPlot = _stmt.getText(_columnIndexOfPlot)
          }
          val _tmpDurationSeconds: Long?
          if (_stmt.isNull(_columnIndexOfDurationSeconds)) {
            _tmpDurationSeconds = null
          } else {
            _tmpDurationSeconds = _stmt.getLong(_columnIndexOfDurationSeconds)
          }
          val _tmpContainerExtension: String?
          if (_stmt.isNull(_columnIndexOfContainerExtension)) {
            _tmpContainerExtension = null
          } else {
            _tmpContainerExtension = _stmt.getText(_columnIndexOfContainerExtension)
          }
          val _tmpDirectSource: String?
          if (_stmt.isNull(_columnIndexOfDirectSource)) {
            _tmpDirectSource = null
          } else {
            _tmpDirectSource = _stmt.getText(_columnIndexOfDirectSource)
          }
          val _tmpAddedAt: Long?
          if (_stmt.isNull(_columnIndexOfAddedAt)) {
            _tmpAddedAt = null
          } else {
            _tmpAddedAt = _stmt.getLong(_columnIndexOfAddedAt)
          }
          _result = EpisodeEntity(_tmpEpisodeId,_tmpSeriesId,_tmpSeasonNumber,_tmpEpisodeNumber,_tmpTitle,_tmpArtworkUrl,_tmpPlot,_tmpDurationSeconds,_tmpContainerExtension,_tmpDirectSource,_tmpAddedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun episodeCountForSeries(seriesId: Long): Int {
    val _sql: String = "SELECT COUNT(*) FROM episodes WHERE seriesId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, seriesId)
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeGuide(
    channelEpgId: String,
    fromEpochMillis: Long,
    limit: Int,
  ): Flow<List<EpgEventEntity>> {
    val _sql: String = "SELECT * FROM epg_events WHERE channelEpgId = ? AND endEpochMillis >= ? ORDER BY startEpochMillis LIMIT ?"
    return createFlow(__db, false, arrayOf("epg_events")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, channelEpgId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, fromEpochMillis)
        _argIndex = 3
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfChannelEpgId: Int = getColumnIndexOrThrow(_stmt, "channelEpgId")
        val _columnIndexOfStartEpochMillis: Int = getColumnIndexOrThrow(_stmt, "startEpochMillis")
        val _columnIndexOfEndEpochMillis: Int = getColumnIndexOrThrow(_stmt, "endEpochMillis")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _result: MutableList<EpgEventEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: EpgEventEntity
          val _tmpChannelEpgId: String
          _tmpChannelEpgId = _stmt.getText(_columnIndexOfChannelEpgId)
          val _tmpStartEpochMillis: Long
          _tmpStartEpochMillis = _stmt.getLong(_columnIndexOfStartEpochMillis)
          val _tmpEndEpochMillis: Long
          _tmpEndEpochMillis = _stmt.getLong(_columnIndexOfEndEpochMillis)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          _item = EpgEventEntity(_tmpChannelEpgId,_tmpStartEpochMillis,_tmpEndEpochMillis,_tmpTitle,_tmpDescription)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun futureGuideCount(channelEpgId: String, fromEpochMillis: Long): Int {
    val _sql: String = "SELECT COUNT(*) FROM epg_events WHERE channelEpgId = ? AND endEpochMillis >= ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, channelEpgId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, fromEpochMillis)
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun searchChannels(
    query: String,
    includeAdult: Boolean,
    limit: Int,
  ): List<ChannelEntity> {
    val _sql: String = """
        |
        |        SELECT * FROM channels
        |        WHERE name LIKE '%' || ? || '%'
        |          AND (? = 1 OR isAdult = 0)
        |          AND categoryRemoteId NOT IN (
        |              SELECT remoteId FROM categories WHERE section = 'live' AND hidden = 1
        |          )
        |        ORDER BY name
        |        LIMIT ?
        |        
        """.trimMargin()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, query)
        _argIndex = 2
        val _tmp: Int = if (includeAdult) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 3
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfStreamId: Int = getColumnIndexOrThrow(_stmt, "streamId")
        val _columnIndexOfCategoryRemoteId: Int = getColumnIndexOrThrow(_stmt, "categoryRemoteId")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfChannelNumber: Int = getColumnIndexOrThrow(_stmt, "channelNumber")
        val _columnIndexOfLogoUrl: Int = getColumnIndexOrThrow(_stmt, "logoUrl")
        val _columnIndexOfEpgChannelId: Int = getColumnIndexOrThrow(_stmt, "epgChannelId")
        val _columnIndexOfContainerExtension: Int = getColumnIndexOrThrow(_stmt, "containerExtension")
        val _columnIndexOfDirectSource: Int = getColumnIndexOrThrow(_stmt, "directSource")
        val _columnIndexOfCustomSid: Int = getColumnIndexOrThrow(_stmt, "customSid")
        val _columnIndexOfIsAdult: Int = getColumnIndexOrThrow(_stmt, "isAdult")
        val _columnIndexOfHasCatchup: Int = getColumnIndexOrThrow(_stmt, "hasCatchup")
        val _columnIndexOfCatchupDurationHours: Int = getColumnIndexOrThrow(_stmt, "catchupDurationHours")
        val _columnIndexOfAddedAt: Int = getColumnIndexOrThrow(_stmt, "addedAt")
        val _result: MutableList<ChannelEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ChannelEntity
          val _tmpStreamId: Long
          _tmpStreamId = _stmt.getLong(_columnIndexOfStreamId)
          val _tmpCategoryRemoteId: String
          _tmpCategoryRemoteId = _stmt.getText(_columnIndexOfCategoryRemoteId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpChannelNumber: Int?
          if (_stmt.isNull(_columnIndexOfChannelNumber)) {
            _tmpChannelNumber = null
          } else {
            _tmpChannelNumber = _stmt.getLong(_columnIndexOfChannelNumber).toInt()
          }
          val _tmpLogoUrl: String?
          if (_stmt.isNull(_columnIndexOfLogoUrl)) {
            _tmpLogoUrl = null
          } else {
            _tmpLogoUrl = _stmt.getText(_columnIndexOfLogoUrl)
          }
          val _tmpEpgChannelId: String?
          if (_stmt.isNull(_columnIndexOfEpgChannelId)) {
            _tmpEpgChannelId = null
          } else {
            _tmpEpgChannelId = _stmt.getText(_columnIndexOfEpgChannelId)
          }
          val _tmpContainerExtension: String?
          if (_stmt.isNull(_columnIndexOfContainerExtension)) {
            _tmpContainerExtension = null
          } else {
            _tmpContainerExtension = _stmt.getText(_columnIndexOfContainerExtension)
          }
          val _tmpDirectSource: String?
          if (_stmt.isNull(_columnIndexOfDirectSource)) {
            _tmpDirectSource = null
          } else {
            _tmpDirectSource = _stmt.getText(_columnIndexOfDirectSource)
          }
          val _tmpCustomSid: String?
          if (_stmt.isNull(_columnIndexOfCustomSid)) {
            _tmpCustomSid = null
          } else {
            _tmpCustomSid = _stmt.getText(_columnIndexOfCustomSid)
          }
          val _tmpIsAdult: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsAdult).toInt()
          _tmpIsAdult = _tmp_1 != 0
          val _tmpHasCatchup: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfHasCatchup).toInt()
          _tmpHasCatchup = _tmp_2 != 0
          val _tmpCatchupDurationHours: Int
          _tmpCatchupDurationHours = _stmt.getLong(_columnIndexOfCatchupDurationHours).toInt()
          val _tmpAddedAt: Long?
          if (_stmt.isNull(_columnIndexOfAddedAt)) {
            _tmpAddedAt = null
          } else {
            _tmpAddedAt = _stmt.getLong(_columnIndexOfAddedAt)
          }
          _item = ChannelEntity(_tmpStreamId,_tmpCategoryRemoteId,_tmpName,_tmpChannelNumber,_tmpLogoUrl,_tmpEpgChannelId,_tmpContainerExtension,_tmpDirectSource,_tmpCustomSid,_tmpIsAdult,_tmpHasCatchup,_tmpCatchupDurationHours,_tmpAddedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun searchMovies(
    query: String,
    includeAdult: Boolean,
    limit: Int,
  ): List<MovieEntity> {
    val _sql: String = """
        |
        |        SELECT * FROM movies
        |        WHERE name LIKE '%' || ? || '%'
        |          AND (? = 1 OR isAdult = 0)
        |          AND categoryRemoteId NOT IN (
        |              SELECT remoteId FROM categories WHERE section = 'movies' AND hidden = 1
        |          )
        |        ORDER BY name
        |        LIMIT ?
        |        
        """.trimMargin()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, query)
        _argIndex = 2
        val _tmp: Int = if (includeAdult) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 3
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfStreamId: Int = getColumnIndexOrThrow(_stmt, "streamId")
        val _columnIndexOfCategoryRemoteId: Int = getColumnIndexOrThrow(_stmt, "categoryRemoteId")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfArtworkUrl: Int = getColumnIndexOrThrow(_stmt, "artworkUrl")
        val _columnIndexOfPlot: Int = getColumnIndexOrThrow(_stmt, "plot")
        val _columnIndexOfRating: Int = getColumnIndexOrThrow(_stmt, "rating")
        val _columnIndexOfReleaseYear: Int = getColumnIndexOrThrow(_stmt, "releaseYear")
        val _columnIndexOfContainerExtension: Int = getColumnIndexOrThrow(_stmt, "containerExtension")
        val _columnIndexOfDirectSource: Int = getColumnIndexOrThrow(_stmt, "directSource")
        val _columnIndexOfIsAdult: Int = getColumnIndexOrThrow(_stmt, "isAdult")
        val _columnIndexOfAddedAt: Int = getColumnIndexOrThrow(_stmt, "addedAt")
        val _result: MutableList<MovieEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: MovieEntity
          val _tmpStreamId: Long
          _tmpStreamId = _stmt.getLong(_columnIndexOfStreamId)
          val _tmpCategoryRemoteId: String
          _tmpCategoryRemoteId = _stmt.getText(_columnIndexOfCategoryRemoteId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpArtworkUrl: String?
          if (_stmt.isNull(_columnIndexOfArtworkUrl)) {
            _tmpArtworkUrl = null
          } else {
            _tmpArtworkUrl = _stmt.getText(_columnIndexOfArtworkUrl)
          }
          val _tmpPlot: String?
          if (_stmt.isNull(_columnIndexOfPlot)) {
            _tmpPlot = null
          } else {
            _tmpPlot = _stmt.getText(_columnIndexOfPlot)
          }
          val _tmpRating: String?
          if (_stmt.isNull(_columnIndexOfRating)) {
            _tmpRating = null
          } else {
            _tmpRating = _stmt.getText(_columnIndexOfRating)
          }
          val _tmpReleaseYear: String?
          if (_stmt.isNull(_columnIndexOfReleaseYear)) {
            _tmpReleaseYear = null
          } else {
            _tmpReleaseYear = _stmt.getText(_columnIndexOfReleaseYear)
          }
          val _tmpContainerExtension: String?
          if (_stmt.isNull(_columnIndexOfContainerExtension)) {
            _tmpContainerExtension = null
          } else {
            _tmpContainerExtension = _stmt.getText(_columnIndexOfContainerExtension)
          }
          val _tmpDirectSource: String?
          if (_stmt.isNull(_columnIndexOfDirectSource)) {
            _tmpDirectSource = null
          } else {
            _tmpDirectSource = _stmt.getText(_columnIndexOfDirectSource)
          }
          val _tmpIsAdult: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsAdult).toInt()
          _tmpIsAdult = _tmp_1 != 0
          val _tmpAddedAt: Long?
          if (_stmt.isNull(_columnIndexOfAddedAt)) {
            _tmpAddedAt = null
          } else {
            _tmpAddedAt = _stmt.getLong(_columnIndexOfAddedAt)
          }
          _item = MovieEntity(_tmpStreamId,_tmpCategoryRemoteId,_tmpName,_tmpArtworkUrl,_tmpPlot,_tmpRating,_tmpReleaseYear,_tmpContainerExtension,_tmpDirectSource,_tmpIsAdult,_tmpAddedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun searchSeries(
    query: String,
    includeAdult: Boolean,
    limit: Int,
  ): List<SeriesEntity> {
    val _sql: String = """
        |
        |        SELECT * FROM series
        |        WHERE name LIKE '%' || ? || '%'
        |          AND (? = 1 OR isAdult = 0)
        |          AND categoryRemoteId NOT IN (
        |              SELECT remoteId FROM categories WHERE section = 'series' AND hidden = 1
        |          )
        |        ORDER BY name
        |        LIMIT ?
        |        
        """.trimMargin()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, query)
        _argIndex = 2
        val _tmp: Int = if (includeAdult) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 3
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfSeriesId: Int = getColumnIndexOrThrow(_stmt, "seriesId")
        val _columnIndexOfCategoryRemoteId: Int = getColumnIndexOrThrow(_stmt, "categoryRemoteId")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfArtworkUrl: Int = getColumnIndexOrThrow(_stmt, "artworkUrl")
        val _columnIndexOfPlot: Int = getColumnIndexOrThrow(_stmt, "plot")
        val _columnIndexOfRating: Int = getColumnIndexOrThrow(_stmt, "rating")
        val _columnIndexOfReleaseYear: Int = getColumnIndexOrThrow(_stmt, "releaseYear")
        val _columnIndexOfIsAdult: Int = getColumnIndexOrThrow(_stmt, "isAdult")
        val _columnIndexOfAddedAt: Int = getColumnIndexOrThrow(_stmt, "addedAt")
        val _result: MutableList<SeriesEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SeriesEntity
          val _tmpSeriesId: Long
          _tmpSeriesId = _stmt.getLong(_columnIndexOfSeriesId)
          val _tmpCategoryRemoteId: String
          _tmpCategoryRemoteId = _stmt.getText(_columnIndexOfCategoryRemoteId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpArtworkUrl: String?
          if (_stmt.isNull(_columnIndexOfArtworkUrl)) {
            _tmpArtworkUrl = null
          } else {
            _tmpArtworkUrl = _stmt.getText(_columnIndexOfArtworkUrl)
          }
          val _tmpPlot: String?
          if (_stmt.isNull(_columnIndexOfPlot)) {
            _tmpPlot = null
          } else {
            _tmpPlot = _stmt.getText(_columnIndexOfPlot)
          }
          val _tmpRating: String?
          if (_stmt.isNull(_columnIndexOfRating)) {
            _tmpRating = null
          } else {
            _tmpRating = _stmt.getText(_columnIndexOfRating)
          }
          val _tmpReleaseYear: String?
          if (_stmt.isNull(_columnIndexOfReleaseYear)) {
            _tmpReleaseYear = null
          } else {
            _tmpReleaseYear = _stmt.getText(_columnIndexOfReleaseYear)
          }
          val _tmpIsAdult: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsAdult).toInt()
          _tmpIsAdult = _tmp_1 != 0
          val _tmpAddedAt: Long?
          if (_stmt.isNull(_columnIndexOfAddedAt)) {
            _tmpAddedAt = null
          } else {
            _tmpAddedAt = _stmt.getLong(_columnIndexOfAddedAt)
          }
          _item = SeriesEntity(_tmpSeriesId,_tmpCategoryRemoteId,_tmpName,_tmpArtworkUrl,_tmpPlot,_tmpRating,_tmpReleaseYear,_tmpIsAdult,_tmpAddedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeFavoriteIds(targetType: String): Flow<List<String>> {
    val _sql: String = "SELECT targetId FROM favorite_items WHERE targetType = ?"
    return createFlow(__db, false, arrayOf("favorite_items")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, targetType)
        val _result: MutableList<String> = mutableListOf()
        while (_stmt.step()) {
          val _item: String
          _item = _stmt.getText(0)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeFavoriteCount(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM favorite_items"
    return createFlow(__db, false, arrayOf("favorite_items")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeFavoriteItems(targetType: String, limit: Int): Flow<List<FavoriteItemEntity>> {
    val _sql: String = "SELECT * FROM favorite_items WHERE targetType = ? ORDER BY addedAt DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("favorite_items")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, targetType)
        _argIndex = 2
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfTargetType: Int = getColumnIndexOrThrow(_stmt, "targetType")
        val _columnIndexOfTargetId: Int = getColumnIndexOrThrow(_stmt, "targetId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfSubtitle: Int = getColumnIndexOrThrow(_stmt, "subtitle")
        val _columnIndexOfArtworkUrl: Int = getColumnIndexOrThrow(_stmt, "artworkUrl")
        val _columnIndexOfAddedAt: Int = getColumnIndexOrThrow(_stmt, "addedAt")
        val _result: MutableList<FavoriteItemEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: FavoriteItemEntity
          val _tmpTargetType: String
          _tmpTargetType = _stmt.getText(_columnIndexOfTargetType)
          val _tmpTargetId: String
          _tmpTargetId = _stmt.getText(_columnIndexOfTargetId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpSubtitle: String?
          if (_stmt.isNull(_columnIndexOfSubtitle)) {
            _tmpSubtitle = null
          } else {
            _tmpSubtitle = _stmt.getText(_columnIndexOfSubtitle)
          }
          val _tmpArtworkUrl: String?
          if (_stmt.isNull(_columnIndexOfArtworkUrl)) {
            _tmpArtworkUrl = null
          } else {
            _tmpArtworkUrl = _stmt.getText(_columnIndexOfArtworkUrl)
          }
          val _tmpAddedAt: Long
          _tmpAddedAt = _stmt.getLong(_columnIndexOfAddedAt)
          _item = FavoriteItemEntity(_tmpTargetType,_tmpTargetId,_tmpTitle,_tmpSubtitle,_tmpArtworkUrl,_tmpAddedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeContinueWatching(limit: Int): Flow<List<PlaybackHistoryEntity>> {
    val _sql: String = "SELECT * FROM playback_history ORDER BY lastPlayedAt DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("playback_history")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTargetType: Int = getColumnIndexOrThrow(_stmt, "targetType")
        val _columnIndexOfTargetId: Int = getColumnIndexOrThrow(_stmt, "targetId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfSubtitle: Int = getColumnIndexOrThrow(_stmt, "subtitle")
        val _columnIndexOfArtworkUrl: Int = getColumnIndexOrThrow(_stmt, "artworkUrl")
        val _columnIndexOfPositionMs: Int = getColumnIndexOrThrow(_stmt, "positionMs")
        val _columnIndexOfDurationMs: Int = getColumnIndexOrThrow(_stmt, "durationMs")
        val _columnIndexOfLastPlayedAt: Int = getColumnIndexOrThrow(_stmt, "lastPlayedAt")
        val _result: MutableList<PlaybackHistoryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: PlaybackHistoryEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTargetType: String
          _tmpTargetType = _stmt.getText(_columnIndexOfTargetType)
          val _tmpTargetId: String
          _tmpTargetId = _stmt.getText(_columnIndexOfTargetId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpSubtitle: String?
          if (_stmt.isNull(_columnIndexOfSubtitle)) {
            _tmpSubtitle = null
          } else {
            _tmpSubtitle = _stmt.getText(_columnIndexOfSubtitle)
          }
          val _tmpArtworkUrl: String?
          if (_stmt.isNull(_columnIndexOfArtworkUrl)) {
            _tmpArtworkUrl = null
          } else {
            _tmpArtworkUrl = _stmt.getText(_columnIndexOfArtworkUrl)
          }
          val _tmpPositionMs: Long
          _tmpPositionMs = _stmt.getLong(_columnIndexOfPositionMs)
          val _tmpDurationMs: Long
          _tmpDurationMs = _stmt.getLong(_columnIndexOfDurationMs)
          val _tmpLastPlayedAt: Long
          _tmpLastPlayedAt = _stmt.getLong(_columnIndexOfLastPlayedAt)
          _item = PlaybackHistoryEntity(_tmpId,_tmpTargetType,_tmpTargetId,_tmpTitle,_tmpSubtitle,_tmpArtworkUrl,_tmpPositionMs,_tmpDurationMs,_tmpLastPlayedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeRecentChannels(limit: Int): Flow<List<RecentChannelEntity>> {
    val _sql: String = "SELECT * FROM recent_channels ORDER BY lastPlayedAt DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("recent_channels")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfChannelId: Int = getColumnIndexOrThrow(_stmt, "channelId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfArtworkUrl: Int = getColumnIndexOrThrow(_stmt, "artworkUrl")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "categoryId")
        val _columnIndexOfLastPlayedAt: Int = getColumnIndexOrThrow(_stmt, "lastPlayedAt")
        val _result: MutableList<RecentChannelEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: RecentChannelEntity
          val _tmpChannelId: Long
          _tmpChannelId = _stmt.getLong(_columnIndexOfChannelId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpArtworkUrl: String?
          if (_stmt.isNull(_columnIndexOfArtworkUrl)) {
            _tmpArtworkUrl = null
          } else {
            _tmpArtworkUrl = _stmt.getText(_columnIndexOfArtworkUrl)
          }
          val _tmpCategoryId: String?
          if (_stmt.isNull(_columnIndexOfCategoryId)) {
            _tmpCategoryId = null
          } else {
            _tmpCategoryId = _stmt.getText(_columnIndexOfCategoryId)
          }
          val _tmpLastPlayedAt: Long
          _tmpLastPlayedAt = _stmt.getLong(_columnIndexOfLastPlayedAt)
          _item = RecentChannelEntity(_tmpChannelId,_tmpTitle,_tmpArtworkUrl,_tmpCategoryId,_tmpLastPlayedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeChannelCount(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM channels"
    return createFlow(__db, false, arrayOf("channels")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeMovieCount(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM movies"
    return createFlow(__db, false, arrayOf("movies")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeSeriesCount(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM series"
    return createFlow(__db, false, arrayOf("series")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeEpisodeCount(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM episodes"
    return createFlow(__db, false, arrayOf("episodes")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun channelIdsForCategory(categoryId: String, includeAdult: Boolean): List<Long> {
    val _sql: String = """
        |
        |        SELECT streamId FROM channels
        |        WHERE categoryRemoteId = ?
        |          AND (? = 1 OR isAdult = 0)
        |        ORDER BY
        |            CASE WHEN channelNumber IS NULL THEN 1 ELSE 0 END,
        |            channelNumber,
        |            name
        |        
        """.trimMargin()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, categoryId)
        _argIndex = 2
        val _tmp: Int = if (includeAdult) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        val _result: MutableList<Long> = mutableListOf()
        while (_stmt.step()) {
          val _item: Long
          _item = _stmt.getLong(0)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearCategories(section: String) {
    val _sql: String = "DELETE FROM categories WHERE section = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, section)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearChannels() {
    val _sql: String = "DELETE FROM channels"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearMovies() {
    val _sql: String = "DELETE FROM movies"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearSeries() {
    val _sql: String = "DELETE FROM series"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearEpisodesForSeries(seriesId: Long) {
    val _sql: String = "DELETE FROM episodes WHERE seriesId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, seriesId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearEpg() {
    val _sql: String = "DELETE FROM epg_events"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteFavorite(targetType: String, targetId: String) {
    val _sql: String = "DELETE FROM favorite_items WHERE targetType = ? AND targetId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, targetType)
        _argIndex = 2
        _stmt.bindText(_argIndex, targetId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun setCategoryHidden(
    section: String,
    remoteId: String,
    hidden: Boolean,
  ) {
    val _sql: String = "UPDATE categories SET hidden = ? WHERE section = ? AND remoteId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (hidden) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindText(_argIndex, section)
        _argIndex = 3
        _stmt.bindText(_argIndex, remoteId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateSeriesDetails(
    seriesId: Long,
    artworkUrl: String?,
    plot: String?,
    rating: String?,
    releaseYear: String?,
  ) {
    val _sql: String = "UPDATE series SET artworkUrl = ?, plot = ?, rating = ?, releaseYear = ? WHERE seriesId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        if (artworkUrl == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, artworkUrl)
        }
        _argIndex = 2
        if (plot == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, plot)
        }
        _argIndex = 3
        if (rating == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, rating)
        }
        _argIndex = 4
        if (releaseYear == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, releaseYear)
        }
        _argIndex = 5
        _stmt.bindLong(_argIndex, seriesId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
