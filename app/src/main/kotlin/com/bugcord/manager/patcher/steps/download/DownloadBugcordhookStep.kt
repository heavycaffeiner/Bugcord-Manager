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
import com.bugcord.manager.patcher.steps.prepare.FetchInfoStep
import com.github.diamondminer88.zip.ZipReader
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Download a packaged AAR of the latest Bugcordhook build from the Bugcord maven.
 * Provides [ReorganizeDexStep] with the dex through the [IDexProvider] implementation.
 */
@Stable
class DownloadBugcordhookStep : DownloadStep<SemVer>(), IDexProvider, KoinComponent {
    private val paths: PathManager by inject()
    private val hook: BugcordhookService by inject()

    override val localizedName = R.string.patch_step_dl_bugcordhook

    override fun getRemoteUrl(container: StepRunner) =
        hook.getBugcordhookUrl(getVersion(container))

    override fun getVersion(container: StepRunner) =
        container.getStep<FetchInfoStep>().bugcordhookVersion

    override fun getStoredFile(container: StepRunner) =
        paths.cachedBugcordhookAAR(getVersion(container))

    override val dexPriority = 0
    override val dexCount = 1
    override fun getDexFiles(container: StepRunner): List<ByteArray> {
        val dexBytes = ZipReader(getStoredFile(container)).use { zip ->
            zip.openEntry("classes.dex")?.read()
                ?: throw IllegalStateException("No prebuilt classes.dex in downloaded bugcordhook build")
        }

        return listOf(dexBytes)
    }
}
