package com.codexlabs.auroratv.data

import com.codexlabs.auroratv.settings.SettingsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val EMPTY_GUIDE_CACHE_TTL_MS = 60L * 60L * 1000L
private const val MAX_BROWSE_WINDOW_SIZE = 600
private val SearchTokenSplitRegex = Regex("\\s+")

class IptvRepository(
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository,
    private val xtreamApi: XtreamApi,
) {
    private val dao = database.mediaDao()
    private val lazyLoadMutex = Mutex()
    private val inFlightGuideFetches = mutableMapOf<String, CompletableDeferred<Unit>>()
    private val inFlightSeriesLoads = mutableMapOf<Long, CompletableDeferred<Unit>>()
    private val emptyGuideCache = mutableMapOf<String, Long>()

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

    fun observeChannels(categoryId: String?, includeAdult: Boolean, limit: Int): Flow<List<ChannelListItem>> {
        return dao.observeChannels(categoryId, includeAdult, limit.coerceIn(1, MAX_BROWSE_WINDOW_SIZE))
    }

    fun observeMovies(categoryId: String?, includeAdult: Boolean, limit: Int): Flow<List<MovieListItem>> {
        return dao.observeMovies(categoryId, includeAdult, limit.coerceIn(1, MAX_BROWSE_WINDOW_SIZE))
    }

    fun observeSeries(categoryId: String?, includeAdult: Boolean, limit: Int): Flow<List<SeriesListItem>> {
        return dao.observeSeries(categoryId, includeAdult, limit.coerceIn(1, MAX_BROWSE_WINDOW_SIZE))
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
        val syncToken = System.currentTimeMillis()

        val (liveCategories, movieCategories, seriesCategories) = coroutineScope {
            val live = async {
                xtreamApi.fetchCategories(credentials, LibrarySection.LIVE)
                    .map { it.copy(section = LibrarySection.LIVE) }
            }
            val movies = async {
                xtreamApi.fetchCategories(credentials, LibrarySection.MOVIES)
                    .map { it.copy(section = LibrarySection.MOVIES) }
            }
            val series = async {
                xtreamApi.fetchCategories(credentials, LibrarySection.SERIES)
                    .map { it.copy(section = LibrarySection.SERIES) }
            }
            Triple(live.await(), movies.await(), series.await())
        }

        onProgress("Saving category layout")
        saveCategories(LibrarySection.LIVE, liveCategories, syncToken)
        saveCategories(LibrarySection.MOVIES, movieCategories, syncToken)
        saveCategories(LibrarySection.SERIES, seriesCategories, syncToken)

        val liveAdultMap = liveCategories.associate { it.remoteId to it.isAdult }
        val movieAdultMap = movieCategories.associate { it.remoteId to it.isAdult }
        val seriesAdultMap = seriesCategories.associate { it.remoteId to it.isAdult }
        val liveHiddenIds = dao.hiddenCategoryIds(LibrarySection.LIVE.rawValue).toSet()
        val movieHiddenIds = dao.hiddenCategoryIds(LibrarySection.MOVIES.rawValue).toSet()
        val seriesHiddenIds = dao.hiddenCategoryIds(LibrarySection.SERIES.rawValue).toSet()

        val syncWriteMutex = Mutex()
        coroutineScope {
            val liveJob = async {
                onProgress("Syncing live TV channels")
                xtreamApi.streamChannels(credentials, liveAdultMap) { chunk ->
                    syncWriteMutex.withLock {
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
                                    categoryHidden = payload.categoryRemoteId in liveHiddenIds,
                                    hasCatchup = payload.hasCatchup,
                                    catchupDurationHours = payload.catchupDurationHours,
                                    addedAt = payload.addedAt,
                                    syncToken = syncToken,
                                )
                            },
                        )
                        dao.upsertChannelSearch(
                            chunk.map { payload ->
                                ChannelSearchEntity(
                                    rowId = payload.streamId,
                                    name = payload.name,
                                )
                            },
                        )
                    }
                }
                syncWriteMutex.withLock {
                    dao.deleteStaleChannels(syncToken)
                    dao.deleteStaleChannelSearch()
                }
            }

            val movieJob = async {
                onProgress("Syncing movie library")
                xtreamApi.streamMovies(credentials, movieAdultMap) { chunk ->
                    syncWriteMutex.withLock {
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
                                    categoryHidden = payload.categoryRemoteId in movieHiddenIds,
                                    addedAt = payload.addedAt,
                                    syncToken = syncToken,
                                )
                            },
                        )
                        dao.upsertMovieSearch(
                            chunk.map { payload ->
                                MovieSearchEntity(
                                    rowId = payload.streamId,
                                    name = payload.name,
                                )
                            },
                        )
                    }
                }
                syncWriteMutex.withLock {
                    dao.deleteStaleMovies(syncToken)
                    dao.deleteStaleMovieSearch()
                }
            }

            val seriesJob = async {
                onProgress("Syncing series library")
                xtreamApi.streamSeries(credentials, seriesAdultMap) { chunk ->
                    syncWriteMutex.withLock {
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
                                    categoryHidden = payload.categoryRemoteId in seriesHiddenIds,
                                    addedAt = payload.addedAt,
                                    syncToken = syncToken,
                                )
                            },
                        )
                        dao.upsertSeriesSearch(
                            chunk.map { payload ->
                                SeriesSearchEntity(
                                    rowId = payload.seriesId,
                                    name = payload.name,
                                )
                            },
                        )
                    }
                }
                syncWriteMutex.withLock {
                    dao.deleteStaleSeries(syncToken)
                    dao.deleteStaleSeriesSearch()
                }
            }

            liveJob.await()
            movieJob.await()
            seriesJob.await()
        }

        onProgress("TV guide will load on demand")

        settingsRepository.markSyncCompleted(System.currentTimeMillis())
    }

    suspend fun seedDebugCatalog(onProgress: suspend (String) -> Unit = {}) = withContext(Dispatchers.IO) {
        val syncToken = System.currentTimeMillis()
        val liveCategories = (1..48).map { index ->
            CategoryEntity(
                section = LibrarySection.LIVE.rawValue,
                remoteId = "debug-live-$index",
                name = "Debug Live $index",
                isAdult = false,
                hidden = false,
                sortOrder = index,
                syncToken = syncToken,
            )
        }
        val movieCategories = (1..36).map { index ->
            CategoryEntity(
                section = LibrarySection.MOVIES.rawValue,
                remoteId = "debug-movies-$index",
                name = "Debug Movies $index",
                isAdult = false,
                hidden = false,
                sortOrder = index,
                syncToken = syncToken,
            )
        }
        val seriesCategories = (1..30).map { index ->
            CategoryEntity(
                section = LibrarySection.SERIES.rawValue,
                remoteId = "debug-series-$index",
                name = "Debug Series $index",
                isAdult = false,
                hidden = false,
                sortOrder = index,
                syncToken = syncToken,
            )
        }

        onProgress("Seeding debug categories")
        dao.upsertCategories(liveCategories + movieCategories + seriesCategories)
        dao.deleteStaleCategories(LibrarySection.LIVE.rawValue, syncToken)
        dao.deleteStaleCategories(LibrarySection.MOVIES.rawValue, syncToken)
        dao.deleteStaleCategories(LibrarySection.SERIES.rawValue, syncToken)

        onProgress("Seeding debug live channels")
        (1L..5_000L).chunked(400).forEach { ids ->
            val channels = ids.map { id ->
                val category = liveCategories[((id - 1) % liveCategories.size).toInt()]
                ChannelEntity(
                    streamId = 9_000_000L + id,
                    categoryRemoteId = category.remoteId,
                    name = "Debug Channel ${id.toString().padStart(4, '0')}",
                    channelNumber = id.toInt(),
                    logoUrl = null,
                    epgChannelId = "debug-epg-$id",
                    containerExtension = "ts",
                    directSource = null,
                    customSid = null,
                    isAdult = false,
                    categoryHidden = false,
                    hasCatchup = false,
                    catchupDurationHours = 0,
                    addedAt = syncToken,
                    syncToken = syncToken,
                )
            }
            dao.upsertChannels(channels)
            dao.upsertChannelSearch(channels.map { ChannelSearchEntity(it.streamId, it.name) })
        }
        dao.deleteStaleChannels(syncToken)
        dao.deleteStaleChannelSearch()

        onProgress("Seeding debug movies")
        (1L..8_000L).chunked(400).forEach { ids ->
            val movies = ids.map { id ->
                val category = movieCategories[((id - 1) % movieCategories.size).toInt()]
                MovieEntity(
                    streamId = 8_000_000L + id,
                    categoryRemoteId = category.remoteId,
                    name = "Debug Movie ${id.toString().padStart(4, '0')}",
                    artworkUrl = null,
                    plot = "Synthetic movie row for browse and search performance measurement.",
                    rating = null,
                    releaseYear = "2026",
                    containerExtension = "mp4",
                    directSource = null,
                    isAdult = false,
                    categoryHidden = false,
                    addedAt = syncToken,
                    syncToken = syncToken,
                )
            }
            dao.upsertMovies(movies)
            dao.upsertMovieSearch(movies.map { MovieSearchEntity(it.streamId, it.name) })
        }
        dao.deleteStaleMovies(syncToken)
        dao.deleteStaleMovieSearch()

        onProgress("Seeding debug series")
        (1L..3_000L).chunked(400).forEach { ids ->
            val series = ids.map { id ->
                val category = seriesCategories[((id - 1) % seriesCategories.size).toInt()]
                SeriesEntity(
                    seriesId = 7_000_000L + id,
                    categoryRemoteId = category.remoteId,
                    name = "Debug Series ${id.toString().padStart(4, '0')}",
                    artworkUrl = null,
                    plot = "Synthetic series row for browse and search performance measurement.",
                    rating = null,
                    releaseYear = "2026",
                    isAdult = false,
                    categoryHidden = false,
                    addedAt = syncToken,
                    syncToken = syncToken,
                )
            }
            dao.upsertSeries(series)
            dao.upsertSeriesSearch(series.map { SeriesSearchEntity(it.seriesId, it.name) })
        }
        dao.deleteStaleSeries(syncToken)
        dao.deleteStaleSeriesSearch()
        settingsRepository.markSyncCompleted(syncToken)
    }

    suspend fun ensureSeriesLoaded(seriesId: Long) = withContext(Dispatchers.IO) {
        if (dao.episodeCountForSeries(seriesId) > 0) return@withContext
        runSingleFlight(seriesId, inFlightSeriesLoads) {
            if (dao.episodeCountForSeries(seriesId) > 0) return@runSingleFlight
            val settings = settingsRepository.settings.first()
            val credentials = settings.credentialsOrNull() ?: return@runSingleFlight
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
    }

    suspend fun search(query: String, includeAdult: Boolean): SearchResults = withContext(Dispatchers.IO) {
        val normalized = query.trim()
        if (normalized.isBlank()) return@withContext SearchResults()
        val ftsQuery = normalized.toFtsPrefixQuery() ?: return@withContext SearchResults()
        SearchResults(
            channels = dao.searchChannels(ftsQuery, includeAdult, 24).map {
                SearchResultItem(
                    targetType = TargetType.CHANNEL,
                    targetId = it.streamId.toString(),
                    title = it.name,
                    subtitle = "Live TV",
                    artworkUrl = it.logoUrl,
                )
            },
            movies = dao.searchMovies(ftsQuery, includeAdult, 24).map {
                SearchResultItem(
                    targetType = TargetType.MOVIE,
                    targetId = it.streamId.toString(),
                    title = it.name,
                    subtitle = "Movie",
                    artworkUrl = it.artworkUrl,
                )
            },
            series = dao.searchSeries(ftsQuery, includeAdult, 24).map {
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
        when (section) {
            LibrarySection.LIVE -> dao.setChannelCategoryHidden(remoteId, hidden)
            LibrarySection.MOVIES -> dao.setMovieCategoryHidden(remoteId, hidden)
            LibrarySection.SERIES -> dao.setSeriesCategoryHidden(remoteId, hidden)
            else -> Unit
        }
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
        val epgChannelId = channel.epgChannelId?.takeIf { it.isNotBlank() } ?: return@withContext
        val now = System.currentTimeMillis()
        if (hasFreshEmptyGuideCache(epgChannelId, now)) return@withContext
        if (dao.futureGuideCount(epgChannelId, now) > 0) {
            forgetEmptyGuideCache(epgChannelId)
            return@withContext
        }

        runSingleFlight(epgChannelId, inFlightGuideFetches) {
            val fetchStartedAt = System.currentTimeMillis()
            if (hasFreshEmptyGuideCache(epgChannelId, fetchStartedAt)) return@runSingleFlight
            if (dao.futureGuideCount(epgChannelId, fetchStartedAt) > 0) {
                forgetEmptyGuideCache(epgChannelId)
                return@runSingleFlight
            }
            val settings = settingsRepository.settings.first()
            val credentials = settings.credentialsOrNull() ?: return@runSingleFlight
            val guide = xtreamApi.fetchShortEpg(credentials, streamId, epgChannelId)
            if (guide.isEmpty()) {
                rememberEmptyGuideCache(epgChannelId, fetchStartedAt)
            } else {
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
                forgetEmptyGuideCache(epgChannelId)
            }
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
        syncToken: Long,
    ) {
        val hiddenIds = dao.hiddenCategoryIds(section.rawValue).toSet()
        dao.upsertCategories(
            categories.mapIndexed { index, payload ->
                CategoryEntity(
                    section = section.rawValue,
                    remoteId = payload.remoteId,
                    name = payload.name,
                    isAdult = payload.isAdult,
                    hidden = payload.remoteId in hiddenIds,
                    sortOrder = index,
                    syncToken = syncToken,
                )
            },
        )
        dao.deleteStaleCategories(section.rawValue, syncToken)
    }

    private suspend fun <K> runSingleFlight(
        key: K,
        inFlight: MutableMap<K, CompletableDeferred<Unit>>,
        block: suspend () -> Unit,
    ) {
        val (deferred, isLeader) = lazyLoadMutex.withLock {
            val existing = inFlight[key]
            if (existing != null) {
                existing to false
            } else {
                val created = CompletableDeferred<Unit>()
                inFlight[key] = created
                created to true
            }
        }

        if (!isLeader) {
            deferred.await()
            return
        }

        var failure: Throwable? = null
        try {
            block()
        } catch (throwable: Throwable) {
            failure = throwable
            throw throwable
        } finally {
            lazyLoadMutex.withLock {
                if (inFlight[key] === deferred) {
                    inFlight.remove(key)
                }
            }
            failure?.let(deferred::completeExceptionally) ?: deferred.complete(Unit)
        }
    }

    private suspend fun hasFreshEmptyGuideCache(channelEpgId: String, now: Long): Boolean {
        return lazyLoadMutex.withLock {
            val cachedAt = emptyGuideCache[channelEpgId] ?: return@withLock false
            if (now - cachedAt < EMPTY_GUIDE_CACHE_TTL_MS) {
                true
            } else {
                emptyGuideCache.remove(channelEpgId)
                false
            }
        }
    }

    private suspend fun rememberEmptyGuideCache(channelEpgId: String, now: Long) {
        lazyLoadMutex.withLock {
            emptyGuideCache[channelEpgId] = now
        }
    }

    private suspend fun forgetEmptyGuideCache(channelEpgId: String) {
        lazyLoadMutex.withLock {
            emptyGuideCache.remove(channelEpgId)
        }
    }

    private fun String.toFtsPrefixQuery(): String? {
        return split(SearchTokenSplitRegex)
            .mapNotNull { token ->
                token.filter { it.isLetterOrDigit() }
                    .takeIf { it.isNotBlank() }
            }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = " ") { "${it}*" }
            ?.takeIf { it.isNotBlank() }
    }
}
