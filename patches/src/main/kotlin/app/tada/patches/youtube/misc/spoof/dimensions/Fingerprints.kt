package app.tada.patches.youtube.misc.spoof.dimensions

import app.tada.patcher.Fingerprint
import app.tada.patcher.string

internal object DeviceDimensionsModelToStringFingerprint : Fingerprint(
    returnType = "L",
    filters = listOf(
        string("minh."),
        string(";maxh.")
    )
)
