/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches/pull/2937
 *
 * See the included NOTICE file for GPLv3 Section 7 terms and conditions that apply to this code.
 */

package app.tada.patches.reddit.misc.appicon

import app.tada.patcher.patch.bytecodePatch
import app.tada.patches.reddit.misc.settings.settingsPatch
import app.tada.patches.reddit.shared.Constants.COMPATIBILITY_REDDIT
import app.tada.util.setExtensionIsPatchIncluded

private const val EXTENSION_CLASS =
    "Lapp/tada/extension/reddit/patches/AppIconPatch;"

@Suppress("unused")
val appIconPatch = bytecodePatch(
    name = "App icon",
    description = "Adds an option to select from the Reddit app icons available in the manifest."
) {
    compatibleWith(COMPATIBILITY_REDDIT)

    dependsOn(settingsPatch)

    execute {
        setExtensionIsPatchIncluded(EXTENSION_CLASS)
    }
}
