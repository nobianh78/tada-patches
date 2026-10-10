/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.patches.youtube.misc.toolbar

import app.tada.patcher.extensions.InstructionExtensions.addInstructions
import app.tada.patcher.extensions.InstructionExtensions.getInstruction
import app.tada.patcher.patch.bytecodePatch
import app.tada.patcher.util.proxy.mutableTypes.MutableMethod
import app.tada.patches.youtube.misc.extension.sharedExtensionPatch
import app.tada.patches.youtube.shared.ToolBarButtonFingerprint
import app.tada.util.addInstructionsAtControlFlowLabel
import app.tada.util.findFreeRegister
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import java.lang.ref.WeakReference

internal const val EXTENSION_CLASS = "Lapp/tada/extension/youtube/patches/ToolBarPatch;"

private lateinit var toolbarMethod : WeakReference<MutableMethod>

val toolBarHookPatch = bytecodePatch(
    description = "toolBarHookPatch"
) {
    dependsOn(
        sharedExtensionPatch,
    )

    execute {
        ToolBarButtonFingerprint.let {
            it.clearMatch()
            it.method.apply {
                val enumIndex = it.instructionMatches[3].index
                val enumRegister = getInstruction<OneRegisterInstruction>(enumIndex).registerA

                val imageViewIndex = it.instructionMatches[6].index
                val imageViewReference = getInstruction<ReferenceInstruction>(imageViewIndex).reference

                val insertIndex = enumIndex + 1
                val freeRegister = findFreeRegister(insertIndex, enumRegister)

                addInstructionsAtControlFlowLabel(
                    insertIndex,
                    """
                        iget-object v$freeRegister, p0, $imageViewReference
                        invoke-static { v$enumRegister, v$freeRegister }, $EXTENSION_CLASS->hookToolBar(Ljava/lang/Enum;Landroid/widget/ImageView;)V
                    """
                )
            }
        }

        toolbarMethod = WeakReference(ToolBarPatchFingerprint.method)
    }
}

internal fun hookToolBar(descriptor: String) = toolbarMethod.get()!!.addInstructions(
    0,
    "invoke-static { p0, p1, p2 }, " +
            "$descriptor(Ljava/lang/String;Landroid/view/View;Landroid/widget/ImageView;)V"
)
