/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches/pull/3387
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.patches.youtube.layout.feedrefresh

import app.tada.patcher.Fingerprint
import app.tada.patcher.InstructionLocation
import app.tada.patcher.anyInstruction
import app.tada.patcher.fieldAccess
import app.tada.patcher.opcode
import app.tada.patcher.string
import com.android.tools.smali.dexlib2.Opcode

/**
 * Stores when the loaded feed expires (the first field) and when the feed refresh is scheduled
 * (the second field), both in milliseconds.
 */
internal object FeedExpirationFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string("FEmemberships_and_purchases"),
        string("FEmembership_detail"),
        opcode(Opcode.ADD_LONG),
        fieldAccess(
            opcode = Opcode.IPUT_WIDE,
            definingClass = "this",
            type = "J",
            location = InstructionLocation.MatchAfterImmediately()
        ),
        anyInstruction(
            opcode(Opcode.ADD_LONG),
            opcode(Opcode.ADD_LONG_2ADDR),
            location = InstructionLocation.MatchAfterWithin(10)
        ),
        fieldAccess(
            opcode = Opcode.IPUT_WIDE,
            definingClass = "this",
            type = "J",
            location = InstructionLocation.MatchAfterImmediately()
        )
    )
)
