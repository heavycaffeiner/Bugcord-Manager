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
import java.io.InputStream
import java.util.zip.ZipFile

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
        val supplied = sourcePath?.let(::File)
            ?: throw IllegalStateException("No Discord APK selected, pick one before installing")
        if (!supplied.isFile)
            throw IllegalStateException("Selected Discord APK no longer exists: ${supplied.absolutePath}")

        container.log("Reading supplied Discord APK ${supplied.name}")

        val base = findBaseApk(supplied, container)
            ?: throw IllegalStateException(
                "No Discord $SUPPORTED_VERSION_NAME APK in ${supplied.name}"
            )

        container.log("Verifying Discord signature")
        DiscordApkVerifier.verifyDiscordSignature(base)

        storedFile = paths.cachedDiscordApk(SUPPORTED_DISCORD_VERSION)
        storedFile.parentFile!!.mkdirs()
        base.copyTo(storedFile, overwrite = true)
    }

    /**
     * The supplied file is either the APK itself or a bundle whose base member is the target.
     * Members are staged on disk before they are read, since they have to be verified.
     */
    private fun findBaseApk(supplied: File, container: StepRunner): File? {
        if (versionCodeOf(supplied) == SUPPORTED_DISCORD_VERSION) return supplied

        val staged = paths.patchingDownloadDir.resolve("source-apk").apply { mkdirs() }

        ZipFile(supplied).use { bundle ->
            for (entry in bundle.entries()) {
                if (entry.isDirectory || !entry.name.endsWith(".apk")) continue

                val file = staged.resolve(entry.name.substringAfterLast('/'))
                container.log("Checking bundle member ${entry.name}")
                bundle.getInputStream(entry).use { input -> writeBounded(input, file, MAX_MEMBER_BYTES) }

                if (versionCodeOf(file) == SUPPORTED_DISCORD_VERSION) return file
                file.delete()
            }
        }

        return null
    }

    private fun versionCodeOf(apk: File): Int? =
        application.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)?.versionCode

    private fun writeBounded(input: InputStream, target: File, limit: Long) {
        target.outputStream().use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var written = 0L

            while (true) {
                val read = input.read(buffer)
                if (read < 0) break

                written += read
                if (written > limit) {
                    target.delete()
                    throw IllegalStateException("Archive member is larger than expected")
                }

                output.write(buffer, 0, read)
            }
        }
    }

    private companion object {
        /** Last Discord version before the React Native rewrite. */
        const val SUPPORTED_DISCORD_VERSION = 126021
        const val SUPPORTED_VERSION_NAME = "126.21"
        /** Bundle members are APKs of a few hundred MiB at most. */
        const val MAX_MEMBER_BYTES = 512L * 1024 * 1024
    }
}
