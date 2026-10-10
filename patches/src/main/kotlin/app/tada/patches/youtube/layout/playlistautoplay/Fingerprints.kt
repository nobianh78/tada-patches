/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches/pull/2628
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.patches.youtube.layout.playlistautoplay

import app.tada.patcher.Fingerprint
import app.tada.patcher.opcode
import app.tada.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object NavigationIntentEnumFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    filters = listOf(
        string("NEXT"),
        string("PREVIOUS"),
        string("AUTOPLAY"),
        string("AUTONAV"),
        string("JUMP"),
        opcode(Opcode.RETURN_VOID),
    ),
    custom = { _, classDef ->
        classDef.methods.any { it.name == "<init>" && it.parameterTypes.size > 2 }
    },
)
