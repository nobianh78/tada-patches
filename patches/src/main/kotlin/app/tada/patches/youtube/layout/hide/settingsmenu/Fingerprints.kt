/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches/pull/2029
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.patches.youtube.layout.hide.settingsmenu

import app.tada.patcher.Fingerprint
import app.tada.patcher.methodCall
import app.tada.patcher.opcode
import app.tada.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Synthetic Runnable from YT settings intent handling, fired after the PreferenceScreen builds.
 */
internal object PreferenceScreenSyntheticFingerprint : Fingerprint(
    name = "run",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "V",
    filters = listOf(
        string(":android:show_fragment_args"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = listOf(),
            returnType = "Landroidx/preference/PreferenceScreen;"
        ),
        opcode(Opcode.RETURN_VOID)
    )
)
