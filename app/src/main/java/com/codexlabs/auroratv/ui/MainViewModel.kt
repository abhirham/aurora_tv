package com.codexlabs.auroratv.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codexlabs.auroratv.app.AuroraTvApplication
import com.codexlabs.auroratv.data.AppSettings
import com.codexlabs.auroratv.data.BufferProfile
import com.codexlabs.auroratv.data.IptvRepository
import com.codexlabs.auroratv.data.LibrarySection
import com.codexlabs.auroratv.data.LibraryStats
import com.codexlabs.auroratv.data.PlaybackDescriptor
import com.codexlabs.auroratv.data.PreferredPlayer
import com.codexlabs.auroratv.data.SearchResults
import com.codexlabs.auroratv.data.TargetType
import com.codexlabs.auroratv.settings.SettingsRepository
import com.codexlabs.auroratv.sync.SyncWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val app = application as AuroraTvApplication
    private val repository: IptvRepository = app.container.repository
    private val settingsRepository: SettingsRepository = app.container.settingsRepository

    val settings: StateFlow<AppSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    val libraryStats: StateFlow<LibraryStats> = repository.libraryStats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryStats(),
    )

    val continueWatching = repository.observeContinueWatching()
    val recentChannels = repository.observeRecentChannels()
    val favoriteChannels = repository.observeFavoriteItems(TargetType.CHANNEL)
    val favoriteMovies = repository.observeFavoriteItems(TargetType.MOVIE)
    val favoriteSeries = repository.observeFavoriteItems(TargetType.SERIES)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow(SearchResults())
    val searchResults: StateFlow<SearchResults> = _searchResults.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            settings.collect { appSettings ->
                if (appSettings.isConfigured && appSettings.autoSyncEnabled) {
                    SyncWorker.schedulePeriodic(getApplication())
                    maybeRefreshOnLaunch(appSettings)
                }
            }
        }
    }

    fun observeCategories(section: LibrarySection, includeAdult: Boolean) =
        repository.observeCategories(section, includeAdult)

    fun observeChannels(categoryId: String?, includeAdult: Boolean) =
        repository.observeChannels(categoryId, includeAdult)

    fun observeMovies(categoryId: String?, includeAdult: Boolean) =
        repository.observeMovies(categoryId, includeAdult)

    fun observeSeries(categoryId: String?, includeAdult: Boolean) =
        repository.observeSeries(categoryId, includeAdult)

    fun observeEpisodes(seriesId: Long) =
        repository.observeEpisodes(seriesId)

    fun observeGuide(streamId: Long) =
        repository.observeGuide(streamId)

    fun observeFavoriteIds(targetType: TargetType) =
        repository.observeFavoriteIds(targetType)

    fun syncAll(force: Boolean = true) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            val appSettings = settings.value
            if (!appSettings.isConfigured) return@launch
            if (!force && !isRefreshDue(appSettings)) return@launch

            _isSyncing.value = true
            runCatching {
                repository.syncAll { status ->
                    _syncMessage.value = status
                }
            }.onSuccess {
                _syncMessage.value = "Library sync completed"
            }.onFailure { throwable ->
                _syncMessage.value = throwable.message ?: "Library sync failed"
            }
            _isSyncing.value = false
        }
    }

    fun saveProvider(
        baseUrl: String,
        username: String,
        password: String,
    ) {
        viewModelScope.launch {
            settingsRepository.saveProviderCredentials(baseUrl, username, password)
            SyncWorker.schedulePeriodic(getApplication())
            delay(250)
            syncAll(force = true)
        }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoSyncEnabled(enabled)
            if (enabled) {
                SyncWorker.schedulePeriodic(getApplication())
            }
        }
    }

    fun setAdultContent(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAdultContentEnabled(enabled)
        }
    }

    fun setPreferredPlayer(preferredPlayer: PreferredPlayer) {
        viewModelScope.launch {
            settingsRepository.setPreferredPlayer(preferredPlayer)
        }
    }

    fun setBufferProfile(bufferProfile: BufferProfile) {
        viewModelScope.launch {
            settingsRepository.setBufferProfile(bufferProfile)
        }
    }

    fun setEpgWindowHours(hours: Int) {
        viewModelScope.launch {
            settingsRepository.setEpgWindowHours(hours)
        }
    }

    fun setParentalPin(pin: String) {
        viewModelScope.launch {
            settingsRepository.setParentalPin(pin)
        }
    }

    fun toggleFavorite(
        targetType: TargetType,
        targetId: String,
        title: String,
        subtitle: String?,
        artworkUrl: String?,
    ) {
        viewModelScope.launch {
            repository.toggleFavorite(targetType, targetId, title, subtitle, artworkUrl)
        }
    }

    fun setCategoryHidden(section: LibrarySection, remoteId: String, hidden: Boolean) {
        viewModelScope.launch {
            repository.setCategoryHidden(section, remoteId, hidden)
        }
    }

    fun ensureSeriesLoaded(seriesId: Long) {
        viewModelScope.launch {
            repository.ensureSeriesLoaded(seriesId)
        }
    }

    fun ensureGuide(streamId: Long) {
        viewModelScope.launch {
            repository.ensureGuide(streamId)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250)
            _searchResults.value = repository.search(query, settings.value.adultContentEnabled)
        }
    }

    suspend fun resolvePlayback(
        targetType: TargetType,
        targetId: String,
        categoryIdHint: String? = null,
    ): PlaybackDescriptor {
        return repository.resolvePlaybackDescriptor(targetType, targetId, categoryIdHint)
    }

    suspend fun resolveAdjacentChannel(
        currentChannelId: Long,
        categoryId: String,
        direction: Int,
    ): PlaybackDescriptor? {
        return repository.resolveAdjacentChannel(
            currentChannelId = currentChannelId,
            categoryId = categoryId,
            offset = direction,
            includeAdult = settings.value.adultContentEnabled,
        )
    }

    fun registerRecentChannel(
        channelId: Long,
        title: String,
        artworkUrl: String?,
        categoryId: String?,
    ) {
        viewModelScope.launch {
            repository.registerRecentChannel(channelId, title, artworkUrl, categoryId)
        }
    }

    fun updatePlaybackHistory(
        descriptor: PlaybackDescriptor,
        positionMs: Long,
        durationMs: Long,
    ) {
        viewModelScope.launch {
            repository.updatePlaybackHistory(descriptor, positionMs, durationMs)
        }
    }

    private fun maybeRefreshOnLaunch(settings: AppSettings) {
        if (_isSyncing.value) return
        if (isRefreshDue(settings)) {
            syncAll(force = false)
        }
    }

    private fun isRefreshDue(settings: AppSettings): Boolean {
        val lastSync = settings.lastSyncEpochMillis ?: return true
        return System.currentTimeMillis() - lastSync > 6 * 60 * 60 * 1000L
    }
}
