package com.bugcord.manager.patcher.steps.patch

import android.os.Build
import com.bugcord.manager.R
import com.bugcord.manager.patcher.StepRunner
import com.bugcord.manager.patcher.steps.StepGroup
import com.bugcord.manager.patcher.steps.base.Step
import com.bugcord.manager.patcher.steps.download.CopyDependenciesStep
import com.bugcord.manager.patcher.steps.download.DownloadVoiceEngineStep
import com.github.diamondminer88.zip.ZipCompression
import com.github.diamondminer88.zip.ZipReader
import com.github.diamondminer88.zip.ZipWriter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Replaces Discord's bundled libdiscord.so with the verified modern voice library. */
class ReplaceVoiceEngineStep : Step(), KoinComponent {
    override val group = StepGroup.Patch
    override val localizedName = R.string.patch_step_replace_voice_engine

    override suspend fun execute(container: StepRunner) {
        val abi = Build.SUPPORTED_ABIS.firstOrNull()
            ?: throw IllegalStateException("No supported ABI is available")
        val source = container.getStep<DownloadVoiceEngineStep>().getStoredFile(container)
        val apk = container.getStep<CopyDependenciesStep>().apk
        val libPath = "lib/$abi/libdiscord.so"
        val libBytes = source.readBytes()

        if (libBytes.size < MIN_LIBRARY_BYTES)
            throw IllegalStateException("Downloaded voice engine is too small to be libdiscord.so")

        val existing = ZipReader(apk).use { it.entryNames.toHashSet() }
        ZipWriter(apk, true).use { zip ->
            container.log("Writing $libPath (${libBytes.size} bytes); existing=${libPath in existing}")
            if (libPath in existing) zip.deleteEntry(libPath)
            zip.writeEntry(libPath, libBytes, ZipCompression.NONE)
        }
    }

    private companion object {
        const val MIN_LIBRARY_BYTES = 1_000_000
    }
}
