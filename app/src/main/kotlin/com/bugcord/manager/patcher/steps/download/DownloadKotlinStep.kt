package com.bugcord.manager.patcher.steps.download

import androidx.compose.runtime.Stable
import com.bugcord.manager.R
import com.bugcord.manager.manager.PathManager
import com.bugcord.manager.network.utils.SemVer
import com.bugcord.manager.patcher.StepRunner
import com.bugcord.manager.patcher.steps.base.DownloadStep
import com.bugcord.manager.patcher.steps.base.IDexProvider
import com.bugcord.manager.patcher.steps.patch.ReorganizeDexStep
import com.bugcord.manager.patcher.steps.prepare.FetchInfoStep
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Download the most recent available Kotlin stdlib build that is supported.
 * Provides [ReorganizeDexStep] with the dex through the [IDexProvider] implementation.
 */
@Stable
class DownloadKotlinStep : DownloadStep<SemVer>(), IDexProvider, KoinComponent {
    private val paths: PathManager by inject()

    override val localizedName = R.string.patch_step_dl_kotlin

    override fun getRemoteUrl(container: StepRunner) = URL

    override fun getVersion(container: StepRunner) =
        container.getStep<FetchInfoStep>().data.kotlinVersion

    override fun getStoredFile(container: StepRunner) =
        paths.cachedKotlinDex(getVersion(container))

    override val dexCount = 1
    override val dexPriority = -1
    override fun getDexFiles(container: StepRunner) =
        listOf(getStoredFile(container).readBytes())

    private companion object {
        const val URL = "https://raw.githubusercontent.com/heavycaffeiner/Bugcord/builds/kotlin.dex"
    }
}
