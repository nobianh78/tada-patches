package app.tada.patches.music.misc.extension.hooks

import app.tada.patcher.Fingerprint
import app.tada.patcher.string
import app.tada.patches.all.misc.extension.ExtensionHook
import app.tada.patches.music.shared.MusicActivityOnCreateFingerprint

internal object YouTubeMusicApplicationInitFingerprint : Fingerprint(
    name = "onCreate",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        string("activity")
    )
)

internal val youTubeMusicApplicationInitHook = ExtensionHook(YouTubeMusicApplicationInitFingerprint)
internal val youTubeMusicApplicationInitOnCreateHook = ExtensionHook(MusicActivityOnCreateFingerprint)
