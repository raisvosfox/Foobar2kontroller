package com.foxings.foobarthingy

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.foxings.foobarthingy.ui.theme.FoobarThingyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Random

val syneMonoFamily = FontFamily(
    Font(R.font.synemono)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        val prefs = getSharedPreferences("beefweb_prefs", MODE_PRIVATE)
        val savedUrl = prefs.getString("server_url", null)
        if (savedUrl != null) {
            BeefwebClient.initialize(savedUrl)
        }
        
        enableEdgeToEdge()
        setContent {
            FoobarThingyTheme {
                var isInitialized by remember { mutableStateOf(BeefwebClient.isInitialized()) }
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF000000)
                ) { innerPadding ->
                    if (!isInitialized) {
                        SetupScreen(
                            modifier = Modifier.padding(innerPadding),
                            onUrlSet = { url ->
                                BeefwebClient.initialize(url)
                                prefs.edit().putString("server_url", url).apply()
                                isInitialized = true
                            }
                        )
                    } else {
                        PlayerScreen(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(modifier: Modifier = Modifier) {
    var artist by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var playbackState by rememberSaveable { mutableStateOf("stopped") }
    var artworkUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        try {
            val response = BeefwebClient.api.getPlayerState()
            val activeItem = response.player.activeItem
            val columns = activeItem.columns
            artist = columns.getOrElse(0) { "" }
            title = columns.getOrElse(1) { "" }
            playbackState = response.player.playbackState
            artworkUrl = BeefwebClient.artworkUrl(activeItem.playlistId, activeItem.index)
            errorText = null
        } catch (e: Exception) {
            errorText = e.message
        }
    }

    fun sendCommand(action: suspend () -> Unit) {
        scope.launch {
            try {
                action()
                refresh()
            } catch (e: Exception) {
                errorText = e.message
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            delay(1000)
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val view = LocalView.current

    LaunchedEffect(isLandscape) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        if (isLandscape) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    if (isLandscape) {
        Row(modifier = modifier.fillMaxSize()) {
            AlbumArt(
                url = artworkUrl,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                InfoAndControls(
                    title = title,
                    artist = artist,
                    playbackState = playbackState,
                    errorText = errorText
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AlbumArt(
                url = artworkUrl,
                modifier = Modifier.size(280.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            InfoAndControls(
                title = title,
                artist = artist,
                playbackState = playbackState,
                errorText = errorText
            )
        }
    }
}

@Composable
fun AlbumArt(url: String?, modifier: Modifier = Modifier) {
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = "Album art",
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun InfoAndControls(
    title: String,
    artist: String,
    playbackState: String,
    errorText: String?
) {
    // 1. Song name - Syne Mono, Larger and White
    Text(
        text = title.ifBlank { "Nothing playing" },
        style = MaterialTheme.typography.headlineLarge,
        fontFamily = syneMonoFamily,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
    // 2. Artist name - Syne Mono, White
    Text(
        text = artist,
        style = MaterialTheme.typography.headlineSmall,
        fontFamily = syneMonoFamily,
        color = Color.White.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(24.dp))

    // 3. Playing state - Syne Mono, White
    Text(
        text = "State: $playbackState",
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = syneMonoFamily,
        color = Color.White.copy(alpha = 0.5f)
    )

    errorText?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Error: $it",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = syneMonoFamily
        )
    }
}

@Composable
fun SetupScreen(
    modifier: Modifier = Modifier,
    onUrlSet: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Enter Beefweb URL",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontFamily = syneMonoFamily
        )
        Text(
            text = "Example: 192.168.1.100:8880",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            fontFamily = syneMonoFamily
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("http://192.168.x.x:8880") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { if (url.isNotBlank()) onUrlSet(url) },
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("Connect", fontFamily = syneMonoFamily)
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 640)
@Composable
fun PlayerScreenPortraitPreview() {
    FoobarThingyTheme {
        PlayerScreen()
    }
}

@Preview(showBackground = true, widthDp = 640, heightDp = 320)
@Composable
fun PlayerScreenLandscapePreview() {
    FoobarThingyTheme {
        PlayerScreen()
    }
}