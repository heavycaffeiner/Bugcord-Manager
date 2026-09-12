package com.bugcord.manager.ui.screens.componentopts

import android.app.Application
import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.screenModelScope
import com.bugcord.manager.R
import com.bugcord.manager.manager.PathManager
import com.bugcord.manager.network.utils.SemVer
import com.bugcord.manager.ui.util.ScreenModelWithResult
import com.bugcord.manager.ui.util.ScreenResultKey
import com.bugcord.manager.util.*
import kotlinx.coroutines.launch
import kotlin.time.Instant

class ComponentOptionsModel(
    screenResultKey: ScreenResultKey,
    private val paths: PathManager,
    private val context: Application,
) : ScreenModelWithResult<PatchComponent?>(screenResultKey) {
    val components = mutableStateListOf<PatchComponent>()
    var selected by mutableStateOf<PatchComponent?>(null)
        private set

    fun selectComponent(component: PatchComponent?) {
        selected = component
    }

    fun deleteComponent(component: PatchComponent) = screenModelScope.launchIO {
        component.getFile(paths).delete()

        mainThread {
            components.remove(component)
            context.showToast(R.string.componentopts_deleted)
        }
    }

    /**
     * Loads the available imported custom components for a specified type.
     */
    suspend fun refreshComponents(type: PatchComponent.Type) {
        val files = when (type) {
            PatchComponent.Type.Injector -> paths.customInjectors()
            PatchComponent.Type.Patches -> paths.customSmaliPatches()
        }

        // ${timestamp}_${componentVersion}.${componentFile.extension}
        val componentNameRegex = """^(\d+)_(\d+\.\d+.\d+)\.\w+$""".toRegex()

        val newComponents = files.mapNotNull { file ->
            val match = componentNameRegex.find(file.name)
                ?: return@mapNotNull null
            val (_, timestamp, version) = match.groupValues

            PatchComponent(
                type = type,
                version = SemVer.parse(version),
                timestamp = Instant.fromEpochMilliseconds(timestamp.toLong()),
            )
        }.sortedByDescending { it.timestamp }

        mainThread {
            components.clear()
            components.addAll(newComponents)
        }
    }

    override fun onDispose() {
        screenModelScope.launch { setResult(selected) }
    }
}
