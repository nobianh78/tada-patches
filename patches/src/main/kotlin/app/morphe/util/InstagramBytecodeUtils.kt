package app.morphe.util

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

fun BytecodePatchContext.superclassChain(type: String, maxDepth: Int = 20): Sequence<String> = sequence {
    val seen = mutableSetOf<String>()
    var at: String? = type
    var depth = 0
    while (at != null && depth++ < maxDepth && seen.add(at)) {
        yield(at)
        at = classDefByOrNull(at)?.superclass
    }
}

fun BytecodePatchContext.extendsClass(type: String, target: String, maxDepth: Int = 20): Boolean =
    superclassChain(type, maxDepth).any { it == target }

fun BytecodePatchContext.traverseClassHierarchy(targetClass: MutableClass, callback: MutableClass.() -> Unit) {
    callback(targetClass)

    targetClass.superclass ?: return

    mutableClassDefByOrNull(targetClass.superclass!!)?.let {
        traverseClassHierarchy(it, callback)
    }
}

fun Method.moveResultRegisterAfter(callIndex: Int, what: String): Int {
    val next = implementation?.instructions?.elementAtOrNull(callIndex + 1)
        ?: throw PatchException("$what: nothing follows the call at $callIndex in $definingClass->$name")
    if (next.opcode != Opcode.MOVE_RESULT && next.opcode != Opcode.MOVE_RESULT_OBJECT &&
        next.opcode != Opcode.MOVE_RESULT_WIDE
    ) {
        throw PatchException(
            "$what: the call at $callIndex in $definingClass->$name is followed by " +
                "${next.opcode.name}, not a move-result, so its result is not kept.",
        )
    }
    return (next as OneRegisterInstruction).registerA
}

val conditionalBranchOpcodes: java.util.EnumSet<Opcode> = java.util.EnumSet.of(
    Opcode.IF_EQ, Opcode.IF_NE, Opcode.IF_LT, Opcode.IF_GE, Opcode.IF_GT, Opcode.IF_LE,
    Opcode.IF_EQZ, Opcode.IF_NEZ, Opcode.IF_LTZ, Opcode.IF_GEZ, Opcode.IF_GTZ, Opcode.IF_LEZ
)

val com.android.tools.smali.dexlib2.iface.instruction.Instruction.isConditionalBranchInstruction: Boolean
    get() = this.opcode in conditionalBranchOpcodes

