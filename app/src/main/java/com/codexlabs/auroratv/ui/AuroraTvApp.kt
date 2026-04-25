package com.codexlabs.auroratv.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.codexlabs.auroratv.data.AppSettings
import com.codexlabs.auroratv.data.BufferProfile
import com.codexlabs.auroratv.data.CategoryEntity
import com.codexlabs.auroratv.data.ChannelEntity
import com.codexlabs.auroratv.data.EpgEventEntity
import com.codexlabs.auroratv.data.EpisodeEntity
import com.codexlabs.auroratv.data.LibrarySection
import com.codexlabs.auroratv.data.MovieEntity
import com.codexlabs.auroratv.data.PreferredPlayer
import com.codexlabs.auroratv.data.SearchResultItem
import com.codexlabs.auroratv.data.SeriesEntity
import com.codexlabs.auroratv.data.TargetType
import com.codexlabs.auroratv.player.PlayerActivity
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private val AppBackground = Color(0xFF050505)
private val Panel = Color(0xFF111111)
private val PanelRaised = Color(0xFF1A1A1A)
private val NetflixRed = Color(0xFFE50914)
private val GuideGreen = Color(0xFF2CE88D)
private val SoftLine = Color(0xFF2A2A2A)
private val MutedText = Color(0xFFB8B8B8)

@Composable
fun AuroraTvApp(
    viewModel: MainViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val stats by viewModel.libraryStats.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    var selectedSection by rememberSaveable { mutableStateOf(LibrarySection.HOME) }
    var movieDialog by remember { mutableStateOf<MovieEntity?>(null) }
    var seriesDialog by remember { mutableStateOf<SeriesEntity?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (settings.isConfigured) {
        LaunchedEffect(settings.isConfigured) {
            viewModel.syncAll(force = false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A0507),
                        AppBackground,
                        Color(0xFF090909),
                    ),
                ),
            )
            .padding(24.dp),
    ) {
        if (!settings.isConfigured) {
            SetupScreen(
                settings = settings,
                onSave = viewModel::saveProvider,
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Header(
                    appSettings = settings,
                    stats = stats,
                    syncMessage = syncMessage,
                    isSyncing = isSyncing,
                    onRefresh = { viewModel.syncAll(force = true) },
                )

                Spacer(modifier = Modifier.height(24.dp))

                SectionTabs(
                    selected = selectedSection,
                    onSelected = { selectedSection = it },
                )

                Spacer(modifier = Modifier.height(24.dp))

                when (selectedSection) {
                    LibrarySection.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onOpenMovie = { movieDialog = it },
                        onOpenSeries = { seriesDialog = it },
                        onPlay = { targetType, targetId, categoryId ->
                            launchPlayback(
                                context = context,
                                scope = scope,
                                viewModel = viewModel,
                                settings = settings,
                                targetType = targetType,
                                targetId = targetId,
                                categoryId = categoryId,
                            )
                        },
                    )

                    LibrarySection.LIVE -> LiveTvScreen(
                        settings = settings,
                        viewModel = viewModel,
                        onPlayChannel = { channel, categoryId ->
                            launchPlayback(
                                context = context,
                                scope = scope,
                                viewModel = viewModel,
                                settings = settings,
                                targetType = TargetType.CHANNEL,
                                targetId = channel.streamId.toString(),
                                categoryId = categoryId,
                            )
                        },
                    )

                    LibrarySection.MOVIES -> MovieScreen(
                        settings = settings,
                        viewModel = viewModel,
                        onOpenMovie = { movieDialog = it },
                    )

                    LibrarySection.SERIES -> SeriesScreen(
                        settings = settings,
                        viewModel = viewModel,
                        onOpenSeries = { seriesDialog = it },
                    )

                    LibrarySection.SEARCH -> SearchScreen(
                        settings = settings,
                        viewModel = viewModel,
                        onOpenMovie = { movieDialog = it },
                        onOpenSeries = { seriesDialog = it },
                        onPlay = { targetType, targetId ->
                            launchPlayback(
                                context = context,
                                scope = scope,
                                viewModel = viewModel,
                                settings = settings,
                                targetType = targetType,
                                targetId = targetId,
                                categoryId = null,
                            )
                        },
                    )

                    LibrarySection.SETTINGS -> SettingsScreen(
                        settings = settings,
                        isSyncing = isSyncing,
                        onSaveProvider = viewModel::saveProvider,
                        onRefresh = { viewModel.syncAll(force = true) },
                        onAutoSyncChanged = viewModel::setAutoSync,
                        onAdultContentChanged = viewModel::setAdultContent,
                        onPreferredPlayerChanged = viewModel::setPreferredPlayer,
                        onBufferProfileChanged = viewModel::setBufferProfile,
                        onEpgWindowChanged = viewModel::setEpgWindowHours,
                        onPinChanged = viewModel::setParentalPin,
                    )
                }
            }
        }
    }

    movieDialog?.let { movie ->
        MovieDialog(
            movie = movie,
            viewModel = viewModel,
            onDismiss = { movieDialog = null },
            onPlay = {
                launchPlayback(
                    context = context,
                    scope = scope,
                    viewModel = viewModel,
                    settings = settings,
                    targetType = TargetType.MOVIE,
                    targetId = movie.streamId.toString(),
                    categoryId = null,
                )
            },
        )
    }

    seriesDialog?.let { series ->
        SeriesDialog(
            series = series,
            viewModel = viewModel,
            settings = settings,
            onDismiss = { seriesDialog = null },
            onPlayEpisode = { episode ->
                launchPlayback(
                    context = context,
                    scope = scope,
                    viewModel = viewModel,
                    settings = settings,
                    targetType = TargetType.EPISODE,
                    targetId = episode.episodeId.toString(),
                    categoryId = null,
                )
            },
        )
    }
}

@Composable
private fun Header(
    appSettings: AppSettings,
    stats: com.codexlabs.auroratv.data.LibraryStats,
    syncMessage: String?,
    isSyncing: Boolean,
    onRefresh: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NetflixRed),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("A", fontWeight = FontWeight.Black, color = Color.White)
                }
                Column {
                    Text(
                        text = "AURORA TV",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Live channels, guide data, movies, and series from ${appSettings.providerBaseUrl.ifBlank { "your Xtream provider" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Button(
                onClick = onRefresh,
                enabled = !isSyncing,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(if (isSyncing) "Syncing..." else "Refresh")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("Channels", stats.liveChannels.toString(), Icons.Rounded.LiveTv)
                SummaryCard("Movies", stats.movies.toString(), Icons.Rounded.Movie)
                SummaryCard("Series", stats.series.toString(), Icons.Rounded.Tv)
                SummaryCard("Episodes", stats.episodes.toString(), Icons.Rounded.GridView)
                SummaryCard("Favorites", stats.favorites.toString(), Icons.Rounded.Favorite)
            }
            Text(
                text = "Last sync: ${formatLastSync(appSettings.lastSyncEpochMillis)}",
                color = MutedText,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        syncMessage?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                color = if (isSyncing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Surface(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(8.dp),
        color = PanelRaised.copy(alpha = 0.86f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF252525)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = NetflixRed, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText,
                )
            }
        }
    }
}

@Composable
private fun SectionTabs(
    selected: LibrarySection,
    onSelected: (LibrarySection) -> Unit,
) {
    val tabs = listOf(
        LibrarySection.HOME to "Home",
        LibrarySection.LIVE to "Live TV",
        LibrarySection.MOVIES to "Movies",
        LibrarySection.SERIES to "Series",
        LibrarySection.SEARCH to "Search",
        LibrarySection.SETTINGS to "Settings",
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(tabs) { (section, label) ->
            FocusCard(
                modifier = Modifier.width(150.dp),
                selected = selected == section,
                onClick = { onSelected(section) },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupScreen(
    settings: AppSettings,
    onSave: (String, String, String) -> Unit,
) {
    var baseUrl by rememberSaveable { mutableStateOf(settings.providerBaseUrl) }
    var username by rememberSaveable { mutableStateOf(settings.providerUsername) }
    var password by rememberSaveable { mutableStateOf(settings.providerPassword) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .clip(RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = Panel.copy(alpha = 0.96f),
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(34.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Connect Your Xtream Provider",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Aurora TV is tuned for one Xtream provider with room-friendly remote navigation, local library caching, and full live TV support.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MutedText,
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Provider URL") },
                    placeholder = { Text("http://provider.example.com:8080") },
                    singleLine = true,
                    colors = darkTextFieldColors(),
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username") },
                    singleLine = true,
                    colors = darkTextFieldColors(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    singleLine = true,
                    colors = darkTextFieldColors(),
                )
                Button(
                    onClick = { onSave(baseUrl, username, password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = baseUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Save Provider And Sync")
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    viewModel: MainViewModel,
    onOpenMovie: (MovieEntity) -> Unit,
    onOpenSeries: (SeriesEntity) -> Unit,
    onPlay: (TargetType, String, String?) -> Unit,
) {
    val continueWatching by viewModel.continueWatching.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentChannels by viewModel.recentChannels.collectAsStateWithLifecycle(initialValue = emptyList())
    val favoriteChannels by viewModel.favoriteChannels.collectAsStateWithLifecycle(initialValue = emptyList())
    val favoriteMovies by viewModel.favoriteMovies.collectAsStateWithLifecycle(initialValue = emptyList())
    val favoriteSeries by viewModel.favoriteSeries.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        HomeStrip(
            title = "Continue Watching",
            items = continueWatching.map {
                StripCard(
                    id = it.targetId,
                    title = it.title,
                    subtitle = it.subtitle ?: progressLabel(it.positionMs, it.durationMs),
                    artworkUrl = it.artworkUrl,
                    targetType = TargetType.from(it.targetType),
                    categoryId = null,
                )
            },
            onClick = { item ->
                when (item.targetType) {
                    TargetType.MOVIE,
                    TargetType.EPISODE,
                    TargetType.CHANNEL,
                    -> onPlay(item.targetType, item.id, item.categoryId)
                    TargetType.SERIES -> Unit
                }
            },
        )

        HomeStrip(
            title = "Recent Channels",
            items = recentChannels.map {
                StripCard(
                    id = it.channelId.toString(),
                    title = it.title,
                    subtitle = "Live TV",
                    artworkUrl = it.artworkUrl,
                    targetType = TargetType.CHANNEL,
                    categoryId = it.categoryId,
                )
            },
            onClick = { item -> onPlay(item.targetType, item.id, item.categoryId) },
        )

        HomeStrip(
            title = "Favorite Channels",
            items = favoriteChannels.map {
                StripCard(
                    id = it.targetId,
                    title = it.title,
                    subtitle = it.subtitle,
                    artworkUrl = it.artworkUrl,
                    targetType = TargetType.from(it.targetType),
                    categoryId = null,
                )
            },
            onClick = { item -> onPlay(item.targetType, item.id, item.categoryId) },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                HomeStrip(
                    title = "Favorite Movies",
                    items = favoriteMovies.map {
                        StripCard(
                            id = it.targetId,
                            title = it.title,
                            subtitle = it.subtitle,
                            artworkUrl = it.artworkUrl,
                            targetType = TargetType.from(it.targetType),
                            categoryId = null,
                        )
                    },
                    onClick = { item ->
                        onOpenMovie(
                            MovieEntity(
                                streamId = item.id.toLong(),
                                categoryRemoteId = item.categoryId.orEmpty(),
                                name = item.title,
                                artworkUrl = item.artworkUrl,
                                plot = null,
                                rating = null,
                                releaseYear = item.subtitle,
                                containerExtension = null,
                                directSource = null,
                                isAdult = false,
                                addedAt = null,
                            ),
                        )
                    },
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                HomeStrip(
                    title = "Favorite Series",
                    items = favoriteSeries.map {
                        StripCard(
                            id = it.targetId,
                            title = it.title,
                            subtitle = it.subtitle,
                            artworkUrl = it.artworkUrl,
                            targetType = TargetType.from(it.targetType),
                            categoryId = null,
                        )
                    },
                    onClick = { item ->
                        onOpenSeries(
                            SeriesEntity(
                                seriesId = item.id.toLong(),
                                categoryRemoteId = item.categoryId.orEmpty(),
                                name = item.title,
                                artworkUrl = item.artworkUrl,
                                plot = null,
                                rating = null,
                                releaseYear = item.subtitle,
                                isAdult = false,
                                addedAt = null,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun LiveTvScreen(
    settings: AppSettings,
    viewModel: MainViewModel,
    onPlayChannel: (ChannelEntity, String?) -> Unit,
) {
    val categories by viewModel.observeCategories(LibrarySection.LIVE, settings.adultContentEnabled)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(categories) {
        if (selectedCategoryId == null && categories.isNotEmpty()) {
            selectedCategoryId = categories.first().remoteId
        }
        if (categories.none { it.remoteId == selectedCategoryId }) {
            selectedCategoryId = categories.firstOrNull()?.remoteId
        }
    }

    val channels by viewModel.observeChannels(selectedCategoryId, settings.adultContentEnabled)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var focusedChannelId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(channels) {
        if (channels.none { it.streamId == focusedChannelId }) {
            focusedChannelId = channels.firstOrNull()?.streamId
        }
    }

    val highlightedChannel = remember(channels, focusedChannelId) {
        channels.firstOrNull { it.streamId == focusedChannelId } ?: channels.firstOrNull()
    }

    LaunchedEffect(highlightedChannel?.streamId) {
        highlightedChannel?.streamId?.let(viewModel::ensureGuide)
    }
    val guideFlow: Flow<List<EpgEventEntity>> = remember(highlightedChannel?.streamId) {
        highlightedChannel?.streamId?.let(viewModel::observeGuide) ?: flowOf(emptyList())
    }
    val guide by guideFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        CategoryRail(
            modifier = Modifier.width(260.dp),
            title = "Groups",
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategoryFocused = { selectedCategoryId = it.remoteId },
            onCategoryClicked = { selectedCategoryId = it.remoteId },
            onHideCategory = { category ->
                viewModel.setCategoryHidden(LibrarySection.LIVE, category.remoteId, true)
            },
        )

        Surface(
            modifier = Modifier.width(520.dp).fillMaxHeight(),
            shape = RoundedCornerShape(8.dp),
            color = Panel.copy(alpha = 0.92f),
        ) {
            if (channels.isEmpty()) {
                EmptyState("No channels in this group")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(channels, key = { it.streamId }) { channel ->
                        FocusCard(
                            selected = channel.streamId == highlightedChannel?.streamId,
                            onClick = { onPlayChannel(channel, selectedCategoryId) },
                            onFocused = { focusedChannelId = channel.streamId },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Artwork(
                                    url = channel.logoUrl,
                                    modifier = Modifier.size(56.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentScale = ContentScale.Fit,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = channel.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = buildString {
                                            append("CH ")
                                            append(channel.channelNumber ?: "--")
                                            if (channel.hasCatchup) append("  ·  Catch-up")
                                        },
                                        color = MutedText,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        LiveDetailPanel(
            modifier = Modifier.weight(1f),
            channel = highlightedChannel,
            guide = guide,
            onPlay = {
                highlightedChannel?.let { onPlayChannel(it, selectedCategoryId) }
            },
            onToggleFavorite = {
                highlightedChannel?.let {
                    viewModel.toggleFavorite(
                        targetType = TargetType.CHANNEL,
                        targetId = it.streamId.toString(),
                        title = it.name,
                        subtitle = "Live TV",
                        artworkUrl = it.logoUrl,
                    )
                }
            },
        )
    }
}

@Composable
private fun MovieScreen(
    settings: AppSettings,
    viewModel: MainViewModel,
    onOpenMovie: (MovieEntity) -> Unit,
) {
    val categories by viewModel.observeCategories(LibrarySection.MOVIES, settings.adultContentEnabled)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(categories) {
        if (selectedCategoryId == null && categories.isNotEmpty()) {
            selectedCategoryId = categories.first().remoteId
        }
        if (categories.none { it.remoteId == selectedCategoryId }) {
            selectedCategoryId = categories.firstOrNull()?.remoteId
        }
    }

    val movies by viewModel.observeMovies(selectedCategoryId, settings.adultContentEnabled)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var focusedMovieId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(movies) {
        if (movies.none { it.streamId == focusedMovieId }) {
            focusedMovieId = movies.firstOrNull()?.streamId
        }
    }
    val highlightedMovie = remember(movies, focusedMovieId) {
        movies.firstOrNull { it.streamId == focusedMovieId } ?: movies.firstOrNull()
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        CategoryRail(
            modifier = Modifier.width(240.dp),
            title = "Groups",
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategoryFocused = { selectedCategoryId = it.remoteId },
            onCategoryClicked = { selectedCategoryId = it.remoteId },
            onHideCategory = { viewModel.setCategoryHidden(LibrarySection.MOVIES, it.remoteId, true) },
        )

        Surface(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(8.dp),
            color = Panel.copy(alpha = 0.92f),
        ) {
            if (movies.isEmpty()) {
                EmptyState("No movies available in this group")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(170.dp),
                    contentPadding = PaddingValues(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(movies, key = { it.streamId }) { movie ->
                        FocusCard(
                            modifier = Modifier.fillMaxWidth(),
                            selected = movie.streamId == focusedMovieId,
                            onClick = { onOpenMovie(movie) },
                            onFocused = { focusedMovieId = movie.streamId },
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Artwork(
                                    url = movie.artworkUrl,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.72f),
                                    shape = RoundedCornerShape(8.dp),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = movie.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        DetailPanel(
            modifier = Modifier.width(400.dp).fillMaxHeight(),
            artworkUrl = highlightedMovie?.artworkUrl,
            title = highlightedMovie?.name ?: "Select a movie",
            subtitle = highlightedMovie?.releaseYear ?: highlightedMovie?.rating,
            body = highlightedMovie?.plot ?: "Choose a title to inspect its details and play it.",
            primaryLabel = "Open Details",
            onPrimary = { highlightedMovie?.let(onOpenMovie) },
            secondaryLabel = "Favorite",
            onSecondary = {
                highlightedMovie?.let { movie ->
                    viewModel.toggleFavorite(
                        targetType = TargetType.MOVIE,
                        targetId = movie.streamId.toString(),
                        title = movie.name,
                        subtitle = movie.releaseYear ?: movie.rating,
                        artworkUrl = movie.artworkUrl,
                    )
                }
            },
        )
    }
}

@Composable
private fun SeriesScreen(
    settings: AppSettings,
    viewModel: MainViewModel,
    onOpenSeries: (SeriesEntity) -> Unit,
) {
    val categories by viewModel.observeCategories(LibrarySection.SERIES, settings.adultContentEnabled)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(categories) {
        if (selectedCategoryId == null && categories.isNotEmpty()) {
            selectedCategoryId = categories.first().remoteId
        }
        if (categories.none { it.remoteId == selectedCategoryId }) {
            selectedCategoryId = categories.firstOrNull()?.remoteId
        }
    }

    val seriesItems by viewModel.observeSeries(selectedCategoryId, settings.adultContentEnabled)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var focusedSeriesId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(seriesItems) {
        if (seriesItems.none { it.seriesId == focusedSeriesId }) {
            focusedSeriesId = seriesItems.firstOrNull()?.seriesId
        }
    }
    val highlightedSeries = remember(seriesItems, focusedSeriesId) {
        seriesItems.firstOrNull { it.seriesId == focusedSeriesId } ?: seriesItems.firstOrNull()
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        CategoryRail(
            modifier = Modifier.width(240.dp),
            title = "Groups",
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategoryFocused = { selectedCategoryId = it.remoteId },
            onCategoryClicked = { selectedCategoryId = it.remoteId },
            onHideCategory = { viewModel.setCategoryHidden(LibrarySection.SERIES, it.remoteId, true) },
        )

        Surface(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(8.dp),
            color = Panel.copy(alpha = 0.92f),
        ) {
            if (seriesItems.isEmpty()) {
                EmptyState("No series available in this group")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(170.dp),
                    contentPadding = PaddingValues(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(seriesItems, key = { it.seriesId }) { series ->
                        FocusCard(
                            modifier = Modifier.fillMaxWidth(),
                            selected = series.seriesId == focusedSeriesId,
                            onClick = { onOpenSeries(series) },
                            onFocused = { focusedSeriesId = series.seriesId },
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Artwork(
                                    url = series.artworkUrl,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.72f),
                                    shape = RoundedCornerShape(8.dp),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = series.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        DetailPanel(
            modifier = Modifier.width(400.dp).fillMaxHeight(),
            artworkUrl = highlightedSeries?.artworkUrl,
            title = highlightedSeries?.name ?: "Select a series",
            subtitle = highlightedSeries?.releaseYear ?: highlightedSeries?.rating,
            body = highlightedSeries?.plot ?: "Open a series to load seasons and episodes from the provider.",
            primaryLabel = "Open Details",
            onPrimary = { highlightedSeries?.let(onOpenSeries) },
            secondaryLabel = "Favorite",
            onSecondary = {
                highlightedSeries?.let { series ->
                    viewModel.toggleFavorite(
                        targetType = TargetType.SERIES,
                        targetId = series.seriesId.toString(),
                        title = series.name,
                        subtitle = series.releaseYear ?: series.rating,
                        artworkUrl = series.artworkUrl,
                    )
                }
            },
        )
    }
}

@Composable
private fun SearchScreen(
    settings: AppSettings,
    viewModel: MainViewModel,
    onOpenMovie: (MovieEntity) -> Unit,
    onOpenSeries: (SeriesEntity) -> Unit,
    onPlay: (TargetType, String) -> Unit,
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::updateSearchQuery,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search live TV, movies, and series") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            colors = darkTextFieldColors(),
        )

        if (query.isBlank()) {
            EmptyState("Start typing to search your synced provider catalog")
        } else {
            SearchResultSection("Channels", results.channels) { item ->
                onPlay(item.targetType, item.targetId)
            }
            SearchResultSection("Movies", results.movies) { item ->
                onOpenMovie(
                    MovieEntity(
                        streamId = item.targetId.toLong(),
                        categoryRemoteId = "",
                        name = item.title,
                        artworkUrl = item.artworkUrl,
                        plot = null,
                        rating = null,
                        releaseYear = item.subtitle,
                        containerExtension = null,
                        directSource = null,
                        isAdult = settings.adultContentEnabled,
                        addedAt = null,
                    ),
                )
            }
            SearchResultSection("Series", results.series) { item ->
                onOpenSeries(
                    SeriesEntity(
                        seriesId = item.targetId.toLong(),
                        categoryRemoteId = "",
                        name = item.title,
                        artworkUrl = item.artworkUrl,
                        plot = null,
                        rating = null,
                        releaseYear = item.subtitle,
                        isAdult = settings.adultContentEnabled,
                        addedAt = null,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SearchResultSection(
    title: String,
    items: List<SearchResultItem>,
    onClick: (SearchResultItem) -> Unit,
) {
    if (items.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Panel.copy(alpha = 0.92f),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(items, key = { "${it.targetType.rawValue}:${it.targetId}" }) { item ->
                    FocusCard(
                        modifier = Modifier.width(230.dp),
                        onClick = { onClick(item) },
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Artwork(
                                url = item.artworkUrl,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.9f),
                                shape = RoundedCornerShape(8.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            item.subtitle?.let {
                                Text(
                                    text = it,
                                    color = MutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: AppSettings,
    isSyncing: Boolean,
    onSaveProvider: (String, String, String) -> Unit,
    onRefresh: () -> Unit,
    onAutoSyncChanged: (Boolean) -> Unit,
    onAdultContentChanged: (Boolean) -> Unit,
    onPreferredPlayerChanged: (PreferredPlayer) -> Unit,
    onBufferProfileChanged: (BufferProfile) -> Unit,
    onEpgWindowChanged: (Int) -> Unit,
    onPinChanged: (String) -> Unit,
) {
    var baseUrl by rememberSaveable { mutableStateOf(settings.providerBaseUrl) }
    var username by rememberSaveable { mutableStateOf(settings.providerUsername) }
    var password by rememberSaveable { mutableStateOf(settings.providerPassword) }
    var epgHours by rememberSaveable { mutableStateOf(settings.epgWindowHours.toString()) }
    var pin by rememberSaveable { mutableStateOf(settings.parentalPin) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(8.dp),
        color = Panel.copy(alpha = 0.92f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = "Playback And Provider Settings",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Provider URL") },
                singleLine = true,
                colors = darkTextFieldColors(),
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Username") },
                singleLine = true,
                colors = darkTextFieldColors(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                singleLine = true,
                colors = darkTextFieldColors(),
            )

            Button(
                onClick = { onSaveProvider(baseUrl, username, password) },
                enabled = !isSyncing && baseUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
            ) {
                Text("Save Provider")
            }

            SettingToggleRow(
                label = "Auto Sync Library",
                description = "Refresh the local database every 12 hours in the background.",
                checked = settings.autoSyncEnabled,
                onCheckedChange = onAutoSyncChanged,
            )

            SettingToggleRow(
                label = "Show Adult Content",
                description = "Allows groups and titles marked as adult to be displayed.",
                checked = settings.adultContentEnabled,
                onCheckedChange = onAdultContentChanged,
            )

            OutlinedTextField(
                value = epgHours,
                onValueChange = {
                    epgHours = it.filter(Char::isDigit).take(2)
                    it.toIntOrNull()?.let(onEpgWindowChanged)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("EPG Window Hours") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = darkTextFieldColors(),
            )

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    pin = it.filter(Char::isDigit).take(4)
                    onPinChanged(pin)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Parental PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                colors = darkTextFieldColors(),
            )

            Text("Preferred Player", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlayerPreferenceButton(
                    selected = settings.preferredPlayer == PreferredPlayer.INTERNAL,
                    label = "Internal",
                    onClick = { onPreferredPlayerChanged(PreferredPlayer.INTERNAL) },
                )
                PlayerPreferenceButton(
                    selected = settings.preferredPlayer == PreferredPlayer.EXTERNAL,
                    label = "External",
                    onClick = { onPreferredPlayerChanged(PreferredPlayer.EXTERNAL) },
                )
            }

            Text("Buffer Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlayerPreferenceButton(
                    selected = settings.bufferProfile == BufferProfile.LOW_LATENCY,
                    label = "Low Latency",
                    onClick = { onBufferProfileChanged(BufferProfile.LOW_LATENCY) },
                )
                PlayerPreferenceButton(
                    selected = settings.bufferProfile == BufferProfile.BALANCED,
                    label = "Balanced",
                    onClick = { onBufferProfileChanged(BufferProfile.BALANCED) },
                )
                PlayerPreferenceButton(
                    selected = settings.bufferProfile == BufferProfile.STABLE,
                    label = "Stable",
                    onClick = { onBufferProfileChanged(BufferProfile.STABLE) },
                )
            }

            Button(
                onClick = onRefresh,
                enabled = !isSyncing,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(if (isSyncing) "Syncing..." else "Refresh Library Now")
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = PanelRaised.copy(alpha = 0.86f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, color = MutedText)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun PlayerPreferenceButton(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    FocusCard(
        modifier = Modifier.width(190.dp),
        selected = selected,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CategoryRail(
    modifier: Modifier,
    title: String,
    categories: List<CategoryEntity>,
    selectedCategoryId: String?,
    onCategoryFocused: (CategoryEntity) -> Unit,
    onCategoryClicked: (CategoryEntity) -> Unit,
    onHideCategory: (CategoryEntity) -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(8.dp),
        color = Panel.copy(alpha = 0.92f),
    ) {
        if (categories.isEmpty()) {
            EmptyState("No visible groups")
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(20.dp),
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(categories, key = { "${it.section}:${it.remoteId}" }) { category ->
                        FocusCard(
                            selected = selectedCategoryId == category.remoteId,
                            onClick = { onCategoryClicked(category) },
                            onFocused = { onCategoryFocused(category) },
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = category.name,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = if (category.isAdult) "Adult group" else "Browsable group",
                                    color = MutedText,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
                categories.firstOrNull { it.remoteId == selectedCategoryId }?.let { selected ->
                    Button(
                        onClick = { onHideCategory(selected) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                    ) {
                        Text("Hide Current Group")
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveDetailPanel(
    modifier: Modifier,
    channel: ChannelEntity?,
    guide: List<EpgEventEntity>,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val now = System.currentTimeMillis()
    val current = guide.firstOrNull { now in it.startEpochMillis until it.endEpochMillis } ?: guide.firstOrNull()
    val next = guide.firstOrNull { it.startEpochMillis > (current?.startEpochMillis ?: 0L) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Panel.copy(alpha = 0.92f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Artwork(
                    url = channel?.logoUrl,
                    modifier = Modifier.size(118.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentScale = ContentScale.Fit,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = channel?.name ?: "Select a channel",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildString {
                            channel?.channelNumber?.let { append("Channel $it") }
                            if (channel?.hasCatchup == true) append("  ·  Catch-up ready")
                        }.ifBlank { "Live guide preview" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MutedText,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onPlay,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Play Live")
                        }
                        Button(
                            onClick = onToggleFavorite,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                        ) {
                            Icon(Icons.Rounded.Favorite, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Favorite")
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF151515),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Now Playing", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = current?.title ?: "Guide data will appear here after sync or short-EPG fallback.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    current?.let {
                        Text(formatEpgTime(it), color = GuideGreen, style = MaterialTheme.typography.bodyMedium)
                        it.description?.takeIf(String::isNotBlank)?.let { description ->
                            Text(
                                description,
                                color = MutedText,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    next?.let {
                        Text("Next: ${formatEpgTime(it)}  ${it.title}", color = MutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Text("Program Guide", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF101010),
            ) {
                if (guide.isEmpty()) {
                    EmptyState("No EPG rows synced for this channel yet")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(guide.take(24), key = { "${it.startEpochMillis}:${it.title}" }) { event ->
                            val isCurrent = now in event.startEpochMillis until event.endEpochMillis
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) NetflixRed.copy(alpha = 0.22f) else PanelRaised,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Text(
                                        text = formatEpgTime(event),
                                        modifier = Modifier.width(130.dp),
                                        color = if (isCurrent) GuideGreen else MutedText,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(event.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        event.description?.takeIf(String::isNotBlank)?.let {
                                            Text(it, color = MutedText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailPanel(
    modifier: Modifier,
    artworkUrl: String?,
    title: String,
    subtitle: String?,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Panel.copy(alpha = 0.92f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Artwork(
                url = artworkUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.45f),
                shape = RoundedCornerShape(8.dp),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.takeIf(String::isNotBlank)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MutedText,
                )
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MutedText,
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onPrimary,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
            ) {
                Text(primaryLabel)
            }
            Button(
                onClick = onSecondary,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
            ) {
                Text(secondaryLabel)
            }
        }
    }
}

@Composable
private fun MovieDialog(
    movie: MovieEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .fillMaxHeight(0.84f),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(26.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Artwork(
                    url = movie.artworkUrl,
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(8.dp),
                    contentScale = ContentScale.Crop,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(movie.name, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text(
                        movie.releaseYear ?: movie.rating ?: "Movie",
                        style = MaterialTheme.typography.titleLarge,
                        color = MutedText,
                    )
                    Text(movie.plot ?: "No plot available.", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = onPlay, modifier = Modifier.fillMaxWidth()) {
                        Text("Play Movie")
                    }
                    Button(
                        onClick = {
                            viewModel.toggleFavorite(
                                targetType = TargetType.MOVIE,
                                targetId = movie.streamId.toString(),
                                title = movie.name,
                                subtitle = movie.releaseYear ?: movie.rating,
                                artworkUrl = movie.artworkUrl,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Favorite")
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesDialog(
    series: SeriesEntity,
    viewModel: MainViewModel,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onPlayEpisode: (EpisodeEntity) -> Unit,
) {
    LaunchedEffect(series.seriesId) {
        viewModel.ensureSeriesLoaded(series.seriesId)
    }
    val episodes by viewModel.observeEpisodes(series.seriesId).collectAsStateWithLifecycle(initialValue = emptyList())
    val seasons = remember(episodes) { episodes.groupBy { it.seasonNumber }.toSortedMap() }
    var selectedSeason by rememberSaveable { mutableStateOf(seasons.keys.firstOrNull() ?: 1) }
    LaunchedEffect(seasons.keys.toList()) {
        if (selectedSeason !in seasons.keys) {
            selectedSeason = seasons.keys.firstOrNull() ?: 1
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(26.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Artwork(
                    url = series.artworkUrl,
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(8.dp),
                )

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(series.name, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text(
                        series.releaseYear ?: series.rating ?: "Series",
                        style = MaterialTheme.typography.titleLarge,
                        color = MutedText,
                    )
                    Text(series.plot ?: "Episode details are loaded on demand from the provider.", style = MaterialTheme.typography.bodyLarge)

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(seasons.keys.toList()) { season ->
                            FocusCard(
                                modifier = Modifier.width(140.dp),
                                selected = season == selectedSeason,
                                onClick = { selectedSeason = season },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("Season $season", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF151515),
                    ) {
                        if (episodes.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(seasons[selectedSeason].orEmpty(), key = { it.episodeId }) { episode ->
                                    FocusCard(
                                        onClick = { onPlayEpisode(episode) },
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Artwork(
                                                url = episode.artworkUrl ?: series.artworkUrl,
                                                modifier = Modifier.size(72.dp),
                                                shape = RoundedCornerShape(8.dp),
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "E${episode.episodeNumber}  ${episode.title}",
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                episode.plot?.takeIf(String::isNotBlank)?.let {
                                                    Text(
                                                        text = it,
                                                        color = MutedText,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.toggleFavorite(
                                targetType = TargetType.SERIES,
                                targetId = series.seriesId.toString(),
                                title = series.name,
                                subtitle = series.releaseYear ?: series.rating,
                                artworkUrl = series.artworkUrl,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Favorite Series")
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeStrip(
    title: String,
    items: List<StripCard>,
    onClick: (StripCard) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Panel.copy(alpha = 0.92f),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (items.isEmpty()) {
                EmptyState("Nothing here yet")
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(items, key = { "${it.targetType.rawValue}:${it.id}" }) { item ->
                        FocusCard(
                            modifier = Modifier.width(220.dp),
                            onClick = { onClick(item) },
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Artwork(
                                    url = item.artworkUrl,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f),
                                    shape = RoundedCornerShape(8.dp),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                item.subtitle?.let {
                                    Text(
                                        text = it,
                                        color = MutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
    onFocused: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused || selected) 1.03f else 1f,
        label = "card-scale",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            focused -> Color.White
            selected -> MaterialTheme.colorScheme.secondary
            else -> Color.Transparent
        },
        label = "card-border",
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .focusable()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (focused || selected) Color(0xFF2D2D2D) else PanelRaised.copy(alpha = 0.76f),
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor, RoundedCornerShape(8.dp)),
        ) {
            content()
        }
    }
}

@Composable
private fun Artwork(
    url: String?,
    modifier: Modifier,
    shape: RoundedCornerShape,
    contentScale: ContentScale = ContentScale.Crop,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2A2A2A),
                        Color(0xFF090909),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = NetflixRed,
                modifier = Modifier.size(42.dp),
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            color = MutedText,
        )
    }
}

@Composable
private fun darkTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedBorderColor = NetflixRed,
    unfocusedBorderColor = SoftLine,
    focusedLabelColor = MaterialTheme.colorScheme.onSurface,
    unfocusedLabelColor = MutedText,
    cursorColor = NetflixRed,
    focusedLeadingIconColor = NetflixRed,
    unfocusedLeadingIconColor = MutedText,
)

private fun formatLastSync(epochMillis: Long?): String {
    if (epochMillis == null) return "Not synced yet"
    return DateFormat.getDateTimeInstance().format(Date(epochMillis))
}

private fun formatEpgTime(event: EpgEventEntity): String {
    val formatter = SimpleDateFormat("EEE HH:mm", Locale.getDefault())
    return "${formatter.format(Date(event.startEpochMillis))} - ${formatter.format(Date(event.endEpochMillis))}"
}

private fun progressLabel(positionMs: Long, durationMs: Long): String {
    if (durationMs <= 0L) return "Resume"
    val percent = (positionMs.toFloat() / durationMs.toFloat() * 100f).toInt().coerceIn(0, 100)
    return "$percent% watched"
}

private fun launchPlayback(
    context: Context,
    scope: CoroutineScope,
    viewModel: MainViewModel,
    settings: AppSettings,
    targetType: TargetType,
    targetId: String,
    categoryId: String?,
) {
    scope.launch {
        val descriptor = viewModel.resolvePlayback(targetType, targetId, categoryId)

        if (descriptor.isLive) {
            viewModel.registerRecentChannel(
                channelId = descriptor.targetId.toLong(),
                title = descriptor.title,
                artworkUrl = descriptor.artworkUrl,
                categoryId = descriptor.categoryId,
            )
        }

        val launchedExternal = if (settings.preferredPlayer == PreferredPlayer.EXTERNAL) {
            val externalIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(descriptor.mediaUrl), "video/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (externalIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(externalIntent)
                true
            } else {
                false
            }
        } else {
            false
        }

        if (!launchedExternal) {
            context.startActivity(
                PlayerActivity.createIntent(
                    context = context,
                    targetType = descriptor.targetType,
                    targetId = descriptor.targetId,
                    categoryId = descriptor.categoryId,
                ),
            )
        }
    }
}

private data class StripCard(
    val id: String,
    val title: String,
    val subtitle: String?,
    val artworkUrl: String?,
    val targetType: TargetType,
    val categoryId: String?,
)
