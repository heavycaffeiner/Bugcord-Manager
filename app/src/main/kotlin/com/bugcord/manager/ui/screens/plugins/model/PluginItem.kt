package com.bugcord.manager.ui.screens.plugins.model

import androidx.compose.runtime.*

@Stable
data class PluginItem(
    val manifest: PluginManifest,
    val path: String,
) {
    // Plugins are enabled by default unless disabled in Bugcord settings
    var enabled by mutableStateOf(true)
}
