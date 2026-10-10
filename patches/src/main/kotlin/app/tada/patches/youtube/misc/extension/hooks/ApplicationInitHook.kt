package app.tada.patches.youtube.misc.extension.hooks

import app.tada.patcher.Fingerprint
import app.tada.patcher.string
import app.tada.patches.all.misc.extension.ExtensionHook
import app.tada.patches.youtube.shared.YouTubeActivityOnCreateFingerprint

internal object YouTubeApplicationInitFingerprint : Fingerprint(
    // Does _not_ resolve to the YouTube main activity.
    // Required as some hooked code runs before the main activity is launched.
    filters = listOf(
        string("Application.onCreate"),
        string("Application creation")
    )
)

internal val applicationInitHook = ExtensionHook(YouTubeApplicationInitFingerprint)
internal val applicationInitOnCreateHook = ExtensionHook(YouTubeActivityOnCreateFingerprint)
