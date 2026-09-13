package com.bugcord.manager.ui.screens.patchopts

import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.Navigator
import com.bugcord.manager.R
import com.bugcord.manager.manager.PathManager
import com.bugcord.manager.manager.PreferencesManager
import com.bugcord.manager.ui.screens.componentopts.ComponentOptionsScreen
import com.bugcord.manager.ui.screens.componentopts.PatchComponent
import com.bugcord.manager.ui.util.pushForResult
import com.bugcord.manager.util.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.UUID

class PatchOptionsModel(
    prefilledOptions: PatchOptions,
    private val context: Context,
    private val prefs: PreferencesManager,
    private val paths: PathManager,
) : ScreenModel {
    // ---------- Package name state ----------
    var packageName by mutableStateOf(prefilledOptions.packageName)
        private set

    var packageNameState by mutableStateOf(PackageNameState.Ok)
        private set

    fun changePackageName(newPackageName: String) {
        packageName = newPackageName
        fetchPkgNameStateDebounced()
    }

    // ---------- App name state ----------
    var appName by mutableStateOf(prefilledOptions.appName)
        private set

    var appNameIsError by mutableStateOf(false)
        private set

    fun changeAppName(newAppName: String) {
        appName = newAppName
        appNameIsError = newAppName.length !in (1..150)
    }

    // ---------- Debuggable state ----------
    var debuggable by mutableStateOf(prefilledOptions.debuggable)
        private set

    fun changeDebuggable(value: Boolean) {
        debuggable = value
    }

    // ---------- Custom components state ----------
    var customInjector by mutableStateOf<PatchComponent?>(null)
        private set
    var customPatches by mutableStateOf<PatchComponent?>(null)
        private set

    // ---------- Supplied Voice Engine APK / APKM ----------
    var voiceEnginePath by mutableStateOf(prefilledOptions.voiceEnginePath)
        private set
    var replaceVoiceEngine by mutableStateOf(prefilledOptions.replaceVoiceEngine)
        private set

    fun changeReplaceVoiceEngine(value: Boolean) {
        replaceVoiceEngine = value
    }

    fun importVoiceEngine(uri: Uri) = importApk(uri, paths.voiceEngineDir, "engine") { voiceEnginePath = it }

    /**
     * Copies a picked document into app storage, since the picked URI does not survive
     * the process that receives it.
     */
    private fun importApk(uri: Uri, dir: File, prefix: String, assign: (String) -> Unit) =
        screenModelScope.launchIO {
            val target = dir.resolve("$prefix-${documentName(uri)}")
            val temp = dir.resolve("${target.name}.tmp")

            try {
                dir.mkdirs()
                target.delete()
                val opened = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Cannot open the selected file")
                opened.use { input -> temp.outputStream().use(input::copyTo) }

                if (!temp.renameTo(target)) throw IOException("Cannot store the selected APK")
                mainThread { assign(target.absolutePath) }
            } catch (e: Exception) {
                temp.delete()
                mainThread { context.showToast(R.string.patchopts_apk_import_fail) }
            }
        }

    /**
     * The name the picker showed, so the selection stays recognisable in the options screen.
     */
    private fun documentName(uri: Uri): String {
        val queried = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull()

        val cleaned = (queried ?: uri.lastPathSegment ?: "discord.apk")
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .takeLast(64)

        return if (cleaned.contains('.')) cleaned else "$cleaned.apk"
    }

    fun selectCustomInjector(navigator: Navigator) = screenModelScope.launch {
        customInjector = navigator.pushForResult(
            ComponentOptionsScreen(
                default = customInjector,
                componentType = PatchComponent.Type.Injector,
            )
        )
    }

    fun selectCustomPatches(navigator: Navigator) = screenModelScope.launch {
        customPatches = navigator.pushForResult(
            ComponentOptionsScreen(
                default = customPatches,
                componentType = PatchComponent.Type.Patches,
            )
        )
    }

    // ---------- Config generation ----------
    val isConfigValid by derivedStateOf {
        val invalidChecks = arrayOf(
            packageNameState == PackageNameState.Invalid,
            appNameIsError,
            replaceVoiceEngine && voiceEnginePath == null,
        )

        invalidChecks.none { it }
    }

    fun generateConfig(icon: PatchOptions.IconReplacement): PatchOptions {
        if (!isConfigValid) error("invalid config state")

        return PatchOptions(
            appName = appName,
            packageName = packageName,
            debuggable = debuggable,
            iconReplacement = icon,
            customInjector = customInjector,
            customPatches = customPatches,
            voiceEnginePath = voiceEnginePath,
            replaceVoiceEngine = replaceVoiceEngine,
        )
    }

    // ---------- Other ----------
    val isDevMode: Boolean
        get() = prefs.devMode

    // A throttled variant of fetchPkgNameState()
    private val fetchPkgNameStateDebounced: () -> Unit =
        screenModelScope.debounce(100L, function = ::fetchPkgNameState)

    private suspend fun fetchPkgNameState() {
        val state = if (packageName.length !in (3..150) || !PACKAGE_REGEX.matches(this.packageName)) {
            PackageNameState.Invalid
        } else {
            try {
                context.packageManager.getPackageInfo(packageName, 0)
                PackageNameState.Taken
            } catch (_: NameNotFoundException) {
                PackageNameState.Ok
            }
        }

        mainThread { packageNameState = state }
    }

    init {
        screenModelScope.launchBlock { fetchPkgNameState() }
    }

    companion object {
        private val PACKAGE_REGEX = """^[a-z]\w*(\.[a-z]\w*)+$"""
            .toRegex(RegexOption.IGNORE_CASE)
    }
}

enum class PackageNameState {
    Ok,
    Invalid,
    Taken,
}
