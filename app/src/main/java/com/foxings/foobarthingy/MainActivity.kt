package com.foxings.foobarthingy

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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

enum class AppScreen {
    Setup, Player, Settings
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        val prefs = getSharedPreferences("beefweb_prefs", MODE_PRIVATE)
        
        enableEdgeToEdge()
        setContent {
            FoobarThingyTheme {
                var currentScreen by remember { 
                    mutableStateOf(if (BeefwebClient.isInitialized()) AppScreen.Player else AppScreen.Setup) 
                }
                
                var showControls by remember { mutableStateOf(prefs.getBoolean("show_controls", false)) }
                var bgColorHex by remember { mutableStateOf(prefs.getString("bg_color", "#000000") ?: "#000000") }
                var textColorHex by remember { mutableStateOf(prefs.getString("text_color", "#FFFFFF") ?: "#FFFFFF") }
                
                val bgColor = remember(bgColorHex) { 
                    try { Color(android.graphics.Color.parseColor(bgColorHex)) } catch (e: Exception) { Color.Black } 
                }
                val textColor = remember(textColorHex) { 
                    try { Color(android.graphics.Color.parseColor(textColorHex)) } catch (e: Exception) { Color.White } 
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = bgColor
                ) { innerPadding ->
                    when (currentScreen) {
                        AppScreen.Setup -> {
                            SetupScreen(
                                modifier = Modifier.padding(innerPadding),
                                onUrlSet = { url ->
                                    BeefwebClient.initialize(url)
                                    prefs.edit().putString("server_url", url).apply()
                                    currentScreen = AppScreen.Player
                                }
                            )
                        }
                        AppScreen.Player -> {
                            BackHandler {
                                currentScreen = AppScreen.Settings
                            }
                            PlayerScreen(
                                modifier = Modifier.padding(innerPadding),
                                showControls = showControls,
                                textColor = textColor
                            )
                        }
                        AppScreen.Settings -> {
                            BackHandler {
                                currentScreen = AppScreen.Player
                            }
                            SettingsScreen(
                                modifier = Modifier.padding(innerPadding),
                                showControls = showControls,
                                onShowControlsChange = { 
                                    showControls = it
                                    prefs.edit().putBoolean("show_controls", it).apply()
                                },
                                bgColorHex = bgColorHex,
                                onBgColorChange = {
                                    bgColorHex = it
                                    prefs.edit().putString("bg_color", it).apply()
                                },
                                textColorHex = textColorHex,
                                onTextColorChange = {
                                    textColorHex = it
                                    prefs.edit().putString("text_color", it).apply()
                                },
                                serverUrl = prefs.getString("server_url", "") ?: "",
                                onUrlUpdate = { url ->
                                    BeefwebClient.initialize(url)
                                    prefs.edit().putString("server_url", url).apply()
                                },
                                onBack = { currentScreen = AppScreen.Player },
                                currentTextColor = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    showControls: Boolean,
    textColor: Color
) {
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
                    errorText = errorText,
                    showControls = showControls,
                    textColor = textColor,
                    onPrevious = { sendCommand { BeefwebClient.api.previous() } },
                    onPlayPause = {
                        sendCommand {
                            if (playbackState == "playing") BeefwebClient.api.pause()
                            else BeefwebClient.api.play()
                        }
                    },
                    onNext = { sendCommand { BeefwebClient.api.next() } },
                    onStop = { sendCommand { BeefwebClient.api.stop() } }
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
                errorText = errorText,
                showControls = showControls,
                textColor = textColor,
                onPrevious = { sendCommand { BeefwebClient.api.previous() } },
                onPlayPause = {
                    sendCommand {
                        if (playbackState == "playing") BeefwebClient.api.pause()
                        else BeefwebClient.api.play()
                    }
                },
                onNext = { sendCommand { BeefwebClient.api.next() } },
                onStop = { sendCommand { BeefwebClient.api.stop() } }
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
    errorText: String?,
    showControls: Boolean,
    textColor: Color,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit
) {
    // 1. Song name - Syne Mono, Larger and White
    Text(
        text = title.ifBlank { "Nothing playing" },
        style = MaterialTheme.typography.headlineLarge,
        fontFamily = syneMonoFamily,
        color = textColor,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
    // 2. Artist name - Syne Mono, White
    Text(
        text = artist,
        style = MaterialTheme.typography.headlineSmall,
        fontFamily = syneMonoFamily,
        color = textColor.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(24.dp))

    // 3. Playing state - Syne Mono, White
    Text(
        text = "State: $playbackState",
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = syneMonoFamily,
        color = textColor.copy(alpha = 0.5f)
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

    if (showControls) {
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.size(32.dp),
                    tint = textColor
                )
            }
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    if (playbackState == "playing") Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(32.dp),
                    tint = textColor
                )
            }
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.size(32.dp),
                    tint = textColor
                )
            }
            IconButton(
                onClick = onStop,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "Stop",
                    modifier = Modifier.size(32.dp),
                    tint = textColor
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    showControls: Boolean,
    onShowControlsChange: (Boolean) -> Unit,
    bgColorHex: String,
    onBgColorChange: (String) -> Unit,
    textColorHex: String,
    onTextColorChange: (String) -> Unit,
    serverUrl: String,
    onUrlUpdate: (String) -> Unit,
    onBack: () -> Unit,
    currentTextColor: Color
) {
    var urlInput by remember { mutableStateOf(serverUrl) }
    var bgInput by remember { mutableStateOf(bgColorHex) }
    var textInput by remember { mutableStateOf(textColorHex) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = currentTextColor,
            fontFamily = syneMonoFamily
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Show Playback Controls",
                modifier = Modifier.weight(1f),
                color = currentTextColor,
                fontFamily = syneMonoFamily
            )
            Switch(checked = showControls, onCheckedChange = onShowControlsChange)
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = currentTextColor.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Background Color (Hex)",
            modifier = Modifier.fillMaxWidth(),
            color = currentTextColor,
            fontFamily = syneMonoFamily
        )
        TextField(
            value = bgInput,
            onValueChange = { 
                bgInput = it
                if (it.length == 7 && it.startsWith("#")) onBgColorChange(it)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Text Color (Hex)",
            modifier = Modifier.fillMaxWidth(),
            color = currentTextColor,
            fontFamily = syneMonoFamily
        )
        TextField(
            value = textInput,
            onValueChange = { 
                textInput = it
                if (it.length == 7 && it.startsWith("#")) onTextColorChange(it)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = currentTextColor.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Beefweb URL",
            modifier = Modifier.fillMaxWidth(),
            color = currentTextColor,
            fontFamily = syneMonoFamily
        )
        TextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = { onUrlUpdate(urlInput) },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Update URL", fontFamily = syneMonoFamily)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onBack) {
            Text("Back to Player", fontFamily = syneMonoFamily)
        }
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
        PlayerScreen(showControls = true, textColor = Color.White)
    }
}

@Preview(showBackground = true, widthDp = 640, heightDp = 320)
@Composable
fun PlayerScreenLandscapePreview() {
    FoobarThingyTheme {
        PlayerScreen(showControls = true, textColor = Color.White)
    }
}