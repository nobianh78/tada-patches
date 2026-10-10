package app.tada.extension.youtube.patches;

import app.tada.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class HideTimestampPatch {
    public static boolean hideTimestamp() {
        return Settings.HIDE_TIMESTAMP.get();
    }
}
