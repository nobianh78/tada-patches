/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.extension.youtube.settings.preference;

import static app.tada.extension.shared.StringRef.str;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

import app.tada.extension.shared.spoof.SpoofVideoStreamsPatch;

@SuppressWarnings({"deprecation", "unused"})
public class LiveStreamDVRPreference extends SwitchPreference {

    {
        // Live stream DVR is not available in SABR playback.
        String summary = SpoofVideoStreamsPatch.spoofingToClientWithSABROrSpoofingDisabled()
                ? str("tada_live_stream_dvr_not_available")
                : str("tada_live_stream_dvr_summary");
        setSummary(summary);
    }

    public LiveStreamDVRPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
    public LiveStreamDVRPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public LiveStreamDVRPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public LiveStreamDVRPreference(Context context) {
        super(context);
    }
}
