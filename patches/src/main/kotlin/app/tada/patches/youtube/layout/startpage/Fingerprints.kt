package app.tada.patches.youtube.layout.startpage

import app.tada.patcher.Fingerprint
import app.tada.patcher.fieldAccess
import app.tada.patcher.literal
import app.tada.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object IntentActionFingerprint : Fingerprint(
    parameters = listOf("Landroid/content/Intent;"),
    filters = listOf(
        string("has_handled_intent")
    )
)

internal object BrowseIdFingerprint : Fingerprint(
    returnType = "L",

    //parameters() // 20.30 and earlier is no parameters = listOf(.  20.31+ parameter is L.),
    filters = listOf(
        string("FEwhat_to_watch"),
        literal(512),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, type = "Ljava/lang/String;")
    )
)
