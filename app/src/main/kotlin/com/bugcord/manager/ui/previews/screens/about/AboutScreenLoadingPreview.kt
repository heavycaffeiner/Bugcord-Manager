package com.bugcord.manager.ui.previews.screens.about

import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.bugcord.manager.ui.screens.about.AboutScreenContent
import com.bugcord.manager.ui.screens.about.AboutScreenState
import com.bugcord.manager.ui.theme.ManagerTheme

// This preview cannot be properly viewed from an IDE

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun AboutScreenFailedPreview() {
    ManagerTheme {
        AboutScreenContent(
            state = remember { mutableStateOf(AboutScreenState.Loading) },
        )
    }
}

