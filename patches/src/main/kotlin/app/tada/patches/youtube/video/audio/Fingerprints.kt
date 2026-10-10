/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches/pull/3412
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.patches.youtube.video.audio

import app.tada.patcher.Fingerprint
import app.tada.patcher.InstructionLocation.MatchAfterImmediately
import app.tada.patcher.methodCall
import app.tada.patcher.opcode
import app.tada.patcher.string
import com.android.tools.smali.dexlib2.Opcode

/**
 * Media3 DefaultAudioSink, where the audio session id of a newly created AudioTrack is read.
 */
internal object AudioTrackSessionIdFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        filters = listOf(
            string("ExoPlayer:AudioTrackReleaseThread")
        )
    ),
    filters = listOf(
        methodCall(smali = "Landroid/media/AudioTrack;->getAudioSessionId()I"),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately())
    )
)
