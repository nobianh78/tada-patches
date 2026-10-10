/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.patches.shared.misc.fix.bitmap

import app.tada.patcher.Fingerprint
import app.tada.patcher.extensions.InstructionExtensions.replaceInstruction
import app.tada.patcher.methodCall
import app.tada.patcher.patch.bytecodePatch
import app.tada.util.fiveRegisters
import app.tada.util.matchAllMethodIndicesForEach

private const val EXTENSION_CLASS =
    "Lapp/tada/extension/shared/patches/FixRecycledBitmapPatch;"

val fixRecycledBitmapPatch = bytecodePatch(
    description = "Fixes recycled bitmap crashes by routing putBitmap through the extension class."
) {

    execute {
        Fingerprint(
            filters = listOf(
                methodCall(
                    definingClass = $$"Landroid/media/MediaMetadata$Builder;",
                    name = "putBitmap",
                    parameters = listOf("Ljava/lang/String;", "Landroid/graphics/Bitmap;")
                )
            ),
            custom = { _, classDef ->
                !classDef.type.startsWith("Lapp/tada/extension")
            }
        ).matchAllMethodIndicesForEach(requireMatches = false) { index ->
            val registers = fiveRegisters(index)

            replaceInstruction(
                index,
                $"invoke-static { $registers }, $EXTENSION_CLASS->putBitmap(" +
                        "Landroid/media/MediaMetadata\$Builder;Ljava/lang/String;Landroid/graphics/Bitmap;)" +
                        "Landroid/media/MediaMetadata\$Builder;"
            )
        }
    }
}