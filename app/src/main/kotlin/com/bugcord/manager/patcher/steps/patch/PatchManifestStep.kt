package com.bugcord.manager.patcher.steps.patch

import com.bugcord.manager.R
import com.bugcord.manager.patcher.StepRunner
import com.bugcord.manager.patcher.steps.StepGroup
import com.bugcord.manager.patcher.steps.base.Step
import com.bugcord.manager.patcher.steps.download.CopyDependenciesStep
import com.bugcord.manager.patcher.util.ManifestPatcher
import com.bugcord.manager.ui.screens.patchopts.PatchOptions
import com.github.diamondminer88.zip.ZipReader
import com.github.diamondminer88.zip.ZipWriter

/**
 * Patch the APK's AndroidManifest.xml
 */
class PatchManifestStep(private val options: PatchOptions) : Step() {
    override val group = StepGroup.Patch
    override val localizedName = R.string.patch_step_patch_manifests

    override suspend fun execute(container: StepRunner) {
        val apk = container.getStep<CopyDependenciesStep>().apk

        container.log("Reading manifest from apk")
        val manifest = ZipReader(apk)
            .use { zip -> zip.openEntry("AndroidManifest.xml")?.read() }
            ?: throw IllegalArgumentException("No manifest found in APK")

        container.log("Patching manifest")
        val patchedManifest = ManifestPatcher.patchManifest(
            manifestBytes = manifest,
            packageName = options.packageName,
            appName = options.appName,
            debuggable = options.debuggable,
        )

        container.log("Writing patched manifest to apk unaligned compressed")
        ZipWriter(apk, /* append = */ true).use {
            it.deleteEntry("AndroidManifest.xml")
            it.writeEntry("AndroidManifest.xml", patchedManifest)
        }
    }
}
