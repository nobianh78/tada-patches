package app.tada.patches.youtube.misc.links

import app.tada.patcher.Fingerprint
import app.tada.patcher.extensions.InstructionExtensions.addInstructions
import app.tada.patcher.extensions.InstructionExtensions.getInstruction
import app.tada.patcher.patch.bytecodePatch
import app.tada.patcher.string
import app.tada.patches.shared.misc.settings.preference.SwitchPreference
import app.tada.patches.youtube.misc.settings.PreferenceScreen
import app.tada.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE
import app.tada.util.matchAllMethodIndicesForEach
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/tada/extension/youtube/patches/OpenLinksExternallyPatch;"

val openLinksExternallyPatch = bytecodePatch(
    name = "Open links externally",
    description = "Adds an option to always open links in your browser instead of with the in-app browser.",
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        PreferenceScreen.MISC.addPreferences(
            SwitchPreference("tada_external_browser", summary = true),
        )

        Fingerprint(
            filters = listOf(
                string("android.support.customtabs.action.CustomTabsService")
            ),
            custom = { _, classDef ->
                !classDef.type.startsWith("Lapp/tada/")
            }
        ).matchAllMethodIndicesForEach { index ->
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            addInstructions(
                index + 1,
                """
                    invoke-static { v$register }, $EXTENSION_CLASS->getIntent(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$register
                """
            )
        }
    }
}
