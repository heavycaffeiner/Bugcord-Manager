package com.bugcord.manager.patcher.steps.prepare

import android.app.Application
import android.content.pm.PackageManager.NameNotFoundException
import com.bugcord.manager.R
import com.bugcord.manager.installers.InstallerResult
import com.bugcord.manager.manager.InstallerManager
import com.bugcord.manager.patcher.StepRunner
import com.bugcord.manager.patcher.steps.StepGroup
import com.bugcord.manager.patcher.steps.base.Step
import com.bugcord.manager.patcher.steps.base.StepState
import com.bugcord.manager.ui.screens.patchopts.PatchOptions
import com.bugcord.manager.util.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Prompt the user to uninstall a previous version of Bugcord if it has a larger version code.
 * (Prevent conflicts from downgrading)
 */
class DowngradeCheckStep(private val options: PatchOptions) : Step(), KoinComponent {
    private val context: Application by inject()
    private val installers: InstallerManager by inject()

    override val group = StepGroup.Prepare
    override val localizedName = R.string.patch_step_downgrade_check

    override suspend fun execute(container: StepRunner) {
        container.log("Fetching version of package ${options.packageName}")
        val (_, currentVersion) = try {
            context.getPackageVersion(options.packageName)
        }
        // Package is not installed
        catch (_: NameNotFoundException) {
            state = StepState.Skipped
            container.log("Package not uninstalled, skipping check")
            return
        }
        container.log("Version of installed Discord app: $currentVersion")

        val targetVersion = container
            .getStep<FetchInfoStep>()
            .data.discordVersionCode

        container.log("Target discord version: $targetVersion")

        if (currentVersion > targetVersion) {
            container.log("Current installed version is greater than target, forcing uninstallation")
            mainThread { context.showToast(R.string.installer_uninstall_new) }

            when (val result = installers.getActiveInstaller().waitUninstall(options.packageName)) {
                is InstallerResult.Error -> throw Error("Failed to uninstall Bugcord: ${result.getDebugReason()}")
                is InstallerResult.Cancelled -> {
                    mainThread { context.showToast(R.string.installer_uninstall_new) }
                    throw Error("Newer versions of Bugcord must be uninstalled prior to installing an older version")
                }

                else -> {}
            }
        }
    }
}
