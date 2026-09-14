package com.bugcord.manager.patcher.steps.patch

import com.bugcord.manager.BuildConfig
import com.bugcord.manager.R
import com.bugcord.manager.network.utils.SemVer
import com.bugcord.manager.patcher.InstallMetadata
import com.bugcord.manager.patcher.StepRunner
import com.bugcord.manager.patcher.steps.StepGroup
import com.bugcord.manager.patcher.steps.base.Step
import com.bugcord.manager.patcher.steps.download.*
import com.bugcord.manager.ui.screens.patchopts.PatchOptions
import com.github.diamondminer88.zip.ZipWriter
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Store the install options and additional data inside the APK for future use,
 * for example checking what library versions were used, or performing "updates" while
 * maintaining the same install options as what was used upon first install.
 */
class SaveMetadataStep(private val options: PatchOptions) : Step(), KoinComponent {
    private val json: Json by inject()

    override val group = StepGroup.Patch
    override val localizedName = R.string.patch_step_save_metadata

    override suspend fun execute(container: StepRunner) {
        val apk = container.getStep<CopyDependenciesStep>().apk
        val bugcordhook = container.getStep<DownloadBugcordhookStep>()
        val injector = container.getStep<DownloadInjectorStep>()
        val patches = container.getStep<DownloadPatchesStep>()
        val kotlin = container.getStep<DownloadKotlinStep>()
        val info = container.getStep<com.bugcord.manager.patcher.steps.prepare.FetchInfoStep>()

        val metadata = InstallMetadata(
            customManager = !BuildConfig.RELEASE,
            managerVersion = SemVer.parse(BuildConfig.VERSION_NAME),
            bugcordhookVersion = bugcordhook.getVersion(container),
            injectorVersion = injector.getVersion(container),
            patchesVersion = patches.getVersion(container),
            kotlinVersion = kotlin.getVersion(container),
            coreVersion = info.data.coreVersion,
            options = options,
        )


        container.log("Writing serialized install metadata to APK")
        ZipWriter(apk, /* append = */ true).use {
            it.writeEntry("bugcord.json", json.encodeToString<InstallMetadata>(metadata))
        }
    }
}
