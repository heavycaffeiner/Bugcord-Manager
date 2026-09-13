package com.bugcord.manager.patcher.steps.download

import androidx.compose.runtime.Stable
import com.bugcord.manager.R
import com.bugcord.manager.manager.PathManager
import com.bugcord.manager.network.utils.SemVer
import com.bugcord.manager.patcher.StepRunner
import com.bugcord.manager.patcher.steps.base.*
import com.bugcord.manager.patcher.steps.patch.ReorganizeDexStep
import com.bugcord.manager.patcher.steps.prepare.FetchInfoStep
import com.bugcord.manager.ui.screens.componentopts.PatchComponent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.FileNotFoundException

/**
 * Download a compiled dex file to be injected into the APK as the first `classes.dex` to override an entry point class.
 * Provides [ReorganizeDexStep] with the dex through the [IDexProvider] implementation.
 */
@Stable
class DownloadInjectorStep(
    private val custom: PatchComponent?,
) : DownloadStep<SemVer>(), IDexProvider, KoinComponent {
    private val paths: PathManager by inject()

    override val localizedName = R.string.patch_step_dl_injector

    override fun getRemoteUrl(container: StepRunner) = URL

    override fun getVersion(container: StepRunner) =
        custom?.version ?: container.getStep<FetchInfoStep>().data.injectorVersion

    override fun getStoredFile(container: StepRunner) =
        custom?.getFile(paths) ?: paths.cachedInjector(getVersion(container))

    override val dexCount = 1
    override val dexPriority = 3
    override fun getDexFiles(container: StepRunner) =
        listOf(getStoredFile(container).readBytes())

    override suspend fun execute(container: StepRunner) {
        if (custom != null) {
            container.log("Using custom injector with version ${custom.version} built ${custom.timestamp}")

            if (!custom.getFile(paths).exists()) {
                throw FileNotFoundException(
                    "Selected custom component does not exist on disk! If this is an update, " +
                        "updates cannot occur when the originally selected custom component has been deleted."
                )
            }

            state = StepState.Skipped
            return
        }

        super.execute(container)
    }

    private companion object {
        const val URL = "https://raw.githubusercontent.com/thirdscam/Bugcord/builds/Injector.dex"
    }
}
