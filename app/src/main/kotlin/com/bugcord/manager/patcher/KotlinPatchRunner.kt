package com.bugcord.manager.patcher

import com.bugcord.manager.patcher.steps.download.*
import com.bugcord.manager.patcher.steps.install.*
import com.bugcord.manager.patcher.steps.patch.*
import com.bugcord.manager.patcher.steps.prepare.*
import com.bugcord.manager.ui.screens.patchopts.PatchOptions
import kotlinx.collections.immutable.toPersistentList

/**
 * Used for installing the old Kotlin Discord app.
 */
class KotlinPatchRunner(
    options: PatchOptions,
) : StepRunner() {
    override val steps = buildList {
        // Prepare
        add(FetchInfoStep())
        add(DowngradeCheckStep(options))
        add(RestoreDownloadsStep())

        // Download
        add(DownloadDiscordStep())
        add(DownloadInjectorStep(options.customInjector))
        add(DownloadBugcordhookStep())
        add(DownloadKotlinStep())
        add(DownloadBugcordvoiceStep())
        add(DownloadPatchesStep(options.customPatches))
        add(CopyDependenciesStep())

        // Patch
        add(SmaliPatchStep())
        add(PatchIconsStep(options))
        add(PatchManifestStep(options))
        add(PatchCertsStep())
        add(ReorganizeDexStep())
        add(AddBugcordhookLibsStep())
        if (options.replaceVoiceEngine) add(ReplaceVoiceEngineStep(options.voiceEnginePath))
        add(SaveMetadataStep(options))

        // Install
        add(AlignmentStep())
        add(SigningStep(options))
        add(InstallStep(options))
        add(CleanupStep())
    }.toPersistentList()
}
