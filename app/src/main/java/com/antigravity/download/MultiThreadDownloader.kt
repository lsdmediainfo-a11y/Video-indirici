package com.antigravity.download

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

class MultiThreadDownloader(private val threadCount: Int = 8) {

    suspend fun download(url: String, destFile: File, onProgress: (Int) -> Unit) = withContext(Dispatchers.IO) {
        val contentLength = getContentLength(url)
        if (contentLength <= 0) {
            // Fallback to single thread if server doesn't support Range requests
            downloadSingle(url, destFile, onProgress)
            return@withContext
        }

        val chunkSize = contentLength / threadCount
        val chunks = (0 until threadCount).map { i ->
            val start = i * chunkSize
            val end = if (i == threadCount - 1) contentLength - 1 else (start + chunkSize - 1)
            Chunk(start, end)
        }

        // Pre-allocate the full file on disk for random access
        RandomAccessFile(destFile, "rw").use { it.setLength(contentLength) }

        var downloadedBytes = 0L

        coroutineScope {
            val deferreds = chunks.mapIndexed { index, chunk ->
                async {
                    downloadChunk(url, destFile, chunk, index) { bytes ->
                        synchronized(this@MultiThreadDownloader) {
                            downloadedBytes += bytes
                            val progress = ((downloadedBytes.toDouble() / contentLength) * 100).toInt()
                            onProgress(progress)
                        }
                    }
                }
            }
            deferreds.awaitAll()
        }
        Log.d("Antigravity-DL", "Multi-thread download completed: ${destFile.absolutePath}")
    }

    private suspend fun downloadChunk(urlStr: String, file: File, chunk: Chunk, index: Int, onBytesRead: (Int) -> Unit) {
        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("Range", "bytes=${chunk.start}-${chunk.end}")
        connection.connect()

        if (connection.responseCode in 200..299) {
            val inputStream: InputStream = connection.inputStream
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(chunk.start)
                val buffer = ByteArray(8192)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    raf.write(buffer, 0, read)
                    onBytesRead(read)
                }
            }
        }
        connection.disconnect()
    }

    private fun getContentLength(urlStr: String): Long {
        val connection = URL(urlStr).openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        connection.connect()
        val length = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            connection.contentLengthLong
        } else {
            connection.contentLength.toLong()
        }
        connection.disconnect()
        return length
    }

    private suspend fun downloadSingle(url: String, destFile: File, onProgress: (Int) -> Unit) {
        // Implementation for standard single-thread download
    }

    data class Chunk(val start: Long, val end: Long)
}
