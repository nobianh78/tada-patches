package app.tada.extension.youtube.settings.preference;

import static app.tada.extension.shared.StringRef.str;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

import app.tada.extension.shared.spoof.SpoofVideoStreamsPatch;

@SuppressWarnings({"deprecation", "unused"})
public class HideAudioFlyoutMenuPreference extends SwitchPreference {

    {
        // Audio menu is not available if spoofing to most client types.
        if (SpoofVideoStreamsPatch.spoofingToClientWithNoMultiAudioStreams()) {
            String summary = str("tada_hide_player_flyout_audio_track_not_available");
            setSummary(summary);
        }
    }

    public HideAudioFlyoutMenuPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
    public HideAudioFlyoutMenuPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public HideAudioFlyoutMenuPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public HideAudioFlyoutMenuPreference(Context context) {
        super(context);
    }
}
