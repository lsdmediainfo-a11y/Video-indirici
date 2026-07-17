package com.antigravity.resolver

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession

class Level1Network : MediaResolver {
    override var nextResolver: MediaResolver? = null
    
    private val _mediaFlow = MutableSharedFlow<MediaResult>(extraBufferCapacity = 10)
    val mediaFlow = _mediaFlow.asSharedFlow()
    private val scope = CoroutineScope(Dispatchers.IO)

    override suspend fun resolve(context: ResolveContext): MediaResult? {
        val url = context.url
        if (url.contains(".m3u8") || url.contains(".mp4") || url.contains(".mpd")) {
            val type = when {
                url.contains(".m3u8") -> "HLS (m3u8)"
                url.contains(".mpd") -> "DASH (mpd)"
                else -> "MP4 Video"
            }
            val result = MediaResult(url, type)
            // Emit to UI
            _mediaFlow.tryEmit(result)
            Log.d("Antigravity-L1", "Media Detected: ${result.url}")
            return result
        }
        
        // Pass to next resolver (Level 2: DOM Parser) if not found
        return nextResolver?.resolve(context)
    }

    // GeckoView Navigation Delegate to intercept page and iframe loads
    val navigationDelegate = object : GeckoSession.NavigationDelegate {
        override fun onLoadRequest(
            session: GeckoSession, 
            request: GeckoSession.NavigationDelegate.LoadRequest
        ): GeckoSession.NavigationDelegate.LoadRequest {
            scope.launch {
                resolve(ResolveContext(url = request.uri))
            }
            return request
        }
    }
}
