package app.tada.patches.youtube.layout.startpage

import app.tada.patcher.extensions.InstructionExtensions.addInstruction
import app.tada.patcher.extensions.InstructionExtensions.addInstructions
import app.tada.patcher.extensions.InstructionExtensions.getInstruction
import app.tada.patcher.patch.bytecodePatch
import app.tada.patches.shared.misc.settings.preference.ListPreference
import app.tada.patches.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory
import app.tada.patches.youtube.misc.extension.sharedExtensionPatch
import app.tada.patches.youtube.misc.settings.PreferenceScreen
import app.tada.patches.youtube.misc.settings.settingsPatch
import app.tada.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/tada/extension/youtube/patches/ChangeStartPagePatch;"

val changeStartPagePatch = bytecodePatch(
    name = "Change start page",
    description = "Adds an option to set which page the app opens in instead of the homepage.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
    )

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        PreferenceScreen.GENERAL.addPreferences(
            noTitleUnsortedPreferenceCategory(
                ListPreference(
                    key = "tada_change_start_page",
                    tag = "app.tada.extension.shared.settings.preference.SortedListPreference"
                )
            )
        )

        // Hook browseId.
        BrowseIdFingerprint.let {
            it.method.apply {
                val browseIdIndex = it.instructionMatches.first().index
                val browseIdRegister = getInstruction<OneRegisterInstruction>(browseIdIndex).registerA

                addInstructions(
                    browseIdIndex + 1,
                    """
                        invoke-static { v$browseIdRegister }, $EXTENSION_CLASS->overrideBrowseId(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$browseIdRegister
                    """
                )
            }
        }

        // There is no browserId assigned to Shorts and Search.
        // Just hook the Intent action.
        IntentActionFingerprint.method.addInstruction(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->overrideIntentAction(Landroid/content/Intent;)V",
        )
    }
}
