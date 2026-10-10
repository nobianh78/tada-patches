/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches/pull/3075
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to TADa contributions.
 */

package app.tada.patches.youtube.layout.returnyoutubedislike

import app.tada.patcher.Fingerprint
import app.tada.patcher.InstructionLocation.MatchAfterWithin
import app.tada.patcher.literal
import app.tada.patcher.methodCall
import app.tada.patcher.newInstance
import app.tada.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

internal object TextComponentDataFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf("L", "L"),
    filters = listOf(
        string("text")
    ),
    custom = { _, classDef ->
        classDef.fields.find { it.type == "Ljava/util/BitSet;" } != null
    }
)
