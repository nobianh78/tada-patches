package app.tada.patches.youtube.layout.hide.infocards

import app.tada.patcher.Fingerprint
import app.tada.patcher.InstructionLocation.MatchAfterImmediately
import app.tada.patcher.opcode
import app.tada.patcher.string
import app.tada.patcher.resource.ResourceType
import app.tada.patcher.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private object InfoCardsIncognitoParentFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    filters = listOf(
        string("player_overlay_info_card_teaser")
    )
)

internal object InfoCardsIncognitoFingerprint : Fingerprint(
    classFingerprint = InfoCardsIncognitoParentFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Boolean;",
    parameters = listOf("L", "J"),
    filters = listOf(
        string("vibrator")
    )
)

internal object InfoCardsMethodCallFingerprint : Fingerprint(
    filters = listOf(
        opcode(Opcode.INVOKE_VIRTUAL),
        opcode(Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        opcode(Opcode.INVOKE_INTERFACE, location = MatchAfterImmediately()),
        resourceLiteral(ResourceType.ID, "info_cards_drawer_header")
    ),
    strings = listOf("Missing ControlsOverlayPresenter for InfoCards to work.")
)
