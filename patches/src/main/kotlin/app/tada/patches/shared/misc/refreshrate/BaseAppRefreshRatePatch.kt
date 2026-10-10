/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches/pull/2695
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.patches.shared.misc.refreshrate

import app.tada.patcher.extensions.InstructionExtensions.addInstruction
import app.tada.patcher.patch.BytecodePatchBuilder
import app.tada.patcher.patch.BytecodePatchContext
import app.tada.patcher.patch.bytecodePatch
import app.tada.patches.shared.misc.settings.preference.BasePreferenceScreen
import app.tada.patches.shared.misc.settings.preference.ListPreference
import app.tada.patches.shared.misc.settings.preference.NonInteractivePreference
import app.tada.patches.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory
import app.tada.util.setExtensionIsPatchIncluded

private const val EXTENSION_CLASS = "Lapp/tada/extension/shared/patches/BaseAppRefreshRatePatch;"

fun baseAppRefreshRatePatch(
    preferenceScreen: BasePreferenceScreen.Screen,
    useRefreshRateType: Boolean,
    block: BytecodePatchBuilder.() -> Unit,
    executeBlock: BytecodePatchContext.() -> Unit = {},
) = bytecodePatch(
    name = "App refresh rate",
    description = "Adds an option to change the app refresh rate."
) {
    block()

    execute {
        val refreshPreference = NonInteractivePreference(
            key = "tada_app_refresh_rate",
            summaryKey = null,
            tag = "app.tada.extension.shared.settings.preference.AppRefreshRateListPreference",
            selectable = true
        )
        preferenceScreen.addPreferences(
            if (useRefreshRateType) {
                noTitleUnsortedPreferenceCategory(
                    refreshPreference,
                    ListPreference("tada_app_refresh_rate_type")
                )
            } else {
                refreshPreference
            }
        )

        ActivityOnCreateFingerprint.matchAll().forEach {
            it.method.addInstruction(
                0,
                "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->" +
                        "setActivityRefreshRate(Landroid/app/Activity;)V"
            )
        }

        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        executeBlock()
    }
}
