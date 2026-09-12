package com.bugcord.manager.patcher.steps.download

import androidx.compose.runtime.Stable
import com.bugcord.manager.R
import com.bugcord.manager.manager.PathManager
import com.bugcord.manager.network.services.BugcordhookService
import com.bugcord.manager.network.utils.SemVer
import com.bugcord.manager.patcher.StepRunner
import com.bugcord.manager.patcher.steps.base.DownloadStep
import com.bugcord.manager.patcher.steps.base.IDexProvider
import com.bugcord.manager.patcher.steps.patch.ReorganizeDexStep
import com.bugcord.manager.patcher.steps.patch.ReplaceLibdiscordStep
import com.bugcord.manager.patcher.steps.prepare.FetchInfoStep
import com.github.diamondminer88.zip.ZipReader
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Download a packaged AAR of the latest BugcordVoice build.
 * The AAR carries the prebuilt webrtc.dex (org.webrtc). libdiscord.so is sourced
 * separately by [ReplaceLibdiscordStep] from the Discord split APK.
 * Provides [ReorganizeDexStep] with the dex through the [IDexProvider] implementation.
 */
@Stable
class DownloadBugcordvoiceStep : DownloadStep<SemVer>(), IDexProvider, KoinComponent {
    private val paths: PathManager by inject()
    private val hook: BugcordhookService by inject()

    override val localizedName = R.string.patch_step_dl_bugcordvoice

    override fun getRemoteUrl(container: StepRunner) = hook.getBugcordvoiceUrl()

    override fun getVersion(container: StepRunner) =
        container.getStep<FetchInfoStep>().bugcordvoiceVersion

    override fun getStoredFile(container: StepRunner) =
        paths.cachedBugcordvoiceAAR(getVersion(container))

    override val dexPriority = 1
    override val dexCount get() = dexFiles?.size ?: 1

    // R8/d8 may split across webrtc.dex + classes*.dex
    private var dexFiles: List<ByteArray>? = null

    override suspend fun execute(container: StepRunner) {
        super.execute(container)

        dexFiles = ZipReader(getStoredFile(container)).use { aar ->
            aar.entryNames
                .filter { !it.contains('/') && it.endsWith(".dex") }
                .sorted()
                .map { name ->
                    aar.openEntry(name)?.read()
                        ?: throw IllegalStateException("Failed to read $name from bugcordvoice aar")
                }
        }

        if (dexFiles!!.isEmpty())
            throw IllegalStateException("No prebuilt dex files in downloaded bugcordvoice build")
    }

    override fun getDexFiles(container: StepRunner): List<ByteArray> =
        dexFiles ?: throw IllegalStateException("Bugcordvoice dex files not loaded, download step likely failed")
}
