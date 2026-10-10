/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches/pull/2029
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.patches.music.misc.settings

import app.tada.patcher.Fingerprint
import app.tada.patcher.InstructionLocation.MatchAfterImmediately
import app.tada.patcher.anyInstruction
import app.tada.patcher.fieldAccess
import app.tada.patcher.methodCall
import app.tada.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal const val SETTINGS_HEADERS_FRAGMENT_CLASS =
    "Lcom/google/android/apps/youtube/music/settings/fragment/SettingsHeadersFragment;"

/**
 * YT Music top-level Settings fragment. onCreatePreferences reassigns p0 to the peer object at
 * entry, so the captured iget-object recovers the original fragment from `peer.fragment` at exit.
 *
 * Shared by the patches that hook this method. They all insert at the end of it, so the matched
 * instructions near the method entry keep the indexes cached here.
 */
internal object SettingsHeadersOnCreatePreferencesFingerprint : Fingerprint(
    definingClass = SETTINGS_HEADERS_FRAGMENT_CLASS,
    name = "onCreatePreferences",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;", "Ljava/lang/String;"),
    filters = listOf(
        anyInstruction(
            methodCall(
                definingClass = "this",
                name = "peer"
            ),
            methodCall(
                definingClass = "this",
                name = "internalPeer"
            )
        ),
        opcode(opcode = Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = SETTINGS_HEADERS_FRAGMENT_CLASS
        )
    )
)
