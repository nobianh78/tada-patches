package app.tada.patches.youtube.misc.settings

import app.tada.patcher.Fingerprint
import app.tada.patcher.literal
import app.tada.patcher.methodCall
import app.tada.patcher.opcode
import app.tada.patcher.string
import app.tada.patcher.resource.ResourceType
import app.tada.patcher.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object SetThemeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf(),
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "app_theme_appearance_dark"),
    )
)

internal object CairoFragmentConfigFingerprint : Fingerprint(
    filters = listOf(
        literal(45532100L)
    )
)

/**
 * Synthetic Runnable from YT settings intent handling, fired after the PreferenceScreen builds.
 */
internal object SettingsPreferenceScreenSyntheticFingerprint : Fingerprint(
    name = "run",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "V",
    filters = listOf(
        string(":android:show_fragment_args"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = listOf(),
            returnType = "Landroidx/preference/PreferenceScreen;"
        ),
        opcode(Opcode.RETURN_VOID)
    )
)
