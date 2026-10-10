/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.patches.reddit.misc.openlink

import app.tada.patcher.extensions.InstructionExtensions.getInstruction
import app.tada.patcher.patch.bytecodePatch
import app.tada.patcher.util.proxy.mutableTypes.MutableMethod
import app.tada.patches.reddit.misc.settings.settingsPatch
import app.tada.patches.reddit.misc.version.is_2026_11_0_or_greater
import app.tada.patches.reddit.misc.version.versionCheckPatch
import app.tada.util.getMutableMethod
import app.tada.util.getReference
import app.tada.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.lang.ref.WeakReference

lateinit var screenNavigatorMethodRef: WeakReference<MutableMethod>

val screenNavigatorMethodResolverPatch = bytecodePatch(
    description = "screenNavigatorMethodResolverPatch"
) {
    dependsOn(settingsPatch, versionCheckPatch)

    execute {
        var targetMethod = CustomReportsFingerprint.instructionMatches[2].getMethodCalled()

        targetMethod.apply {
            if (is_2026_11_0_or_greater) {
                val targetIndex = indexOfFirstInstructionOrThrow {
                    val targetReference = getReference<MethodReference>()
                    targetReference?.returnType == "V" &&
                            targetReference.parameterTypes.firstOrNull() == "Landroid/app/Activity;"
                }

                targetMethod = getInstruction<ReferenceInstruction>(targetIndex)
                    .getReference<MethodReference>()!!
                    .getMutableMethod()
            }
        }

        screenNavigatorMethodRef = WeakReference(targetMethod)
    }
}
