/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to TADa contributions.
 */

package app.tada.patches.youtube.layout.shortsnoresume

import app.tada.patcher.Fingerprint
import app.tada.patcher.InstructionLocation
import app.tada.patcher.StringComparisonType
import app.tada.patcher.checkCast
import app.tada.patcher.literal
import app.tada.patcher.methodCall
import app.tada.patcher.opcode
import app.tada.patcher.string
import com.android.tools.smali.dexlib2.Opcode

/**
 * 21.30+
 */
internal object UserWasInShortsEvaluateFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_DIRECT_RANGE,
            name = "<init>",
            parameters = listOf("L", "Z", "Z", "L", "Z")
        ),
        methodCall(
            opcode = Opcode.INVOKE_DIRECT_RANGE,
            name = "<init>",
            parameters = listOf("L", "L", "L", "L", "L", "L",  "Ljava/lang/String;"),
            location = InstructionLocation.MatchAfterWithin(50)
        )
    )
)

/**
 * 21.03 - 21.29
 */
internal object UserWasInShortsEvaluateLegacyFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_DIRECT_RANGE,
            name = "<init>",
            parameters = listOf("L", "Z", "Z", "L", "Z")
        ),
        methodCall(
            opcode = Opcode.INVOKE_DIRECT_RANGE,
            name = "<init>",
            parameters = listOf("L", "L", "L", "L", "L", "I"),
            location = InstructionLocation.MatchAfterWithin(50)
        )
    )
)


/**
 * 20.02+
 */
internal object UserWasInShortsListenerFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        checkCast("Ljava/lang/Boolean;"),
        methodCall(smali = "Ljava/lang/Boolean;->booleanValue()Z", location = InstructionLocation.MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, InstructionLocation.MatchAfterImmediately()),
        string("ShortsStartup SetUserWasInShortsListener", StringComparisonType.CONTAINS, InstructionLocation.MatchAfterWithin(30))
    )
)

/**
 * 18.15.40+
 */
internal object UserWasInShortsConfigFingerprint : Fingerprint(
    filters = listOf(
        literal(45358360L)
    )
)
