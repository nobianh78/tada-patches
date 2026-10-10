/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches/pull/1962
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.patches.music.interaction.dislikeredirection

import app.tada.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.tada.patcher.extensions.InstructionExtensions.getInstruction
import app.tada.patcher.patch.bytecodePatch
import app.tada.patcher.util.proxy.mutableTypes.MutableMethod
import app.tada.patcher.util.smali.ExternalLabel
import app.tada.patches.music.misc.extension.sharedExtensionPatch
import app.tada.patches.music.misc.playservice.is_9_32_or_greater
import app.tada.patches.music.misc.playservice.is_9_35_or_greater
import app.tada.patches.music.misc.settings.PreferenceScreen
import app.tada.patches.music.misc.settings.settingsPatch
import app.tada.patches.music.shared.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.tada.patches.shared.misc.settings.preference.SwitchPreference
import app.tada.util.getReference
import app.tada.util.indexOfFirstInstructionOrThrow
import app.tada.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS =
    "Lapp/tada/extension/music/patches/DisableDislikeRedirectionPatch;"

@Suppress("unused")
val disableDislikeRedirectionPatch = bytecodePatch(
    name = "Disable dislike redirection",
    description = "Adds an option to prevent skipping to the next track when the dislike " +
            "button is pressed."
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
    )

    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

    execute {
        PreferenceScreen.PLAYER.addPreferences(
            SwitchPreference("tada_music_disable_dislike_redirection", summary = true)
        )

        if (is_9_32_or_greater) {
            val notificationFingerprint = NotificationLikeButtonOnClickListenerFingerprint
            val notificationOnClickIndex = notificationFingerprint.instructionMatches.last().index
            notificationFingerprint.method.injectRedirectionGuard(notificationOnClickIndex)

            if (is_9_35_or_greater) {
                // Skips to the next track in more than one place (dislike command handler
                // and, on 9.36+, the like button click listener).
                DislikeSkipToNextFingerprint.matchAll().forEach { match ->
                    match.method.injectRedirectionGuard(match.instructionMatches.last().index)
                }
            } else {
                DislikeButtonOnClickListenerFingerprint.method.apply {
                    val onClickIndex = indexOfFirstInstructionReversedOrThrow {
                        (opcode == Opcode.INVOKE_INTERFACE || opcode == Opcode.INVOKE_VIRTUAL) &&
                                getReference<MethodReference>()?.returnType == "V"
                    }
                    injectRedirectionGuard(onClickIndex)
                }
            }
        } else {
            // The notification and player handlers share the same onClick dispatch interface method,
            // extract its reference here to locate the twin call inside the player handler below.
            val onClickReference = NotificationLikeButtonOnClickListenerFingerprint.let { fingerprint ->
                fingerprint.method.let { method ->
                    val onClickIndex = fingerprint.instructionMatches.last().index
                    val reference = method.getInstruction<ReferenceInstruction>(onClickIndex).reference

                    method.injectRedirectionGuard(onClickIndex)
                    reference
                }
            }

            DislikeButtonOnClickListenerLegacyFingerprint.method.apply {
                val onClickIndex = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>() == onClickReference
                }
                injectRedirectionGuard(onClickIndex)
            }
        }
    }
}

// Register from the existing IF_EQZ is reused for move-result; both branches
// reassign it before the next read, so the clobber is safe.
private fun MutableMethod.injectRedirectionGuard(onClickIndex: Int) {
    val targetIndex = indexOfFirstInstructionReversedOrThrow(onClickIndex, Opcode.IF_EQZ)
    val insertRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

    addInstructionsWithLabels(
        targetIndex + 1,
        """
            invoke-static { }, $EXTENSION_CLASS->disableDislikeRedirection()Z
            move-result v$insertRegister
            if-nez v$insertRegister, :disable
        """,
        ExternalLabel("disable", getInstruction(onClickIndex + 1))
    )
}
