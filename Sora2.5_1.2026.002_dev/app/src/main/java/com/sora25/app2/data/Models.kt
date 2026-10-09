package com.sora25.app2.data

import com.google.firebase.Timestamp

/** Документ коллекции "videos" в Firestore. */
data class VideoItem(
    val id: String = "",
    val uid: String = "",
    val prompt: String = "",
    val mode: String = "text",          // text | character | image
    val status: String = "queued",      // queued | generating | ready | failed
    val published: Boolean = false,
    val videoUrl: String = "",
    val error: String = "",
    val createdAt: Timestamp? = null,
)
