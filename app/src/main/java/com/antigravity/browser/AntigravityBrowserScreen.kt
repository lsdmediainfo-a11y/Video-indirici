package com.antigravity.browser

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.antigravity.resolver.Level1Network
import com.antigravity.resolver.MediaResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntigravityBrowserScreen() {
    val context = LocalContext.current
    val geckoRuntime = remember { GeckoRuntime.create(context) }
    
    val level1Network = remember { Level1Network() }
    var detectedMedia by remember { mutableStateOf<MediaResult?>(null) }

    LaunchedEffect(Unit) {
        level1Network.mediaFlow.collect { media ->
            detectedMedia = media
        }
    }

    val geckoSession = remember { 
        GeckoSession().apply {
            open(geckoRuntime)
            // Attach the sniffer delegate
            navigationDelegate = level1Network.navigationDelegate
            // Open a test page with an HLS stream
            loadUri("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8") 
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Antigravity Browser") })
        },
        floatingActionButton = {
            if (detectedMedia != null) {
                FloatingActionButton(
                    onClick = {
                        // Open Download Options Dialog (Phase 3)
                    }
                ) {
                    Text("↓ DL")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    GeckoView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setSession(geckoSession)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Status text at the bottom
            if (detectedMedia != null) {
                Text(
                    text = "Medya Bulundu: ${detectedMedia!!.type}",
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
                )
            }
        }
    }
}
