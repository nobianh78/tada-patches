package app.tada.patches.youtube.misc.playertype

import app.tada.patcher.Fingerprint
import app.tada.patcher.extensions.InstructionExtensions.addInstruction
import app.tada.patcher.extensions.InstructionExtensions.addInstructions
import app.tada.patcher.extensions.InstructionExtensions.getInstruction
import app.tada.patcher.fieldAccess
import app.tada.patcher.patch.bytecodePatch
import app.tada.patcher.resource.ResourceType
import app.tada.patcher.resourceLiteral
import app.tada.patches.youtube.misc.extension.sharedExtensionPatch
import app.tada.patches.youtube.shared.getPlayerTypeFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val EXTENSION_CLASS = "Lapp/tada/extension/youtube/patches/PlayerTypeHookPatch;"

val playerTypeHookPatch = bytecodePatch(
    description = "Hook to get the current player type and video playback state.",
) {
    dependsOn(sharedExtensionPatch)

    execute {
        getPlayerTypeFingerprint().method.addInstruction(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->setPlayerType(Ljava/lang/Enum;)V",
        )

        ReelWatchPagerFingerprint.let {
            it.method.apply {
                val index = it.instructionMatches.last().index
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstruction(
                    index + 1,
                    "invoke-static { v$register }, $EXTENSION_CLASS->onShortsCreate(Landroid/view/View;)V"
                )
            }
        }

        val controlStateType = ControlsStateToStringFingerprint.originalClassDef.type
        val videoStateType = VideoStateEnumFingerprint.originalClassDef.type

        Fingerprint(
            accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
            returnType = "V",
            parameters = listOf(element = controlStateType),
            filters = listOf(
                // Obfuscated parameter field name.
                fieldAccess(
                    definingClass = controlStateType,
                    type = videoStateType
                ),
                resourceLiteral(ResourceType.STRING, "accessibility_play"),
                resourceLiteral(ResourceType.STRING, "accessibility_pause")
            )
        ).let {
            it.method.apply {
                val videoStateFieldName = getInstruction<ReferenceInstruction>(
                    it.instructionMatches.first().index
                ).reference

                addInstructions(
                    0,
                    """
                        iget-object v0, p1, $videoStateFieldName  # copy VideoState parameter field
                        invoke-static {v0}, $EXTENSION_CLASS->setVideoState(Ljava/lang/Enum;)V
                    """
                )
            }
        }
    }
}
