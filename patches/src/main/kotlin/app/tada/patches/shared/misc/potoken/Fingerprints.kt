/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches/pull/2618
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.patches.shared.misc.potoken

import app.tada.patcher.Fingerprint
import app.tada.patcher.InstructionLocation.MatchAfterWithin
import app.tada.patcher.methodCall
import app.tada.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ServiceBindIntentUtilsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    filters = listOf(
        string("ServiceBindIntentUtils"),
        string("serviceActionBundleKey"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = "Landroid/os/Bundle;->putString(Ljava/lang/String;Ljava/lang/String;)V",
            location = MatchAfterWithin(5)
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = "Landroid/content/ContentResolver;->acquireUnstableContentProviderClient(Landroid/net/Uri;)Landroid/content/ContentProviderClient;",
            location = MatchAfterWithin(10)
        )
    )
)

