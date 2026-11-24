package com.neon.musica

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val player: ExoPlayer = ExoPlayer.Builder(app)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .build()

    var songs by mutableStateOf<List<Song>>(emptyList())
        private set

    var currentIndex by mutableIntStateOf(-1)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var positionMs by mutableLongStateOf(0L)
        private set

    var durationMs by mutableLongStateOf(0L)
        private set

    var shuffle by mutableStateOf(false)
        private set

    var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)
        private set

    private var loaded = false

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                this@PlayerViewModel.isPlaying = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentIndex = player.currentMediaItemIndex
                positionMs = 0L
                durationMs = player.duration.coerceAtLeast(0L)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    durationMs = player.duration.coerceAtLeast(0L)
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                shuffle = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                this@PlayerViewModel.repeatMode = repeatMode
            }
        })

        // Actualiza la barra de progreso 4 veces por segundo.
        viewModelScope.launch {
            while (true) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                if (durationMs <= 0L) {
                    durationMs = player.duration.coerceAtLeast(0L)
                }
                delay(250)
            }
        }
    }

    fun load() {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                MusicRepository.loadSongs(getApplication<Application>())
            }
            songs = list
            if (list.isEmpty()) {
                loaded = false // permite volver a intentar
                return@launch
            }
            player.setMediaItems(list.map { MediaItem.fromUri(it.uri) })
            player.prepare()
            currentIndex = player.currentMediaItemIndex
        }
    }

    fun playSong(index: Int) {
        if (index !in songs.indices) return
        player.seekTo(index, 0L)
        player.play()
    }

    fun togglePlayPause() {
        if (songs.isEmpty()) return
        when {
            player.playbackState == Player.STATE_ENDED -> {
                player.seekTo(0, 0L)
                player.play()
            }
            player.isPlaying -> player.pause()
            else -> player.play()
        }
    }

    fun next() {
        player.seekToNextMediaItem()
    }

    fun previous() {
        // Si ya pasaron unos segundos, reinicia la canción; si no, va a la anterior.
        player.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) {
        player.seekTo(ms)
        positionMs = ms
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun cycleRepeat() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
