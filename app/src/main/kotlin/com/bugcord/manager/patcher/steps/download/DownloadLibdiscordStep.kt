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
import com.github.diamondminer88.zip.ZipReader
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

    /**
     * A CDN error page or the wrong split is still a non-empty file, so the engine entry is checked directly.
     */
    override suspend fun verify(container: StepRunner) {
        super.verify(container)

        val entrySize = try {
            ZipReader(getStoredFile(container)).use { split ->
                split.openEntry("lib/$currentDeviceArch/libdiscord.so")?.read()?.size
            }
        } catch (e: Exception) {
            throw IllegalStateException("Downloaded voice engine split is not a readable APK", e)
        }

        if (entrySize == null || entrySize <= 0)
            throw IllegalStateException("Voice engine split for $currentDeviceArch carries no libdiscord.so")
    }
}
