package app.tada.patches.shared.misc.checks

import app.tada.patcher.Fingerprint
import app.tada.patcher.extensions.InstructionExtensions.addInstruction
import app.tada.patcher.patch.bytecodePatch
import app.tada.patches.shared.misc.settings.RecommendedAppVersionUtilsFingerprint
import app.tada.util.returnEarly

private const val EXTENSION_CLASS = "Lapp/tada/extension/shared/patches/ExperimentalAppNoticePatch;"

internal fun experimentalAppNoticePatch(
    mainActivityFingerprint: Fingerprint,
    recommendedAppVersion: String
) = bytecodePatch(
    description = "Shows a use dialog message the first time a user launches an experimentally patched app",
) {
    execute {
        RecommendedAppVersionUtilsFingerprint.method.returnEarly(recommendedAppVersion)

        mainActivityFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->showExperimentalNoticeIfNeeded(Landroid/app/Activity;)V",
        )
    }
}
