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
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * Replaces Discord's bundled libdiscord.so with the one from the APK the user supplied.
 *
 * The supplied file is untrusted, so it is inspected through ZIP metadata only, every read is
 * size bounded, and the APK a library is taken from is signature checked before it is read.
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

        val libPath = "lib/$currentDeviceArch/libdiscord.so"
        container.log("Looking for $libPath in ${supplied.name}")

        val engine = locateEngineApk(supplied, libPath, container)
            ?: throw IllegalStateException("No $libPath in ${supplied.name}")

        container.log("Verifying Discord signature of ${engine.name}")
        DiscordApkVerifier.verifyDiscordSignature(engine)

        val libBytes = readBounded(engine, libPath, MAX_LIBRARY_BYTES)
            ?: throw IllegalStateException("$libPath vanished from ${engine.name}")

        val existing = ZipReader(apk).use { it.entryNames.toHashSet() }

        ZipWriter(apk, true).use { zip ->
            container.log("Writing $libPath (${libBytes.size} bytes); existing=${libPath in existing}")
            if (libPath in existing) zip.deleteEntry(libPath)
            zip.writeEntry(libPath, libBytes, ZipCompression.NONE)
        }
    }

    /**
     * The supplied file is either an APK that already carries the library, or a bundle whose ABI
     * split does. Bundle members are staged on disk, bounded, and verified by the caller.
     */
    private fun locateEngineApk(supplied: File, libPath: String, container: StepRunner): File? {
        if (hasEntry(supplied, libPath)) return supplied

        val staged = paths.patchingDownloadDir.resolve("voice-engine").apply { mkdirs() }

        ZipFile(supplied).use { bundle ->
            for (entry in bundle.entries()) {
                if (entry.isDirectory || !entry.name.endsWith(".apk")) continue

                val file = staged.resolve(entry.name.substringAfterLast('/'))
                container.log("Checking bundle member ${entry.name}")
                bundle.getInputStream(entry).use { input -> writeBounded(input, file, MAX_MEMBER_BYTES) }

                if (hasEntry(file, libPath)) return file
                file.delete()
            }
        }

        return null
    }

    private fun hasEntry(file: File, name: String): Boolean = try {
        ZipFile(file).use { it.getEntry(name) != null }
    } catch (e: Exception) {
        false
    }

    private fun readBounded(file: File, name: String, limit: Long): ByteArray? = try {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry(name) ?: return null
            if (entry.size > limit) throw IllegalStateException("$name is larger than expected")

            val bytes = zip.getInputStream(entry).use { it.readBytesBounded(limit) }
            bytes
        }
    } catch (e: IllegalStateException) {
        throw e
    } catch (e: Exception) {
        null
    }

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

    private fun InputStream.readBytesBounded(limit: Long): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var written = 0L

        while (true) {
            val read = read(buffer)
            if (read < 0) break

            written += read
            if (written > limit) throw IllegalStateException("Archive member is larger than expected")

            output.write(buffer, 0, read)
        }

        return output.toByteArray()
    }

    private companion object {
        /** A libdiscord.so is around 12 MiB; anything far larger is not one. */
        const val MAX_LIBRARY_BYTES = 64L * 1024 * 1024
        /** Bundle members are APKs of a few hundred MiB at most. */
        const val MAX_MEMBER_BYTES = 512L * 1024 * 1024
    }
}
