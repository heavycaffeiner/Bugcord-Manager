package com.bugcord.manager.patcher.util

import android.os.Build
import com.android.apksig.ApkVerifier
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import java.io.File

/**
 * Ownership proof for APKs whose contents end up inside the patched app, so a renamed
 * archive cannot smuggle native code into a signed install.
 */
object DiscordApkVerifier {
    /** SHA-256 of Discord's APK signing certificate. */
    private const val DISCORD_CERTIFICATE_SHA256 =
        "3c39d23cf9367849a5c699395647fe0e5bfea5a1f1f40d8c717ddc70f8bfa113"

    /**
     * Verifies that [apk] is signed by Discord.
     *
     * @throws UnsupportedOperationException when the signature scheme API is unavailable,
     * because an unverifiable APK must not be trusted.
     */
    fun verifyDiscordSignature(apk: File) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            throw UnsupportedOperationException("Verifying APK signatures requires Android 9 or newer")

        val result = try {
            ApkVerifier.Builder(apk).build().verify()
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

    class SignatureVerificationException(errors: List<ApkVerifier.IssueWithParams>) : Exception(
        "Failed to verify APK signatures! " +
            "This is an unoriginal APK that has been tampered with. " +
            "Verification errors: " + errors.joinToString()
    )
}
