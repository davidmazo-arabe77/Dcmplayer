package com.neon.musica.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.neon.musica.PlayerViewModel
import com.neon.musica.Song
import com.neon.musica.ui.theme.DeepBg
import com.neon.musica.ui.theme.NeonBlue
import com.neon.musica.ui.theme.NeonBrush
import com.neon.musica.ui.theme.NeonPurple
import com.neon.musica.ui.theme.SurfaceDark
import com.neon.musica.ui.theme.TextPrimary
import com.neon.musica.ui.theme.TextSecondary

@Composable
fun PlayerScreen(
    vm: PlayerViewModel,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1B0638), DeepBg, Color(0xFF031A2B))
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Text(
                text = "NEON PLAYER",
                style = TextStyle(
                    brush = NeonBrush,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                ),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            when {
                !hasPermission -> MessageBox(
                    text = "Necesito permiso para leer la música de tu teléfono.",
                    buttonText = "Dar permiso",
                    onClick = onRequestPermission
                )

                vm.songs.isEmpty() -> MessageBox(
                    text = "No encontré canciones en tu teléfono.",
                    buttonText = "Buscar de nuevo",
                    onClick = { vm.load() }
                )

                else -> {
                    NowPlaying(vm)
                    SongList(vm, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MessageBox(text: String, buttonText: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text, color = TextPrimary, fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = Color.White)
        ) {
            Text(buttonText)
        }
    }
}

@Composable
private fun NowPlaying(vm: PlayerViewModel) {
    val song = vm.songs.getOrNull(vm.currentIndex)

    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    val maxValue = vm.durationMs.coerceAtLeast(1L).toFloat()
    val shownValue = if (dragging) dragValue else vm.positionMs.toFloat().coerceIn(0f, maxValue)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Portada con borde neón
        Box(
            Modifier
                .size(170.dp)
                .shadow(24.dp, RoundedCornerShape(32.dp), ambientColor = NeonPurple, spotColor = NeonPurple)
                .clip(RoundedCornerShape(32.dp))
                .background(NeonBrush)
                .padding(3.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(SurfaceDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = NeonBlue,
                modifier = Modifier.size(80.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = song?.title ?: "—",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song?.artist ?: "",
            color = TextSecondary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(8.dp))

        Slider(
            value = shownValue,
            onValueChange = {
                dragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                vm.seekTo(dragValue.toLong())
                dragging = false
            },
            valueRange = 0f..maxValue,
            colors = SliderDefaults.colors(
                thumbColor = NeonBlue,
                activeTrackColor = NeonPurple,
                inactiveTrackColor = Color(0x33FFFFFF)
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(shownValue.toLong()), color = TextSecondary, fontSize = 12.sp)
            Text(formatTime(vm.durationMs), color = TextSecondary, fontSize = 12.sp)
        }

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = vm::toggleShuffle) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = "Aleatorio",
                    tint = if (vm.shuffle) NeonBlue else TextSecondary
                )
            }
            IconButton(onClick = vm::previous, modifier = Modifier.size(52.dp)) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "Anterior",
                    tint = TextPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }
            Box(
                Modifier
                    .size(72.dp)
                    .shadow(18.dp, CircleShape, ambientColor = NeonBlue, spotColor = NeonPurple)
                    .clip(CircleShape)
                    .background(NeonBrush)
                    .clickable(onClick = vm::togglePlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (vm.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (vm.isPlaying) "Pausar" else "Reproducir",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }
            IconButton(onClick = vm::next, modifier = Modifier.size(52.dp)) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Siguiente",
                    tint = TextPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }
            IconButton(onClick = vm::cycleRepeat) {
                Icon(
                    imageVector = if (vm.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = "Repetir",
                    tint = if (vm.repeatMode == Player.REPEAT_MODE_OFF) TextSecondary else NeonBlue
                )
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SongList(vm: PlayerViewModel, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        itemsIndexed(vm.songs, key = { _, song -> song.id }) { index, song ->
            SongRow(
                song = song,
                selected = index == vm.currentIndex,
                playing = vm.isPlaying,
                onClick = { vm.playSong(index) }
            )
        }
    }
}

@Composable
private fun SongRow(song: Song, selected: Boolean, playing: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(shape)
            .background(if (selected) Color(0x2200D4FF) else Color.Transparent)
            .then(if (selected) Modifier.border(1.dp, NeonBrush, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) NeonBrush else SolidColor(SurfaceDark)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected && playing) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = if (selected) Color.White else NeonPurple,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (selected) NeonBlue else TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(formatTime(song.durationMs), color = TextSecondary, fontSize = 12.sp)
    }
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0L)
    return "%d:%02d".format(total / 60, total % 60)
}
