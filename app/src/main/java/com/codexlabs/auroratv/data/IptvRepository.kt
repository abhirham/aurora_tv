package com.codexlabs.auroratv.data

import com.codexlabs.auroratv.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class IptvRepository(
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository,
    private val xtreamApi: XtreamApi,
) {
    private val dao = database.mediaDao()

    val settings = settingsRepository.settings

    val libraryStats: Flow<LibraryStats> = combine(
        dao.observeChannelCount(),
        dao.observeMovieCount(),
        dao.observeSeriesCount(),
        dao.observeEpisodeCount(),
        dao.observeFavoriteCount(),
    ) { live, movies, series, episodes, favorites ->
        LibraryStats(
            liveChannels = live,
            movies = movies,
            series = series,
            episodes = episodes,
            favorites = favorites,
        )
    }

    fun observeCategories(section: LibrarySection, includeAdult: Boolean): Flow<List<CategoryEntity>> {
        return dao.observeVisibleCategories(section.rawValue, includeAdult)
    }

    fun observeChannels(categoryId: String?, includeAdult: Boolean): Flow<List<ChannelEntity>> {
        return dao.observeChannels(categoryId, includeAdult)
    }

    fun observeMovies(categoryId: String?, includeAdult: Boolean): Flow<List<MovieEntity>> {
        return dao.observeMovies(categoryId, includeAdult)
    }

    fun observeSeries(categoryId: String?, includeAdult: Boolean): Flow<List<SeriesEntity>> {
        return dao.observeSeries(categoryId, includeAdult)
    }

    fun observeEpisodes(seriesId: Long): Flow<List<EpisodeEntity>> {
        return dao.observeEpisodes(seriesId)
    }

    fun observeGuide(streamId: Long): Flow<List<EpgEventEntity>> {
        return flow {
            val channel = dao.channelById(streamId)
            if (channel?.epgChannelId.isNullOrBlank()) {
                emit(emptyList())
            } else {
                emitAll(dao.observeGuide(channel.epgChannelId!!, System.currentTimeMillis(), 18))
            }
        }
    }

    fun observeFavoriteItems(targetType: TargetType, limit: Int = 24): Flow<List<FavoriteItemEntity>> {
        return dao.observeFavoriteItems(targetType.rawValue, limit)
    }

    fun observeContinueWatching(limit: Int = 20): Flow<List<PlaybackHistoryEntity>> {
        return dao.observeContinueWatching(limit)
    }

    fun observeRecentChannels(limit: Int = 20): Flow<List<RecentChannelEntity>> {
        return dao.observeRecentChannels(limit)
    }

    suspend fun syncAll(onProgress: suspend (String) -> Unit = {}) = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.first()
        val credentials = settings.credentialsOrNull() ?: error("Provider is not configured")

        val liveCategories = xtreamApi.fetchCategories(credentials, LibrarySection.LIVE)
            .map { it.copy(section = LibrarySection.LIVE) }
        val movieCategories = xtreamApi.fetchCategories(credentials, LibrarySection.MOVIES)
            .map { it.copy(section = LibrarySection.MOVIES) }
        val seriesCategories = xtreamApi.fetchCategories(credentials, LibrarySection.SERIES)
            .map { it.copy(section = LibrarySection.SERIES) }

        onProgress("Saving category layout")
        saveCategories(LibrarySection.LIVE, liveCategories)
        saveCategories(LibrarySection.MOVIES, movieCategories)
        saveCategories(LibrarySection.SERIES, seriesCategories)

        val liveAdultMap = liveCategories.associate { it.remoteId to it.isAdult }
        val movieAdultMap = movieCategories.associate { it.remoteId to it.isAdult }
        val seriesAdultMap = seriesCategories.associate { it.remoteId to it.isAdult }

        onProgress("Syncing live TV channels")
        dao.clearChannels()
        xtreamApi.streamChannels(credentials, liveAdultMap) { chunk ->
            dao.upsertChannels(
                chunk.map { payload ->
                    ChannelEntity(
                        streamId = payload.streamId,
                        categoryRemoteId = payload.categoryRemoteId,
                        name = payload.name,
                        channelNumber = payload.channelNumber,
                        logoUrl = payload.logoUrl,
                        epgChannelId = payload.epgChannelId,
                        containerExtension = payload.containerExtension,
                        directSource = payload.directSource,
                        customSid = payload.customSid,
                        isAdult = payload.isAdult,
                        hasCatchup = payload.hasCatchup,
                        catchupDurationHours = payload.catchupDurationHours,
                        addedAt = payload.addedAt,
                    )
                },
            )
        }

        onProgress("Syncing movie library")
        dao.clearMovies()
        xtreamApi.streamMovies(credentials, movieAdultMap) { chunk ->
            dao.upsertMovies(
                chunk.map { payload ->
                    MovieEntity(
                        streamId = payload.streamId,
                        categoryRemoteId = payload.categoryRemoteId,
                        name = payload.name,
                        artworkUrl = payload.artworkUrl,
                        plot = payload.plot,
                        rating = payload.rating,
                        releaseYear = payload.releaseYear,
                        containerExtension = payload.containerExtension,
                        directSource = payload.directSource,
                        isAdult = payload.isAdult,
                        addedAt = payload.addedAt,
                    )
                },
            )
        }

        onProgress("Syncing series library")
        dao.clearSeries()
        xtreamApi.streamSeries(credentials, seriesAdultMap) { chunk ->
            dao.upsertSeries(
                chunk.map { payload ->
                    SeriesEntity(
                        seriesId = payload.seriesId,
                        categoryRemoteId = payload.categoryRemoteId,
                        name = payload.name,
                        artworkUrl = payload.artworkUrl,
                        plot = payload.plot,
                        rating = payload.rating,
                        releaseYear = payload.releaseYear,
                        isAdult = payload.isAdult,
                        addedAt = payload.addedAt,
                    )
                },
            )
        }

        onProgress("Syncing TV guide")
        runCatching {
            dao.clearEpg()
            xtreamApi.streamXmlTv(credentials, settings.epgWindowHours) { chunk ->
                dao.upsertEpg(
                    chunk.map { payload ->
                        EpgEventEntity(
                            channelEpgId = payload.channelEpgId,
                            startEpochMillis = payload.startEpochMillis,
                            endEpochMillis = payload.endEpochMillis,
                            title = payload.title,
                            description = payload.description,
                        )
                    },
                )
            }
        }

        settingsRepository.markSyncCompleted(System.currentTimeMillis())
    }

    suspend fun ensureSeriesLoaded(seriesId: Long) = withContext(Dispatchers.IO) {
        if (dao.episodeCountForSeries(seriesId) > 0) return@withContext
        val settings = settingsRepository.settings.first()
        val credentials = settings.credentialsOrNull() ?: return@withContext
        val detail = xtreamApi.fetchSeriesInfo(credentials, seriesId)
        dao.clearEpisodesForSeries(seriesId)
        if (detail.episodes.isNotEmpty()) {
            dao.upsertEpisodes(
                detail.episodes.map { payload ->
                    EpisodeEntity(
                        episodeId = payload.episodeId,
                        seriesId = payload.seriesId,
                        seasonNumber = payload.seasonNumber,
                        episodeNumber = payload.episodeNumber,
                        title = payload.title,
                        artworkUrl = payload.artworkUrl,
                        plot = payload.plot,
                        durationSeconds = payload.durationSeconds,
                        containerExtension = payload.containerExtension,
                        directSource = payload.directSource,
                        addedAt = payload.addedAt,
                    )
                },
            )
        }
        dao.updateSeriesDetails(
            seriesId = seriesId,
            artworkUrl = detail.artworkUrl,
            plot = detail.plot,
            rating = detail.rating,
            releaseYear = detail.releaseYear,
        )
    }

    suspend fun search(query: String, includeAdult: Boolean): SearchResults = withContext(Dispatchers.IO) {
        val normalized = query.trim()
        if (normalized.isBlank()) return@withContext SearchResults()
        SearchResults(
            channels = dao.searchChannels(normalized, includeAdult, 24).map {
                SearchResultItem(
                    targetType = TargetType.CHANNEL,
                    targetId = it.streamId.toString(),
                    title = it.name,
                    subtitle = "Live TV",
                    artworkUrl = it.logoUrl,
                )
            },
            movies = dao.searchMovies(normalized, includeAdult, 24).map {
                SearchResultItem(
                    targetType = TargetType.MOVIE,
                    targetId = it.streamId.toString(),
                    title = it.name,
                    subtitle = "Movie",
                    artworkUrl = it.artworkUrl,
                )
            },
            series = dao.searchSeries(normalized, includeAdult, 24).map {
                SearchResultItem(
                    targetType = TargetType.SERIES,
                    targetId = it.seriesId.toString(),
                    title = it.name,
                    subtitle = "Series",
                    artworkUrl = it.artworkUrl,
                )
            },
        )
    }

    suspend fun setCategoryHidden(
        section: LibrarySection,
        remoteId: String,
        hidden: Boolean,
    ) = withContext(Dispatchers.IO) {
        dao.setCategoryHidden(section.rawValue, remoteId, hidden)
    }

    suspend fun toggleFavorite(
        targetType: TargetType,
        targetId: String,
        title: String,
        subtitle: String?,
        artworkUrl: String?,
    ) = withContext(Dispatchers.IO) {
        val isFavorite = dao.isFavorite(targetType.rawValue, targetId)
        if (isFavorite) {
            dao.deleteFavorite(targetType.rawValue, targetId)
        } else {
            dao.upsertFavorite(
                FavoriteItemEntity(
                    targetType = targetType.rawValue,
                    targetId = targetId,
                    title = title,
                    subtitle = subtitle,
                    artworkUrl = artworkUrl,
                    addedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun resolvePlaybackDescriptor(
        targetType: TargetType,
        targetId: String,
        categoryIdHint: String? = null,
    ): PlaybackDescriptor = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.first()
        val credentials = settings.credentialsOrNull() ?: error("Provider is not configured")

        when (targetType) {
            TargetType.CHANNEL -> {
                val channel = dao.channelById(targetId.toLong()) ?: error("Channel not found")
                PlaybackDescriptor(
                    targetType = targetType,
                    targetId = targetId,
                    title = channel.name,
                    subtitle = "Live TV",
                    artworkUrl = channel.logoUrl,
                    mediaUrl = xtreamApi.liveStreamUrl(credentials, channel),
                    isLive = true,
                    categoryId = categoryIdHint ?: channel.categoryRemoteId,
                    epgChannelId = channel.epgChannelId,
                )
            }
            TargetType.MOVIE -> {
                val movie = dao.movieById(targetId.toLong()) ?: error("Movie not found")
                PlaybackDescriptor(
                    targetType = targetType,
                    targetId = targetId,
                    title = movie.name,
                    subtitle = movie.releaseYear,
                    artworkUrl = movie.artworkUrl,
                    mediaUrl = xtreamApi.movieStreamUrl(credentials, movie),
                    isLive = false,
                )
            }
            TargetType.EPISODE -> {
                val episode = dao.episodeById(targetId.toLong()) ?: error("Episode not found")
                val nextEpisode = dao.nextEpisodeAfter(
                    seriesId = episode.seriesId,
                    seasonNumber = episode.seasonNumber,
                    episodeNumber = episode.episodeNumber,
                    episodeId = episode.episodeId,
                )
                PlaybackDescriptor(
                    targetType = targetType,
                    targetId = targetId,
                    title = episode.title,
                    subtitle = "S${episode.seasonNumber} · E${episode.episodeNumber}",
                    artworkUrl = episode.artworkUrl,
                    mediaUrl = xtreamApi.episodeStreamUrl(credentials, episode),
                    isLive = false,
                    nextEpisodeId = nextEpisode?.episodeId?.toString(),
                    nextEpisodeTitle = nextEpisode?.title,
                )
            }
            TargetType.SERIES -> error("Series items require an episode target")
        }
    }

    suspend fun resolveAdjacentChannel(
        currentChannelId: Long,
        categoryId: String,
        offset: Int,
        includeAdult: Boolean,
    ): PlaybackDescriptor? = withContext(Dispatchers.IO) {
        if (offset == 0) return@withContext null
        val adjacentChannel = if (offset > 0) {
            dao.nextChannelInCategory(currentChannelId, categoryId, includeAdult)
        } else {
            dao.previousChannelInCategory(currentChannelId, categoryId, includeAdult)
        } ?: return@withContext null
        resolvePlaybackDescriptor(
            targetType = TargetType.CHANNEL,
            targetId = adjacentChannel.streamId.toString(),
            categoryIdHint = categoryId,
        )
    }

    suspend fun ensureGuide(streamId: Long) = withContext(Dispatchers.IO) {
        val channel = dao.channelById(streamId) ?: return@withContext
        val epgChannelId = channel.epgChannelId ?: return@withContext
        if (dao.futureGuideCount(epgChannelId, System.currentTimeMillis()) > 0) return@withContext
        val settings = settingsRepository.settings.first()
        val credentials = settings.credentialsOrNull() ?: return@withContext
        val guide = xtreamApi.fetchShortEpg(credentials, streamId, epgChannelId)
        if (guide.isNotEmpty()) {
            dao.upsertEpg(
                guide.map {
                    EpgEventEntity(
                        channelEpgId = it.channelEpgId,
                        startEpochMillis = it.startEpochMillis,
                        endEpochMillis = it.endEpochMillis,
                        title = it.title,
                        description = it.description,
                    )
                },
            )
        }
    }

    suspend fun updatePlaybackHistory(
        descriptor: PlaybackDescriptor,
        positionMs: Long,
        durationMs: Long,
    ) = withContext(Dispatchers.IO) {
        if (descriptor.isLive) return@withContext
        dao.upsertPlaybackHistory(
            PlaybackHistoryEntity(
                id = "${descriptor.targetType.rawValue}:${descriptor.targetId}",
                targetType = descriptor.targetType.rawValue,
                targetId = descriptor.targetId,
                title = descriptor.title,
                subtitle = descriptor.subtitle,
                artworkUrl = descriptor.artworkUrl,
                positionMs = positionMs,
                durationMs = durationMs,
                lastPlayedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun registerRecentChannel(
        channelId: Long,
        title: String,
        artworkUrl: String?,
        categoryId: String?,
    ) = withContext(Dispatchers.IO) {
        dao.upsertRecentChannel(
            RecentChannelEntity(
                channelId = channelId,
                title = title,
                artworkUrl = artworkUrl,
                categoryId = categoryId,
                lastPlayedAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun saveCategories(
        section: LibrarySection,
        categories: List<XtreamApi.CategoryPayload>,
    ) {
        val hiddenIds = dao.hiddenCategoryIds(section.rawValue).toSet()
        dao.clearCategories(section.rawValue)
        dao.upsertCategories(
            categories.mapIndexed { index, payload ->
                CategoryEntity(
                    section = section.rawValue,
                    remoteId = payload.remoteId,
                    name = payload.name,
                    isAdult = payload.isAdult,
                    hidden = payload.remoteId in hiddenIds,
                    sortOrder = index,
                )
            },
        )
    }
}
