package app.tada.patches.music.layout.navigationbar

import app.tada.patcher.Fingerprint
import app.tada.patcher.InstructionLocation.MatchAfterImmediately
import app.tada.patcher.InstructionLocation.MatchAfterWithin
import app.tada.patcher.anyInstruction
import app.tada.patcher.checkCast
import app.tada.patcher.fieldAccess
import app.tada.patcher.methodCall
import app.tada.patcher.newInstance
import app.tada.patcher.opcode
import app.tada.patcher.string
import app.tada.patcher.resource.ResourceType
import app.tada.patcher.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object TabLayoutTextFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        anyInstruction(
            string("FEmusic_search"), // 8.49 and lower.
            string("FEsearch"), // 8.50+
            newInstance("Ljava/util/ArrayList;") // 9.21+
        ),

        // Hide navigation label.
        resourceLiteral(
            type = ResourceType.ID,
            name = "text1"
        ),
        methodCall(
            smali = "Landroid/view/View;->findViewById(I)Landroid/view/View;",
            location = MatchAfterWithin(5)
        ),
        checkCast(
            type = "Landroid/widget/TextView;",
            location = MatchAfterWithin(5)
        ),

        // Set navigation enum.
        anyInstruction(
            opcode(Opcode.SGET_OBJECT),
            opcode(Opcode.IGET_OBJECT)
        ),
        fieldAccess(
            opcode = Opcode.IGET,
            type = "I",
            location = MatchAfterWithin(5)
        ),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            returnType = "L",
            parameters = listOf("I"),
            location = MatchAfterWithin(5)
        ),
        opcode(
            opcode = Opcode.MOVE_RESULT_OBJECT,
            location = MatchAfterImmediately()
        ),

        // Hide navigation buttons.
        methodCall(
            name = "getVisibility"
        )
    )
)
