package app.tada.patches.youtube.video.quality

import app.tada.patcher.extensions.InstructionExtensions.addInstructions
import app.tada.patcher.extensions.InstructionExtensions.getInstruction
import app.tada.patcher.patch.bytecodePatch
import app.tada.patches.shared.misc.settings.preference.SwitchPreference
import app.tada.patches.youtube.misc.extension.sharedExtensionPatch
import app.tada.patches.youtube.misc.settings.settingsPatch
import app.tada.patches.youtube.video.information.EXTENSION_VIDEO_QUALITY_INTERFACE
import app.tada.patches.youtube.video.information.videoInformationPatch
import app.tada.util.getReference
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS =
    "Lapp/tada/extension/youtube/patches/playback/quality/HidePremiumVideoQualityPatch;"

internal val hidePremiumVideoQualityPatch = bytecodePatch {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        videoInformationPatch,
    )

    execute {
        settingsMenuVideoQualityGroup.add(
            SwitchPreference("tada_hide_premium_video_quality")
        )

        // Class name is obfuscated in 21.02+
        val videoQualityArray = DefaultOverflowOverlayOnClickFingerprint.instructionMatches.last()
            .instruction.getReference<FieldReference>()!!.type

        // To avoid ClassCastException, declare the new array as original video quality class instead of [EXTENSION_VIDEO_QUALITY_INTERFACE]
        HidePremiumVideoQualityGetArrayFingerprint.method.addInstructions(
            0,
            """
                new-array p1, p1, $videoQualityArray
                return-object p1
            """
        )

        getCurrentVideoFormatConstructorFingerprint(videoQualityArray).let {
            it.method.apply {
                val index = it.instructionMatches.last().index
                val register = getInstruction<TwoRegisterInstruction>(index).registerA

                addInstructions(
                    index,
                    """
                        invoke-static/range { v$register .. v$register }, $EXTENSION_CLASS->hidePremiumVideoQuality([$EXTENSION_VIDEO_QUALITY_INTERFACE)[Ljava/lang/Object;
                        move-result-object v$register
                        check-cast v$register, $videoQualityArray
                    """
                )
            }
        }
    }
}
