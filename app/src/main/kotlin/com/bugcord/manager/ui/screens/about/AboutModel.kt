package com.bugcord.manager.ui.screens.about

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.bugcord.manager.network.services.BugcordGithubService
import com.bugcord.manager.network.utils.fold
import com.bugcord.manager.ui.util.toUnsafeImmutable
import com.bugcord.manager.util.launchIO

class AboutModel(
    private val bugcordGithubService: BugcordGithubService,
) : StateScreenModel<AboutScreenState>(AboutScreenState.Loading) {
    init {
        fetchContributors()
    }

    fun fetchContributors() = screenModelScope.launchIO {
        mutableState.value = AboutScreenState.Loading

        val response = bugcordGithubService.getContributors()

        mutableState.value = response.fold(
            success = { contributors ->
                AboutScreenState.Loaded(contributors.toUnsafeImmutable())
            },
            fail = { AboutScreenState.Failure },
        )
    }
}
