package com.bugcord.manager.patcher.steps.patch

import android.os.Build
import com.bugcord.manager.R
import com.bugcord.manager.manager.PathManager
import com.bugcord.manager.patcher.StepRunner
import com.bugcord.manager.patcher.steps.StepGroup
import com.bugcord.manager.patcher.steps.base.Step
import com.bugcord.manager.patcher.steps.download.CopyDependenciesStep
import com.bugcord.manager.patcher.util.DiscordApkVerifier
import com.github.diamondminer88.zip.ZipCompression
import com.github.diamondminer88.zip.ZipReader
import com.github.diamondminer88.zip.ZipWriter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.zip.ZipFile

/**
 * Replaces Discord's bundled libdiscord.so with the one from the APK the user supplied.
 *
 * The supplied APK is untrusted, so it is signature checked before any of its native
 * code is written into the patched app.
 */
class ReplaceVoiceEngineStep(private val enginePath: String?) : Step(), KoinComponent {
    private val paths: PathManager by inject()

    override val group = StepGroup.Patch
    override val localizedName = R.string.patch_step_replace_voice_engine

    override suspend fun execute(container: StepRunner) {
        val currentDeviceArch = Build.SUPPORTED_ABIS.first()
        val apk = container.getStep<CopyDependenciesStep>().apk

        val supplied = enginePath?.let(::File)
            ?: throw IllegalStateException("No voice engine APK selected, pick one before installing")
        if (!supplied.isFile)
            throw IllegalStateException("Selected voice engine APK no longer exists: ${supplied.absolutePath}")

        container.log("Looking for libdiscord.so for $currentDeviceArch in ${supplied.name}")
        val engine = findEngineApk(supplied, currentDeviceArch, container)
            ?: throw IllegalStateException("No libdiscord.so for $currentDeviceArch in ${supplied.name}")

        container.log("Verifying Discord signature of ${engine.name}")
        DiscordApkVerifier.verifyDiscordSignature(engine)

        val libBytes = readEntry(engine, "lib/$currentDeviceArch/libdiscord.so")
            ?: throw IllegalStateException("libdiscord.so vanished from ${engine.name}")

        val apkLibPath = "lib/$currentDeviceArch/libdiscord.so"
        val existing = ZipReader(apk).use { it.entryNames.toHashSet() }

        ZipWriter(apk, true).use { zip ->
            container.log("Writing $apkLibPath (${libBytes.size} bytes); existing=${apkLibPath in existing}")
            if (apkLibPath in existing) zip.deleteEntry(apkLibPath)
            zip.writeEntry(apkLibPath, libBytes, ZipCompression.NONE)
        }
    }

    /**
     * The supplied file is either an APK that already carries the engine, or a bundle
     * whose ABI split does. Bundle members are staged on disk because they have to be verified.
     */
    private fun findEngineApk(supplied: File, abi: String, container: StepRunner): File? {
        if (readEntry(supplied, "lib/$abi/libdiscord.so") != null) return supplied

        val entryName = "lib/$abi/libdiscord.so"
        val staged = paths.patchingDownloadDir.resolve("voice-engine").apply { mkdirs() }

        return ZipFile(supplied).use { bundle ->
            bundle.entries().asSequence()
                .filter { !it.isDirectory && it.name.endsWith(".apk") }
                .mapNotNull { entry ->
                    val file = staged.resolve(entry.name.substringAfterLast('/'))
                    bundle.getInputStream(entry).use { input -> file.outputStream().use(input::copyTo) }
                    container.log("Checking bundle member ${entry.name}")
                    file.takeIf { readEntry(it, entryName) != null }
                }
                .firstOrNull()
        }
    }

    /** Reads a single ZIP entry, or null when the file is not a ZIP or lacks the entry. */
    private fun readEntry(file: File, name: String): ByteArray? = try {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry(name) ?: return null
            zip.getInputStream(entry).use { it.readBytes() }
        }
    } catch (e: Exception) {
        null
    }
}
