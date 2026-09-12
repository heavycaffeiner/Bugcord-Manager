package com.bugcord.manager.patcher.steps.download

import android.os.Build
import androidx.compose.runtime.Stable
import com.bugcord.manager.R
import com.bugcord.manager.manager.PathManager
import com.bugcord.manager.network.services.BugcordhookService
import com.bugcord.manager.patcher.StepRunner
import com.bugcord.manager.patcher.steps.base.DownloadStep
import com.bugcord.manager.patcher.steps.patch.ReplaceLibdiscordStep
import com.bugcord.manager.patcher.steps.prepare.FetchInfoStep
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Download the Discord split APK matching the device's primary ABI, which carries the
 * libdiscord.so used as the voice engine (consumed by [ReplaceLibdiscordStep]).
 */
@Stable
class DownloadLibdiscordStep : DownloadStep<Int>(), KoinComponent {
    private val paths: PathManager by inject()
    private val hook: BugcordhookService by inject()

    val currentDeviceArch: String = Build.SUPPORTED_ABIS.first()

    override val localizedName = R.string.patch_step_dl_libdiscord

    override fun getVersion(container: StepRunner) =
        container.getStep<FetchInfoStep>().libdiscordVersion

    override fun getRemoteUrl(container: StepRunner) =
        hook.getLibraryApkUrl(getVersion(container), currentDeviceArch)

    override fun getStoredFile(container: StepRunner) =
        paths.cachedLibdiscordApk(getVersion(container), currentDeviceArch)
}
