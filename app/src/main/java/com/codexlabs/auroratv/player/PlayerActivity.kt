package com.codexlabs.auroratv.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codexlabs.auroratv.app.configureTvWindow
import com.codexlabs.auroratv.data.AppSettings
import com.codexlabs.auroratv.data.BufferProfile
import com.codexlabs.auroratv.data.EpgEventEntity
import com.codexlabs.auroratv.data.PlaybackDescriptor
import com.codexlabs.auroratv.data.TargetType
import com.codexlabs.auroratv.ui.MainViewModel
import com.codexlabs.auroratv.ui.theme.AuroraTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

private val TvSelectKeys = setOf(
    Key.DirectionCenter,
    Key.Enter,
    Key.NumPadEnter,
)

private const val SEEK_STEP_MS = 10_000L
private const val SEEK_REPEAT_INITIAL_DELAY_MS = 500L
private const val SEEK_REPEAT_INTERVAL_MS = 100L
private const val PLAYBACK_PROGRESS_UPDATE_MS = 100L

private val KnownTrackLanguageNames: List<String> by lazy {
    Locale.getAvailableLocales()
        .flatMap { locale ->
            listOf(
                locale.getDisplayLanguage(Locale.ENGLISH),
                locale.getDisplayLanguage(Locale.getDefault()),
            )
        }
        .map { it.trim() }
        .filter { languageName ->
            languageName.length > 2 && languageName.any { it.isLetter() }
        }
        .distinctBy { it.lowercase(Locale.US) }
        .sortedByDescending { it.length }
}

private data class SubtitleTrackOption(
    val label: String,
    val mediaTrackGroup: TrackGroup,
    val trackIndex: Int,
    val selected: Boolean,
)

private data class AudioTrackOption(
    val label: String,
    val shortLabel: String,
    val mediaTrackGroup: TrackGroup,
    val trackIndex: Int,
    val selected: Boolean,
    val isDefault: Boolean,
)

private data class AudioTrackLabels(
    val shortLabel: String,
    val detailLabel: String,
)

class PlayerActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_TARGET_TYPE = "extra_target_type"
        private const val EXTRA_TARGET_ID = "extra_target_id"
        private const val EXTRA_CATEGORY_ID = "extra_category_id"
        private const val EXTRA_RESUME_POSITION_MS = "extra_resume_position_ms"

        fun createIntent(
            context: Context,
            targetType: TargetType,
            targetId: String,
            categoryId: String?,
            resumePositionMs: Long? = null,
        ): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_TARGET_TYPE, targetType.rawValue)
                putExtra(EXTRA_TARGET_ID, targetId)
                putExtra(EXTRA_CATEGORY_ID, categoryId)
                resumePositionMs?.takeIf { it > 0L }?.let {
                    putExtra(EXTRA_RESUME_POSITION_MS, it)
                }
            }
        }
    }

    private val viewModel: MainViewModel by viewModels()

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null

    private var overlayVisible by mutableStateOf(true)
    private var activeDescriptor by mutableStateOf<PlaybackDescriptor?>(null)
    private var currentSettings by mutableStateOf(AppSettings())
    private var errorMessage by mutableStateOf<String?>(null)
    private var revealOverlayKeyUpCode: Int? = null
    private var overlayActivityTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureTvWindow(keepScreenOn = true)

        currentSettings = viewModel.settings.value
        player = ExoPlayer.Builder(this)
            .setLoadControl(buildLoadControl(currentSettings.bufferProfile))
            .build()
            .apply {
                playWhenReady = true
            }
        mediaSession = MediaSession.Builder(this, player).build()

        lifecycleScope.launch {
            viewModel.settings.collect { settings ->
                currentSettings = settings
            }
        }

        val targetType = TargetType.from(intent.getStringExtra(EXTRA_TARGET_TYPE).orEmpty())
        val targetId = intent.getStringExtra(EXTRA_TARGET_ID).orEmpty()
        val categoryId = intent.getStringExtra(EXTRA_CATEGORY_ID)
        val resumePositionMs = if (intent.hasExtra(EXTRA_RESUME_POSITION_MS)) {
            intent.getLongExtra(EXTRA_RESUME_POSITION_MS, 0L).coerceAtLeast(0L)
        } else {
            0L
        }

        lifecycleScope.launch {
            loadDescriptor(targetType, targetId, categoryId, resumePositionMs)
        }

        setContent {
            AuroraTheme {
                PlayerScreen(
                    viewModel = viewModel,
                    player = player,
                    descriptor = activeDescriptor,
                    overlayVisible = overlayVisible,
                    overlayActivityTick = overlayActivityTick,
                    errorMessage = errorMessage,
                    onHideOverlay = { overlayVisible = false },
                    onSeekBack = { seekFromOverlay(-SEEK_STEP_MS) },
                    onSeekForward = { seekFromOverlay(SEEK_STEP_MS) },
                    onTogglePlayPause = {
                        if (player.isPlaying) player.pause() else player.play()
                    },
                    onStartOver = { startOver() },
                    onNextEpisode = { playNextEpisode() },
                    onPrevChannel = { switchChannel(-1) },
                    onNextChannel = { switchChannel(1) },
                    onEnterPip = { enterPictureInPictureIfSupported() },
                    onClose = { finish() },
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            configureTvWindow(keepScreenOn = true)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP && event.keyCode == revealOverlayKeyUpCode) {
            revealOverlayKeyUpCode = null
            return true
        }

        if (event.action != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event)
        }

        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (overlayVisible) {
                overlayVisible = false
                revealOverlayKeyUpCode = event.keyCode
            } else {
                finish()
            }
            return true
        }

        activeDescriptor ?: return super.dispatchKeyEvent(event)
        if (overlayVisible) {
            noteOverlayActivity()
        }
        when (event.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                if (player.isPlaying) player.pause() else player.play()
                revealOverlay(event.keyCode)
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            -> {
                if (!overlayVisible) {
                    revealOverlay(event.keyCode)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun revealOverlay(keyCode: Int) {
        overlayVisible = true
        revealOverlayKeyUpCode = keyCode
        noteOverlayActivity()
    }

    private fun noteOverlayActivity() {
        overlayActivityTick += 1
    }

    override fun onStop() {
        persistProgress()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !isInPictureInPictureMode) {
            player.pause()
        }
        super.onStop()
    }

    override fun onDestroy() {
        persistProgress()
        mediaSession?.release()
        player.release()
        super.onDestroy()
    }

    private suspend fun loadDescriptor(
        targetType: TargetType,
        targetId: String,
        categoryId: String?,
        resumePositionMs: Long = 0L,
    ) {
        runCatching {
            viewModel.resolvePlayback(targetType, targetId, categoryId)
        }.onSuccess { descriptor ->
            playDescriptor(descriptor, resumePositionMs)
        }.onFailure { throwable ->
            errorMessage = throwable.message ?: "Unable to load stream"
        }
    }

    private fun playDescriptor(descriptor: PlaybackDescriptor, resumePositionMs: Long = 0L) {
        activeDescriptor = descriptor
        overlayVisible = true
        errorMessage = null

        val mediaItem = MediaItem.Builder()
            .setUri(descriptor.mediaUrl)
            .setMediaId("${descriptor.targetType.rawValue}:${descriptor.targetId}")
            .apply {
                if (descriptor.isLive) {
                    setLiveConfiguration(MediaItem.LiveConfiguration.Builder().build())
                }
            }
            .build()

        val startPositionMs = resumePositionMs.takeIf { !descriptor.isLive && it > 0L }
        if (startPositionMs != null) {
            player.setMediaItem(mediaItem, startPositionMs)
        } else {
            player.setMediaItem(mediaItem)
        }
        player.prepare()
        player.playWhenReady = true
        player.play()

        if (descriptor.isLive) {
            viewModel.registerRecentChannel(
                channelId = descriptor.targetId.toLong(),
                title = descriptor.title,
                artworkUrl = descriptor.artworkUrl,
                categoryId = descriptor.categoryId,
            )
        }
    }

    private fun switchChannel(direction: Int) {
        val descriptor = activeDescriptor ?: return
        val categoryId = descriptor.categoryId ?: return
        if (!descriptor.isLive) return

        lifecycleScope.launch {
            val adjacent = viewModel.resolveAdjacentChannel(
                currentChannelId = descriptor.targetId.toLong(),
                categoryId = categoryId,
                direction = direction,
            ) ?: return@launch
            playDescriptor(adjacent)
        }
    }

    private fun seekBy(deltaMs: Long) {
        val duration = player.duration.takeIf { it > 0 }
        val newPosition = if (duration != null) {
            (player.currentPosition + deltaMs).coerceIn(0L, duration)
        } else {
            (player.currentPosition + deltaMs).coerceAtLeast(0L)
        }
        player.seekTo(newPosition)
    }

    private fun seekFromOverlay(deltaMs: Long) {
        noteOverlayActivity()
        seekBy(deltaMs)
    }

    private fun startOver() {
        val descriptor = activeDescriptor ?: return
        if (descriptor.isLive) return
        player.seekTo(0L)
        player.play()
        overlayVisible = true
    }

    private fun playNextEpisode() {
        val nextEpisodeId = activeDescriptor?.nextEpisodeId ?: return
        lifecycleScope.launch {
            loadDescriptor(TargetType.EPISODE, nextEpisodeId, null)
        }
    }

    private fun persistProgress() {
        val descriptor = activeDescriptor ?: return
        if (descriptor.isLive) return
        val duration = player.duration.takeIf { it > 0 } ?: 0L
        viewModel.updatePlaybackHistory(
            descriptor = descriptor,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
        )
    }

    private fun enterPictureInPictureIfSupported() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build(),
            )
        }
    }

    private fun buildLoadControl(bufferProfile: BufferProfile): DefaultLoadControl {
        val (minBufferMs, maxBufferMs, playBufferMs, rebufferMs) = when (bufferProfile) {
            BufferProfile.LOW_LATENCY -> listOf(2_000, 8_000, 500, 1_000)
            BufferProfile.BALANCED -> listOf(5_000, 20_000, 1_500, 2_500)
            BufferProfile.STABLE -> listOf(8_000, 30_000, 2_500, 4_000)
        }
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(minBufferMs, maxBufferMs, playBufferMs, rebufferMs)
            .build()
    }
}

@Composable
private fun PlayerScreen(
    viewModel: MainViewModel,
    player: ExoPlayer,
    descriptor: PlaybackDescriptor?,
    overlayVisible: Boolean,
    overlayActivityTick: Int,
    errorMessage: String?,
    onHideOverlay: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onStartOver: () -> Unit,
    onNextEpisode: () -> Unit,
    onPrevChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onEnterPip: () -> Unit,
    onClose: () -> Unit,
) {
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var audioOptions by remember { mutableStateOf(audioTrackOptions(player.currentTracks)) }
    var subtitleOptions by remember { mutableStateOf(subtitleTrackOptions(player.currentTracks)) }
    var languageOptionsVisible by remember { mutableStateOf(false) }

    val guideFlow: Flow<List<EpgEventEntity>> = remember(descriptor?.targetId, descriptor?.isLive) {
        if (descriptor?.isLive == true) {
            viewModel.observeGuide(descriptor.targetId.toLong())
        } else {
            flowOf(emptyList())
        }
    }
    val guide by guideFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val positionAndDuration by produceState(initialValue = 0L to 0L, descriptor?.targetId) {
        while (true) {
            value = player.currentPosition to (player.duration.takeIf { it > 0 } ?: 0L)
            delay(PLAYBACK_PROGRESS_UPDATE_MS)
        }
    }

    LaunchedEffect(descriptor?.targetId) {
        if (descriptor?.isLive == true) {
            viewModel.ensureGuide(descriptor.targetId.toLong())
        }
        if (descriptor != null) {
            player.playWhenReady = true
            player.play()
        }
    }

    LaunchedEffect(overlayVisible, isPlaying, descriptor?.targetId, overlayActivityTick, languageOptionsVisible) {
        if (overlayVisible && isPlaying && !languageOptionsVisible) {
            delay(4_500)
            if (player.isPlaying) onHideOverlay()
        }
    }

    LaunchedEffect(overlayVisible) {
        if (!overlayVisible) {
            languageOptionsVisible = false
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingState: Boolean) {
                isPlaying = isPlayingState
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isPlaying = false
            }

            override fun onTracksChanged(tracks: Tracks) {
                audioOptions = audioTrackOptions(tracks)
                subtitleOptions = subtitleTrackOptions(tracks)
            }
        }
        player.addListener(listener)
        audioOptions = audioTrackOptions(player.currentTracks)
        subtitleOptions = subtitleTrackOptions(player.currentTracks)
        onDispose {
            player.removeListener(listener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    this.player = player
                }
            },
            update = { playerView ->
                playerView.player = player
            },
        )

        if (overlayVisible) {
            PlayerOverlay(
                descriptor = descriptor,
                isPlaying = isPlaying,
                guide = guide,
                positionMs = positionAndDuration.first,
                durationMs = positionAndDuration.second,
                onTogglePlayPause = onTogglePlayPause,
                onSeekBack = onSeekBack,
                onSeekForward = onSeekForward,
                onStartOver = onStartOver,
                onNextEpisode = onNextEpisode,
                onPrevChannel = onPrevChannel,
                onNextChannel = onNextChannel,
                onEnterPip = onEnterPip,
                onClose = onClose,
                audioOptions = audioOptions,
                subtitleOptions = subtitleOptions,
                onOriginalAudioSelected = { option ->
                    selectAudioTrack(player, option)
                    selectSubtitleTrack(player, null)
                    audioOptions = audioTrackOptions(player.currentTracks, selectedOption = option)
                    subtitleOptions = subtitleTrackOptions(player.currentTracks, subtitlesDisabled = true)
                },
                onOpenLanguageOptions = { languageOptionsVisible = true },
            )
        }

        if (languageOptionsVisible && overlayVisible) {
            LanguageOptionsSidebar(
                audioOptions = audioOptions,
                subtitleOptions = subtitleOptions,
                onAudioSelected = { option ->
                    selectAudioTrack(player, option)
                    audioOptions = audioTrackOptions(player.currentTracks, selectedOption = option)
                },
                onSubtitleSelected = { option ->
                    selectSubtitleTrack(player, option)
                    subtitleOptions = subtitleTrackOptions(
                        tracks = player.currentTracks,
                        selectedOption = option,
                        subtitlesDisabled = option == null,
                    )
                },
                onDismiss = { languageOptionsVisible = false },
            )
        }

        errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp),
                color = Color(0xCC7A0E1B),
                tonalElevation = 6.dp,
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun PlayerOverlay(
    descriptor: PlaybackDescriptor?,
    isPlaying: Boolean,
    guide: List<EpgEventEntity>,
    positionMs: Long,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onStartOver: () -> Unit,
    onNextEpisode: () -> Unit,
    onPrevChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onEnterPip: () -> Unit,
    onClose: () -> Unit,
    audioOptions: List<AudioTrackOption>,
    subtitleOptions: List<SubtitleTrackOption>,
    onOriginalAudioSelected: (AudioTrackOption) -> Unit,
    onOpenLanguageOptions: () -> Unit,
) {
    val now = System.currentTimeMillis()
    val currentShow = guide.firstOrNull { now in it.startEpochMillis until it.endEpochMillis } ?: guide.firstOrNull()
    val nextShow = guide.firstOrNull { it.startEpochMillis > (currentShow?.startEpochMillis ?: now) }
    val isLive = descriptor?.isLive == true
    val canStartOver = descriptor?.isLive == false
    val playButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(50)
        playButtonFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xE6000000),
                        Color(0x33000000),
                        Color(0xF2000000),
                    ),
                ),
            ),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 64.dp, top = 56.dp)
                .focusRestorer()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.Top,
        ) {
            TopPlayerAction(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                label = "Back",
                onClick = onClose,
            )
            if (canStartOver) {
                TopPlayerAction(
                    icon = Icons.Rounded.Replay,
                    label = "Start Over",
                    onClick = onStartOver,
                )
                if (descriptor?.nextEpisodeId != null) {
                    TopPlayerAction(
                        icon = Icons.Rounded.SkipNext,
                        label = "Next Episode",
                        onClick = onNextEpisode,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 54.dp, end = 62.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Dolby Vision", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                descriptor?.title ?: "Loading stream",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            descriptor?.subtitle?.let {
                Text(it, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (isLive) {
                currentShow?.let {
                    Text("Now  ${it.title}", color = Color(0xFFE0E0E0), style = MaterialTheme.typography.bodyLarge)
                }
                nextShow?.let {
                    Text("Next  ${it.title}", color = Color(0xFFBDBDBD), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 64.dp, end = 64.dp, bottom = 54.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRestorer()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LargeRoundPlayButton(
                    isPlaying = isPlaying,
                    onClick = onTogglePlayPause,
                    onSeekBack = onSeekBack,
                    onSeekForward = onSeekForward,
                    seekEnabled = !isLive,
                    focusRequester = playButtonFocusRequester,
                )
                Text(
                    text = if (isLive) "LIVE" else formatProgressTime(positionMs),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                PlaybackProgressBar(
                    modifier = Modifier.weight(1f),
                    positionMs = positionMs,
                    durationMs = durationMs,
                    isLive = isLive,
                )
                Text(
                    text = if (isLive) "LIVE" else formatProgressTime(durationMs),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            val originalAudioOption = originalAudioOption(audioOptions)
            val selectedAudioOption = audioOptions.firstOrNull { it.selected } ?: originalAudioOption
            val selectedSubtitleOption = subtitleOptions.firstOrNull { it.selected }
            val selectedAudioIsOriginal = selectedAudioOption != null &&
                originalAudioOption != null &&
                isSameTrack(selectedAudioOption, originalAudioOption)
            val currentLanguageLabel = selectedAudioOption
                ?.takeIf { selectedSubtitleOption != null || !selectedAudioIsOriginal }
                ?.let { option ->
                    val audioLabel = audioPillLabel(option, originalAudioOption)
                    if (selectedSubtitleOption != null) "$audioLabel with Subtitles" else audioLabel
                }

            if (audioOptions.isNotEmpty() || subtitleOptions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .focusRestorer()
                        .focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    originalAudioOption?.let { option ->
                        PlayerPill(
                            label = audioPillLabel(option, originalAudioOption),
                            checked = selectedAudioIsOriginal && selectedSubtitleOption == null,
                            onClick = { onOriginalAudioSelected(option) },
                        )
                    }
                    currentLanguageLabel?.let { label ->
                        PlayerPill(
                            label = label,
                            checked = true,
                            onClick = onOpenLanguageOptions,
                        )
                    }
                    PlayerPill(
                        label = "Other...",
                        checked = false,
                        onClick = onOpenLanguageOptions,
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionsSidebar(
    audioOptions: List<AudioTrackOption>,
    subtitleOptions: List<SubtitleTrackOption>,
    onAudioSelected: (AudioTrackOption) -> Unit,
    onSubtitleSelected: (SubtitleTrackOption?) -> Unit,
    onDismiss: () -> Unit,
) {
    val firstFocusRequester = remember { FocusRequester() }
    val originalAudioOption = originalAudioOption(audioOptions)

    LaunchedEffect(Unit) {
        delay(50)
        firstFocusRequester.requestFocus()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(390.dp)
                .background(Color(0xEA000000))
                .focusRestorer()
                .focusGroup()
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.DirectionLeft || event.key == Key.Back) {
                        if (event.type == KeyEventType.KeyUp) onDismiss()
                        true
                    } else {
                        false
                    }
                }
                .verticalScroll(rememberScrollState())
                .padding(start = 34.dp, top = 84.dp, end = 42.dp, bottom = 54.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            TrackSectionTitle("Closed captions")
            TrackSelectionRow(
                label = "Off",
                selected = subtitleOptions.none { it.selected },
                focusRequester = firstFocusRequester,
                onClick = { onSubtitleSelected(null) },
            )
            subtitleOptions.forEach { option ->
                TrackSelectionRow(
                    label = option.label,
                    selected = option.selected,
                    onClick = { onSubtitleSelected(option) },
                )
            }

            if (audioOptions.isNotEmpty()) {
                TrackSectionTitle("Audio tracks")
                audioOptions.forEach { option ->
                    TrackSelectionRow(
                        label = audioDrawerLabel(option, originalAudioOption),
                        selected = option.selected,
                        onClick = { onAudioSelected(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackSectionTitle(label: String) {
    Text(
        text = label,
        color = Color.White,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Black,
    )
}

@Composable
private fun TrackSelectionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(if (focused) Color(0xFFE8EEFF) else Color.Transparent)
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.key in TvSelectKeys) {
                    if (event.type == KeyEventType.KeyUp) onClick()
                    true
                } else {
                    false
                }
            }
            .focusable()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = if (focused) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = label,
            color = if (focused) Color.Black else Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlaybackProgressBar(
    modifier: Modifier = Modifier,
    positionMs: Long,
    durationMs: Long,
    isLive: Boolean,
) {
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = PLAYBACK_PROGRESS_UPDATE_MS.toInt(),
            easing = LinearEasing,
        ),
        label = "playback-progress",
    )
    Box(
        modifier = modifier
            .height(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xCCFFFFFF)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xCCFFFFFF)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (isLive) 1f else animatedProgress)
                    .height(4.dp)
                    .background(Color(0xFFE50914)),
            )
        }
    }
}

@Composable
private fun TopPlayerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.key in TvSelectKeys) {
                    if (event.type == KeyEventType.KeyUp) onClick()
                    true
                } else {
                    false
                }
            }
            .focusable()
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(if (focused) Color.White else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (focused) Color.Black else Color.White,
                modifier = Modifier.size(34.dp),
            )
        }
        Text(
            label,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun LargeRoundPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    seekEnabled: Boolean,
    focusRequester: FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }
    var activeSeekDirection by remember { mutableIntStateOf(0) }
    var seekRepeatJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    fun stopSeekRepeat(direction: Int? = null) {
        if (direction == null || activeSeekDirection == direction) {
            seekRepeatJob?.cancel()
            seekRepeatJob = null
            activeSeekDirection = 0
        }
    }

    fun startSeekRepeat(direction: Int, seek: () -> Unit) {
        if (activeSeekDirection == direction && seekRepeatJob != null) return
        stopSeekRepeat()
        activeSeekDirection = direction
        seek()
        seekRepeatJob = scope.launch {
            delay(SEEK_REPEAT_INITIAL_DELAY_MS)
            while (true) {
                seek()
                delay(SEEK_REPEAT_INTERVAL_MS)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { stopSeekRepeat() }
    }

    Box(
        modifier = Modifier
            .size(82.dp)
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .clip(RoundedCornerShape(82.dp))
            .background(if (focused) Color.White else Color.Transparent)
            .onFocusChanged {
                focused = it.isFocused
                if (!it.isFocused) stopSeekRepeat()
            }
            .onPreviewKeyEvent { event ->
                when {
                    seekEnabled && event.key == Key.DirectionLeft -> {
                        if (event.type == KeyEventType.KeyDown) {
                            startSeekRepeat(-1, onSeekBack)
                        } else if (event.type == KeyEventType.KeyUp) {
                            stopSeekRepeat(-1)
                        }
                        true
                    }
                    seekEnabled && event.key == Key.DirectionRight -> {
                        if (event.type == KeyEventType.KeyDown) {
                            startSeekRepeat(1, onSeekForward)
                        } else if (event.type == KeyEventType.KeyUp) {
                            stopSeekRepeat(1)
                        }
                        true
                    }
                    event.key in TvSelectKeys -> {
                        if (event.type == KeyEventType.KeyUp) onClick()
                        true
                    }
                    else -> false
                }
            }
            .focusable()
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = if (focused) Color.Black else Color.White,
            modifier = Modifier.size(52.dp),
        )
    }
}

@Composable
private fun PlayerPill(
    label: String,
    checked: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(if (focused) Color.White else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.key in TvSelectKeys) {
                    if (event.type == KeyEventType.KeyUp) onClick()
                    true
                } else {
                    false
                }
            }
            .focusable()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (checked) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = if (focused) Color.Black else Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            label,
            color = if (focused) Color.Black else Color.White,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SmallCircleControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(44.dp))
            .background(if (focused) Color.White else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.key in TvSelectKeys) {
                    if (event.type == KeyEventType.KeyUp) onClick()
                    true
                } else {
                    false
                }
            }
            .focusable()
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (focused) Color.Black else Color.White, modifier = Modifier.size(24.dp))
    }
}

private fun audioTrackOptions(
    tracks: Tracks,
    selectedOption: AudioTrackOption? = null,
): List<AudioTrackOption> {
    var optionNumber = 1
    return tracks.groups
        .filter { it.type == C.TRACK_TYPE_AUDIO }
        .flatMap { group ->
            (0 until group.length)
                .filter { trackIndex -> group.isTrackSupported(trackIndex, true) }
                .map { trackIndex ->
                    val format = group.getTrackFormat(trackIndex)
                    val labels = audioTrackLabels(format, optionNumber++)
                    val selected = selectedOption?.let {
                        isSameTrackGroup(it.mediaTrackGroup, group.getMediaTrackGroup()) &&
                            it.trackIndex == trackIndex
                    } ?: group.isTrackSelected(trackIndex)
                    AudioTrackOption(
                        label = labels.detailLabel,
                        shortLabel = labels.shortLabel,
                        mediaTrackGroup = group.getMediaTrackGroup(),
                        trackIndex = trackIndex,
                        selected = selected,
                        isDefault = format.selectionFlags and C.SELECTION_FLAG_DEFAULT != 0 ||
                            format.roleFlags and C.ROLE_FLAG_MAIN != 0,
                    )
                }
        }
}

private fun subtitleTrackOptions(
    tracks: Tracks,
    selectedOption: SubtitleTrackOption? = null,
    subtitlesDisabled: Boolean = false,
): List<SubtitleTrackOption> {
    var optionNumber = 1
    return tracks.groups
        .filter { it.type == C.TRACK_TYPE_TEXT }
        .flatMap { group ->
            (0 until group.length)
                .filter { trackIndex -> group.isTrackSupported(trackIndex, true) }
                .map { trackIndex ->
                    val selected = when {
                        subtitlesDisabled -> false
                        selectedOption != null -> isSameTrackGroup(selectedOption.mediaTrackGroup, group.getMediaTrackGroup()) &&
                            selectedOption.trackIndex == trackIndex
                        else -> group.isTrackSelected(trackIndex)
                    }
                    SubtitleTrackOption(
                        label = subtitleTrackLabel(group.getTrackFormat(trackIndex), optionNumber++),
                        mediaTrackGroup = group.getMediaTrackGroup(),
                        trackIndex = trackIndex,
                        selected = selected,
                    )
                }
        }
}

private fun selectAudioTrack(player: Player, option: AudioTrackOption) {
    player.trackSelectionParameters = player.trackSelectionParameters
        .buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
        .setOverrideForType(TrackSelectionOverride(option.mediaTrackGroup, option.trackIndex))
        .build()
}

private fun selectSubtitleTrack(player: Player, option: SubtitleTrackOption?) {
    val builder = player.trackSelectionParameters
        .buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_TEXT)

    if (option == null) {
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
    } else {
        builder
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setOverrideForType(TrackSelectionOverride(option.mediaTrackGroup, option.trackIndex))
    }
    player.trackSelectionParameters = builder.build()
}

private fun audioTrackLabels(format: Format, fallbackIndex: Int): AudioTrackLabels {
    val language = trackLanguageName(format) ?: "Audio $fallbackIndex"
    val channelLabel = audioChannelLabel(format)
    val role = when {
        format.roleFlags and C.ROLE_FLAG_DESCRIBES_VIDEO != 0 -> "Audio Description"
        format.roleFlags and C.ROLE_FLAG_COMMENTARY != 0 -> "Commentary"
        format.roleFlags and C.ROLE_FLAG_DUB != 0 -> "Dub"
        format.roleFlags and C.ROLE_FLAG_ALTERNATE != 0 -> "Alternate"
        else -> null
    }
    val detailedBase = listOfNotNull(language, channelLabel)
        .joinToString(", ")
    val detailedLabel = listOfNotNull(detailedBase, role)
        .joinToString(" - ")
    val shortLabel = listOfNotNull(language, role?.takeIf { channelLabel == null })
        .joinToString(" - ")
    return AudioTrackLabels(
        shortLabel = shortLabel,
        detailLabel = detailedLabel,
    )
}

private fun subtitleTrackLabel(format: Format, fallbackIndex: Int): String {
    val language = trackLanguageName(format)
    val role = when {
        format.selectionFlags and C.SELECTION_FLAG_FORCED != 0 -> "Forced"
        format.roleFlags and C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND != 0 -> "SDH"
        else -> null
    }
    return listOfNotNull(language, role)
        .joinToString(", ")
        .takeIf { it.isNotBlank() }
        ?: "Subtitle $fallbackIndex"
}

private fun trackLanguageName(format: Format): String? {
    return displayLanguageName(format.language)
        ?: extractLanguageName(rawTrackText(format))
}

private fun displayLanguageName(languageTag: String?): String? {
    return languageTag
        ?.takeIf { it.isNotBlank() && it.lowercase(Locale.US) != "und" }
        ?.let { tag ->
            val normalizedTag = tag.replace('_', '-')
            if (!isLikelyLanguageTag(normalizedTag)) return@let null
            Locale.forLanguageTag(normalizedTag)
                .getDisplayName(Locale.getDefault())
                .takeIf { it.isNotBlank() && it.lowercase(Locale.getDefault()) != normalizedTag.lowercase(Locale.getDefault()) }
                ?: normalizedTag.uppercase(Locale.getDefault())
        }
}

private fun isLikelyLanguageTag(languageTag: String): Boolean {
    return Regex("^[A-Za-z]{2,3}(-[A-Za-z]{4})?(-([A-Za-z]{2}|[0-9]{3}))?$")
        .matches(languageTag)
}

private fun extractLanguageName(rawTrackText: String?): String? {
    val normalizedTrackText = rawTrackText
        ?.replace('_', ' ')
        ?.takeIf { it.isNotBlank() }
        ?: return null

    return KnownTrackLanguageNames.firstOrNull { languageName ->
        Regex(
            pattern = "(^|[^\\p{L}])${Regex.escape(languageName)}([^\\p{L}]|$)",
            option = RegexOption.IGNORE_CASE,
        ).containsMatchIn(normalizedTrackText)
    }
}

private fun audioChannelLabel(format: Format): String? {
    return when (format.channelCount) {
        1 -> "Mono"
        2 -> "Stereo"
        6 -> "5.1 surround sound"
        8 -> "7.1 surround sound"
        else -> audioChannelLabelFromRawText(rawTrackText(format))
    }
}

private fun audioChannelLabelFromRawText(rawTrackText: String?): String? {
    val rawText = rawTrackText?.takeIf { it.isNotBlank() } ?: return null
    return when {
        Regex("(?i)(^|[^0-9])7\\.1([^0-9]|$)").containsMatchIn(rawText) -> "7.1 surround sound"
        Regex("(?i)(^|[^0-9])5\\.1([^0-9]|$)").containsMatchIn(rawText) -> "5.1 surround sound"
        Regex("(?i)(^|[^0-9])2\\.0([^0-9]|$)|stereo").containsMatchIn(rawText) -> "Stereo"
        Regex("(?i)(^|[^0-9])1\\.0([^0-9]|$)|mono").containsMatchIn(rawText) -> "Mono"
        else -> null
    }
}

private fun rawTrackText(format: Format): String? {
    return listOfNotNull(format.language, format.label)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .takeIf { it.isNotBlank() }
}

private fun originalAudioOption(audioOptions: List<AudioTrackOption>): AudioTrackOption? {
    return audioOptions.firstOrNull { it.isDefault } ?: audioOptions.firstOrNull()
}

private fun audioPillLabel(
    option: AudioTrackOption,
    originalAudioOption: AudioTrackOption?,
): String {
    return if (originalAudioOption != null && isSameTrack(option, originalAudioOption)) {
        option.shortLabel.withOriginalSuffix()
    } else {
        option.shortLabel
    }
}

private fun audioDrawerLabel(
    option: AudioTrackOption,
    originalAudioOption: AudioTrackOption?,
): String {
    return if (originalAudioOption != null && isSameTrack(option, originalAudioOption)) {
        option.label.withOriginalSuffix()
    } else {
        option.label
    }
}

private fun String.withOriginalSuffix(): String {
    return if (contains("original", ignoreCase = true)) this else "$this [Original]"
}

private fun isSameTrack(first: AudioTrackOption, second: AudioTrackOption): Boolean {
    return isSameTrackGroup(first.mediaTrackGroup, second.mediaTrackGroup) &&
        first.trackIndex == second.trackIndex
}

private fun isSameTrackGroup(first: TrackGroup, second: TrackGroup): Boolean {
    return first == second
}

private fun formatGuideTime(event: EpgEventEntity): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return "${formatter.format(Date(event.startEpochMillis))}-${formatter.format(Date(event.endEpochMillis))}"
}

private fun formatProgressTime(timeMs: Long): String {
    if (timeMs <= 0L) return "--:--"
    val totalSeconds = timeMs / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
