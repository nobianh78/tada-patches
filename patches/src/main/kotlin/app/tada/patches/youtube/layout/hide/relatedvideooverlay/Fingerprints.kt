package app.tada.patches.youtube.layout.hide.relatedvideooverlay

import app.tada.patcher.Fingerprint
import app.tada.patcher.resource.ResourceType
import app.tada.patcher.resourceLiteral

private object RelatedEndScreenResultsParentFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "app_related_endscreen_results")
    )
)

internal object RelatedEndScreenResultsFingerprint : Fingerprint(
    classFingerprint = RelatedEndScreenResultsParentFingerprint,
    returnType = "V",
    parameters = listOf(
        "I",
        "Z",
        "I",
    )
)
