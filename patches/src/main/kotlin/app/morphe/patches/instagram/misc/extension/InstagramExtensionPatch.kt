package app.morphe.patches.instagram.misc.extension

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.extension.EXTENSION_CLASS_DESCRIPTOR
import app.morphe.patches.shared.misc.extension.MorpheUtilsBuildIdentityFingerprint
import app.morphe.patches.shared.misc.extension.MorpheUtilsPatchesVersionFingerprint
import app.morphe.util.returnEarly
import app.morphe.util.superclassChain

/** The application class the manifest names. A manifest name is never obfuscated. */
internal const val INSTAGRAM_APPLICATION = "Lcom/instagram/app/InstagramAppShell;"

/**
 * Loads the Instagram extension payload and hands it the application context.
 */
val instagramExtensionPatch = bytecodePatch {
    extendWith("extensions/instagram.mpe")

    execute {
        // Verify the extension class exists in the merged dex.
        classDefBy(EXTENSION_CLASS_DESCRIPTOR)

        val declaring = superclassChain(INSTAGRAM_APPLICATION)
            .mapNotNull { type -> mutableClassDefByOrNull(type) }
            .firstNotNullOfOrNull { classDef ->
                classDef.methods.singleOrNull { method ->
                    method.name == "onCreate" && method.parameterTypes.isEmpty() &&
                        method.returnType == "V" && method.implementation != null
                }
            } ?: throw PatchException(
                "Neither $INSTAGRAM_APPLICATION nor a superclass in the APK declares onCreate()",
            )

        declaring.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->setContext(Landroid/content/Context;)V",
        )
    }

    finalize {
        MorpheUtilsPatchesVersionFingerprint.method.apply {
            returnEarly("2.0.14")
        }
        MorpheUtilsBuildIdentityFingerprint.method.apply {
            returnEarly("hg1:0000000000000000000000000000000000000000000000000000000000000000")
        }
    }
}
