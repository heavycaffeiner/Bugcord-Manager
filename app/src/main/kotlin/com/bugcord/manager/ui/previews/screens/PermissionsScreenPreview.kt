package com.bugcord.manager.ui.previews.screens

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.bugcord.manager.manager.InstallerSetting
import com.bugcord.manager.ui.screens.permissions.PermissionsScreenContent
import com.bugcord.manager.ui.theme.ManagerTheme

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
fun PermissionsScreenPreview() {
    ManagerTheme {
        PermissionsScreenContent(
            installer = InstallerSetting.PackageInstaller,
            openInstallersDialog = {},
            storagePermsGranted = true,
            onGrantStoragePerms = {},
            unknownSourcesPermsGranted = true,
            onGrantUnknownSourcesPerms = {},
            notificationsPermsGranted = false,
            onGrantNotificationsPerms = {},
            batteryPermsGranted = false,
            onGrantBatteryPerms = {},
            canContinue = true,
            onContinue = {},
        )
    }
}
