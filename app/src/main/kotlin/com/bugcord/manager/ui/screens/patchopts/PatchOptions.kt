package com.bugcord.manager.ui.screens.patchopts

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.bugcord.manager.ui.screens.componentopts.PatchComponent
import com.bugcord.manager.util.serialization.ColorParceler
import com.bugcord.manager.util.serialization.ColorSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Parcelize
@Serializable
data class PatchOptions(
    /**
     * The app name that's user-facing in launchers.
     */
    val appName: String,

    /**
     * Changes the installation package name.
     */
    val packageName: String,

    /**
     * Adding the debuggable APK flag.
     */
    val debuggable: Boolean,

    /**
     * Replacement of the user-facing launcher icon.
     */
    val iconReplacement: IconReplacement,

    /**
     * A custom build of injector that was used rather than the latest.
     */
    val customInjector: PatchComponent? = null,

    /**
     * A custom smali patches bundle that was used rather than the latest.
     */
    val customPatches: PatchComponent? = null,
) : Parcelable {
    @Immutable
    @Parcelize
    @Serializable
    sealed interface IconReplacement : Parcelable {
        /**
         * Keeps the original icons that are present in the APK.
         */
        @Immutable
        @Parcelize
        @Serializable
        @SerialName("original")
        data object Original : IconReplacement

        /**
         * Changes the foreground logo to Discord's old logo and the
         * background color to old blurple, like prior to early 2021.
         */
        @Immutable
        @Parcelize
        @Serializable
        @SerialName("old_discord")
        data object OldDiscord : IconReplacement

        /**
         * Changes the background of the icon to a specific color without
         * altering the foreground or monochrome variants.
         */
        @Immutable
        @Parcelize
        @Serializable
        @SerialName("color")
        data class CustomColor(
            @TypeParceler<Color, ColorParceler>
            @Serializable(ColorSerializer::class)
            val color: Color,
        ) : IconReplacement

        /**
         * Replaces the foreground image of the icon entirely and sets the background to transparent.
         * This does not affect the monochrome icon.
         */
        @Immutable
        @Parcelize
        @Serializable
        @SerialName("image")
        data class CustomImage(val imageBytes: ByteArray) : IconReplacement {
            override fun hashCode() = imageBytes.contentHashCode()
            override fun equals(other: Any?) = this === other
                || (javaClass == other?.javaClass && imageBytes.contentEquals((other as CustomImage).imageBytes))
        }

        companion object {
            /**
             * The default Bugcord background color.
             */
            val BugcordColor = Color(0xFF00C853)

            /**
             * The new Discord blurple used in icons.
             */
            val BlurpleColor = Color(0xFF5865F2)

            /**
             * The old Discord icon color (before the accessibility redesign).
             */
            val OldBlurpleColor = Color(0xFF7289DA)
        }
    }

    companion object {
        val Default = PatchOptions(
            appName = "Bugcord",
            packageName = "com.bugcord",
            debuggable = false,
            iconReplacement = IconReplacement.CustomColor(IconReplacement.BugcordColor),
            customInjector = null,
            customPatches = null,
        )
    }
}
