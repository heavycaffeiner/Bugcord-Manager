package com.bugcord.manager

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import com.bugcord.manager.di.*
import com.bugcord.manager.installers.dhizuku.DhizukuInstaller
import com.bugcord.manager.installers.intent.IntentInstaller
import com.bugcord.manager.installers.pm.PMInstaller
import com.bugcord.manager.installers.root.RootInstaller
import com.bugcord.manager.installers.shizuku.ShizukuInstaller
import com.bugcord.manager.manager.*
import com.bugcord.manager.manager.download.AndroidDownloadManager
import com.bugcord.manager.manager.download.KtorDownloadManager
import com.bugcord.manager.network.services.*
import com.bugcord.manager.ui.screens.about.AboutModel
import com.bugcord.manager.ui.screens.componentopts.ComponentOptionsModel
import com.bugcord.manager.ui.screens.home.HomeModel
import com.bugcord.manager.ui.screens.iconopts.IconOptionsModel
import com.bugcord.manager.ui.screens.log.LogScreenModel
import com.bugcord.manager.ui.screens.logs.LogsListScreenModel
import com.bugcord.manager.ui.screens.patching.PatchingScreenModel
import com.bugcord.manager.ui.screens.patchopts.PatchOptionsModel
import com.bugcord.manager.ui.screens.permissions.PermissionsModel
import com.bugcord.manager.ui.screens.plugins.PluginsModel
import com.bugcord.manager.ui.screens.settings.SettingsModel
import com.bugcord.manager.ui.widgets.updater.UpdaterViewModel
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.*
import org.koin.dsl.module

class ManagerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            // Android activities & context
            androidContext(this@ManagerApplication)
            modules(module(createdAtStart = true) {
                singleOf(::ActivityProvider)
            })

            // HTTP
            modules(module {
                single { provideJson() }
                single { provideHttpClient() }
            })

            // Services
            modules(module {
                singleOf(::HttpService)
                singleOf(::BugcordGithubService)
                singleOf(::BugcordhookService)
            })

            // UI Models
            modules(module {
                factoryOf(::HomeModel)
                factoryOf(::PluginsModel)
                factoryOf(::AboutModel)
                factoryOf(::PatchingScreenModel)
                factoryOf(::SettingsModel)
                factoryOf(::PatchOptionsModel)
                factoryOf(::IconOptionsModel)
                factoryOf(::ComponentOptionsModel)
                factoryOf(::LogScreenModel)
                factoryOf(::LogsListScreenModel)
                factoryOf(::PermissionsModel)
                viewModelOf(::UpdaterViewModel)
            })

            // Managers
            modules(module {
                single { providePreferences() }
                singleOf(::PathManager)
                singleOf(::InstallerManager)
                singleOf(::OverlayManager)
                singleOf(::InstallLogManager)

                singleOf(::ShizukuManager)
                singleOf(::DhizukuManager)

                singleOf(::AndroidDownloadManager)
                singleOf(::KtorDownloadManager)
            })

            // Installers
            modules(module {
                singleOf(::PMInstaller)
                singleOf(::RootInstaller)
                singleOf(::IntentInstaller)
                singleOf(::ShizukuInstaller)
                singleOf(::DhizukuInstaller)
            })
        }

        // Limit parallel fetching of images using Coil
        @OptIn(DelicateCoilApi::class)
        SingletonImageLoader.setUnsafe { context ->
            ImageLoader.Builder(context)
                .fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(5))
                .build()
        }
    }
}
