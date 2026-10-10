package app.tada.patches.youtube.layout.player.overlay

import app.tada.patcher.Fingerprint
import app.tada.patcher.InstructionLocation.MatchAfterWithin
import app.tada.patcher.checkCast
import app.tada.patcher.resource.ResourceType
import app.tada.patcher.resourceLiteral
import app.tada.patches.youtube.layout.sponsorblock.ControlsOverlayFingerprint
import app.tada.patches.youtube.misc.playercontrols.PlayerBottomGradientScrimFingerprint

/**
 * Matches same method as [ControlsOverlayFingerprint] and [PlayerBottomGradientScrimFingerprint].
 */
internal object CreatePlayerOverviewFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        resourceLiteral(ResourceType.ID, "scrim_overlay"),
        checkCast("Landroid/widget/ImageView;", location = MatchAfterWithin(10))
    )
)
