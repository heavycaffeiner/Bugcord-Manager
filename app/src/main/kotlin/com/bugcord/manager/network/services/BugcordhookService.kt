package com.bugcord.manager.network.services

import com.bugcord.manager.BuildConfig
import com.bugcord.manager.di.cacheControl
import com.bugcord.manager.network.models.GithubRelease
import com.bugcord.manager.network.utils.*
import io.ktor.client.request.url
import io.ktor.http.CacheControl
import io.ktor.http.HttpStatusCode

/**
 * Resolves the hook library from the releases of its own repository.
 * The release tag is the library version and the AAR is attached to it.
 */
class BugcordhookService(private val http: HttpService) {
    suspend fun getBugcordhookVersion(force: Boolean = false): ApiResponse<SemVer> {
        val releaseResponse = http.request<GithubRelease> {
            url(LATEST_RELEASE_URL)

            if (!force) {
                cacheControl(CacheControl.MaxAge(maxAgeSeconds = 60 * 30)) // 30 min
            }
        }

        return releaseResponse.transform { release ->
            SemVer.parseOrNull(release.tagName)
                ?: return ApiResponse.Error(ApiError(HttpStatusCode.OK, "Invalid hook release tag: ${release.tagName}"))
        }
    }

    fun getBugcordhookUrl(version: SemVer): String = "$DOWNLOAD_BASE/$version/$AAR_NAME"

    /**
     * The voice library is built by the core repository and published to its builds branch.
     */
    fun getBugcordvoiceUrl(): String = VOICE_URL

    private companion object {
        const val ORG = "heavycaffeiner"
        const val HOOK_REPO = "Bugcord-Hook"
        const val AAR_NAME = "Bugcordhook.aar"
        const val VOICE_URL =
            "https://raw.githubusercontent.com/$ORG/Bugcord/builds/Bugcordvoice.aar"

        const val LATEST_RELEASE_URL = "https://api.github.com/repos/$ORG/$HOOK_REPO/releases/latest"
        const val DOWNLOAD_BASE = "https://github.com/$ORG/$HOOK_REPO/releases/download"
    }
}
