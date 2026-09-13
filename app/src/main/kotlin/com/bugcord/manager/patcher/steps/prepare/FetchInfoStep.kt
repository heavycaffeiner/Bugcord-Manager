package com.bugcord.manager.patcher.steps.prepare

import androidx.compose.runtime.Stable
import com.bugcord.manager.R
import com.bugcord.manager.network.models.BuildInfo
import com.bugcord.manager.network.services.BugcordGithubService
import com.bugcord.manager.network.services.BugcordhookService
import com.bugcord.manager.network.utils.SemVer
import com.bugcord.manager.network.utils.getOrThrow
import com.bugcord.manager.patcher.StepRunner
import com.bugcord.manager.patcher.steps.StepGroup
import com.bugcord.manager.patcher.steps.base.Step
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Fetches versions data from various sources to be used during the installation.
 */
@Stable
class FetchInfoStep : Step(), KoinComponent {
    private val github: BugcordGithubService by inject()
    private val hook: BugcordhookService by inject()

    override val group = StepGroup.Prepare
    override val localizedName = R.string.patch_step_fetch_kt_version

    /**
     * Remote build data about the latest versions of components.
     */
    lateinit var data: BuildInfo
        private set

    /**
     * Remote data about the latest Bugcordhook version available from the Bugcord maven.
     */
    lateinit var bugcordhookVersion: SemVer
        private set

    /**
     * Version of the BugcordVoice library published to the core repository's builds branch.
     * Bumped by the core build, so a stale cached AAR cannot outlive its version.
     */
    lateinit var bugcordvoiceVersion: SemVer
        private set

    /**
     * Discord build whose libdiscord.so is used as the voice engine.
     */
    val libdiscordVersion: Int = 333012

    override suspend fun execute(container: StepRunner) {
        container.log("Fetching ${BugcordGithubService.DATA_JSON_URL}")
        data = github.getBuildData(force = true).getOrThrow()
        container.log("Fetched build data: $data")

        container.log("Obtaining latest bugcordhook version")
        bugcordhookVersion = hook.getBugcordhookVersion(force = true).getOrThrow()
        container.log("Fetched bugcordhook version: $bugcordhookVersion")

        bugcordvoiceVersion = data.voiceVersion
        container.log("Fetched bugcordvoice version: $bugcordvoiceVersion")
    }
}
