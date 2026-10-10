/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.patches.youtube.misc.fix.likebutton

import app.tada.patcher.Fingerprint
import app.tada.patcher.InstructionLocation.MatchAfterImmediately
import app.tada.patcher.InstructionLocation.MatchAfterWithin
import app.tada.patcher.methodCall
import app.tada.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object LottieAnimationViewTagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(
        methodCall(
            opcodes = listOf(
                Opcode.INVOKE_INTERFACE,
                Opcode.INVOKE_INTERFACE_RANGE
            ),
            parameters = listOf(),
            returnType = "Ljava/lang/String;"
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        methodCall(
            smali = "Lcom/airbnb/lottie/LottieAnimationView;->getTag(I)Ljava/lang/Object;",
            location = MatchAfterWithin(5)
        )
    )
)
