package com.antigravity.resolver

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class Level3IPC : MediaResolver {
    override var nextResolver: MediaResolver? = null
    
    // IPC port matching the Go server
    private val engineUrl = "http://127.0.0.1:48192/resolve"

    override suspend fun resolve(context: ResolveContext): MediaResult? = withContext(Dispatchers.IO) {
        Log.d("Antigravity-L3", "Sending complex URL to Go Engine: ${context.url}")
        
        try {
            val url = URL(engineUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonPayload = JSONObject().apply {
                put("url", context.url)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonPayload.toString())
                writer.flush()
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(responseText)
                
                val resolvedUrl = responseJson.getString("url")
                val type = responseJson.getString("type")
                
                val result = MediaResult(resolvedUrl, type)
                Log.d("Antigravity-L3", "Go Engine successfully resolved: $resolvedUrl")
                return@withContext result
            }
        } catch (e: Exception) {
            Log.e("Antigravity-L3", "Go Engine IPC failed. Engine might be down.", e)
        }
        
        // Pass down to Level 4 (Manifest parser) if applicable
        return@withContext nextResolver?.resolve(context)
    }
}
