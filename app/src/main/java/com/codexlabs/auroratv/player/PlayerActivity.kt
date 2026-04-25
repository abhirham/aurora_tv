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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

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
        }
        player.addListener(listener)
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
) {
    val now = System.currentTimeMillis()
    val currentShow = guide.firstOrNull { now in it.startEpochMillis until it.endEpochMillis } ?: guide.firstOrNull()
    val nextShow = guide.firstOrNull { it.startEpochMillis > (currentShow?.startEpochMillis ?: now) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xB3050C18),
                        Color.Transparent,
                        Color(0x99050C18),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 28.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = descriptor?.title ?: "Loading stream",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
            )
            descriptor?.subtitle?.let {
                Text(it, color = Color(0xFFE0E6EC), style = MaterialTheme.typography.titleLarge)
            }
            currentShow?.let {
                Text(
                    text = "Now: ${formatGuideTime(it)}  ${it.title}",
                    color = Color(0xFF8FF3DE),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            nextShow?.let {
                Text(
                    text = "Next: ${formatGuideTime(it)}  ${it.title}",
                    color = Color(0xFFF0CFAE),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (descriptor?.isLive == false) {
                Text(
                    text = "${formatProgressTime(positionMs)} / ${formatProgressTime(durationMs)}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = "Tip: hide controls, then use up/down to change channels",
                    color = Color(0xFFE0E6EC),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onTogglePlayPause) {
                    Text(if (isPlaying) "Pause" else "Play")
                }
                if (descriptor?.isLive == true) {
                    Button(onClick = onPrevChannel) { Text("Previous") }
                    Button(onClick = onNextChannel) { Text("Next") }
                } else {
                    Button(onClick = onSeekBack) { Text("-10s") }
                    Button(onClick = onSeekForward) { Text("+10s") }
                }
                Button(onClick = onEnterPip) { Text("PiP") }
                Button(onClick = onClose) { Text("Close") }
            }
        }
    }
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
