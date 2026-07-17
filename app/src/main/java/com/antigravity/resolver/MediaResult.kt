package com.antigravity.resolver

data class MediaResult(
    val url: String,
    val type: String, // e.g., "HLS (m3u8)", "MP4 Video"
    val metadata: Map<String, String> = emptyMap()
)
