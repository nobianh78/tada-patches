/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.morphe.patches.instagram.profile.suggested

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patches.instagram.misc.extension.classesLoadingString
import app.morphe.patches.instagram.misc.extension.jumpTargets
import app.morphe.patches.instagram.misc.extension.stringLoadedAt
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val PATCH = "Hide suggested people on profiles"

internal const val FOLLOW_LIST = "$PROFILE_SUGGESTIONS->followList(I)I"

/**
 * The argument the followers and following list reads to know whether to fetch the accounts it
 * suggests under the list. Instagram keeps the key: it's the name of the fragment's argument.
 */
internal const val FETCH_SUGGESTED = "FollowListFragment.ShouldFetchSuggestedUsers"

/** The reads of a boolean argument the follow list makes, either spelling of it. */
private val GET_BOOLEAN = setOf(
    "Landroid/os/BaseBundle;->getBoolean(Ljava/lang/String;Z)Z",
    "Landroid/os/Bundle;->getBoolean(Ljava/lang/String;Z)Z",
)

/** The follow list's read of [FETCH_SUGGESTED]: its method, and the move-result holding the answer. */
internal class FollowListSite(val type: String, val name: String, val parameters: List<String>, val result: Int, val register: Int)

private fun refuse(detail: String): Nothing = throw PatchException("$PATCH: $detail")

/**
 * Finds the one place that reads [FETCH_SUGGESTED] out of a fragment's arguments: the key loaded
 * (or asked of Redex's string pool, which differs by build), the next call a `getBoolean` handed
 * that key, and its move-result right after. Putting the key into arguments is a `putBoolean`, so
 * the places opening the list don't count. Fails before anything changes when there isn't exactly
 * one such read, or something jumps past its answer, since that's an update this patch hasn't seen.
 */
internal fun BytecodePatchContext.findFollowListSuggestions(): FollowListSite {
    val reads = mutableListOf<FollowListSite>()
    classesLoadingString(FETCH_SUGGESTED).forEach { classDef ->
        classDef.methods.forEach { method ->
            val code = method.implementation?.instructions?.toList() ?: return@forEach
            code.indices.filter { stringLoadedAt(code, it) == FETCH_SUGGESTED }.forEach { at ->
                val key = keyRegister(code, at) ?: return@forEach
                val call = (at + 1 until code.size).firstOrNull { code[it].methodReference() != null } ?: return@forEach
                if (code[call].methodReference().toString() !in GET_BOOLEAN) return@forEach
                if ((code[call] as? FiveRegisterInstruction)?.registerD != key) return@forEach
                val result = code.getOrNull(call + 1)
                if (result?.opcode != Opcode.MOVE_RESULT) refuse("${classDef.type}->${method.name} reads $FETCH_SUGGESTED into nothing")
                if (call + 2 in method.jumpTargets()) refuse("something in ${classDef.type}->${method.name} jumps past its read of $FETCH_SUGGESTED")
                reads += FollowListSite(
                    classDef.type, method.name, method.parameterTypes.map(CharSequence::toString), call + 1,
                    (result as OneRegisterInstruction).registerA,
                )
            }
        }
    }
    return reads.singleOrNull() ?: refuse("expected one read of the follow list's $FETCH_SUGGESTED, found ${reads.size}")
}

/**
 * Hands the follow list's answer to [FOLLOW_LIST] right after its move-result and puts the
 * extension's answer back in the same register. A 0 means the list never asks for suggestions, so
 * no Suggested for you section is fetched or drawn under it.
 */
internal fun BytecodePatchContext.hideFollowListSuggestions(site: FollowListSite) {
    val method = mutableClassDefBy(site.type).methods.single {
        it.name == site.name && it.parameterTypes.map(CharSequence::toString) == site.parameters
    }
    method.addInstructions(
        site.result + 1,
        """
            invoke-static/range { v${site.register} .. v${site.register} }, $FOLLOW_LIST
            move-result v${site.register}
        """,
    )
}

/** The register holding the key loaded at [at]: a const-string's, or the move-result after a pool's answer. */
private fun keyRegister(code: List<Instruction>, at: Int): Int? = when (code[at].opcode) {
    Opcode.CONST_STRING, Opcode.CONST_STRING_JUMBO -> (code[at] as OneRegisterInstruction).registerA
    else -> code.getOrNull(at + 1)?.takeIf { it.opcode == Opcode.MOVE_RESULT_OBJECT }?.let { (it as OneRegisterInstruction).registerA }
}

private fun Instruction.methodReference(): MethodReference? = (this as? ReferenceInstruction)?.reference as? MethodReference
