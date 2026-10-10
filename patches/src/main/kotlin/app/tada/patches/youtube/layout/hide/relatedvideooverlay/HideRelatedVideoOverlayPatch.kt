package app.tada.patches.youtube.layout.hide.relatedvideooverlay

import app.tada.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.tada.patcher.extensions.InstructionExtensions.getInstruction
import app.tada.patcher.patch.bytecodePatch
import app.tada.patcher.util.smali.ExternalLabel
import app.tada.patches.shared.misc.settings.preference.SwitchPreference
import app.tada.patches.youtube.misc.extension.sharedExtensionPatch
import app.tada.patches.youtube.misc.settings.PreferenceScreen
import app.tada.patches.youtube.misc.settings.settingsPatch
import app.tada.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE

private const val EXTENSION_CLASS =
    "Lapp/tada/extension/youtube/patches/HideRelatedVideoOverlayPatch;"

@Suppress("unused")
val hideRelatedVideoOverlayPatch = bytecodePatch(
    name = "Hide related video overlay",
    description = "Adds an option to hide the related video overlay shown when swiping up in fullscreen.",
) {
    dependsOn(
        settingsPatch,
        sharedExtensionPatch,
    )

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        PreferenceScreen.PLAYER.addPreferences(
            SwitchPreference("tada_hide_player_related_videos_overlay", summary = true)
        )

        RelatedEndScreenResultsFingerprint.method.apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS->hideRelatedVideoOverlay()Z
                    move-result v0
                    if-eqz v0, :show
                    return-void
                """,
                ExternalLabel("show", getInstruction(0))
            )
        }
    }
}
