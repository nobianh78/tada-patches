/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.morphe.patches.instagram.profile.tabs

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.instagram.misc.extension.EXTENSION_PACKAGE
import app.morphe.patches.instagram.misc.extension.classesLoadingString
import app.morphe.patches.instagram.misc.extension.enableStatus
import app.morphe.patches.instagram.misc.extension.instagramExtensionPatch
import app.morphe.patches.instagram.misc.extension.jumpTargets
import app.morphe.patches.instagram.misc.extension.loadsString
import app.morphe.patches.instagram.misc.extension.requireStatusMethod
import app.morphe.patches.instagram.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val PATCH = "Hide profile tabs"
internal const val PROFILE_TABS = "$EXTENSION_PACKAGE/profile/ProfileTabs;"
internal const val TABS_HOOK = "$PROFILE_TABS->tabs(Ljava/util/List;)Ljava/util/List;"

private const val LIST = "Ljava/util/List;"
private const val USER = "Lcom/instagram/user/model/User;"

/** The keys Instagram gives the profile's tabs, which the builder of a profile's tab list names. */
internal val PROFILE_TAB_KEYS = listOf(
    "profile_media_grid", "profile_clips", "profile_reposts", "profile_tagged_media_photos_of_you",
)

/**
 * Leaves the Reels, Reposts and Tagged tabs off profiles, yours and other people's, as the user
 * picks. Included in the default selection with nothing picked, so every tab stays until the user
 * turns one off.
 *
 * Instagram builds the list of a profile's tab keys in one method, and the strip, the pager and the
 * tab providers are made from that list. The patch hands the list to the extension just before the
 * method returns it, so a tab is dropped by its key and not by where it sits. The grid is never
 * offered. With a single tab left Instagram draws no strip, as it does for an account that only
 * has a grid.
 */
@Suppress("unused")
val hideProfileTabsPatch = bytecodePatch(
    name = "Hide profile tabs",
    description = "Lets you take the Reels, Reposts and Tagged tabs off profiles, yours and other people's, and keep " +
        "the grid. Starts with every tab showing. Pick which to hide in TADa settings > Profiles.",
    default = true,
) {
    category("Profiles")
    dependsOn(settingsPatch, instagramExtensionPatch)
    compatibleWith(*AppCompatibilities.instagram())

    execute {
        requireStatusMethod("profileTabs")
        hideProfileTabs(findTabList())
        enableStatus("profileTabs")
    }
}

private fun refuse(detail: String): Nothing = throw PatchException("$PATCH: $detail")

/** Where the hook goes: the builder of a profile's tab keys and its one return. */
internal class TabListSite(val type: String, val name: String, val parameters: List<String>, val returns: Int, val register: Int)

/**
 * Finds the builder of a profile's tab keys: the one method taking a user and answering a list that
 * names all four of [PROFILE_TAB_KEYS]. It has to be the only one, and return in one place, with
 * nothing jumping onto that return, since a jump keeps its target on the original instruction and
 * would skip the hook. Fails before anything changes when any of that isn't so, since that's an
 * update this patch hasn't seen.
 */
internal fun BytecodePatchContext.findTabList(): TabListSite {
    val found = classesLoadingString(PROFILE_TAB_KEYS.last()).flatMap { holder ->
        holder.methods.filter { method ->
            method.returnType == LIST && method.parameterTypes.map(CharSequence::toString) == listOf(USER) &&
                PROFILE_TAB_KEYS.all { key -> loadsString(method, key) }
        }
    }
    val builder = found.singleOrNull()
        ?: refuse("expected one builder of a profile's tab keys, found ${found.size}")
    val where = "${builder.definingClass}->${builder.name}"
    val code = builder.implementation?.instructions?.toList().orEmpty()
    val returns = code.indices.filter { code[it].opcode == Opcode.RETURN_OBJECT }
    val at = returns.singleOrNull() ?: refuse("$where returns in ${returns.size} places")
    if (at in builder.jumpTargets()) refuse("$where has a jump onto its return")
    val register = (code[at] as OneRegisterInstruction).registerA
    return TabListSite(builder.definingClass, builder.name, builder.parameterTypes.map(CharSequence::toString), at, register)
}

/** Hands the list to [TABS_HOOK] right before the builder returns it, and returns what comes back. */
internal fun BytecodePatchContext.hideProfileTabs(site: TabListSite) {
    val builder = mutableClassDefBy(site.type).methods.single {
        it.name == site.name && it.parameterTypes.map(CharSequence::toString) == site.parameters
    }
    val v = site.register
    builder.addInstructions(
        site.returns,
        """
            invoke-static/range { v$v .. v$v }, $TABS_HOOK
            move-result-object v$v
        """,
    )
}
