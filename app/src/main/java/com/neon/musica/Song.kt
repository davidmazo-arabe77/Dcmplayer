package com.neon.musica

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: Uri,
    val artworkUri: Uri,
    val durationMs: Long
)
