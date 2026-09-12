package com.bugcord.manager.patcher.steps.patch

import android.os.Build
import com.bugcord.manager.R
import com.bugcord.manager.patcher.StepRunner
import com.bugcord.manager.patcher.steps.StepGroup
import com.bugcord.manager.patcher.steps.base.IDexProvider
import com.bugcord.manager.patcher.steps.base.Step
import com.bugcord.manager.patcher.steps.download.CopyDependenciesStep
import com.bugcord.manager.patcher.steps.download.DownloadBugcordhookStep
import com.github.diamondminer88.zip.*
import org.koin.core.component.KoinComponent

/**
 * Add the Bugcordhook library's native libs.
 * The dex is handled by [ReorganizeDexStep] through the [IDexProvider] implementation of [DownloadBugcordhookStep].
 */
class AddBugcordhookLibsStep : Step(), KoinComponent {
    override val group = StepGroup.Patch
    override val localizedName = R.string.patch_step_add_bugcordhook

    override suspend fun execute(container: StepRunner) {
        val currentDeviceArch = Build.SUPPORTED_ABIS.first()
        val apk = container.getStep<CopyDependenciesStep>().apk
        val bugcordhook = container.getStep<DownloadBugcordhookStep>().getStoredFile(container)

        ZipWriter(apk, /* append = */ true).use { patchedApk ->
            ZipReader(bugcordhook).use { bugcordhook ->
                for (libFile in arrayOf("libbugcordhook.so", "libc++_shared.so", "liblsplant.so")) {
                    container.log("Reading bugcordhook lib $libFile with arch $currentDeviceArch")

                    val apkLibPath = "lib/$currentDeviceArch/$libFile"
                    val libBytes = bugcordhook.openEntry("jni/$currentDeviceArch/$libFile")?.read()
                        ?: throw IllegalStateException("Failed to read $libFile from bugcordhook aar")

                    container.log("Writing to $apkLibPath in APK unaligned uncompressed")
                    patchedApk.writeEntry(apkLibPath, libBytes, ZipCompression.NONE)
                }
            }
        }
    }
}
