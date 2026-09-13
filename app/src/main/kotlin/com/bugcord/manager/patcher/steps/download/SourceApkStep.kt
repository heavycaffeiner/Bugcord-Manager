package com.bugcord.manager.patcher.steps.download

import android.app.Application
import com.bugcord.manager.R
import com.bugcord.manager.manager.PathManager
import com.bugcord.manager.patcher.StepRunner
import com.bugcord.manager.patcher.steps.StepGroup
import com.bugcord.manager.patcher.steps.base.Step
import com.bugcord.manager.patcher.util.DiscordApkVerifier
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * Takes the Discord APK the user supplied and stages it as the patch target.
 * The APK is untrusted input, so its build and signature are checked first.
 */
class SourceApkStep(private val sourcePath: String?) : Step(), KoinComponent {
    private val paths: PathManager by inject()
    private val application: Application by inject()

    override val group = StepGroup.Download
    override val localizedName = R.string.patch_step_use_source_apk

    /**
     * Staged copy of the supplied APK, read by the step that copies it for patching.
     */
    lateinit var storedFile: File
        private set

    override suspend fun execute(container: StepRunner) {
        val source = sourcePath?.let(::File)
            ?: throw IllegalStateException("No Discord APK selected, pick one before installing")
        if (!source.isFile)
            throw IllegalStateException("Selected Discord APK no longer exists: ${source.absolutePath}")

        container.log("Reading supplied Discord APK ${source.absolutePath}")

        val info = application.packageManager.getPackageArchiveInfo(source.absolutePath, 0)
            ?: throw IllegalStateException("Selected file is not an APK")
        if (info.versionCode != SUPPORTED_DISCORD_VERSION)
            throw IllegalStateException(
                "Discord ${info.versionName} is not supported, install needs version $SUPPORTED_VERSION_NAME"
            )

        container.log("Verifying Discord signature")
        DiscordApkVerifier.verifyDiscordSignature(source)

        storedFile = paths.cachedDiscordApk(SUPPORTED_DISCORD_VERSION)
        storedFile.parentFile!!.mkdirs()
        source.copyTo(storedFile, overwrite = true)
    }

    private companion object {
        /** Last Discord version before the React Native rewrite. */
        const val SUPPORTED_DISCORD_VERSION = 126021
        const val SUPPORTED_VERSION_NAME = "126.21"
    }
}
