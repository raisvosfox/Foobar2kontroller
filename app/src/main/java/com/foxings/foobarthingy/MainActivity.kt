package com.foxings.foobarthingy

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
        val savedUrl = prefs.getString("server_url", null)
        if (savedUrl != null) {
            BeefwebClient.initialize(savedUrl)
        }
        
        enableEdgeToEdge()
        setContent {
            FoobarThingyTheme {
                var currentScreen by remember { 
                    mutableStateOf(if (BeefwebClient.isInitialized()) AppScreen.Player else AppScreen.Setup) 
                }
                
                var showPlaybackControls by remember { mutableStateOf(prefs.getBoolean("show_playback_controls", false)) }
                var showVolumeControl by remember { mutableStateOf(prefs.getBoolean("show_volume_control", false)) }
                var showSeekBar by remember { mutableStateOf(prefs.getBoolean("show_seek_bar", false)) }
                var screenSaverEnabled by remember { mutableStateOf(prefs.getBoolean("screensaver_enabled", false)) }
                
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
                                showPlaybackControls = showPlaybackControls,
                                showVolumeControl = showVolumeControl,
                                showSeekBar = showSeekBar,
                                screenSaverEnabled = screenSaverEnabled,
                                textColor = textColor
                            )
                        }
                        AppScreen.Settings -> {
                            BackHandler {
                                currentScreen = AppScreen.Player
                            }
                            SettingsScreen(
                                modifier = Modifier.padding(innerPadding),
                                showPlaybackControls = showPlaybackControls,
                                onShowPlaybackControlsChange = { 
                                    showPlaybackControls = it
                                    prefs.edit().putBoolean("show_playback_controls", it).apply()
                                },
                                showVolumeControl = showVolumeControl,
                                onShowVolumeControlChange = {
                                    showVolumeControl = it
                                    prefs.edit().putBoolean("show_volume_control", it).apply()
                                },
                                showSeekBar = showSeekBar,
                                onShowSeekBarChange = {
                                    showSeekBar = it
                                    prefs.edit().putBoolean("show_seek_bar", it).apply()
                                },
                                screenSaverEnabled = screenSaverEnabled,
                                onScreenSaverChange = {
                                    screenSaverEnabled = it
                                    prefs.edit().putBoolean("screensaver_enabled", it).apply()
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
    showPlaybackControls: Boolean,
    showVolumeControl: Boolean,
    showSeekBar: Boolean,
    screenSaverEnabled: Boolean,
    textColor: Color
) {
    var artist by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var playbackState by rememberSaveable { mutableStateOf("stopped") }
    var artworkUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    
    var position by remember { mutableDoubleStateOf(0.0) }
    var duration by remember { mutableDoubleStateOf(0.0) }
    var volume by remember { mutableDoubleStateOf(0.0) }
    var volumeMin by remember { mutableDoubleStateOf(-100.0) }
    var volumeMax by remember { mutableDoubleStateOf(0.0) }

    var localSeekPosition by remember { mutableDoubleStateOf(0.0) }
    var isSeeking by remember { mutableStateOf(false) }
    var localVolume by remember { mutableDoubleStateOf(0.0) }
    var isChangingVolume by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    var isFadingOut by remember { mutableStateOf(false) }
    var isRightAligned by remember { mutableStateOf(false) }
    val displayAlpha by animateFloatAsState(
        targetValue = if (isFadingOut) 0f else 1f,
        animationSpec = tween(durationMillis = 1000)
    )

    suspend fun refresh() {
        try {
            val response = BeefwebClient.api.getPlayerState()
            val activeItem = response.player.activeItem
            val columns = activeItem.columns
            artist = columns.getOrElse(0) { "" }
            title = columns.getOrElse(1) { "" }
            playbackState = response.player.playbackState
            artworkUrl = BeefwebClient.artworkUrl(activeItem.playlistId, activeItem.index)
            
            if (!isSeeking) {
                position = activeItem.position
                localSeekPosition = position
            }
            duration = activeItem.duration
            if (!isChangingVolume) {
                volume = response.player.volume.value
                localVolume = volume
            }
            volumeMin = response.player.volume.min
            volumeMax = response.player.volume.max
            
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

    LaunchedEffect(screenSaverEnabled) {
        if (screenSaverEnabled) {
            while (true) {
                delay(30000) // Change every 30 seconds
                isFadingOut = true
                delay(1100) // Wait for fade out
                isRightAligned = !isRightAligned
                isFadingOut = false
                delay(1100) // Wait for fade in
            }
        } else {
            isFadingOut = false
            isRightAligned = false
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

    Box(modifier = modifier.fillMaxSize().alpha(displayAlpha)) {
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                val art = @Composable {
                    AlbumArt(
                        url = artworkUrl,
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                    )
                }
                val info = @Composable {
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
                            showPlaybackControls = showPlaybackControls,
                            showVolumeControl = showVolumeControl,
                            showSeekBar = showSeekBar,
                            duration = duration,
                            volumeMin = volumeMin,
                            volumeMax = volumeMax,
                            textColor = textColor,
                            onPrevious = { sendCommand { BeefwebClient.api.previous() } },
                            onPlayPause = {
                                sendCommand {
                                    if (playbackState == "playing") BeefwebClient.api.pause()
                                    else BeefwebClient.api.play()
                                }
                            },
                            onNext = { sendCommand { BeefwebClient.api.next() } },
                            onStop = { sendCommand { BeefwebClient.api.stop() } },
                            onVolumeChange = { v -> sendCommand { BeefwebClient.api.updatePlayerState(PlayerUpdateRequest(volume = v, volumeType = "db")) } },
                            localSeekPosition = localSeekPosition,
                            onLocalSeekPositionChange = { localSeekPosition = it },
                            onSeekingStarted = { isSeeking = true },
                            onSeekingFinished = { pos ->
                                isSeeking = false
                                sendCommand { BeefwebClient.api.updatePlayerState(PlayerUpdateRequest(position = pos)) }
                            },
                            localVolume = localVolume,
                            onLocalVolumeChange = { localVolume = it },
                            onVolumeChangeStarted = { isChangingVolume = true },
                            onVolumeChangeFinished = { isChangingVolume = false }
                        )
                    }
                }

                if (isRightAligned) {
                    info()
                    art()
                } else {
                    art()
                    info()
                }
            }
        } else {
            Column(
                modifier = Modifier
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
                    showPlaybackControls = showPlaybackControls,
                    showVolumeControl = showVolumeControl,
                    showSeekBar = showSeekBar,
                    duration = duration,
                    volumeMin = volumeMin,
                    volumeMax = volumeMax,
                    textColor = textColor,
                    onPrevious = { sendCommand { BeefwebClient.api.previous() } },
                    onPlayPause = {
                        sendCommand {
                            if (playbackState == "playing") BeefwebClient.api.pause()
                            else BeefwebClient.api.play()
                        }
                    },
                    onNext = { sendCommand { BeefwebClient.api.next() } },
                    onStop = { sendCommand { BeefwebClient.api.stop() } },
                    onVolumeChange = { v -> sendCommand { BeefwebClient.api.updatePlayerState(PlayerUpdateRequest(volume = v, volumeType = "db")) } },
                    localSeekPosition = localSeekPosition,
                    onLocalSeekPositionChange = { localSeekPosition = it },
                    onSeekingStarted = { isSeeking = true },
                    onSeekingFinished = { pos ->
                        isSeeking = false
                        sendCommand { BeefwebClient.api.updatePlayerState(PlayerUpdateRequest(position = pos)) }
                    },
                    localVolume = localVolume,
                    onLocalVolumeChange = { localVolume = it },
                    onVolumeChangeStarted = { isChangingVolume = true },
                    onVolumeChangeFinished = { isChangingVolume = false }
                )
            }
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
    showPlaybackControls: Boolean,
    showVolumeControl: Boolean,
    showSeekBar: Boolean,
    duration: Double,
    volumeMin: Double,
    volumeMax: Double,
    textColor: Color,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    onVolumeChange: (Double) -> Unit,
    localSeekPosition: Double,
    onLocalSeekPositionChange: (Double) -> Unit,
    onSeekingStarted: () -> Unit,
    onSeekingFinished: (Double) -> Unit,
    localVolume: Double,
    onLocalVolumeChange: (Double) -> Unit,
    onVolumeChangeStarted: () -> Unit,
    onVolumeChangeFinished: (Double) -> Unit
) {
    // 1. Song name - Syne Mono
    Text(
        text = title.ifBlank { "Nothing playing" },
        style = MaterialTheme.typography.headlineLarge,
        fontFamily = syneMonoFamily,
        color = textColor,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
    // 2. Artist name - Syne Mono
    Text(
        text = artist,
        style = MaterialTheme.typography.headlineSmall,
        fontFamily = syneMonoFamily,
        color = textColor.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(24.dp))

    // 3. Playing state - Syne Mono
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

    if (showSeekBar && duration > 0) {
        Spacer(modifier = Modifier.height(16.dp))
        Slider(
            value = localSeekPosition.toFloat(),
            onValueChange = { 
                onSeekingStarted()
                onLocalSeekPositionChange(it.toDouble()) 
            },
            onValueChangeFinished = { 
                onSeekingFinished(localSeekPosition)
            },
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = SliderDefaults.colors(
                thumbColor = textColor,
                activeTrackColor = textColor,
                inactiveTrackColor = textColor.copy(alpha = 0.24f)
            )
        )
    }

    if (showPlaybackControls) {
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", modifier = Modifier.size(32.dp), tint = textColor)
            }
            IconButton(onClick = onPlayPause, modifier = Modifier.size(64.dp)) {
                Icon(if (playbackState == "playing") Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "P/P", modifier = Modifier.size(32.dp), tint = textColor)
            }
            IconButton(onClick = onNext, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(32.dp), tint = textColor)
            }
            IconButton(onClick = onStop, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(32.dp), tint = textColor)
            }
        }
    }

    if (showVolumeControl) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(0.8f)) {
            Text("VOL", style = MaterialTheme.typography.labelSmall, color = textColor, fontFamily = syneMonoFamily)
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = localVolume.toFloat(),
                onValueChange = { 
                    onVolumeChangeStarted()
                    onLocalVolumeChange(it.toDouble())
                    onVolumeChange(it.toDouble())
                },
                onValueChangeFinished = {
                    onVolumeChangeFinished(localVolume)
                },
                valueRange = volumeMin.toFloat()..volumeMax.toFloat(),
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = textColor,
                    activeTrackColor = textColor,
                    inactiveTrackColor = textColor.copy(alpha = 0.24f)
                )
            )
        }
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    showPlaybackControls: Boolean,
    onShowPlaybackControlsChange: (Boolean) -> Unit,
    showVolumeControl: Boolean,
    onShowVolumeControlChange: (Boolean) -> Unit,
    showSeekBar: Boolean,
    onShowSeekBarChange: (Boolean) -> Unit,
    screenSaverEnabled: Boolean,
    onScreenSaverChange: (Boolean) -> Unit,
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
    var githubVersion by remember { mutableStateOf("Loading...") }

    LaunchedEffect(Unit) {
        try {
            val release = GitHubClient.api.getLatestRelease()
            githubVersion = release.tag_name
        } catch (e: Exception) {
            githubVersion = "Unknown"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, color = currentTextColor, fontFamily = syneMonoFamily)
        Spacer(modifier = Modifier.height(24.dp))

        SettingsToggle("Playback Controls", showPlaybackControls, onShowPlaybackControlsChange, currentTextColor)
        SettingsToggle("Volume Control", showVolumeControl, onShowVolumeControlChange, currentTextColor)
        SettingsToggle("Seek Bar", showSeekBar, onShowSeekBarChange, currentTextColor)
        SettingsToggle("Screen Saver", screenSaverEnabled, onScreenSaverChange, currentTextColor)

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = currentTextColor.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        Text("Background Color (Hex)", modifier = Modifier.fillMaxWidth(), color = currentTextColor, fontFamily = syneMonoFamily)
        TextField(value = bgInput, onValueChange = { bgInput = it; if (it.length == 7 && it.startsWith("#")) onBgColorChange(it) }, modifier = Modifier.fillMaxWidth(), singleLine = true)

        Spacer(modifier = Modifier.height(16.dp))

        Text("Text Color (Hex)", modifier = Modifier.fillMaxWidth(), color = currentTextColor, fontFamily = syneMonoFamily)
        TextField(value = textInput, onValueChange = { textInput = it; if (it.length == 7 && it.startsWith("#")) onTextColorChange(it) }, modifier = Modifier.fillMaxWidth(), singleLine = true)

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = currentTextColor.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        Text("Beefweb URL", modifier = Modifier.fillMaxWidth(), color = currentTextColor, fontFamily = syneMonoFamily)
        TextField(value = urlInput, onValueChange = { urlInput = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(onClick = { onUrlUpdate(urlInput) }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Update URL", fontFamily = syneMonoFamily)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Version: $githubVersion", color = currentTextColor.copy(alpha = 0.5f), style = MaterialTheme.typography.labelMedium, fontFamily = syneMonoFamily)

        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onBack) { Text("Back to Player", fontFamily = syneMonoFamily) }
    }
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.weight(1f), color = color, fontFamily = syneMonoFamily)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
        PlayerScreen(showPlaybackControls = true, showVolumeControl = true, showSeekBar = true, screenSaverEnabled = false, textColor = Color.White)
    }
}

@Preview(showBackground = true, widthDp = 640, heightDp = 320)
@Composable
fun PlayerScreenLandscapePreview() {
    FoobarThingyTheme {
        PlayerScreen(showPlaybackControls = true, showVolumeControl = true, showSeekBar = true, screenSaverEnabled = false, textColor = Color.White)
    }
}