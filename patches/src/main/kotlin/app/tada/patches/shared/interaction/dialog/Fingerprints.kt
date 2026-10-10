/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.patches.shared.interaction.dialog

import app.tada.patcher.Fingerprint
import app.tada.patcher.InstructionLocation.MatchAfterImmediately
import app.tada.patcher.InstructionLocation.MatchAfterWithin
import app.tada.patcher.fieldAccess
import app.tada.patcher.methodCall
import app.tada.patcher.opcode
import app.tada.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object AdultContentRunnableFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        opcode(Opcode.IGET_OBJECT),
        string("allowControversialContent"),
        methodCall(
            parameters = listOf(),
            returnType = "Z",
            location = MatchAfterWithin(1)
        ),
        string("allowAdultContent")
    )
)

internal object AdultContentSetPropertiesFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf(
        "lastAudioTurnedOnInlinePlaybackId",
        "lastAudioTurnedOffInlinePlaybackId",
        "captionsRequested",
    ),
    filters = listOf(
        opcode(Opcode.IGET_BOOLEAN),
        string("allowAdultContent", location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_BOOLEAN, location = MatchAfterWithin(2)),
        string("allowControversialContent", location = MatchAfterImmediately()),
    )
)
