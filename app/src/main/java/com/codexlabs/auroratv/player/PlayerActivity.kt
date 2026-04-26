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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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

class PlayerActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_TARGET_TYPE = "extra_target_type"
        private const val EXTRA_TARGET_ID = "extra_target_id"
        private const val EXTRA_CATEGORY_ID = "extra_category_id"

        fun createIntent(
            context: Context,
            targetType: TargetType,
            targetId: String,
            categoryId: String?,
        ): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_TARGET_TYPE, targetType.rawValue)
                putExtra(EXTRA_TARGET_ID, targetId)
                putExtra(EXTRA_CATEGORY_ID, categoryId)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        lifecycleScope.launch {
            loadDescriptor(targetType, targetId, categoryId)
        }

        setContent {
            AuroraTheme {
                PlayerScreen(
                    viewModel = viewModel,
                    player = player,
                    descriptor = activeDescriptor,
                    overlayVisible = overlayVisible,
                    errorMessage = errorMessage,
                    onToggleOverlay = { overlayVisible = !overlayVisible },
                    onSeekBack = { seekBy(-10_000L) },
                    onSeekForward = { seekBy(10_000L) },
                    onTogglePlayPause = {
                        if (player.isPlaying) player.pause() else player.play()
                    },
                    onPrevChannel = { switchChannel(-1) },
                    onNextChannel = { switchChannel(1) },
                    onEnterPip = { enterPictureInPictureIfSupported() },
                    onClose = { finish() },
                )
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event)
        }

        val descriptor = activeDescriptor ?: return super.dispatchKeyEvent(event)
        when (event.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                if (player.isPlaying) player.pause() else player.play()
                overlayVisible = true
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            -> {
                if (!overlayVisible) {
                    overlayVisible = true
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (!overlayVisible && descriptor.isLive) {
                    switchChannel(-1)
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (!overlayVisible && descriptor.isLive) {
                    switchChannel(1)
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (!overlayVisible && !descriptor.isLive) {
                    seekBy(-10_000L)
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (!overlayVisible && !descriptor.isLive) {
                    seekBy(10_000L)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
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
    ) {
        runCatching {
            viewModel.resolvePlayback(targetType, targetId, categoryId)
        }.onSuccess { descriptor ->
            playDescriptor(descriptor)
        }.onFailure { throwable ->
            errorMessage = throwable.message ?: "Unable to load stream"
        }
    }

    private fun playDescriptor(descriptor: PlaybackDescriptor) {
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

        player.setMediaItem(mediaItem)
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
        val newPosition = (player.currentPosition + deltaMs).coerceAtLeast(0L)
        player.seekTo(newPosition)
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
    errorMessage: String?,
    onToggleOverlay: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onEnterPip: () -> Unit,
    onClose: () -> Unit,
) {
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var audioProfileCount by remember { mutableIntStateOf(countAudioProfiles(player.currentTracks)) }

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
            delay(1_000)
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

    LaunchedEffect(overlayVisible, descriptor?.targetId) {
        if (overlayVisible) {
            delay(4_500)
            if (player.isPlaying) onToggleOverlay()
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
                audioProfileCount = countAudioProfiles(tracks)
            }
        }
        player.addListener(listener)
        audioProfileCount = countAudioProfiles(player.currentTracks)
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
                onPrevChannel = onPrevChannel,
                onNextChannel = onNextChannel,
                onEnterPip = onEnterPip,
                onClose = onClose,
                showAudioOptions = audioProfileCount > 1,
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
    onPrevChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onEnterPip: () -> Unit,
    onClose: () -> Unit,
    showAudioOptions: Boolean,
) {
    val now = System.currentTimeMillis()
    val currentShow = guide.firstOrNull { now in it.startEpochMillis until it.endEpochMillis } ?: guide.firstOrNull()
    val nextShow = guide.firstOrNull { it.startEpochMillis > (currentShow?.startEpochMillis ?: now) }
    val isLive = descriptor?.isLive == true

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
                .padding(start = 64.dp, top = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.Top,
        ) {
            MinimalPlayerIcon(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                onClick = onClose,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
                    MinimalPlayerIcon(
                        icon = Icons.Rounded.SkipPrevious,
                        onClick = if (isLive) onPrevChannel else onTogglePlayPause,
                    )
                    MinimalPlayerIcon(
                        icon = Icons.Rounded.SkipNext,
                        onClick = if (isLive) onNextChannel else onTogglePlayPause,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "OPTIONS",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
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

        if (!isPlaying) {
            Text(
                "A",
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFFE50914),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 64.dp, end = 64.dp, bottom = 54.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LargeRoundPlayButton(
                    isPlaying = isPlaying,
                    onClick = onTogglePlayPause,
                    onSeekBack = onSeekBack,
                    onSeekForward = onSeekForward,
                    seekEnabled = !isLive,
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

            if (showAudioOptions) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerPill("English [Original]")
                    Spacer(Modifier.width(12.dp))
                    PlayerPill("English [Original] with Subtitles", checked = true)
                    Spacer(Modifier.width(12.dp))
                    PlayerPill("Other...")
                    Spacer(Modifier.width(12.dp))
                    SmallCircleControl(icon = Icons.Rounded.Settings, onClick = onEnterPip)
                }
            }
        }
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
                    .fillMaxWidth(if (isLive) 1f else progress)
                    .height(4.dp)
                    .background(Color(0xFFE50914)),
            )
        }
    }
}

@Composable
private fun MinimalPlayerIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(50.dp)
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
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (focused) Color.Black else Color.White,
            modifier = Modifier.size(36.dp),
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
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(82.dp)
            .clip(RoundedCornerShape(82.dp))
            .background(if (focused) Color.White else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                when {
                    seekEnabled && event.key == Key.DirectionLeft -> {
                        if (event.type == KeyEventType.KeyDown) onSeekBack()
                        true
                    }
                    seekEnabled && event.key == Key.DirectionRight -> {
                        if (event.type == KeyEventType.KeyDown) onSeekForward()
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
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(if (focused) Color.White else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.key in TvSelectKeys) {
                    true
                } else {
                    false
                }
            }
            .focusable()
            .clickable(role = Role.Button, onClick = {})
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

private fun countAudioProfiles(tracks: Tracks): Int {
    return tracks.groups
        .filter { it.type == C.TRACK_TYPE_AUDIO }
        .sumOf { it.length }
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
