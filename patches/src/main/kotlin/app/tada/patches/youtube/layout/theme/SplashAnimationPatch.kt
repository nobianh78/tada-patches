/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches/pull/3178
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.patches.youtube.layout.theme

import app.tada.patcher.extensions.InstructionExtensions.getInstruction
import app.tada.patcher.extensions.InstructionExtensions.replaceInstruction
import app.tada.patcher.methodCall
import app.tada.patcher.patch.bytecodePatch
import app.tada.patches.shared.misc.lottie.LOTTIE_ANIMATION_VIEW_CLASS_TYPE
import app.tada.patches.shared.misc.lottie.LottieAnimationViewSetAnimationIntFingerprint
import app.tada.patches.shared.misc.lottie.lottieAnimationPatch
import app.tada.patches.youtube.layout.seekbar.LottieSplashScreenFeatureFlagFingerprint
import app.tada.patches.youtube.layout.seekbar.LottieSplashScreenFeatureFlagLegacyFingerprint
import app.tada.patches.youtube.misc.extension.sharedExtensionPatch
import app.tada.patches.youtube.misc.playservice.is_20_31_or_greater
import app.tada.patches.youtube.misc.playservice.versionCheckPatch
import app.tada.patches.youtube.shared.YouTubeActivityOnCreateFingerprint
import app.tada.util.findInstructionIndicesReversedOrThrow
import app.tada.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_CLASS =
    "Lapp/tada/extension/youtube/patches/theme/ThemePatch;"

/**
 * Hands the animation the app plays while it starts over to extension code,
 * which decides what is played and with which colors.
 */
val splashAnimationPatch = bytecodePatch {

    dependsOn(sharedExtensionPatch, lottieAnimationPatch, versionCheckPatch)

    execute {
        // Force newer Lottie animation.
        (if (is_20_31_or_greater) {
            LottieSplashScreenFeatureFlagFingerprint
        } else {
            LottieSplashScreenFeatureFlagLegacyFingerprint
        }).matchAll().forEach {
            it.method.insertLiteralOverride(
                it.instructionMatches.first().index,
                "$EXTENSION_CLASS->useLotteLaunchSplashScreen(Z)Z"
            )
        }

        YouTubeActivityOnCreateFingerprint.method.apply {
            val setAnimationIntMethodName =
                LottieAnimationViewSetAnimationIntFingerprint.originalMethod.name

            findInstructionIndicesReversedOrThrow(
                methodCall(
                    definingClass = LOTTIE_ANIMATION_VIEW_CLASS_TYPE,
                    name = setAnimationIntMethodName
                )
            ).forEach { index ->
                val instruction = getInstruction<FiveRegisterInstruction>(index)

                replaceInstruction(
                    index,
                    "invoke-static { v${instruction.registerC}, v${instruction.registerD} }, " +
                        "$EXTENSION_CLASS->setSplashAnimationLottie(Lcom/airbnb/lottie/LottieAnimationView;I)V"
                )
            }
        }
    }
}
