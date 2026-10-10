/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.patches.youtube.layout.player.buttons

import app.tada.patcher.Fingerprint
import app.tada.patcher.InstructionLocation.MatchAfterImmediately
import app.tada.patcher.methodCall
import app.tada.patcher.opcode
import app.tada.patcher.resource.ResourceType
import app.tada.patcher.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ExploderUIFullscreenButtonFingerprint : Fingerprint(
    classFingerprint = ExploderUIFullscreenButtonParentFingerprint,
    filters = listOf(
        resourceLiteral(ResourceType.ID, "fullscreen_button"),
        methodCall(smali = "Landroid/view/View;->findViewById(I)Landroid/view/View;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately())
    )
)

internal object ExploderUIFullscreenButtonParentFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(
        resourceLiteral(ResourceType.ID, "time_bar_live_label")
    )
)

