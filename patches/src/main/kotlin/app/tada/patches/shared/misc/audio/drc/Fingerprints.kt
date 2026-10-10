/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.patches.shared.misc.audio.drc

import app.tada.patcher.Fingerprint
import app.tada.patcher.InstructionLocation.MatchAfterImmediately
import app.tada.patcher.InstructionLocation.MatchAfterWithin
import app.tada.patcher.opcode
import app.tada.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object VolumeNormalizationConfigLegacyFingerprint : Fingerprint (
    filters = listOf(
        opcode(opcode = Opcode.IGET_OBJECT),
        opcode(opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        opcode(opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        opcode(opcode = Opcode.IF_NEZ, location = MatchAfterWithin(5)),
        opcode(opcode = Opcode.IGET_OBJECT),
        string("rng."),
        string(";trkcfg.")
    )
)

internal object VolumeNormalizationConfigFingerprint : Fingerprint (
    filters = listOf(
        string("rng."),
        string(";trkcfg.")
    ),
    custom = { method, _ ->
        method.parameters.size >= 3
    }
)
