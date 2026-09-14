package com.bugcord.manager.patcher.steps.download
import android.os.Build
import androidx.compose.runtime.Stable
import com.bugcord.manager.R
import com.bugcord.manager.manager.PathManager
import com.bugcord.manager.network.utils.SemVer
import com.bugcord.manager.patcher.StepRunner
import com.bugcord.manager.patcher.steps.base.DownloadStep
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import java.io.File
import java.security.MessageDigest

/** Downloads the ABI-specific libdiscord.so used by the voice backport. */
@Stable
class DownloadVoiceEngineStep : DownloadStep<SemVer>(), KoinComponent {
    private val paths: PathManager by inject()

    override val localizedName = R.string.patch_step_dl_voice_engine

    override fun getVersion(container: StepRunner) = VOICE_ENGINE_VERSION

    override fun getRemoteUrl(container: StepRunner): String {
        val abi = Build.SUPPORTED_ABIS.firstOrNull()
            ?: throw IllegalStateException("No supported ABI is available")
        val asset = ABI_ASSETS[abi]
            ?: throw IllegalStateException("No Bugcord voice engine is published for ABI $abi")
        return "$BASE_URL/$asset"
    }

    override fun getStoredFile(container: StepRunner) =
        paths.cachedVoiceEngine(getVersion(container), Build.SUPPORTED_ABIS.first())

    override suspend fun verify(container: StepRunner) {
        super.verify(container)
        val file = getStoredFile(container)
        if (file.length() < MIN_LIBRARY_BYTES)
            throw IllegalStateException("Downloaded voice engine is too small to be libdiscord.so")

        val expected = ABI_SHA256[Build.SUPPORTED_ABIS.firstOrNull()]
            ?: throw IllegalStateException("No checksum is published for ABI ${Build.SUPPORTED_ABIS.firstOrNull()}")
        val actual = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            .toByteString()
        if (actual != expected.decodeHex())
            throw IllegalStateException("Downloaded voice engine checksum mismatch")
    }

    companion object {
        val VOICE_ENGINE_VERSION = SemVer(344, 0, 13)

        private const val BASE_URL =
            "https://github.com/thirdscam/Bugcord-Maven/releases/download/344013"
        private const val MIN_LIBRARY_BYTES = 1_000_000L
        private val ABI_SHA256 = mapOf(
            "arm64-v8a" to "2973a3048815fc59513d7917136ca05440f97e59577237223860176dfe0c63b4",
            "armeabi-v7a" to "a5a845847a57a2961ea8e7bc9bd769b4de2038a61fdc4fe94c91fab66ad7d284",
            "x86" to "a2882008e86274d8856262f0ac5ae6ea187d1f829cda0e1fb599d8885ab42eb1",
            "x86_64" to "d6027b952b1ec0a45b064676811dddb00445fc27f055e5ceb7c45ef33d4378e1",
        )
        private val ABI_ASSETS = mapOf(
            "arm64-v8a" to "libdiscord-arm64-v8a.so",
            "armeabi-v7a" to "libdiscord-armeabi-v7a.so",
            "x86" to "libdiscord-x86.so",
            "x86_64" to "libdiscord-x86_64.so",
        )
    }
}
