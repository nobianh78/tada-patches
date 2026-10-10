package app.tada.patches.youtube.interaction.seekbar

import app.tada.patcher.extensions.InstructionExtensions.addInstructions
import app.tada.patcher.extensions.InstructionExtensions.getInstruction
import app.tada.patcher.methodCall
import app.tada.patcher.patch.PatchException
import app.tada.patcher.patch.bytecodePatch
import app.tada.patches.shared.misc.settings.preference.SwitchPreference
import app.tada.patches.youtube.misc.extension.sharedExtensionPatch
import app.tada.patches.youtube.misc.playservice.versionCheckPatch
import app.tada.patches.youtube.misc.settings.PreferenceScreen
import app.tada.patches.youtube.misc.settings.settingsPatch
import app.tada.util.findInstructionIndicesReversed
import app.tada.util.getReference
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "Lapp/tada/extension/youtube/patches/SlideToSeekPatch;"

val enableSlideToSeekPatch = bytecodePatch(
    description = "Adds an option to enable slide to seek " +
        "instead of playing at 2x speed when pressing and holding in the video player."
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {
        PreferenceScreen.SEEKBAR.addPreferences(
            SwitchPreference("tada_slide_to_seek", summary = true),
        )

        var modifiedMethods = false

        // Restore the behavior to slide to seek.

        val checkIndex = SlideToSeekFingerprint.instructionMatches.first().index
        val checkReference = SlideToSeekFingerprint.method.getInstruction(checkIndex)
            .getReference<MethodReference>()!!

        val extensionMethodDescriptor = "$EXTENSION_CLASS->isSlideToSeekDisabled(Z)Z"

        // A/B check method was only called on this class.
        SlideToSeekFingerprint.classDef.methods.forEach { method ->
            method.findInstructionIndicesReversed(
                methodCall(reference = checkReference)
            ).forEach { index ->
                method.apply {
                    val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

                    addInstructions(
                        index + 2,
                        """
                            invoke-static { v$register }, $extensionMethodDescriptor
                            move-result v$register
                       """
                    )
                }

                modifiedMethods = true
            }
        }

        if (!modifiedMethods) throw PatchException("Could not find methods to modify")

        // Disable the double speed seek gesture.
        DisableFastForwardGestureFingerprint.let {
            it.method.apply {
                val targetIndex = it.instructionMatches.last().index
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1,
                    """
                        invoke-static { v$targetRegister }, $extensionMethodDescriptor
                        move-result v$targetRegister
                    """
                )
            }
        }
    }
}
