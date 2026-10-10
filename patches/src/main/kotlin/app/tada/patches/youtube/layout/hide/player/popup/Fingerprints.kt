package app.tada.patches.youtube.layout.hide.player.popup

import app.tada.patcher.Fingerprint
import app.tada.patcher.InstructionLocation.MatchAfterWithin
import app.tada.patcher.methodCall
import app.tada.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

internal object PlayerPopupPanelsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;", "L"),
    filters = listOf(
        string(
            "triggered_on_ui_ready",
            location = MatchAfterWithin(6)
        ),
        methodCall(
            smali = "Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
            location = MatchAfterWithin(3)
        ),
        methodCall(
            smali = "Ljava/util/Iterator;->hasNext()Z",
            location = MatchAfterWithin(4)
        )
    )
)
