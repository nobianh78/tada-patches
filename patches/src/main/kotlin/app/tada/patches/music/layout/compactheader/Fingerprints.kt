package app.tada.patches.music.layout.compactheader

import app.tada.patcher.Fingerprint
import app.tada.patcher.InstructionLocation.MatchAfterImmediately
import app.tada.patcher.opcode
import app.tada.patcher.resource.ResourceType
import app.tada.patcher.resourceLiteral
import com.android.tools.smali.dexlib2.Opcode

internal object ChipCloudFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "chip_cloud"),
        opcode(Opcode.CONST_4, location = MatchAfterImmediately()),
        opcode(Opcode.INVOKE_STATIC, location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately())
    )
)