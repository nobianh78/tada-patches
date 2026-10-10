/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to TADa contributions.
 */

package app.tada.patches.youtube.misc.fix.backtoexitgesture

import app.tada.patcher.Fingerprint
import app.tada.patcher.InstructionLocation.MatchAfterImmediately
import app.tada.patcher.OpcodesFilter
import app.tada.patcher.checkCast
import app.tada.patcher.literal
import app.tada.patcher.methodCall
import app.tada.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ScrollPositionFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IF_NEZ,
        Opcode.INVOKE_DIRECT,
        Opcode.RETURN_VOID
    ),
    strings = listOf("scroll_position")
)

internal object RecyclerViewTopScrollingFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(smali = "Ljava/util/Iterator;->next()Ljava/lang/Object;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        checkCast("Landroid/support/v7/widget/RecyclerView;", MatchAfterImmediately()),
        literal(0, location = MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/support/v7/widget/RecyclerView;", location = MatchAfterImmediately()),
        opcode(Opcode.GOTO, MatchAfterImmediately())
    )
)

internal object BackToRefreshFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(45359221)
    )
)
