/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.bugcord.manager.network.models

import com.bugcord.manager.network.utils.SemVer
import com.bugcord.manager.util.serialization.IntAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Remote Bugcord build data available at https://raw.githubusercontent.com/thirdscam/Bugcord/builds/data.json
 * This is used to determine the latest available versions of components.
 */
@Serializable
data class BuildInfo(
    @Serializable(with = IntAsStringSerializer::class)
    @SerialName("versionCode")
    val discordVersionCode: Int,
    /**
     * Version of the Bugcord core and bundled voice library.
     */
    @SerialName("coreVersion")
    val coreVersion: SemVer,

    @SerialName("injectorVersion")
    val injectorVersion: SemVer,
    @SerialName("patchesVersion")
    val patchesVersion: SemVer,
    @SerialName("kotlinVersion")
    val kotlinVersion: SemVer,
    @SerialName("voiceVersion")
    val voiceVersion: SemVer,
)
