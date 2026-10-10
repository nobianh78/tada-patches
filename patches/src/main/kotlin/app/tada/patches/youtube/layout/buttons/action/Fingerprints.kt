/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to TADa contributions.
 */

package app.tada.patches.youtube.layout.buttons.action

import app.tada.patcher.Fingerprint
import app.tada.patcher.InstructionLocation.MatchAfterWithin
import app.tada.patcher.checkCast
import app.tada.patcher.resource.ResourceType
import app.tada.patcher.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Matches the method that processes the quick actions container view.
 * Used to inject a top margin adjustment into the quick actions bar.
 */
internal object QuickActionsElementSyntheticFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/view/View;"),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "quick_actions_element_container"),
        checkCast("Landroid/view/ViewGroup;", location = MatchAfterWithin(10))
    )
)
