/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.morphe.patches.instagram.media

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.instagram.misc.extension.EXTENSION_PACKAGE
import app.morphe.patches.instagram.misc.extension.classesHolding
import app.morphe.patches.instagram.misc.extension.enableStatus
import app.morphe.patches.instagram.misc.extension.instagramExtensionPatch
import app.morphe.patches.instagram.misc.extension.localRegisterCount
import app.morphe.patches.instagram.misc.extension.requireStatusMethod
import app.morphe.patches.instagram.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val PATCH = "Use the system photo picker"
private const val SYSTEM_PHOTO_PICKER = "$EXTENSION_PACKAGE/media/SystemPhotoPicker;"
internal const val GET_PICKER_INTENT = "$SYSTEM_PHOTO_PICKER->getPickerIntent()Landroid/content/Intent;"
internal const val SHOW_ENTRY = "$SYSTEM_PHOTO_PICKER->showEntry(I)Z"
internal const val PICKER_ICON = "$SYSTEM_PHOTO_PICKER->pickerIcon()Landroid/graphics/drawable/Drawable;"
internal const val PICKER_LABEL = "$SYSTEM_PHOTO_PICKER->pickerLabel()Ljava/lang/String;"

/** The component Instagram looks for when it builds its outside-gallery entry. */
internal const val PHOTOS_COMPONENT = "com.google.android.apps.photos"

/** The bundle flag the creation flow reads to decide whether that entry shows in the gallery sheet. */
internal const val ENTRY_POINT_ARG = "args_can_show_google_photos_entry_point"

/**
 * Routes Instagram's own outside-gallery entry to Android's system photo picker. See SystemPhotoPicker
 * in the extension for what each hook answers.
 *
 * Instagram builds an entry for Google Photos when it finds it on the phone: a small object holding
 * the intent the entry fires, its label and its icon. A hidden flag decides whether that entry shows
 * up when you add a photo to a post. The patch asks the extension for the system picker's intent
 * first thing in that factory, and routes the flag through the extension so the entry is there even
 * without Google Photos. With the switch off both stay exactly as Instagram left them.
 */
@Suppress("unused")
val systemPhotoPickerPatch = bytecodePatch(
    name = "Use the system photo picker",
    description = "Adds an entry to Instagram's gallery sheet that opens Android's own photo picker when you make " +
        "a post, so you pick from your photos and any cloud gallery and Instagram only gets what you choose. " +
        "Starts off. Turn it on in TADa settings > Ads and privacy.",
) {
    category("Privacy")
    dependsOn(settingsPatch, instagramExtensionPatch)
    compatibleWith(*AppCompatibilities.instagram())

    execute {
        // Both sites are found and checked, and SettingsStatus is confirmed to carry the switch's
        // method, before either hook changes an instruction.
        requireStatusMethod("systemPhotoPicker")
        injectSystemPhotoPicker()
        enableStatus("systemPhotoPicker")
    }
}

/** Finds and checks both sites, then hooks them. A build that fails any check is left as it was. */
internal fun BytecodePatchContext.injectSystemPhotoPicker() {
    val sites = findPickerSites()
    routeToSystemPicker(sites)
    forceEntryVisible(sites)
}

/**
 * The two sites: the [factory] that builds the outside-gallery entry (a [pojoType] it makes with
 * [initRef] and fills through [intentField], [iconField] and [labelField]), and the [config] method
 * that reads the show-the-entry flag into [configRegister], moved at [configMoveAt].
 */
internal class PickerSites(
    val factory: Method,
    val pojoType: String,
    val initRef: MethodReference,
    val intentField: FieldReference,
    val iconField: FieldReference,
    val labelField: FieldReference,
    val config: Method,
    val configMoveAt: Int,
    val configRegister: Int,
)

internal fun BytecodePatchContext.findPickerSites(): PickerSites {
    // The factory is static, takes a PackageManager, returns the entry object, and names Google
    // Photos' package. The inline click builder names it too, but it's an instance method returning
    // nothing, so this filter leaves it alone.
    val factories = classesHolding(PHOTOS_COMPONENT).flatMap { it.methods }.filter { method ->
        AccessFlags.STATIC.isSet(method.accessFlags) &&
            method.parameterTypes.map(Any::toString) == listOf("Landroid/content/pm/PackageManager;") &&
            method.returnType.startsWith("L") &&
            method.code().any { it.stringLoaded() == PHOTOS_COMPONENT }
    }
    val factory = factories.singleOrNull()
        ?: refuse("expected one Google Photos gallery-entry factory holding \"$PHOTOS_COMPONENT\", found ${factories.size}")
    if (factory.localRegisterCount() < 2) refuse("the gallery-entry factory ${factory.text()} has too few registers to hook")
    val pojoType = factory.returnType
    val code = factory.code()

    // Instagram fills the entry object with the intent it fires, its icon and its label. The fields
    // are read off the factory's own stores, by type, so the hook writes to the same ones whatever
    // this build calls them.
    val fields = code.filter { it.opcode == Opcode.IPUT_OBJECT }.mapNotNull { it.fieldReference() }
        .filter { it.definingClass == pojoType }.distinctBy { it.toString() }
    val intentField = fields.singleOrNull { it.type == "Landroid/content/Intent;" }
        ?: refuse("expected one Intent field on the entry object $pojoType, found ${fields.count { it.type == "Landroid/content/Intent;" }}")
    val iconField = fields.singleOrNull { it.type == "Landroid/graphics/drawable/Drawable;" }
        ?: refuse("expected one Drawable field on the entry object $pojoType, found ${fields.count { it.type == "Landroid/graphics/drawable/Drawable;" }}")
    val labelField = fields.singleOrNull { it.type == "Ljava/lang/String;" }
        ?: refuse("expected one String field on the entry object $pojoType, found ${fields.count { it.type == "Ljava/lang/String;" }}")

    // The constructor the factory uses to make the entry object: the entry has none of its own, so
    // this mirrors whatever <init> the factory calls on it rather than assuming Object's.
    val newAt = code.indexOfFirst { it.opcode == Opcode.NEW_INSTANCE && (it.typeReference() == pojoType) }
    if (newAt < 0) refuse("the factory ${factory.text()} never builds a $pojoType")
    val newReg = (code[newAt] as OneRegisterInstruction).registerA
    val initRef = code.drop(newAt + 1).firstNotNullOfOrNull { insn ->
        val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference
        if (insn.opcode == Opcode.INVOKE_DIRECT && ref?.name == "<init>" &&
            (insn as FiveRegisterInstruction).registerCount >= 1 && insn.registerC == newReg
        ) ref else null
    } ?: refuse("the factory ${factory.text()} never initialises the $pojoType it builds")

    // The flag that shows the entry: exactly one method reads "$ENTRY_POINT_ARG" through getBoolean and
    // moves the result into a register the creation flow then reads. A second method writes the same
    // flag into the args bundle with putBoolean; it holds the string too but never reads it, so matching
    // on the read leaves it alone (Instagram 452 keeps both on one class).
    val readers = classesHolding(ENTRY_POINT_ARG).flatMap { it.methods }
        .mapNotNull { method -> method.readsEntryFlag()?.let { method to it } }
    val (config, flagRead) = readers.singleOrNull()
        ?: refuse("expected one method reading \"$ENTRY_POINT_ARG\" through getBoolean, found ${readers.size}")
    val configMoveAt = flagRead.moveAt
    val configRegister = flagRead.register

    return PickerSites(factory, pojoType, initRef, intentField, iconField, labelField, config, configMoveAt, configRegister)
}

/**
 * First thing in the factory: build the entry from the system picker's intent and return it, if the
 * switch is on and the phone has a picker. Otherwise fall through, so Instagram builds its own Google
 * Photos entry exactly as before. The entry keeps its icon and label from the extension.
 */
private fun BytecodePatchContext.routeToSystemPicker(sites: PickerSites) {
    mutable(sites.factory).apply {
        addInstructionsWithLabels(
            0,
            """
                invoke-static { }, $GET_PICKER_INTENT
                move-result-object v0
                if-eqz v0, :original
                new-instance v1, ${sites.pojoType}
                invoke-direct { v1 }, ${sites.initRef.text()}
                iput-object v0, v1, ${sites.intentField.text()}
                invoke-static { }, $PICKER_ICON
                move-result-object v0
                iput-object v0, v1, ${sites.iconField.text()}
                invoke-static { }, $PICKER_LABEL
                move-result-object v0
                iput-object v0, v1, ${sites.labelField.text()}
                return-object v1
            """,
            ExternalLabel("original", getInstruction(0)),
        )
    }
}

/**
 * After the creation flow reads the show-the-entry flag, the extension's answer in its place: true
 * while the switch is on so the entry is reachable, otherwise the flag as Instagram set it. The range
 * form passes the register whatever its number.
 */
private fun BytecodePatchContext.forceEntryVisible(sites: PickerSites) {
    val register = sites.configRegister
    mutable(sites.config).addInstructions(
        sites.configMoveAt + 1,
        """
            invoke-static/range { v$register .. v$register }, $SHOW_ENTRY
            move-result v$register
        """,
    )
}

/** Where a method reads the show-the-entry flag: getBoolean's result is moved into [register] at [moveAt]. */
private class FlagRead(val moveAt: Int, val register: Int)

/**
 * The flag is read where the method loads "$ENTRY_POINT_ARG", hands that string to getBoolean within a
 * few instructions, and moves the boolean result straight after. The method that only writes the flag
 * (putBoolean, in the args builder) holds the string but never reads it, so it never matches. Each load
 * of the string is tried, so a method that both writes and reads it is still found.
 */
private fun Method.readsEntryFlag(): FlagRead? {
    val code = code()
    code.forEachIndexed { strAt, insn ->
        if (insn.stringLoaded() != ENTRY_POINT_ARG) return@forEachIndexed
        val strReg = (insn as OneRegisterInstruction).registerA
        val getBoolAt = (strAt + 1 until minOf(code.size, strAt + 12)).firstOrNull { at ->
            val call = code[at]
            val ref = (call as? ReferenceInstruction)?.reference as? MethodReference
            (call.opcode == Opcode.INVOKE_VIRTUAL || call.opcode == Opcode.INVOKE_INTERFACE) &&
                ref?.name == "getBoolean" && ref.returnType == "Z" &&
                strReg in (call as FiveRegisterInstruction).argumentRegisters()
        } ?: return@forEachIndexed
        val moveAt = getBoolAt + 1
        if (code.getOrNull(moveAt)?.opcode == Opcode.MOVE_RESULT) {
            return FlagRead(moveAt, (code[moveAt] as OneRegisterInstruction).registerA)
        }
    }
    return null
}

private fun refuse(detail: String): Nothing = throw PatchException("$PATCH: $detail")

private fun BytecodePatchContext.mutable(method: Method): MutableMethod =
    mutableClassDefBy(method.definingClass).methods.single {
        it.name == method.name && it.parameterTypes.map(Any::toString) == method.parameterTypes.map(Any::toString) &&
            it.returnType == method.returnType
    }

private fun Method.code(): List<Instruction> = implementation?.instructions?.toList().orEmpty()

private fun Instruction.stringLoaded(): String? =
    if (opcode != Opcode.CONST_STRING && opcode != Opcode.CONST_STRING_JUMBO) null
    else ((this as ReferenceInstruction).reference as StringReference).string

private fun Instruction.fieldReference(): FieldReference? = (this as? ReferenceInstruction)?.reference as? FieldReference

private fun Instruction.typeReference(): String? = ((this as? ReferenceInstruction)?.reference as? TypeReference)?.type

private fun FiveRegisterInstruction.argumentRegisters(): List<Int> =
    listOf(registerC, registerD, registerE, registerF, registerG).take(registerCount)

private fun FieldReference.text(): String = "$definingClass->$name:$type"

private fun MethodReference.text(): String = "$definingClass->$name(${parameterTypes.joinToString("")})$returnType"

private fun Method.text(): String = "$definingClass->$name(${parameterTypes.joinToString("")})$returnType"
