package app.tada.patches.instagram.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal object Constants {
    val COMPATIBILITY_INSTAGRAM = Compatibility(
        name = "Instagram",
        packageName = "com.instagram.android",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xE1306C,
        signatures = setOf(
            "5f3e50f435583c9ae626302a71f7340044087a7e2c60adacfc254205a993e305",
            "3a10c50c18ba937506c2875e9047be74f8fd8b86fae2ff9feaa50a81fcb4c014"
        ),
        targets = listOf(
            AppTarget("452.0.0.48.64"),
            AppTarget(null)
        )
    )
}
