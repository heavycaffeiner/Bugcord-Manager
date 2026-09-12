package com.bugcord.manager.patcher

import com.bugcord.manager.network.utils.SemVer
import com.bugcord.manager.ui.screens.patchopts.PatchOptions
import kotlinx.serialization.Serializable

/**
 * Data stored inside patched APKs as "bugcord.json" in order to preserve install-time information about Bugcord and the Manager.
 */
@Serializable
data class InstallMetadata(
    /**
     * Whether the manager is a real release or built from source.
     */
    val customManager: Boolean,

    /**
     * The semver version of this manager that performed the installation.
     */
    val managerVersion: SemVer,

    /**
     * Version (commit hash) of the Bugcordhook build that was injected into the APK.
     */
    val bugcordhookVersion: SemVer,

    /**
     * Version of the injector build that was injected into the APK.
     */
    val injectorVersion: SemVer,

    /**
     * Version of the smali patches that were applied onto the APK.
     */
    val patchesVersion: SemVer,

    /**
     * Version of the Kotlin stdlib that was injected into the end of the APK.
     */
    val kotlinVersion: SemVer,

    /**
     * The user-selected options for this installation.
     */
    val options: PatchOptions,
)
