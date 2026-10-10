package app.tada.extension.music.patches;

import app.tada.extension.music.settings.Settings;

@SuppressWarnings("unused")
public class ForceOriginalAudioPatch {

    /**
     * Injection point.
     */
    public static void setEnabled() {
        app.tada.extension.shared.patches.ForceOriginalAudioPatch.setEnabled(
                Settings.SPOOF_VIDEO_STREAMS_CLIENT_TYPE.get()
        );
    }
}