package app.tada.patches.shared.misc.checks

import app.tada.patcher.Fingerprint

internal object PatchInfoFingerprint : Fingerprint(
    definingClass = "Lapp/tada/extension/shared/checks/PatchInfo;"
)

internal object PatchInfoBuildFingerprint : Fingerprint(
    definingClass = $$"Lapp/tada/extension/shared/checks/PatchInfo$Build;"
)
