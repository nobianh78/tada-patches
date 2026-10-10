/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.patches.shared.ad

import app.tada.patcher.extensions.InstructionExtensions.addInstruction
import app.tada.patcher.extensions.InstructionExtensions.addInstructions
import app.tada.patcher.extensions.InstructionExtensions.getInstruction
import app.tada.patcher.patch.bytecodePatch
import app.tada.patches.shared.misc.settings.preference.BasePreferenceScreen
import app.tada.patches.shared.misc.settings.preference.SwitchPreference
import app.tada.util.addInstructionsAtControlFlowLabel
import app.tada.util.cloneParameters
import app.tada.util.findFreeRegister
import app.tada.util.getReference
import app.tada.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val EXTENSION_CLASS = "Lapp/tada/extension/shared/patches/HideFullscreenAdsPatch;"

internal fun hideFullscreenAdsPatch(
    preferenceScreen: BasePreferenceScreen.Screen
) = bytecodePatch(
    description = "Adds an option to hide fullscreen premium popup ads."
) {
    execute {
        preferenceScreen.addPreferences(
            SwitchPreference("tada_hide_fullscreen_ads")
        )

        // non-litho view, used in some old clients
        InterstitialsContainerFingerprint.let {
            it.method.apply {
                val index = it.instructionMatches.first().index + 2
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstruction(
                    index + 1,
                    "invoke-static { v$register }, $EXTENSION_CLASS->hideFullscreenAds(Landroid/view/View;)V"
                )
            }
        }

        LithoDialogBuilderFingerprint.let {
            it.method.cloneParameters().apply {
                val dialogClass = it.instructionMatches.first().instruction
                    .getReference<MethodReference>()!!.definingClass

                val insertIndex = indexOfFirstInstructionReversedOrThrow {
                    opcode == Opcode.IPUT_OBJECT &&
                            getReference<FieldReference>()?.type == dialogClass
                }
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA
                val freeRegister = findFreeRegister(insertIndex, insertRegister)

                addInstructionsAtControlFlowLabel(
                    insertIndex,
                    """
                        move-object/from16 v$freeRegister, p1
                        invoke-static { v$insertRegister, v$freeRegister }, $EXTENSION_CLASS->closeFullscreenAd(Ljava/lang/Object;[B)V
                    """
                )

                // Set close dialog method.
                val customDialogOnBackPressedMethod = CustomDialogOnBackPressedFingerprint.method

                FullscreenAdsPatchFingerprint.method.addInstructions(
                    0,
                    """
                        check-cast p0, ${customDialogOnBackPressedMethod.definingClass}
                        invoke-virtual { p0 }, $customDialogOnBackPressedMethod
                    """
                )
            }
        }
    }
}
