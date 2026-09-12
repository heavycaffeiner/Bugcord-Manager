package com.bugcord.manager.patcher

import com.bugcord.manager.patcher.steps.download.*
import com.bugcord.manager.patcher.steps.install.*
import com.bugcord.manager.patcher.steps.patch.*
import com.bugcord.manager.patcher.steps.prepare.*
import com.bugcord.manager.ui.screens.patchopts.PatchOptions
import kotlinx.collections.immutable.persistentListOf

/**
 * Used for installing the old Kotlin Discord app.
 */
class KotlinPatchRunner(
    options: PatchOptions,
) : StepRunner() {
    override val steps = persistentListOf(
        // Prepare
        FetchInfoStep(),
        DowngradeCheckStep(options),
        RestoreDownloadsStep(),

        // Download
        DownloadDiscordStep(),
        DownloadInjectorStep(options.customInjector),
        DownloadBugcordhookStep(),
        DownloadKotlinStep(),
        DownloadPatchesStep(options.customPatches),
        CopyDependenciesStep(),

        // Patch
        SmaliPatchStep(),
        PatchIconsStep(options),
        PatchManifestStep(options),
        PatchCertsStep(),
        ReorganizeDexStep(),
        AddBugcordhookLibsStep(),
        SaveMetadataStep(options),

        // Install
        AlignmentStep(),
        SigningStep(options),
        InstallStep(options),
        CleanupStep(),
    )
}
