/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.morphe.patches.instagram.stories.seen

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.instagram.misc.extension.EXTENSION_PACKAGE
import app.morphe.patches.instagram.misc.extension.classesCallingInto
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val STORY_TRAY_ORDER = "$EXTENSION_PACKAGE/stories/StoryTrayOrder;"
internal const val TRAY_ORDER = "$STORY_TRAY_ORDER->order(Ljava/util/List;Ljava/lang/Object;)Ljava/util/List;"

private const val JAVA_LIST = "Ljava/util/List;"
private const val ARRAY_LIST = "Ljava/util/ArrayList;"
private const val OBJECT_TYPE = "Ljava/lang/Object;"

private fun refuseTray(detail: String): Nothing = throw PatchException("$PATCH: $detail")

/**
 * What ordering Home's story tray works on, found before anything changes.
 *
 * [lister] is the reel store's method (on [store], taking [parameters] and returning [JAVA_LIST])
 * that Home's feed adapter asks for the tray's reels. It has one return, [at], of the list in
 * register [result], after the monitor-exit that lets go of the store (register [self] is the
 * store). [seenCheck] is the reel's own check (on [reelClass]) that the account has seen it, the
 * one the store's sort uses, and [sessionField] is the store's field holding the account.
 * [isSeen] is the extension's stub that makes the check.
 */
internal class StoryTrayTargets(
    val store: String,
    val lister: String,
    val parameters: List<String>,
    val at: Int,
    val result: Int,
    val self: Int,
    val reelClass: String,
    val seenCheck: String,
    val sessionField: String,
    internal val isSeen: MutableMethod,
)

/**
 * Finds the reel store's list for Home's feed adapter.
 *
 * Instagram keeps the tray's reels in a store. It has a private sort that splits a list of reels
 * into the ones the account hasn't seen and the ones it has, each half in its order, and puts the
 * unseen ones first, asking each reel [reelClass] one question taking the account, "seen?". The
 * store's method that takes the tray's ids and returns a list of reels is the one the feed adapter
 * calls: it runs that sort only when a server flag says to, so with the flag off the list keeps the
 * server's order. That method is public and synchronized, takes a list and returns a list, calls the
 * sort and a static filter of `(account, list)` in its own class, and has one return, after the
 * monitor-exit. Other methods of the store that take no list, or sort alone, don't have the shape.
 */
internal fun BytecodePatchContext.findStoryTray(reelClass: String): StoryTrayTargets {
    val found = classesCallingInto(reelClass).flatMap { classDef ->
        classDef.methods.mapNotNull { method -> trayLister(classDef.type, classDef.methods, reelClass, method) }
    }
    val tray = found.singleOrNull()
        ?: refuseTray("expected one reel store method handing the tray's reels to Home's feed adapter, found ${found.size}")
    return StoryTrayTargets(
        tray.store, tray.lister, tray.parameters, tray.at, tray.result, tray.self, tray.reelClass, tray.seenCheck, tray.sessionField,
        trayStub(),
    )
}

/** What [trayLister] finds in a method, before the extension's stub is looked up. */
private class TrayShape(
    val store: String,
    val lister: String,
    val parameters: List<String>,
    val at: Int,
    val result: Int,
    val self: Int,
    val reelClass: String,
    val seenCheck: String,
    val sessionField: String,
)

private fun trayLister(store: String, methods: Iterable<Method>, reelClass: String, method: Method): TrayShape? {
    if (AccessFlags.STATIC.isSet(method.accessFlags) || !AccessFlags.PUBLIC.isSet(method.accessFlags)) return null
    if (method.returnType != JAVA_LIST || method.parameterTypes.map(Any::toString) != listOf(JAVA_LIST)) return null
    val code = method.implementation?.instructions?.toList() ?: return null
    if (code.none { it.opcode == Opcode.MONITOR_ENTER }) return null
    val calls = code.mapNotNull { it.trayCall() }
    val sort = methods.filter { candidate ->
        !AccessFlags.STATIC.isSet(candidate.accessFlags) && candidate.returnType == ARRAY_LIST &&
            candidate.parameterTypes.map(Any::toString) == listOf(JAVA_LIST) &&
            calls.any { it.definingClass == store && it.name == candidate.name && it.parameterTypes.map(Any::toString) == listOf(JAVA_LIST) }
    }.singleOrNull() ?: return null
    val filter = calls.any {
        it.definingClass == store && it.returnType == ARRAY_LIST && it.parameterTypes.map(Any::toString) == listOf(USER_SESSION, JAVA_LIST)
    }
    if (!filter) return null

    val sortCode = sort.implementation?.instructions?.toList() ?: return null
    val checks = sortCode.filter { it.opcode == Opcode.INVOKE_VIRTUAL || it.opcode == Opcode.INVOKE_VIRTUAL_RANGE }
        .mapNotNull { it.trayCall() }
        .filter { it.definingClass == reelClass && it.returnType == "Z" && it.parameterTypes.map(Any::toString) == listOf(USER_SESSION) }
        .map { it.name }.distinct()
    // Only a virtual call: the stub makes the same call with invoke-virtual.
    val seenCheck = checks.singleOrNull() ?: return null
    val session = sortCode.mapNotNull { (it as? ReferenceInstruction)?.reference as? FieldReference }
        .filter { it.definingClass == store && it.type == USER_SESSION }.map { it.name }.distinct().singleOrNull() ?: return null

    val returns = code.indices.filter { code[it].opcode == Opcode.RETURN_OBJECT }
    val at = returns.singleOrNull() ?: return null
    if (at == 0 || code[at - 1].opcode != Opcode.MONITOR_EXIT) return null
    val result = (code[at] as OneRegisterInstruction).registerA
    val self = method.implementation!!.registerCount - 1 - 1
    // The monitor-exit lets go of the store itself, so its register still holds the store at the return.
    if ((code[at - 1] as OneRegisterInstruction).registerA != self) return null
    if (result > 15 || self > 15 || result == self) return null
    return TrayShape(
        store = store, lister = method.name, parameters = listOf(JAVA_LIST), at = at, result = result, self = self,
        reelClass = reelClass, seenCheck = seenCheck, sessionField = session,
    )
}

private fun Instruction.trayCall(): MethodReference? = (this as? ReferenceInstruction)?.reference as? MethodReference

private fun BytecodePatchContext.trayStub(): MutableMethod {
    val order = classDefByOrNull(STORY_TRAY_ORDER)?.let { mutableClassDefBy(STORY_TRAY_ORDER) }
        ?: refuseTray("$STORY_TRAY_ORDER isn't in the extension")
    order.methods.singleOrNull {
        "${it.name}(${it.parameterTypes.joinToString("")})${it.returnType}" == TRAY_ORDER.substringAfter("->") &&
            AccessFlags.STATIC.isSet(it.accessFlags) && AccessFlags.PUBLIC.isSet(it.accessFlags)
    } ?: refuseTray("$STORY_TRAY_ORDER has no public static hook ${TRAY_ORDER.substringAfter("->")}")
    return order.methods.singleOrNull {
        it.name == "seen" && it.returnType == "Z" && AccessFlags.STATIC.isSet(it.accessFlags) &&
            it.parameterTypes.map(Any::toString) == listOf(OBJECT_TYPE, OBJECT_TYPE)
    } ?: refuseTray("$STORY_TRAY_ORDER has no static Z seen(${OBJECT_TYPE}${OBJECT_TYPE})")
}

/**
 * Hands the list the reel store gives Home's feed adapter, just before it returns, to the
 * extension, which moves the reels the account has watched behind the ones it hasn't while views
 * are held back and watched stories are grayed (#241). The stub asks Instagram's own check.
 */
internal fun BytecodePatchContext.orderStoryTray(found: StoryTrayTargets) {
    val lister = mutableClassDefBy(found.store).methods.single {
        it.name == found.lister && it.parameterTypes.map(Any::toString) == found.parameters && it.returnType == JAVA_LIST
    }
    val code = lister.implementation!!.instructions.toList()
    if (code.getOrNull(found.at)?.opcode != Opcode.RETURN_OBJECT || code.getOrNull(found.at - 1)?.opcode != Opcode.MONITOR_EXIT) {
        throw PatchException("$PATCH: ${found.store}->${found.lister} changed before its list was hooked")
    }
    lister.addInstructionsAtControlFlowLabel(
        found.at,
        """
            invoke-static { v${found.result}, v${found.self} }, $TRAY_ORDER
            move-result-object v${found.result}
        """,
    )
    // p0 and p1 are the store and a reel. The reel is checked for its own class first; every path returns its own value.
    found.isSeen.addInstructionsWithLabels(
        0,
        """
            check-cast p0, ${found.store}
            iget-object p0, p0, ${found.store}->${found.sessionField}:$USER_SESSION
            check-cast p1, ${found.reelClass}
            invoke-virtual { p1, p0 }, ${found.reelClass}->${found.seenCheck}(${USER_SESSION})Z
            move-result p0
            return p0
        """,
    )
}
