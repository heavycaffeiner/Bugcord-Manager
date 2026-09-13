package com.bugcord.manager.patcher.steps.download

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Stable
import com.bugcord.manager.R
import com.bugcord.manager.manager.PathManager
import com.bugcord.manager.manager.download.IDownloadManager
import com.bugcord.manager.manager.download.KtorDownloadManager
import com.bugcord.manager.patcher.StepRunner
import com.bugcord.manager.patcher.steps.base.DownloadStep
import com.bugcord.manager.patcher.steps.base.StepState
import com.bugcord.manager.util.mainThread
import com.bugcord.manager.util.showToast
import com.android.apksig.ApkVerifier
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/**
 * Downloads the unmodified Discord 126.21 base APK from Bugcord-Maven or upstream fallback.
 */
@Stable
class DownloadDiscordStep : DownloadStep<Int>(), KoinComponent {
    private val paths: PathManager by inject()
    private val context: Context by inject()
    private val downloader: KtorDownloadManager by inject()

    override val localizedName = R.string.patch_step_dl_kt_apk

    override fun getVersion(container: StepRunner) = DISCORD_KT_VERSION
    override fun getRemoteUrl(container: StepRunner) = PRIMARY_URL
    override fun getStoredFile(container: StepRunner) = paths.cachedDiscordApk(DISCORD_KT_VERSION)

    override suspend fun execute(container: StepRunner) {
        val file = getStoredFile(container)
        if (file.exists()) {
            container.log("Checking cached Discord APK: ${file.absolutePath}")
            try {
                verify(container)
                state = StepState.Skipped
                container.log("Discord base APK verified, skipping download")
                return
            } catch (t: Throwable) {
                file.delete()
                container.log("Cached Discord base APK corrupt, redownloading")
            }
        }

        for (url in listOf(PRIMARY_URL, FALLBACK_URL)) {
            container.log("Downloading Discord base APK from $url")
            val result = downloader.download(url, file) { progress = it ?: -1f }
            if (result is IDownloadManager.Result.Success) {
                try {
                    verify(container)
                    container.log("Verified Discord base APK")
                    return
                } catch (e: CancellationException) {
                    file.delete()
                    throw e
                } catch (t: Throwable) {
                    file.delete()
                    container.log("Failed to verify downloaded APK from $url, trying next source")
                }
            }
        }

        file.delete()
        state = StepState.Error
        mainThread { context.showToast(R.string.installer_dl_verify_fail) }
        throw IllegalStateException("Failed to download valid Discord 126.21 base APK")
    }

    override suspend fun verify(container: StepRunner) {
        super.verify(container)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            container.log("Verifying APK signature")
            verifySignature(getStoredFile(container))
        } else {
            container.log("Skipping APK signature verification, API level too old")
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun verifySignature(apk: File) {
        val verifier = ApkVerifier.Builder(apk).build()
        val result = try {
            verifier.verify()
        } catch (e: Exception) {
            throw IllegalStateException("Failed to verify APK! It may have been corrupted or tampered with.", e)
        }

        if (!result.isVerified)
            throw SignatureVerificationException(result.allErrors)

        if (result.signerCertificates.singleOrNull()
                ?.let { it.encoded.toByteString().sha256() == DISCORD_CERTIFICATE_SHA256.decodeHex() } != true
        ) {
            throw VerifyError("Failed to verify Discord's APK signatures! This is an unoriginal APK that has been tampered with.")
        }
    }

    private companion object {
        const val DISCORD_KT_VERSION = 126021
        const val DISCORD_CERTIFICATE_SHA256 = "3c39d23cf9367849a5c699395647fe0e5bfea5a1f1f40d8c717ddc70f8bfa113"
        const val PRIMARY_URL = "https://github.com/heavycaffeiner/Bugcord-Maven/releases/download/126021/base.apk"
        const val FALLBACK_URL = "https://maven.aliucord.com/releases/com/discord/discord/126021/discord-126021.apk"
    }

    private class SignatureVerificationException(errors: List<ApkVerifier.IssueWithParams>) : Exception(
        "Failed to verify APK signatures! Verification errors: " + errors.joinToString()
    )
}
