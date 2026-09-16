package com.foxings.foobarthingy

// PlayerModels.kt

data class PlayerResponse(
    val player: PlayerState
)

data class PlayerState(
    val activeItem: ActiveItem,
    val info: PlayerInfo,
    val permissions: Permissions,
    val playbackMode: Int,
    val playbackModes: List<String>,
    val playbackState: String,   // "playing" | "paused" | "stopped"
    val volume: Volume
)

data class ActiveItem(
    val columns: List<String>,   // populated only if you request them via ?columns=
    val duration: Double,        // seconds
    val index: Int,
    val playlistId: String,
    val playlistIndex: Int,
    val position: Double         // seconds, current playback position
)

data class PlayerInfo(
    val name: String,
    val pluginVersion: String,
    val title: String,
    val version: String
)

data class Permissions(
    val changeClientConfig: Boolean,
    val changeOutput: Boolean,
    val changePlaylists: Boolean
)

data class Volume(
    val isMuted: Boolean,
    val max: Double,   // 0.0
    val min: Double,   // -100.0
    val type: String,  // "db"
    val value: Double
)
