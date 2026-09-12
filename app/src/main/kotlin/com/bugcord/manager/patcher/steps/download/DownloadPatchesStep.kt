package com.bugcord.manager.patcher.steps.download

import androidx.compose.runtime.Stable
import com.bugcord.manager.R
import com.bugcord.manager.manager.PathManager
import com.bugcord.manager.network.utils.SemVer
import com.bugcord.manager.patcher.StepRunner
import com.bugcord.manager.patcher.steps.base.DownloadStep
import com.bugcord.manager.patcher.steps.base.StepState
import com.bugcord.manager.patcher.steps.prepare.FetchInfoStep
import com.bugcord.manager.ui.screens.componentopts.PatchComponent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.FileNotFoundException

/**
 * Download a zip of all the smali patches to be applied to the APK during patching.
 */
@Stable
class DownloadPatchesStep(
    private val custom: PatchComponent?,
) : DownloadStep<SemVer>(), KoinComponent {
    private val paths: PathManager by inject()

    override val localizedName = R.string.patch_step_dl_smali

    override fun getRemoteUrl(container: StepRunner) = URL

    override fun getVersion(container: StepRunner) =
        custom?.version ?: container.getStep<FetchInfoStep>().data.patchesVersion

    override fun getStoredFile(container: StepRunner) =
        custom?.getFile(paths) ?: paths.cachedSmaliPatches(getVersion(container))

    override suspend fun execute(container: StepRunner) {
        if (custom != null) {
            container.log("Using custom patches with version ${custom.version} built ${custom.timestamp}")

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
        const val URL = "https://raw.githubusercontent.com/heavycaffeiner/Bugcord/builds/patches.zip"
    }
}
