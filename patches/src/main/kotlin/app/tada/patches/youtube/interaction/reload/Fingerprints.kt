/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to TADa contributions.
 */

package app.tada.patches.youtube.interaction.reload

import app.tada.patcher.Fingerprint
import app.tada.patcher.InstructionLocation.MatchAfterImmediately
import app.tada.patcher.OpcodesFilter
import app.tada.patcher.anyInstruction
import app.tada.patcher.methodCall
import app.tada.patcher.opcode
import app.tada.patcher.parametersMatch
import app.tada.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object MiniAppOpenYtContentCommandEndpointFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        // Matches the same method but different app versions have different strings in different ordering.
        filters = listOf(
            anyInstruction(
                string("InvalidProtocolBufferException while decoding MiniAppMetadata for MiniAppOpenYTContentCommand: "),
                // 21.15+
                string("InvalidProtocolBufferException while decoding MiniAppWebToNativeParams for MiniAppOpenYTContentCommand: ")
            )
        )
    ),
    returnType = "V",
    filters = listOf(
        opcode(Opcode.CHECK_CAST),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            returnType = "V",
            parameters = listOf(),
            location = MatchAfterImmediately()
        )
    ),
    strings = listOf("no error message"),
    custom = { method, _ ->
        !AccessFlags.STATIC.isSet(method.accessFlags) &&
                parametersMatch(
                    method.parameters,
                    listOf("L")
                ) || parametersMatch(
            method.parameters,
            listOf("L", "Ljava/util/Map;") // 21.30+
        )
    }
)

internal object OpenNewVideoIntentParcelableFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf("Landroid/content/Intent;"),
    strings = listOf(
        "android.intent.extra.inventory_identifier",
        "http",
        "vnd.youtube",
        "No video id in the Uri: "
    )
)

internal object BackButtonFinishActivityOnNewVideoIntentFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET,
        Opcode.IF_EQZ,
        Opcode.RETURN_VOID,
        Opcode.IGET_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.IGET_OBJECT,
    ),
)
