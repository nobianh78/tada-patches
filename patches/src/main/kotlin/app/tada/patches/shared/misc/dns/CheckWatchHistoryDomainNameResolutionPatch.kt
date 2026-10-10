package app.tada.patches.shared.misc.dns

import app.tada.patcher.Fingerprint
import app.tada.patcher.extensions.InstructionExtensions.addInstruction
import app.tada.patcher.patch.BytecodePatchBuilder
import app.tada.patcher.patch.BytecodePatchContext
import app.tada.patcher.patch.bytecodePatch

private const val EXTENSION_CLASS =
    "Lapp/tada/extension/shared/patches/CheckWatchHistoryDomainNameResolutionPatch;"

/**
 * Patch shared with YouTube and YT Music.
 */
internal fun checkWatchHistoryDomainNameResolutionPatch(
    block: BytecodePatchBuilder.() -> Unit = {},
    executeBlock: BytecodePatchContext.() -> Unit = {},
    mainActivityFingerprint: Fingerprint
) = bytecodePatch(
    name = "Check watch history domain name resolution",
    description = "Checks if the device DNS server is preventing user watch history from being saved.",
) {
    block()

    execute {
        executeBlock()

        mainActivityFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->checkDnsResolver(Landroid/app/Activity;)V",
        )
    }
}
