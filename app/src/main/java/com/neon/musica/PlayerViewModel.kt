package com.neon.musica

import android.app.Application
import android.content.ComponentName
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Ya no reproduce la música directamente: se conecta al PlaybackService
 * (mediante un MediaController) para que el sonido siga en segundo plano.
 */
class PlayerViewModel(app: Application) : AndroidViewModel(app) {

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
    private var player: Player? = null
    private val ready = CompletableDeferred<Player>()
    private val controllerFuture: ListenableFuture<MediaController>

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncFrom(player)
        }
    }

    init {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(app, token).buildAsync()
        controllerFuture.addListener(
            {
                try {
                    val controller = controllerFuture.get()
                    controller.addListener(listener)
                    player = controller
                    syncFrom(controller)
                    ready.complete(controller)
                } catch (e: Exception) {
                    ready.completeExceptionally(e)
                }
            },
            ContextCompat.getMainExecutor(app)
        )

        // Actualiza la barra de progreso 4 veces por segundo.
        viewModelScope.launch {
            while (true) {
                player?.let { p ->
                    positionMs = p.currentPosition.coerceAtLeast(0L)
                    if (durationMs <= 0L) {
                        durationMs = p.duration.coerceAtLeast(0L)
                    }
                }
                delay(250)
            }
        }
    }

    private fun syncFrom(p: Player) {
        isPlaying = p.isPlaying
        currentIndex = if (p.mediaItemCount > 0) p.currentMediaItemIndex else -1
        durationMs = p.duration.coerceAtLeast(0L)
        shuffle = p.shuffleModeEnabled
        repeatMode = p.repeatMode
    }

    private fun Song.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(artworkUri)
                    .build()
            )
            .build()

    private fun sameQueue(p: Player, list: List<Song>): Boolean {
        if (p.mediaItemCount != list.size) return false
        return list.indices.all { p.getMediaItemAt(it).mediaId == list[it].id.toString() }
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

            val p = try {
                ready.await()
            } catch (e: Exception) {
                loaded = false
                return@launch
            }

            // Si el servicio ya tenía esta misma lista sonando, no la reiniciamos.
            if (!sameQueue(p, list)) {
                p.setMediaItems(list.map { it.toMediaItem() })
                p.prepare()
            }
            syncFrom(p)
        }
    }

    fun playSong(index: Int) {
        val p = player ?: return
        if (index !in songs.indices) return
        p.seekTo(index, 0L)
        p.play()
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (songs.isEmpty()) return
        when {
            p.playbackState == Player.STATE_ENDED -> {
                p.seekTo(0, 0L)
                p.play()
            }
            p.isPlaying -> p.pause()
            else -> p.play()
        }
    }

    fun next() {
        player?.seekToNextMediaItem()
    }

    fun previous() {
        player?.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) {
        player?.seekTo(ms)
        positionMs = ms
    }

    fun toggleShuffle() {
        val p = player ?: return
        p.shuffleModeEnabled = !p.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val p = player ?: return
        p.repeatMode = when (p.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    override fun onCleared() {
        // Solo suelta la conexión: el servicio sigue tocando si hay música sonando.
        MediaController.releaseFuture(controllerFuture)
        super.onCleared()
    }
}
