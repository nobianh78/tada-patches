/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches/pull/2100
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.tada.patches.youtube.misc.chapters

import app.tada.patcher.Fingerprint
import app.tada.patcher.InstructionLocation.MatchAfterImmediately
import app.tada.patcher.InstructionLocation.MatchAfterWithin
import app.tada.patcher.checkCast
import app.tada.patcher.fieldAccess
import app.tada.patcher.methodCall
import app.tada.patcher.opcode
import app.tada.patcher.resource.ResourceType
import app.tada.patcher.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object TimelineMarkerFingerprint : Fingerprint (
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf("TimelineMarker[title=",  ", startMillis=", ", endMillis=")
)

internal fun getTimelineMarkersArrayFingerprint(timelineMarkerClassName: String) = object : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "[$timelineMarkerClassName",
    parameters = listOf("L"),
    filters = listOf(
        checkCast("[$timelineMarkerClassName")
    )
) {}

internal object HeatMapPeakPointFingerprint : Fingerprint (
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = "Lj$/util/Optional;->isPresent()Z"
        ),
        opcode(
            opcode = Opcode.MOVE_RESULT,
            location = MatchAfterImmediately()
        ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Lj$/util/Optional;",
            location = MatchAfterWithin(2)
        ),
        resourceLiteral(
            ResourceType.DIMEN,
            "ic_marker_decoration_size",
            location = MatchAfterWithin(37)
        )
    )
)
