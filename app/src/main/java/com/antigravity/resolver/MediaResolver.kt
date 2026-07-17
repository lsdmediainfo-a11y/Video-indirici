package com.antigravity.resolver

interface MediaResolver {
    var nextResolver: MediaResolver?
    suspend fun resolve(context: ResolveContext): MediaResult?
}

data class ResolveContext(
    val url: String,
    val requestHeaders: Map<String, String> = emptyMap(),
    val responseBody: String? = null
)
