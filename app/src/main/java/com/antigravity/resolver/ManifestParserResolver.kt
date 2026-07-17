package com.antigravity.resolver

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class ManifestParserResolver : MediaResolver {
    override var nextResolver: MediaResolver? = null

    override suspend fun resolve(context: ResolveContext): MediaResult? = withContext(Dispatchers.IO) {
        if (context.url.contains(".m3u8")) {
            Log.d("Antigravity-L4", "Parsing HLS Manifest: ${context.url}")
            
            try {
                // Fetch the master m3u8 file
                val manifestContent = URL(context.url).readText()
                
                // Extremely simple HLS parser to find resolutions
                val resolutions = mutableListOf<String>()
                manifestContent.lines().forEach { line ->
                    if (line.startsWith("#EXT-X-STREAM-INF")) {
                        val resMatch = Regex("RESOLUTION=(\\d+x\\d+)").find(line)
                        resMatch?.let { resolutions.add(it.groupValues[1]) }
                    }
                }
                
                Log.d("Antigravity-L4", "Found resolutions: $resolutions")
                
                val metadata = mapOf("resolutions" to resolutions.joinToString(","))
                return@withContext MediaResult(context.url, "HLS (m3u8)", metadata)
                
            } catch (e: Exception) {
                Log.e("Antigravity-L4", "Failed to parse m3u8", e)
            }
        }
        
        return@withContext nextResolver?.resolve(context)
    }
}
